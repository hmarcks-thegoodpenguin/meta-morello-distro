
DATA_FILE=%DB_DATADIR%
BIN_DIR=%BINDIR%

db_zabbix_template="${DB_ZABBIX_NAME}_template"
db_zabbix_proxy_name="${DB_ZABBIX_NAME}_proxy"

env

append_to_hba_conf() {
    echo "local   ${DB_ZABBIX_USER_SERVER}  ${DB_ZABBIX_NAME}    password" >> "${DATA_FILE}"
    echo "local   ${DB_ZABBIX_USER_SERVER}  ${db_zabbix_proxy_name}    password" >> "${DATA_FILE}"
}

set_reader_writer()
{
    user="${1}"
    sudo -u postgres ${BIN_DIR}/psql -c "GRANT writer TO ${user}" 2> /dev/null
    sudo -u postgres ${BIN_DIR}/psql -c "GRANT reader TO ${user}" 2> /dev/null
}

create_roles() {
    sudo -u postgres ${BIN_DIR}/psql -c "CREATE ROLE writer" 2> /dev/null
    sudo -u postgres ${BIN_DIR}/psql -c "CREATE ROLE reader" 2> /dev/null
    sudo -u postgres ${BIN_DIR}/psql -c "CREATE USER ${DB_ZABBIX_USER_SERVER} WITH PASSWORD '${DB_ZABBIX_PASSWORD}'" 2> /dev/null

    if [ $? -ne 0 ]; then
        echo "[INFO] postgres: failed to add users"
		exit 1
    fi

    set_reader_writer "${DB_ZABBIX_USER_SERVER}"
    set_reader_writer "${DB_ZABBIX_USER_SERVER}"
}

create_db_from_template()
{
    user="${1}"
    db="${2}"
    template="${3}"
    template_owner="${4}"
    sudo -u postgres ${BIN_DIR}/psql -c "CREATE DATABASE ${db} WITH TEMPLATE ${template} OWNER ${user};" 2> /dev/null
    sudo -u postgres ${BIN_DIR}/psql -c "REASSIGN OWNED BY ${template_owner} TO ${user}" ${db} 2> /dev/null
    sudo -u postgres ${BIN_DIR}/psql -c "GRANT ALL ON DATABASE ${db} TO ${user};" 2> /dev/null
    if [ $? -ne 0 ]; then
        echo "[INFO] postgres: failed to create db ${db}"
		exit 1
    fi
}

zabbix_schema_setup()
{
    zabbix_user="${1}"
    zabbix_db="${2}"

    cat ${ZABBIX_SCHEMA_LOCATION}schema.sql | sudo -u postgres ${BIN_DIR}/psql -d ${zabbix_db} ${zabbix_user} 2> /dev/null
    if [ $? -ne 0 ]; then
        echo "[INFO] postgres: failed to add schema to ${zabbix_db}"
        exit 1
    fi
}

zabbix_db_setup()
{
    zabbix_user="${1}"
    zabbix_db="${2}"

    tot=0
    cat ${ZABBIX_SCHEMA_LOCATION}images.sql | sudo -u postgres ${BIN_DIR}/psql -d ${zabbix_db} ${zabbix_user} 2>&1 > /dev/null
    tot=$(($tot+$?))
    cat ${ZABBIX_SCHEMA_LOCATION}data.sql   | sudo -u postgres ${BIN_DIR}/psql -d ${zabbix_db} ${zabbix_user} 2>&1 > /dev/null
    tot=$(($tot+$?))
    echo $tot
}

enforce_security_revoke()
{
    db="${1}"
    sudo -u postgres ${BIN_DIR}/psql -c "REVOKE ALL ON DATABASE ${db} FROM public" 2> /dev/null
}

zabbix_create_proxy_db()
{
    zabbix_create_db_from_template "${DB_ZABBIX_USER_SERVER}" "${db_zabbix_proxy_name}" "${db_zabbix_template}"
}

zabbix_create_db_from_template()
{
    user="${1}"
    db="${2}"
    template="${3}"
    create_db_from_template "${user}" "${db}" "${template}" "${DB_ZABBIX_USER_SERVER}"
}

zabbix_revoke_from_proxy()
{
    sudo -u postgres ${BIN_DIR}/psql -c "REVOKE ALL ON DATABASE ${db_zabbix_proxy_name} FROM public" 2> /dev/null
}

set_zabbix_proxy(){
    set_zabbix_role_perms "${db_zabbix_proxy_name}" "${DB_ZABBIX_USER_SERVER}"
}

set_dbs() {
# Create zabbix db
    sudo -u postgres ${BIN_DIR}/psql -c "CREATE DATABASE ${db_zabbix_template} WITH ENCODING 'Unicode' TEMPLATE template0" 2> /dev/null
    if [ $? -ne 0 ]; then
        echo "[INFO] postgres: failed to create db ${db_zabbix_template}"
		exit 1
    fi
    # Apply scehma
    zabbix_schema_setup "${DB_ZABBIX_USER_SERVER}" "${db_zabbix_template}"

    # Make it a template to save some time
    sudo -u postgres ${BIN_DIR}/psql -c "ALTER DATABASE ${db_zabbix_template} WITH IS_TEMPLATE true;" 2> /dev/null
    if [ $? -ne 0 ]; then
        echo "[INFO] postgres: failed to set template on db ${db_zabbix_template}"
		exit 1
    fi
    # Create a db from that template
    zabbix_create_db_from_template "${DB_ZABBIX_USER_SERVER}" "${DB_ZABBIX_NAME}" "${db_zabbix_template}"

    zabbix_db_setup "${DB_ZABBIX_USER_SERVER}" "${DB_ZABBIX_NAME}"

    zabbix_create_proxy_db "${db_zabbix_proxy_name}"

    enforce_security_revoke "${DB_ZABBIX_NAME}"
}

create_roles
set_dbs

sudo -u postgres ${BIN_DIR}/psql -c "GRANT USAGE ON SCHEMA public TO ${DB_ZABBIX_USER_SERVER}" 2> /dev/null
sudo -u postgres ${BIN_DIR}/psql -d "${DB_ZABBIX_NAME}" -c "GRANT ALL ON DATABASE ${DB_ZABBIX_NAME} TO ${DB_ZABBIX_USER_SERVER}" 2> /dev/null
sudo -u postgres ${BIN_DIR}/psql -d "${DB_ZABBIX_NAME}" -c "GRANT CONNECT ON DATABASE ${DB_ZABBIX_NAME} TO ${DB_ZABBIX_USER_SERVER}" 2> /dev/null
sudo -u postgres ${BIN_DIR}/psql -d "${DB_ZABBIX_NAME}" -c "GRANT ALL ON DATABASE ${DB_ZABBIX_NAME} TO ${DB_ZABBIX_USER_SERVER}" 2> /dev/null
sudo -u postgres ${BIN_DIR}/psql -c "GRANT USAGE ON SCHEMA public TO ${DB_ZABBIX_USER_SERVER}" 2> /dev/null

append_to_hba_conf

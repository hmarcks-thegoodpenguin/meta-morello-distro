inherit purecap-useradd purecap-sysroot

require zabbix-morello.inc

SRC_URI:append = " \
        file://zabbix-server.conf \
        file://zabbix-server.service \
        file://zabbix-server-init.service \
        file://zabbix-server-init.sh \
"

# Seperate user for agent for security reasons. If the user is shared the agent 
# will have full access to the server's DB.
USERADD_PACKAGES += " \
                    ${PN} \
"

EXTRA_OECONF += "--enable-server"

GROUPADD_PARAM:${PN} = " ${DB_ZABBIX_USER_SERVER}"
USERADD_PARAM:${PN} = "-r -g ${DB_ZABBIX_USER_SERVER}  -d ${localstatedir}/lib/${DB_ZABBIX_USER_SERVER} \
    -s /sbin/nologin -c 'Zabbix Monitoring System' ${DB_ZABBIX_USER_SERVER} \
"

RPROVIDES:${PN} += "zabbix-server"
RDEPENDS:${PN}  += " busybox bash "

BPN_ZABBIX = "zabbix-server"

SYSTEMD_AUTO_ENABLE:${PN} = "enable"
SYSTEMD_SERVICE:${PN}     = "zabbix-server.service zabbix-server-init.service"

ZBX_SCHEMA_DIR = "${sysconfdir}/zabbix/schema/"

do_install:append() {

    PSQL_HBA_CONF="/var/lib/postgresql/data/pg_hba.conf"
    install -d ${D}${sysconfdir}/zabbix
    install -d ${D}${systemd_system_unitdir} ${D}${sysconfdir}
    install -m 0755 ${WORKDIR}/zabbix-server-init.sh ${D}${sysconfdir}/zabbix
    install -m 0644 ${WORKDIR}/${BPN_ZABBIX}-init.service ${D}${systemd_system_unitdir}/${BPN_ZABBIX}-init.service
    sed -i -e "s|%ZABBIX_SCHEMA_LOCATION%|${ZBX_SCHEMA_DIR}|g" ${D}${systemd_system_unitdir}/${BPN_ZABBIX}-init.service

    sed -i -e "s|%DB_DATADIR%|${PSQL_HBA_CONF}|g" ${D}${sysconfdir}/zabbix/zabbix-server-init.sh
    sed -i -e "s|%BINDIR%|${bindir}|g" ${D}${sysconfdir}/zabbix/zabbix-server-init.sh
    sed -i -e 's#%SBINDIR%#${sbindir}#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}-init.service
    sed -i -e 's#%SYSCONFDIR%#${sysconfdir}#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}-init.service
    sed -i -e 's#%DB_ZABBIX_USER_SERVER%#${DB_ZABBIX_USER_SERVER}#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}-init.service
    sed -i -e 's#%DB_ZABBIX_NAME%#zabbix#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}-init.service
    sed -i -e 's#%DB_ZABBIX_PASSWORD%#${DB_ZABBIX_PASSWORD}#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}-init.service
}

do_install:append() {

    install -d ${D}${systemd_system_unitdir} ${D}${sysconfdir}
    install -m 0644 ${WORKDIR}/${BPN_ZABBIX}.service ${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service

    sed -i -e 's#%SBINDIR%#${sbindir}#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service
    sed -i -e 's#%SYSCONFDIR%#${sysconfdir}#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service

    sed -i -e 's#%DB_ZABBIX_USER_SERVER%#${DB_ZABBIX_USER_SERVER}#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service
    sed -i -e 's#%DB_ZABBIX_PASSWORD%#${DB_ZABBIX_PASSWORD}#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service
}

do_install:append() {
    install -m 0644 ${WORKDIR}/${BPN_ZABBIX}.conf ${D}${sysconfdir}/zabbix/
    sed -i -e 's#%ZABBIX_SOCKET_DIR%#/tmp/#g' ${D}${sysconfdir}/zabbix/${BPN_ZABBIX}.conf
    sed -i -e 's#%ZABBIX_IP_ADDR%#${ZABBIX_IP_ADDR}#g' ${D}${sysconfdir}/zabbix/${BPN_ZABBIX}.conf
    sed -i -e 's#%DB_ZABBIX_USER_SERVER%#${DB_ZABBIX_USER_SERVER}#g' ${D}${sysconfdir}/zabbix/${BPN_ZABBIX}.conf
    sed -i -e 's#%DB_ZABBIX_NAME%#zabbix#g' ${D}${sysconfdir}/zabbix/${BPN_ZABBIX}.conf
    sed -i -e 's#%DB_ZABBIX_PASSWORD%#${DB_ZABBIX_PASSWORD}#g' ${D}${sysconfdir}/zabbix/${BPN_ZABBIX}.conf

}

do_install:append() {

    install -d ${D}${sysconfdir}/zabbix/schema
    install -d ${D}${ZBX_SCHEMA_DIR}
    install -m 0644 ${S}/database/postgresql/schema.sql  ${D}${ZBX_SCHEMA_DIR}
    install -m 0644 ${S}/database/postgresql/images.sql  ${D}${ZBX_SCHEMA_DIR}
    install -m 0644 ${S}/database/postgresql/data.sql    ${D}${ZBX_SCHEMA_DIR}
}

do_install:append() {
    ${OBJDUMP} -D ${D}${sbindir}/zabbix_server >  ${D}${PURECAP_DEBUGDIR}/zabbix_server.dump
    ${READELF} -a ${D}${sbindir}/zabbix_server >  ${D}${PURECAP_DEBUGDIR}/zabbix_server.readelf
}

FILES:${PN} = " ${libdir} \
                 ${sysconfdir} \
                 ${bindir} \
                 ${sbindir} \
                 ${datadir} \
                 ${systemd_system_unitdir}/${BPN_ZABBIX}.service \
                 ${systemd_system_unitdir}/${BPN_ZABBIX}-init.service \
                 /morello-debug/ \
                 "
FILES:${PN}-dbg = "${datadir}"
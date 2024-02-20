inherit perlnative autotools-brokensep pure-cap-kheaders pkgconfig systemd useradd
inherit purecap-sysroot purecap-useradd

require zabbix-morello.inc

SRC_URI:append = " \
        file://zabbix-proxy.conf \
        file://zabbix-proxy.service \
"

# Seperate user for agent for security reasons. If the user is shared the agent 
# will have full access to the server's DB.
USERADD_PACKAGES += " \
                    ${PN} \
"

EXTRA_OECONF += "--enable-proxy"

USERADD_PARAM:${PN} = "-r -g ${DB_ZABBIX_USER_SERVER} -d ${localstatedir}/lib/${DB_ZABBIX_USER_SERVER} \
    -s /sbin/nologin -c 'Zabbix Monitoring System' ${DB_ZABBIX_USER_SERVER} \
"
GROUPADD_PARAM:${PN} = "-r ${DB_ZABBIX_USER_SERVER}"

RPROVIDES:${PN} += "zabbix-proxy"

BPN_ZABBIX = "zabbix-proxy"

SYSTEMD_AUTO_ENABLE:${PN} = "enable"
SYSTEMD_SERVICE:${PN}     = "zabbix-proxy.service"

do_install:append() {

    install -d ${D}${systemd_system_unitdir}
    install -d ${D}${sbindir}
    install -d ${D}${sysconfdir}

    SERVICE_FILE="${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service"
    install -m 0644 ${WORKDIR}/${BPN_ZABBIX}.service                      ${SERVICE_FILE}
    sed -i -e 's#%SBINDIR%#${sbindir}#g'                                  ${SERVICE_FILE}
    sed -i -e 's#%SYSCONFDIR%#${sbindir}#g'                               ${SERVICE_FILE}
    sed -i -e 's#%ZABBIX_PROXY_CONF%#${sysconfdir}/zabbix-proxy.conf#g'   ${SERVICE_FILE}

    # N.B. For release use Access Tokens or similiar
    sed -i -e 's#%DB_ZABBIX_USER_SERVER%#Admin#g'                         ${SERVICE_FILE}
    sed -i -e 's#%DB_ZABBIX_PASSWORD%#${DB_ZABBIX_PASSWORD}#g'            ${SERVICE_FILE}

    ZABBIX_CONF_DIR="${D}${sysconfdir}/zabbix/"
    install -d ${ZABBIX_CONF_DIR}
    install -m 0644 ${WORKDIR}/${BPN_ZABBIX}.conf ${ZABBIX_CONF_DIR}

    sed -i -e 's#%DB_ZABBIX_NAME%#${DB_ZABBIX_NAME}#g'                  ${ZABBIX_CONF_DIR}/${BPN_ZABBIX}.conf
    sed -i -e 's#%DB_ZABBIX_USER_PROXY%#${DB_ZABBIX_USER_PROXY}#g'      ${ZABBIX_CONF_DIR}/${BPN_ZABBIX}.conf
    sed -i -e 's#%DB_ZABBIX_PASSWORD%#${DB_ZABBIX_PASSWORD}#g'          ${ZABBIX_CONF_DIR}/${BPN_ZABBIX}.conf
    sed -i -e 's#%ZABBIX_SERVER_IPS%#${ZABBIX_IP_ADDR}#g'               ${ZABBIX_CONF_DIR}/${BPN_ZABBIX}.conf
    sed -i -e 's#%STATS_ALLOWED_IPS%#${ZABBIX_IP_ADDR}#g'               ${ZABBIX_CONF_DIR}/${BPN_ZABBIX}.conf

    sed -i -e 's#%ZABBIX_SOCKET_DIR%#/tmp/#g'                           ${ZABBIX_CONF_DIR}/${BPN_ZABBIX}.conf

}


FILES:${PN} += "${libdir} \
                ${systemd_system_unitdir}/${BPN_ZABBIX}.service \
                 "
FILES:${PN}-dbg += "${datadir}"
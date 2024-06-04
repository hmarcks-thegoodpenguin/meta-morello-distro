inherit perlnative autotools-brokensep pure-cap-kheaders pkgconfig systemd
inherit purecap-sysroot

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

GROUPADD_PARAM:${PN} = " ${DB_ZABBIX_USER_PROXY}"
USERADD_PARAM:${PN} = "-r -g ${DB_ZABBIX_USER_PROXY}  -d ${localstatedir}/lib/${DB_ZABBIX_USER_PROXY} \
    -s /sbin/nologin -c 'Zabbix Monitoring System' ${DB_ZABBIX_USER_PROXY} \
"

RPROVIDES:${PN} += "zabbix-proxy"

BPN_ZABBIX = "zabbix-proxy"

SYSTEMD_AUTO_ENABLE:${PN} = "enable"
SYSTEMD_SERVICE:${PN}     = "${BPN_ZABBIX}.service"

SERVICE_FILE = "${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service"

do_install:append() {

    install -d ${D}${systemd_system_unitdir}
    install -d ${D}${sbindir}
    install -d ${D}${sysconfdir}

    install -m 0644 ${WORKDIR}/${BPN_ZABBIX}.service                      ${SERVICE_FILE}
    sed -i -e 's#%SBINDIR%#${sbindir}#g'                                  ${SERVICE_FILE}
    sed -i -e 's#%SYSCONFDIR%#${sbindir}#g'                               ${SERVICE_FILE}
    sed -i -e 's#%ZABBIX_PROXY_CONF%#${sysconfdir}/${BPN_ZABBIX}.conf#g'   ${SERVICE_FILE}

    install -d ${D}${sysconfdir}/zabbix/
    install -m 0644 ${WORKDIR}/${BPN_ZABBIX}.conf ${D}${sysconfdir}/zabbix/
    sed -i -e 's#%ZABBIX_IP_ADDR%#${ZABBIX_IP_ADDR}#g' ${D}${sysconfdir}/${BPN_ZABBIX}.conf
}

do_install:append() {
	${OBJDUMP} -D ${D}${sbindir}/zabbix_proxy >  ${D}${sbindir}/zabbix_proxy.dump
	${READELF} -a ${D}${sbindir}/zabbix_proxy >  ${D}${sbindir}/zabbix_proxy.readelf
}

FILES:${PN} += "${libdir} \
                ${systemd_system_unitdir}/${BPN_ZABBIX}.service \
                 "
FILES:${PN}-dbg += "${datadir}"

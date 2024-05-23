inherit perlnative autotools-brokensep pure-cap-kheaders pkgconfig systemd useradd
inherit purecap-sysroot purecap-useradd

require zabbix-morello.inc

SRC_URI += " \
            file://zabbix-agentd-morello.service \
            file://zabbix-agentd.conf \
            "

# Seperate user for agent for security reasons. If the user is shared the agent
# will have full access to the server's DB.
USERADD_PACKAGES += " \
                    ${PN} \
"

EXTRA_OECONF += "--enable-agent"

GROUPADD_PARAM:${PN} = "-r ${DB_ZABBIX_USER_AGENT} "
USERADD_PARAM:${PN} = "-r -g ${DB_ZABBIX_USER_AGENT} -d /var/lib/${DB_ZABBIX_USER_AGENT} \
    -s /sbin/nologin -c \"Zabbix Monitoring System\" ${DB_ZABBIX_USER_AGENT} \
"
RPROVIDES:${PN} += "zabbix-agentd"

BPN_ZABBIX = "zabbix-agentd"

SYSTEMD_AUTO_ENABLE:${PN} = "enable"
SYSTEMD_SERVICE:${PN}     = "zabbix-agentd.service"

SERVER_HOSTNAME = "${MACHINE}"

do_install:append() {

    install -d ${D}${systemd_system_unitdir} ${D}${sysconfdir}
    install -m 0644 ${WORKDIR}/${BPN}.service ${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service

    sed -i -e 's#%SBINDIR%#${sbindir}#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service
    sed -i -e 's#%SYSCONFDIR%#${sysconfdir}#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service

    install -d ${D}${sysconfdir}/zabbix/${BPN_ZABBIX}.conf.d/
    install -m 0644 ${WORKDIR}/${BPN_ZABBIX}.conf ${D}${sysconfdir}/zabbix/

    sed -i -e 's#%DB_ZABBIX_USER_AGENT%#${DB_ZABBIX_USER_AGENT}#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service
    sed -i -e 's#%DB_ZABBIX_USER_AGENT%#${DB_ZABBIX_USER_AGENT}#g' ${D}${sysconfdir}/zabbix/${BPN_ZABBIX}.conf
    sed -i -e 's#%ZABBIX_USER_NAME%#${DB_ZABBIX_USER_AGENT}#g' ${D}${sysconfdir}/zabbix/${BPN_ZABBIX}.conf

}

do_install:append() {
	${OBJDUMP} -D ${D}${sbindir}/zabbix_agentd >  ${D}${PURECAP_DEBUGDIR}/zabbix_agentd.dump
	${READELF} -a ${D}${sbindir}/zabbix_agentd >  ${D}${PURECAP_DEBUGDIR}/zabbix_agentd.readelf
}

FILES:${PN} += " ${libdir} \
                 ${systemd_system_unitdir}/${BPN_ZABBIX}.service \
                 "
FILES:${PN}-dbg += "${datadir}"
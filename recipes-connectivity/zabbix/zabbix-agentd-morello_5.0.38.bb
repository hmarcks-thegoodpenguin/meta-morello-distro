inherit perlnative autotools-brokensep pure-cap-kheaders pkgconfig systemd useradd
inherit purecap-sysroot purecap-useradd

require zabbix-morello.inc

SRC_URI += " \
            file://zabbix-agentd.service \
            file://zabbix-agentd.conf \
            "

DEPENDS = " curl-morello "
RDEPENDS:${PN} = " curl-morello "

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
SYSTEMD_SERVICE:${PN}     = "${BPN_ZABBIX}.service"

SERVER_HOSTNAME = "${MACHINE}"

do_install:append() {

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/${BPN_ZABBIX}.service ${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service

    sed -i -e 's#%SBINDIR%#${sbindir}#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service
    sed -i -e 's#%SYSCONFDIR%#${sysconfdir}#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service

    install -d ${D}${sysconfdir}/zabbix/
    install -m 0644 ${WORKDIR}/${BPN_ZABBIX}.conf ${D}${sysconfdir}/zabbix/
    sed -i -e 's#%ZABBIX_IP_ADDR%#${ZABBIX_IP_ADDR}#g' ${D}${sysconfdir}/${BPN_ZABBIX}.conf
}

do_install:append() {
	${OBJDUMP} -D ${D}${sbindir}/zabbix_agentd >  ${D}${PURECAP_DEBUGDIR}/zabbix_agentd.dump
	${READELF} -a ${D}${sbindir}/zabbix_agentd >  ${D}${PURECAP_DEBUGDIR}/zabbix_agentd.readelf
}

FILES:${PN} += " ${libdir} \
                 ${systemd_system_unitdir}/${BPN_ZABBIX}.service \
                 ${sysconfdir} \
                 "
FILES:${PN}-dbg += "${datadir}"
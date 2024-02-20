inherit perlnative autotools-brokensep pure-cap-kheaders pkgconfig systemd useradd
inherit purecap-sysroot purecap-useradd

require zabbix-morello.inc

SRC_URI:append = " \
        file://zabbix-server.conf \
        file://zabbix-server-morello.service \
"

# Seperate user for agent for security reasons. If the user is shared the agent 
# will have full access to the server's DB.
USERADD_PACKAGES += " \
                    ${PN} \
"

EXTRA_OECONF += "--enable-server"

USERADD_PARAM:${PN} = "-r -g ${DB_ZABBIX_USER_SERVER} -d ${localstatedir}/lib/${DB_ZABBIX_USER_SERVER} \
    -s /sbin/nologin -c 'Zabbix Monitoring System' ${DB_ZABBIX_USER_SERVER} \
"
GROUPADD_PARAM:${PN} = "-r ${DB_ZABBIX_USER_SERVER}"

RPROVIDES:${PN} += "zabbix-server"
RDEPENDS:${PN}  += " busybox bash "

BPN_ZABBIX = "zabbix-server"

SYSTEMD_AUTO_ENABLE:${PN} = "enable"
SYSTEMD_SERVICE:${PN}     = "zabbix-server.service"


do_install:append() {

    install -d ${D}${systemd_system_unitdir} ${D}${sysconfdir}
    install -m 0644 ${WORKDIR}/${BPN}.service ${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service

    sed -i -e 's#%SBINDIR%#${sbindir}#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service
    sed -i -e 's#%SYSCONFDIR%#${sysconfdir}#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service

    sed -i -e 's#%DB_ZABBIX_USER_SERVER%#Admin#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service
    sed -i -e 's#%DB_ZABBIX_PASSWORD%#${DB_ZABBIX_PASSWORD}#g' ${D}${systemd_system_unitdir}/${BPN_ZABBIX}.service

    install -d ${D}${sysconfdir}/zabbix/${BPN_ZABBIX}.conf.d/
    install -m 0644 ${WORKDIR}/${BPN_ZABBIX}.conf ${D}${sysconfdir}/zabbix/

    install -d ${D}${sysconfdir}/zabbix/schema

    sed -i -e 's#%DB_ZABBIX_NAME%#${DB_ZABBIX_NAME}#g' ${D}${sysconfdir}/zabbix/${BPN_ZABBIX}.conf
    sed -i -e 's#%DB_ZABBIX_USER_SERVER%#${DB_ZABBIX_USER_SERVER}#g' ${D}${sysconfdir}/zabbix/${BPN_ZABBIX}.conf
    sed -i -e 's#%DB_ZABBIX_PASSWORD%#${DB_ZABBIX_PASSWORD}#g' ${D}${sysconfdir}/zabbix/${BPN_ZABBIX}.conf

    sed -i -e 's#%ZABBIX_SOCKET_DIR%#/tmp/#g' ${D}${sysconfdir}/zabbix/${BPN_ZABBIX}.conf

    ZABBIX_SCHEMA_LOC=${D}${sysconfdir}/zabbix/schema

    install -d ${ZABBIX_SCHEMA_LOC}
    install -m 0644 ${S}/database/postgresql/schema.sql  ${ZABBIX_SCHEMA_LOC}
    install -m 0644 ${S}/database/postgresql/images.sql  ${ZABBIX_SCHEMA_LOC}
    install -m 0644 ${S}/database/postgresql/data.sql    ${ZABBIX_SCHEMA_LOC}
}

do_install:append() {
    ${OBJDUMP} -D ${D}${sbindir}/zabbix_server >  ${D}${PURECAP_DEBUGDIR}/zabbix_server.dump
    ${READELF} -a ${D}${sbindir}/zabbix_server >  ${D}${PURECAP_DEBUGDIR}/zabbix_server.readelf
}

FILES:${PN} += " ${libdir} \
                 ${systemd_system_unitdir}/${BPN_ZABBIX}.service \
                 "
FILES:${PN}-dbg += "${datadir}"
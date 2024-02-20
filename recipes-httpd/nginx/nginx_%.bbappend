
FILESEXTRAPATHS:append := "${THISDIR}/files:"

SRC_URI:append = " \
                    file://http_status.conf \
                    file://zabbix-web.conf \
                    file://fastcgi-php.conf \
                "

EXTRA_OECONF+= "\
                --with-http_stub_status_module \
                "

do_install:append() {

    install -d ${D}${sysconfdir}/nginx/conf.d/
    cp ${WORKDIR}/zabbix-web.conf ${D}${sysconfdir}/nginx/conf.d/zabbix.conf

    install -d ${D}${sysconfdir}/nginx/snippets/
    install -m 0644 ${WORKDIR}/fastcgi-php.conf  ${D}${sysconfdir}/nginx/snippets/

    install -d ${D}${sysconfdir}/${BPN}/conf.d/
    install -m 0644 ${WORKDIR}/http_status.conf ${D}${sysconfdir}/${BPN}/conf.d/

    install -d ${D}${localstatedir}/lib/php/sessions
    chown www-data:www-data  -R ${D}${localstatedir}/lib/php/sessions
}

CONFFILES:${PN} = " http_status.conf "

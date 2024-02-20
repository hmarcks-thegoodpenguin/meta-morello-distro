require zabbix-morello.inc

SRC_URI:append = " \
        file://zabbix.conf.php \
"

SYSTEMD_SERVICE:${PN} = ""

DEPENDS = ""
RDEPENDS:${PN} += "bash"

do_compile[noexec] = "1"
do_configure[noexec] = "1"

do_install() {

    ZABBIX_WWW_LOC=${D}${datadir}/zabbix
    install -d ${ZABBIX_WWW_LOC}
    cp -r ${S}/ui/* ${ZABBIX_WWW_LOC}/

    install -m 0644 ${WORKDIR}/zabbix.conf.php ${ZABBIX_WWW_LOC}/conf/
}

FILES:${PN} += "${datadir}/zabbix"
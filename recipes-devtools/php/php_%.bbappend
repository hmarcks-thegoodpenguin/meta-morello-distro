
FILESEXTRAPATHS:append := "${THISDIR}/files:"

SRC_URI:append = " \
                file://php.ini \
                file://zabbix-fpm.conf \
                file://pg_config \
                "

DEPENDS += " openldap jpeg libpng freetype libpq"

PACKAGECONFIG = " opcache pgsql \
                ${@bb.utils.filter('DISTRO_FEATURES', 'ipv6 pam', d)}\
                 \
                "

PACKAGECONFIG[pgsql] = "--with-pgsql=${WORKDIR},--without-pgsql,libpq"

EXTRA_OECONF += " \
                --enable-bcmath \
                --enable-gd \
                --without-openssl \
                --with-ldap=${STAGING_EXECPREFIXDIR} \
                --with-config-file-path=/etc/php.ini \
                --with-freetype \
                --with-jpeg \
                "

SYSTEMD_PACKAGES = " php-fpm "
SYSTEMD_SERVICE:php-fpm += "\
    php-fpm.service \
"
SYSTEMD_AUTO_ENABLE:php-fpm = "enable"

PGLIBDIR     = "${STAGING_DIR_TARGET}${libdir}"
PGINCDIR     = "${STAGING_DIR_TARGET}${includedir}"

do_configure:prepend() {

    # config scripts
    export LIBDIR_IN="${PGLIBDIR}"
    export INCLUDEDIR_IN="${PGINCDIR}"
    export VERSION_IN="14.5"
}

do_install:append:class-target() {

    install -d ${D}/${sysconfdir}/tmpfiles.d/

    # add tmpfile
    echo 'd /run/php/ 0755 www-data www-data -' > ${D}/${sysconfdir}/tmpfiles.d/php.conf
    echo >> ${D}/${sysconfdir}/tmpfiles.d/php.conf

    # add zabbix conf
    install -d ${D}/${sysconfdir}/php-fpm.d/
    install -m 0644 ${WORKDIR}/zabbix-fpm.conf ${D}/${sysconfdir}/php-fpm.d/

    sed -i -e 's#;include=.*.*$#include=/etc/php-fpm.d/*.conf#g' ${D}${sysconfdir}/php-fpm.conf
    sed -i -e 's#;pm.status_path.*$#pm.status_path = /status#g' ${D}${sysconfdir}/php-fpm.conf
    sed -i -e 's#;ping.path.*$#ping.path = /ping#g' ${D}${sysconfdir}/php-fpm.conf

    install -m 0644 ${WORKDIR}/php.ini ${D}/etc/php.ini

    install -d ${D}${systemd_unitdir}/system/multi-user.target.wants
    ln -sf ../php-fpm.service ${D}${systemd_unitdir}/system/multi-user.target.wants/php-fpm.service
}

FILES:${PN}-fpm += "\
                        ${sysconfdir}/php.ini \
                        ${sysconfdir}/php-fpm.d/ \
                        ${sysconfdir}/tmpfiles.d/ \
                        ${systemd_unitdir}/ \
                        ${localstatedir}/lib/${BPN}/sessions \
                    "
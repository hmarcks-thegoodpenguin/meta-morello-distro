inherit perlnative autotools-brokensep pure-cap-kheaders pkgconfig systemd
inherit purecap-useradd

SUMMARY = "Open-source monitoring solution for your IT infrastructure"

MORELLO_SRC = "meta-openembedded/meta-oe/recipes-connectivity/zabbix/zabbix_5.4.12.bb"

DESCRIPTION = "\
ZABBIX is software that monitors numerous parameters of a network and the \
health and integrity of servers. ZABBIX uses a flexible notification \
mechanism that allows users to configure e-mail based alerts for virtually \
any event. This allows a fast reaction to server problems. ZABBIX offers \
excellent reporting and data visualisation features based on the stored \
data. This makes ZABBIX ideal for capacity planning. \
\
ZABBIX supports both polling and trapping. All ZABBIX reports and \
statistics, as well as configuration parameters are accessed through a \
web-based front end. A web-based front end ensures that the status of \
your network and the health of your servers can be assessed from any \
location. Properly configured, ZABBIX can play an important role in \
monitoring IT infrastructure. This is equally true for small \
organisations with a few servers and for large companies with a \
multitude of servers."
HOMEPAGE         = "http://www.zabbix.com/"
SECTION          = "Applications/Internet"
LICENSE          = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://COPYING;md5=300e938ad303147fede2294ed78fe02e"

FILESEXTRAPATHS:prepend := "${THISDIR}/cheri-patches:"
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
TOOLCHAIN                = "${MORELLO_TOOLCHAIN}"

PV   = "5.0.38"
PNPV = "zabbix-${PV}"
SRC_URI = " \
    https://cdn.zabbix.com/zabbix/sources/stable/5.0/${PNPV}.tar.gz \
    "

SRC_URI += "file://0000-sysinfo-fix-build-with-musl-libc.patch \
            file://0000-net-fix-provenance-error.patch \
            file://0001-memalloc-align-and-work-with-16-not-8.patch \
            file://0002-duktape-set-shift-to-5-for-CHERI.patch \
            file://0003-duktape-add-aling-to-16.patch \
            file://0004-embed-use-padding-of-16-not-8-for-alloc.patch \
            file://0005-embed-fix-alignment-issues.patch \
            file://0006-duk_config-use-debug-and-self-test.patch \
            file://0007-duktape-fix-stack-reallocation.patch \
            "

S = "${WORKDIR}/zabbix-${PV}"

SRC_URI[md5sum]    = "0d314f8626cf5e914204c7e0c4f9ca78"
SRC_URI[sha256sum] = "765c36aa0fc9dbf27f9eab06ccfe4e71023264d398d361b1dae0a3c1fe4c1a26"

DEPENDS += "postgresql-morello zlib-morello net-snmp-morello openldap-morello libpcre-morello libevent-morello"
DEPENDS += "curl-morello"

RDEPENDS:${PN} = " \
            logrotate \
            "


PACKAGECONFIG:append = " net-snmp libevent ldap zlib libpthread iconv curl libpcre openssl"

PACKAGECONFIG[openssl]    = "--with-openssl=${STAGING_EXECPREFIXDIR},--without-openssl,openssl-morello"
PACKAGECONFIG[net-snmp]   = "--with-net-snmp=${STAGING_BINDIR}/net-snmp-config,--without-net-snmp,net-snmp-morello"
PACKAGECONFIG[libevent]   = "--with-libevent=${STAGING_EXECPREFIXDIR},--without-libevent,libevent-morello"
PACKAGECONFIG[ldap]       = "--with-ldap=${STAGING_EXECPREFIXDIR},--without-ldap,openldap-morello"
PACKAGECONFIG[zlib]       = "--with-zlib=${STAGING_EXECPREFIXDIR},--without-zlib,zlib-morello"
PACKAGECONFIG[libpthread] = "--with-libpthread=${STAGING_EXECPREFIXDIR},--without-libpthread,"
PACKAGECONFIG[libpcre]    = "--with-libpcre=${STAGING_EXECPREFIXDIR},--without-libpcre,libpcre-morello"
PACKAGECONFIG[curl]       = "--with-libcurl=${STAGING_BINDIR}/crossscripts/curl-config, --without-libcurl, curl-morello"
PACKAGECONFIG[iconv]      = "--with-iconv=${STAGING_EXECPREFIXDIR},--without-iconv,virtual/libiconv"

EXTRA_OECONF = " \
    --disable-option-checking \
    --enable-dependency-tracking \
    --enable-ipv6 \
    --disable-static \
    --with-postgresql=${STAGING_BINDIR}/pgconfig/pg_config \
    --prefix=${prefix} \
    --libdir=${libdir} \
"

CFLAGS:append = " -I${S}/include"
LDFLAGS      += "-pthread -lnetsnmp"

# Lost interest in these errors for now, too many to handle
CC:remove:toolchain-llvm-morello:class-target = "-Werror=format"

KERNEL_VERSION = "6.5"

ZABBIX_IP_ADDR = "127.0.0.1"

do_configure:prepend() {
    export KERNEL_VERSION="${KERNEL_VERSION}"
}

do_install:append() {
    rm -f ${D}${bindir}/.debug
    rm -f ${D}${bindir}/.debug/zabbix_sender
    rm -f ${D}${bindir}/.debug/zabbix_get
    rm -f ${D}${sbindir}/.debug
    rm -f ${D}${sbindir}/.debug/zabbix_agentd
}

SYSROOT_DIRS:remove = "${datadir}"

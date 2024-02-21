inherit autotools-brokensep update-rc.d siteinfo systemd pkgconfig perlnative ptest purecap-sysroot

MORELLO_SRC = "meta-openembedded/meta-networking/recipes-protocols/net-snmp/net-snmp_5.9.3.bb"

SUMMARY = "Various tools relating to the Simple Network Management Protocol"
HOMEPAGE = "http://www.net-snmp.org/"
SECTION = "net"
LICENSE = "BSD-3-Clause & MIT"

LIC_FILES_CHKSUM = "file://COPYING;md5=9d100a395a38584f2ec18a8275261687"

TOOLCHAIN = "${MORELLO_TOOLCHAIN}"
FILESEXTRAPATHS:prepend := "${THISDIR}/cheri-patches:"

DEPENDS += "openssl-morello"
DEPENDS:append:class-target = " pciutils"

SRC_URI = "${SOURCEFORGE_MIRROR}/net-snmp/net-snmp-${PV}.tar.gz \
           file://init \
           file://snmpd.conf \
           file://snmptrapd.conf \
           file://snmpd.service \
           file://snmptrapd.service \
           file://net-snmp-add-knob-whether-nlist.h-are-checked.patch \
           file://fix-libtool-finish.patch \
           file://net-snmp-testing-add-the-output-format-for-ptest.patch \
           file://run-ptest \
           file://0001-config_os_headers-Error-Fix.patch \
           file://0001-snmplib-keytools.c-Don-t-check-for-return-from-EVP_M.patch \
           file://0001-get_pid_from_inode-Include-limit.h.patch \
           file://0004-configure-fix-incorrect-variable.patch \
           file://net-snmp-5.7.2-fix-engineBoots-value-on-SIGHUP.patch \
           file://net-snmp-fix-for-disable-des.patch \
           file://reproducibility-have-printcap.patch \
           file://0001-ac_add_search_path.m4-keep-consistent-between-32bit.patch \
           file://CVE-2022-44792-CVE-2022-44793.patch \
           "

SRC_URI += "\
            file://0001-tools-fix-cheri-provenance.patch \
            file://0002-udp_endpoint_linux-fix-cheri-provenance.patch \
            "

SRC_URI[sha256sum] = "2097f29b7e1bf3f1300b4bae52fa2308d0bb8d5d3998dbe02f9462a413a2ef0a"

S = "${WORKDIR}/net-snmp-${PV}"

BPPNNETSNMP = "net-snmp"

SNMP_INSTALL_DIR = "snmp"

UPSTREAM_CHECK_URI = "https://sourceforge.net/projects/net-snmp/files/net-snmp/"
UPSTREAM_CHECK_REGEX = "/net-snmp/(?P<pver>\d+(\.\d+)+)/"

EXTRA_OEMAKE = "OTHERLDFLAGS='${LDFLAGS}' HOST_CPPFLAGS='${BUILD_CPPFLAGS}'"

PARALLEL_MAKE = ""
CCACHE = ""
CLEANBROKEN = "1"

TARGET_CC_ARCH += "${LDFLAGS}"

PACKAGECONFIG ??= "${@bb.utils.filter('DISTRO_FEATURES', 'ipv6 systemd', d)} des smux"
PACKAGECONFIG[des] = "--enable-des, --disable-des"
# PACKAGECONFIG[elfutils] = "--with-elf, --without-elf, elfutils"
PACKAGECONFIG[ipv6] = "--enable-ipv6, --disable-ipv6"
# PACKAGECONFIG[libnl] = "--with-nl, --without-nl, libnl"
PACKAGECONFIG[smux] = ""
PACKAGECONFIG[systemd] = "--with-systemd, --without-systemd"

EXTRA_OECONF = " \
    --enable-shared \
    --disable-manuals \
    --with-defaults \
    --with-install-prefix=${prefix} \
    --with-persistent-directory=${localstatedir}/lib/net-snmp \
    --with-endianness=${@oe.utils.conditional('SITEINFO_ENDIANNESS', 'le', 'little', 'big', d)} \
    --with-mib-modules='${MIB_MODULES}' \
    --disable-embedded-perl  \
    --with-perl-modules=no \
"

MIB_MODULES = ""
MIB_MODULES:append = " ${@bb.utils.filter('PACKAGECONFIG', 'smux', d)}"

CACHED_CONFIGUREVARS = " \
    ac_cv_header_valgrind_valgrind_h=no \
    ac_cv_header_valgrind_memcheck_h=no \
    ac_cv_ETC_MNTTAB=/etc/mtab \
    lt_cv_shlibpath_overrides_runpath=yes \
    ac_cv_path_UNAMEPROG=${base_bindir}/uname \
    ac_cv_path_PSPROG=${base_bindir}/ps \
    ac_cv_file__etc_printcap=no \
    NETSNMP_CONFIGURE_OPTIONS= \
"

PERLPROG = "/usr/bin/env perl"

PERLPROG:class-native = "${bindir_native}/env perl"
PERLPROG:append = "${@bb.utils.contains('PACKAGECONFIG', 'perl', ' -I${WORKDIR}', '', d)}"
export PERLPROG

HAS_PERL = "0"

PTEST_BUILD_HOST_FILES += "net-snmp-config gen-variables"

do_configure:prepend() {
    sed -i -e "s|I/usr/include|I${STAGING_DIR_TARGET}${includedir}|g" \
        "${S}"/configure \
        "${S}"/configure.d/config_os_libs2
    if [ "${HAS_PERL}" = "1" ]; then
        # this may need to be changed when package perl has any change.
        cp -f ${STAGING_DIR_TARGET}/usr/lib*/perl?/*/Config.pm ${WORKDIR}/
        cp -f ${STAGING_DIR_TARGET}/usr/lib*/perl?/*/*/Config_heavy.pl ${WORKDIR}/
        sed -e "s@libpth => '/usr/lib.*@libpth => '${STAGING_DIR_TARGET}/${libdir} ${STAGING_DIR_TARGET}/${base_libdir}',@g" \
            -e "s@privlibexp => '/usr@privlibexp => '${STAGING_DIR_TARGET}/usr@g" \
            -e "s@scriptdir => '/usr@scriptdir => '${STAGING_DIR_TARGET}/usr@g" \
            -e "s@sitearchexp => '/usr@sitearchexp => '${STAGING_DIR_TARGET}/usr@g" \
            -e "s@sitelibexp => '/usr@sitearchexp => '${STAGING_DIR_TARGET}/usr@g" \
            -e "s@vendorarchexp => '/usr@vendorarchexp => '${STAGING_DIR_TARGET}/usr@g" \
            -e "s@vendorlibexp => '/usr@vendorlibexp => '${STAGING_DIR_TARGET}/usr@g" \
            -i ${WORKDIR}/Config.pm
    fi

}

do_configure:append() {
    sed -e "s@^NSC_INCLUDEDIR=.*@NSC_INCLUDEDIR=${STAGING_DIR_TARGET}\$\{includedir\}@g" \
        -e "s@^NSC_LIBDIR=-L.*@NSC_LIBDIR=-L${STAGING_DIR_TARGET}\$\{libdir\}@g" \
        -e "s@^NSC_LDFLAGS=\"-L.* @NSC_LDFLAGS=\"-L${STAGING_DIR_TARGET}\$\{libdir\} @g" \
        -i ${B}/net-snmp-config
}

do_install:append() {
    install -d ${D}${sysconfdir}/${SNMP_INSTALL_DIR}
    install -d ${D}${sysconfdir}/init.d

    install -m 755 ${WORKDIR}/init ${D}${sysconfdir}/init.d/snmpd
    install -m 644 ${WORKDIR}/snmpd.conf ${D}${sysconfdir}/${SNMP_INSTALL_DIR}/
    install -m 644 ${WORKDIR}/snmptrapd.conf ${D}${sysconfdir}/${SNMP_INSTALL_DIR}/

    install -d ${D}${systemd_unitdir}/system

    install -m 0644 ${WORKDIR}/snmpd.service ${D}${systemd_unitdir}/system/snmpd.service
    install -m 0644 ${WORKDIR}/snmptrapd.service ${D}${systemd_unitdir}/system/snmptrapd.service

    sed -e "s@^NSC_SRCDIR=.*@NSC_SRCDIR=.@g" \
        -i ${D}${bindir}/net-snmp-create-v3-user
    sed -e 's@^NSC_SRCDIR=.*@NSC_SRCDIR=.@g' \
        -e 's@[^ ]*-ffile-prefix-map=[^ "]*@@g' \
        -e 's@[^ ]*-fdebug-prefix-map=[^ "]*@@g' \
        -e 's@[^ ]*-fmacro-prefix-map=[^ "]*@@g' \
        -e 's@[^ ]*--sysroot=[^ "]*@@g' \
        -e 's@[^ ]*--with-libtool-sysroot=[^ "]*@@g' \
        -e 's@[^ ]*--with-install-prefix=[^ "]*@@g' \
        -e 's@[^ ]*PKG_CONFIG_PATH=[^ "]*@@g' \
        -e 's@[^ ]*PKG_CONFIG_LIBDIR=[^ "]*@@g' \
        -i ${D}${bindir}/net-snmp-config

    sed -e 's@[^ ]*-ffile-prefix-map=[^ "]*@@g' \
        -e 's@[^ ]*-fdebug-prefix-map=[^ "]*@@g' \
        -e 's@[^ ]*-fmacro-prefix-map=[^ "]*@@g' \
        -i ${D}${libdir}/pkgconfig/netsnmp*.pc

  	sed -e "s:%PURECAP_DIR%:${base_prefix}:g" -i ${D}${sysconfdir}/init.d/snmpd
  	sed -e "s:%PURECAP_DIR%:${base_prefix}:g" -i ${D}${systemd_unitdir}/system/snmpd.service
  	sed -e "s:%PURECAP_DIR%:${base_prefix}:g" -i ${D}${systemd_unitdir}/system/snmptrapd.service

    # ${STAGING_DIR_HOST} is empty for native builds, and the sed command below
    # will result in errors if run for native.
    if [ "${STAGING_DIR_HOST}" ]; then
        sed -e 's@${STAGING_DIR_HOST}@@g' \
            -i ${D}${bindir}/net-snmp-config ${D}${libdir}/pkgconfig/netsnmp*.pc
    fi

    sed -e "s@^NSC_INCLUDEDIR=.*@NSC_INCLUDEDIR=${base_prefix}\$\{includedir\}@g" \
        -e "s@^NSC_LIBDIR=-L.*@NSC_LIBDIR=-L${base_prefix}\$\{libdir\}@g" \
        -e "s@^NSC_LDFLAGS=\"-L.* @NSC_LDFLAGS=\"-L${base_prefix}\$\{libdir\} @g" \
        -i ${D}${bindir}/net-snmp-config

    # oe_multilib_header net-snmp/net-snmp-config.h

    if [ "${HAS_PERL}" = "1" ]; then
        find ${D}${libdir}/ -type f -name "perllocal.pod" | xargs rm -f
    fi
}

do_install:append() {
  ${OBJDUMP} -D ${D}${libdir}/libnetsnmp.so >  ${D}${PURECAP_DEBUGDIR}/libnetsnmp.so.dump
  ${READELF} -a ${D}${libdir}/libnetsnmp.so >  ${D}${PURECAP_DEBUGDIR}/libnetsnmp.so.readelf
}

PTEST_PATH = "${libdir}/netsnmp/ptest"

do_install_ptest() {
    install -d ${D}${PTEST_PATH}
    for i in ${S}/dist ${S}/include ${B}/include ${S}/mibs ${S}/configure \
        ${B}/net-snmp-config ${S}/testing; do
        if [ -e "$i" ]; then
            cp -R --no-dereference --preserve=mode,links -v "$i" ${D}${PTEST_PATH}
        fi
    done
    echo `autoconf -V|awk '/autoconf/{print $NF}'` > ${D}${PTEST_PATH}/dist/autoconf-version

    rmdlist="${D}${PTEST_PATH}/dist/net-snmp-solaris-build"
    for i in $rmdlist; do
        if [ -d "$i" ]; then
            rm -rf "$i"
        fi
    done
}

SYSROOT_PREPROCESS_FUNCS += "net_snmp_sysroot_preprocess"
SNMP_DBGDIR = "${PURECAP_SYSROOT_DIR}/usr/src/debug/${PN}/${EXTENDPE}${PV}-${PR}"

net_snmp_sysroot_preprocess () {
    if [ -e ${D}${bindir}/net-snmp-config ]; then
        install -d ${SYSROOT_DESTDIR}${bindir_crossscripts}/
        install -m 755 ${D}${bindir}/net-snmp-config ${SYSROOT_DESTDIR}${bindir_crossscripts}/
        sed -e "s@-I/usr/include@-I${STAGING_INCDIR}@g" \
            -e "s@^prefix=.*@prefix=${STAGING_DIR_HOST}${prefix}@g" \
            -e "s@^exec_prefix=.*@exec_prefix=${STAGING_EXECPREFIXDIR}@g" \
            -e "s@^includedir=.*@includedir=${STAGING_INCDIR}@g" \
            -e "s@^libdir=.*@libdir=${STAGING_LIBDIR}@g" \
            -e "s@^NSC_SRCDIR=.*@NSC_SRCDIR=${S}@g" \
            -e "s@-ffile-prefix-map=${SNMP_DBGDIR}@-ffile-prefix-map=${WORKDIR}=${SNMP_DBGDIR}@g" \
            -e "s@-fdebug-prefix-map=${SNMP_DBGDIR}@-fdebug-prefix-map=${WORKDIR}=${SNMP_DBGDIR}@g" \
            -e "s@-fdebug-prefix-map= -fdebug-prefix-map=@-fdebug-prefix-map=${STAGING_DIR_NATIVE}= \
                  -fdebug-prefix-map=${STAGING_DIR_HOST}=@g" \
            -e "s@--sysroot=@--sysroot=${STAGING_DIR_HOST}@g" \
            -e "s@--with-libtool-sysroot=@--with-libtool-sysroot=${STAGING_DIR_HOST}@g" \
            -e "s@--with-install-prefix=@--with-install-prefix=${D}@g" \
          -i  ${SYSROOT_DESTDIR}${bindir_crossscripts}/net-snmp-config
    fi
}

PACKAGES += "${PN}-libs ${PN}-mibs ${PN}-server ${PN}-client \
             ${PN}-server-snmpd ${PN}-server-snmptrapd \
             ${PN}-lib-netsnmp ${PN}-lib-agent ${PN}-lib-helpers \
             ${PN}-lib-mibs ${PN}-lib-trapd"

# perl module
PACKAGES += "${@bb.utils.contains('PACKAGECONFIG', 'perl', '${PN}-perl-modules', '', d)}"

ALLOW_EMPTY:${PN} = "1"
ALLOW_EMPTY:${PN}-server = "1"
ALLOW_EMPTY:${PN}-libs = "1"

FILES:${PN}-perl-modules = "${libdir}/perl?/*"
RDEPENDS:${PN}-perl-modules = "perl"

FILES:${PN}-libs = ""
FILES:${PN}-mibs = "${datadir}/snmp/mibs"
FILES:${PN}-server-snmpd = "${sbindir}/snmpd \
                            ${sysconfdir}/${SNMP_INSTALL_DIR}/snmpd.conf \
                            ${sysconfdir}/init.d \
                            ${systemd_unitdir}/system/snmpd.service \
"

FILES:${PN}-server-snmptrapd = "${sbindir}/snmptrapd \
                                ${sysconfdir}/${SNMP_INSTALL_DIR}/snmptrapd.conf \
                                ${systemd_unitdir}/system/snmptrapd.service \
"

FILES:${PN}-lib-netsnmp = "${libdir}/libnetsnmp${SOLIBS}"
FILES:${PN}-lib-agent = "${libdir}/libnetsnmpagent${SOLIBS}"
FILES:${PN}-lib-helpers = "${libdir}/libnetsnmphelpers${SOLIBS}"
FILES:${PN}-lib-mibs = "${libdir}/libnetsnmpmibs${SOLIBS}"
FILES:${PN}-lib-trapd = "${libdir}/libnetsnmptrapd${SOLIBS}"

FILES:${PN} = "${includedir} ${libdir}"
FILES:${PN}-client = "${bindir}/* ${datadir}/snmp/"
FILES:${PN}-dbg += "${libdir}/.debug/ ${sbindir}/.debug/ ${bindir}/.debug/"
FILES:${PN}-dev += "${bindir}/mib2c \
                    ${bindir}/mib2c-update \
                    ${bindir}/net-snmp-config \
                    ${bindir}/net-snmp-create-v3-user \
"

CONFFILES:${PN}-server-snmpd = "${sysconfdir}/${SNMP_INSTALL_DIR}/snmpd.conf"
CONFFILES:${PN}-server-snmptrapd = "${sysconfdir}/${SNMP_INSTALL_DIR}/snmptrapd.conf"

INITSCRIPT_PACKAGES = "${PN}-server-snmpd"
INITSCRIPT_NAME:${PN}-server-snmpd = "snmpd"
INITSCRIPT_PARAMS:${PN}-server-snmpd = "start 90 2 3 4 5 . stop 60 0 1 6 ."

SYSTEMD_PACKAGES = "${PN}-server-snmpd \
                    ${PN}-server-snmptrapd"

SYSTEMD_SERVICE:${PN}-server-snmpd = "snmpd.service"
SYSTEMD_SERVICE:${PN}-server-snmptrapd =  "snmptrapd.service"

# RDEPENDS:${PN} += "${@bb.utils.contains('PACKAGECONFIG', 'perl', 'net-snmp-perl-modules', '', d)}"
# RDEPENDS:${PN} += "${PN}-client"
# RDEPENDS:${PN}-server-snmpd += "${PN}-mibs"
# RDEPENDS:${PN}-server-snmptrapd += "${PN}-server-snmpd ${PN}-lib-trapd"
# RDEPENDS:${PN}-server += "${PN}-server-snmpd ${PN}-server-snmptrapd"
# RDEPENDS:${PN}-client += "${PN}-mibs ${PN}-libs"
# RDEPENDS:${PN}-libs += "libpci \
#                         ${PN}-lib-netsnmp \
#                         ${PN}-lib-agent \
#                         ${PN}-lib-helpers \
#                         ${PN}-lib-mibs \
# "

RRECOMMENDS:${PN}-dbg = "${PN}-client (= ${EXTENDPKGV}) ${PN}-server (= ${EXTENDPKGV})"

RPROVIDES:${PN}-server-snmpd += "net-snmp-server-snmpd-systemd"
RREPLACES:${PN}-server-snmpd += "net-snmp-server-snmpd-systemd"
RCONFLICTS:${PN}-server-snmpd += "net-snmp-server-snmpd-systemd"

RPROVIDES:${PN}-server-snmptrapd += "net-snmp-server-snmptrapd-systemd"
RREPLACES:${PN}-server-snmptrapd += "net-snmp-server-snmptrapd-systemd"
RCONFLICTS:${PN}-server-snmptrapd += "net-snmp-server-snmptrapd-systemd"

LEAD_SONAME = "libnetsnmp.so"

MULTILIB_SCRIPTS = "${PN}-dev:${bindir}/net-snmp-config"

SYSTEMD_AUTO_ENABLE:${PN} = "enable"

SYSROOT_DIRS += "${bindir}"
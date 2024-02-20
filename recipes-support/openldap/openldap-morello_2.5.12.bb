inherit autotools-brokensep update-rc.d systemd pkgconfig pure-cap-kheaders purecap-sysroot

MORELLO_SRC = "meta-openembedded/meta-oe/recipes-support/openldap/openldap_2.5.12.bb"

SUMMARY = "OpenLDAP Directory Service"

DESCRIPTION = "OpenLDAP Software is an open source implementation of the Lightweight Directory Access Protocol."
HOMEPAGE = "http://www.OpenLDAP.org/license.html"
# The OpenLDAP Public License - see the HOMEPAGE - defines
# the license.  www.openldap.org claims this is Open Source
# (see http://www.openldap.org), the license appears to be
# basically BSD.  opensource.org does not record this license
# at present (so it is apparently not OSI certified).
LICENSE = "OpenLDAP"
LIC_FILES_CHKSUM = "file://COPYRIGHT;md5=beceb5ac7100b6430640c61655b25c1f \
                    file://LICENSE;md5=153d07ef052c4a37a8fac23bc6031972 \
                    "
SECTION = "libs"

BPN_LDAP = "openldap"

TOOLCHAIN = "${MORELLO_TOOLCHAIN}"
FILESEXTRAPATHS:prepend := "${THISDIR}/cheri-patches:"

LDAP_VER = "${@'.'.join(d.getVar('PV').split('.')[0:2])}"

SRC_URI = "http://www.openldap.org/software/download/OpenLDAP/openldap-release/${BPN_LDAP}-${PV}.tgz \
    file://use-urandom.patch \
    file://initscript \
    file://slapd.service \
    file://remove-user-host-pwd-from-version.patch \
    file://0001-ldif-filter-fix-parallel-build-failure.patch \
    file://0001-build-top.mk-unset-STRIP_OPTS.patch \
    file://0001-libraries-Makefile.in-ignore-the-mkdir-errors.patch \
    file://0001-librewrite-include-ldap_pvt_thread.h-before-redefini.patch \
"

SRC_URI += "\
    file://0001-config-fix-provenance-errors.patch \
    file://0002-tpool-remove-errors.patch \
    file://0003-config-Remove-format-error.patch \
    file://0004-main-Remove-format-error.patch \
    file://0005-connection-fix-provenance-error.patch \
    file://0006-sets-fix-provenance-error.patch \
    file://0007-slapd-search-fix-cheri-provenance.patch \
"

SRC_URI[sha256sum] = "d5086cbfc49597fa7d0670a429a9054552d441b16ee8b2435412797ab0e37b96"

S = "${WORKDIR}/${BPN_LDAP}-${PV}"

DEPENDS += "util-linux-morello groff-native libtool-native openssl-morello"
RDEPENDS:${PN} += "openssl-morello"

# CV SETTINGS
# Required to work round AC_FUNC_MEMCMP which gets the wrong answer
# when cross compiling (should be in site?)
EXTRA_OECONF += "ac_cv_func_memcmp_working=yes"

# CONFIG DEFINITIONS
# The following is necessary because it cannot be determined for a
# cross compile automagically.  Select should yield fine on all OE
# systems...
EXTRA_OECONF += "--with-yielding-select=yes"
# Shared libraries are nice...
EXTRA_OECONF += "-disable-modules -disable-static"

PACKAGECONFIG ??= "asyncmeta gnutls modules \
                   mdb ldap meta null passwd proxycache dnssrv \
                   ${@bb.utils.filter('DISTRO_FEATURES', 'ipv6', d)} \
"
#--with-tls              with TLS/SSL support auto|openssl|gnutls [auto]
PACKAGECONFIG[openssl] = "--with-tls=openssl,,openssl-morello"

PACKAGECONFIG[sasl] = "--with-cyrus-sasl,--without-cyrus-sasl,cyrus-sasl"
PACKAGECONFIG[ipv6] = "--enable-ipv6,--disable-ipv6"

# SLAPD options
#
# UNIX crypt(3) passwd support:
EXTRA_OECONF += "--enable-crypt"


# SLAPD BACKEND
#
# The backend must be set by the configuration.  This controls the
# required database.
#
# Backends="asyncmeta dnssrv ldap mdb meta ndb null passwd perl relay sock sql wt"
#
# Note that multiple backends can be built.  The ldbm backend requires a
# build-time choice of database API. To use the gdbm (or other) API the
# Berkely database module must be removed from the build.
md = "${libexecdir}/openldap"

# #--enable-asyncmeta    enable asyncmeta backend no|yes|mod no
PACKAGECONFIG[asyncmeta] = "--enable-asyncmeta=yes,--enable-asyncmeta=no"

# #--enable-dnssrv       enable dnssrv backend no|yes|mod no
PACKAGECONFIG[dnssrv] = "--enable-dnssrv=yes,--enable-dnssrv=no"

# #--enable-ldap         enable ldap backend no|yes|mod no
PACKAGECONFIG[ldap] = "--enable-ldap=yes,--enable-ldap=no,"

# #--enable-mdb          enable mdb database backend no|yes|mod [yes]
PACKAGECONFIG[mdb] = "--enable-mdb=yes,--enable-mdb=no,"

# #--enable-meta         enable metadirectory backend no|yes|mod no
PACKAGECONFIG[meta] = "--enable-meta=yes,--enable-meta=no,"

# #--enable-ndb          enable MySQL NDB Cluster backend no|yes|mod [no]
PACKAGECONFIG[ndb] = "--enable-ndb=yes,--enable-ndb=no,"

# #--enable-null         enable null backend no|yes|mod no
PACKAGECONFIG[null] = "--enable-null=yes,--enable-null=no,"

# #--enable-passwd       enable passwd backend no|yes|mod no
PACKAGECONFIG[passwd] = "--enable-passwd=yes,--enable-passwd=no,"

# #--enable-perl         enable perl backend no|yes|mod no
# #  This requires a loadable perl dynamic library, if enabled without
# #  doing something appropriate (building perl?) the build will pick
# #  up the build machine perl - not good (inherit perlnative?)
PACKAGECONFIG[perl] = "--enable-perl=yes,--enable-perl=no,perl"

# #--enable-relay        enable relay backend no|yes|mod [yes]
PACKAGECONFIG[relay] = "--enable-relay=yes,--enable-relay=no,"

# #--enable-sock         enable sock backend no|yes|mod [no]
PACKAGECONFIG[sock] = "--enable-sock=yes,--enable-sock=no,"

# #--enable-sql          enable sql backend no|yes|mod no
# # sql requires some sql backend which provides sql.h, sqlite* provides
# # sqlite.h (which may be compatible but hasn't been tried.)
PACKAGECONFIG[sql] = "--enable-sql=yes,--enable-sql=no,sqlite3"

# #--enable-wt           enable wt backend no|yes|mod no
# # back-wt is marked currently as experimental
PACKAGECONFIG[wt] = "--enable-wt=yes,--enable-wt=no"

# #--enable-dyngroup     Dynamic Group overlay no|yes|mod no
# #  This is a demo, Proxy Cache defines init_module which conflicts with the
# #  same symbol in dyngroup
PACKAGECONFIG[dyngroup] = "--enable-dyngroup=yes,--enable-dyngroup=no,"

# #--enable-proxycache   Proxy Cache overlay no|yes|mod no
PACKAGECONFIG[proxycache] = "--enable-proxycache=yes,--enable-proxycache=no,"
FILES:${PN}-overlay-proxycache = "${md}/pcache-*.so.*"
PACKAGES += "${PN}-overlay-proxycache"

# Append URANDOM_DEVICE='/dev/urandom' to CPPFLAGS:
# This allows tls to obtain random bits from /dev/urandom, by default
# it was disabled for cross-compiling.
CPPFLAGS:append = " -D_GNU_SOURCE -DURANDOM_DEVICE='/dev/urandom' -fPIC"

LDFLAGS:append = " -pthread"

do_configure() {

    export CPPFLAGS="${CPPFLAGS} ${CC_PURECAP_FLAGS}"

    rm -f ${S}/libtool
    aclocal
    libtoolize --force --copy
    gnu-configize
    cp ${STAGING_DATADIR_NATIVE}/libtool/build-aux/ltmain.sh ${S}/build
    cp ${STAGING_DATADIR_NATIVE}/libtool/build-aux/missing ${S}/build
    cp ${STAGING_DATADIR_NATIVE}/libtool/build-aux/compile ${S}/build
    autoconf
    oe_runconf
}

do_install:append() {
    install -d ${D}${sysconfdir}/init.d
    cat ${WORKDIR}/initscript > ${D}${sysconfdir}/init.d/openldap
    chmod 755 ${D}${sysconfdir}/init.d/openldap
    # This is duplicated in /etc/openldap and is for slapd
    rm -f ${D}{localstatedir}/openldap-data/DB_CONFIG.example

    # Installing slapd under ${sbin} is more FHS and LSB compliance
    mv ${D}${libexecdir}/slapd ${D}${sbindir}/slapd
    rmdir --ignore-fail-on-non-empty ${D}${libexecdir}
    SLAPTOOLS="slapadd slapcat slapdn slapindex slappasswd slaptest slapauth slapacl slapschema slapmodify"
    cd ${D}${sbindir}/
    rm -f ${SLAPTOOLS}
    for i in ${SLAPTOOLS}; do ln -sf slapd $i; done

    rmdir "${D}${localstatedir}/run"
    rmdir --ignore-fail-on-non-empty "${D}${localstatedir}"

    install -d ${D}${systemd_unitdir}/system/
    install -m 0644 ${WORKDIR}/slapd.service ${D}${systemd_unitdir}/system/slapd-morello.service
    sed -i -e 's,@SBINDIR@,${sbindir},g' ${D}${systemd_unitdir}/system/*.service

    # Uses mdm as the database
    #  and localstatedir as data directory ...
    sed -e 's/# modulepath/modulepath/' \
        -e 's/# moduleload\s*back_bdb.*/moduleload    back_mdb/' \
        -e 's/database\s*bdb/database        mdb/' \
        -e 's%^directory\s*.*%directory   ${localstatedir}/${BPN_LDAP}/data/%' \
        -i ${D}${sysconfdir}/openldap/slapd.conf

    mkdir -p ${D}${localstatedir}/${BPN_LDAP}/data
}

do_install:append() {
  ${OBJDUMP_COMMAND} ${D}${libdir}/libldap-2.5.so.0 >  ${D}${PURECAP_DEBUGDIR}/libldap-2.5.dump
  ${READELF_COMMAND} ${D}${libdir}/libldap-2.5.so.0 >  ${D}${PURECAP_DEBUGDIR}/libldap-2.5.readelf

  ${OBJDUMP_COMMAND} ${D}${libdir}/liblber-2.5.so.0 >  ${D}${PURECAP_DEBUGDIR}/liblber-2.5.dump
  ${READELF_COMMAND} ${D}${libdir}/liblber-2.5.so.0 >  ${D}${PURECAP_DEBUGDIR}/liblber-2.5.readelf
}

LEAD_SONAME = "libldap-${LDAP_VER}.so.*"

# The executables go in a separate package.  This allows the
# installation of the libraries with no daemon support.
# Each module also has its own package - see above.
PACKAGES += "${PN}-slapd ${PN}-slurpd ${PN}-bin"

# Package contents - shift most standard contents to -bin
FILES:${PN} = "${libdir}/lib*.so.* ${sysconfdir}/openldap/ldap.* \
               ${localstatedir}/${BPN_LDAP}/data ${libdir} \
              "
FILES:${PN}-slapd = "${sysconfdir}/init.d ${libexecdir}/slapd ${sbindir} ${localstatedir}/run ${localstatedir}/volatile/run \
    ${sysconfdir}/openldap/slapd.* ${sysconfdir}/openldap/schema \
    ${sysconfdir}/openldap/DB_CONFIG.example ${systemd_unitdir}/system/*"
FILES:${PN}-slurpd = "${libexecdir}/slurpd ${localstatedir}/openldap-slurp"
FILES:${PN}-bin = "${bindir}"
FILES:${PN}-dev = "${includedir} ${libdir}/lib*.so ${libdir}/*.la ${libexecdir}/openldap/*.a ${libexecdir}/openldap/*.la ${libexecdir}/openldap/*.so ${libdir}/pkgconfig/*.pc"
FILES:${PN}-dbg += "${libexecdir}/openldap/.debug ${datadir}"

FILES:${PN}-static-dev = "${libdir}/libldap.a ${libdir}/liblber.a"

INITSCRIPT_PACKAGES = "${PN}-slapd"
INITSCRIPT_NAME:${PN}-slapd = "openldap"
INITSCRIPT_PARAMS:${PN}-slapd = "defaults"

SYSTEMD_PACKAGES = "${PN}-slapd"
SYSTEMD_SERVICE:${PN}-slapd = "slapd-morello.service"
SYSTEMD_AUTO_ENABLE:${PN}-slapd ?= "disable"

PACKAGES_DYNAMIC += "^${PN}-backends.* ^${PN}-backend-.*"

# The modules require their .so to be dynamicaly loaded
INSANE_SKIP:${PN}-backend-asyncmeta  += "dev-so"
INSANE_SKIP:${PN}-backend-dnssrv     += "dev-so"
INSANE_SKIP:${PN}-backend-ldap       += "dev-so"
INSANE_SKIP:${PN}-backend-meta       += "dev-so"
INSANE_SKIP:${PN}-backend-mdb        += "dev-so"
INSANE_SKIP:${PN}-backend-null       += "dev-so"
INSANE_SKIP:${PN}-backend-passwd     += "dev-so"

# CVE-2015-3276 has no target code.
CVE_CHECK_IGNORE += "CVE-2015-3276"

inherit lib_package pkgconfig perlnative pure-cap-kheaders purecap-sysroot

MORELLO_SRC = "poky/meta/recipes-connectivity/openssl/openssl_3.0.8.bb"

SUMMARY     = "Secure Socket Layer"
DESCRIPTION = "Secure Socket Layer (SSL) binary and related cryptographic tools, with rebased patches from CHERI BSD repo."
HOMEPAGE    = "http://www.openssl.org/"
BUGTRACKER  = "http://www.openssl.org/news/vulnerabilities.html"
SECTION     = "libs/network"

TOOLCHAIN         = "${MORELLO_TOOLCHAIN}"

RPROVIDES:${PN}   = "openssl-morello"

FILESEXTRAPATHS:prepend := "${THISDIR}/cheri-patches:"

LICENSE          = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=c75985e733726beaba57bc5253e96d04"

SRC_URI = "http://www.openssl.org/source/openssl-3.0.8.tar.gz \
           file://run-ptest \
           file://0001-buildinfo-strip-sysroot-and-debug-prefix-map-from-co.patch \
           file://afalg.patch \
           file://0001-Configure-do-not-tweak-mips-cflags.patch \
           file://0001-purecap-patches.patch \
           "

SRC_URI[sha256sum] = "6c13d2bf38fdf31eac3ce2a347073673f5d63263398f1f69d0df4a41253e4b3e"

PACKAGECONFIG ?= ""

PACKAGECONFIG[no-tls1] = "no-tls1"
PACKAGECONFIG[no-tls1_1] = "no-tls1_1"

S = "${WORKDIR}/openssl-3.0.8"
B = "${WORKDIR}/build"

do_configure[cleandirs] = "${B}"

# no-asm as otherwise crypto wont compile, no-async is needed for musl
EXTRA_OECONF:append = " no-async"
EXTRA_OECONF:append = " no-asm"
EXTRA_OECONF:append = " shared"

EXTRA_OECONF:remove:toolchain-llvm-morello = "--disable-static"

# This allows disabling deprecated or undesirable crypto algorithms.
# The default is to trust upstream choices.
DEPRECATED_CRYPTO_FLAGS ?= ""

do_configure () {
  HASHBANGPERL="/usr/bin/env perl" PERL=perl PERL5LIB="${S}/external/perl/Text-Template-1.46/lib/" \
  perl ${S}/Configure ${EXTRA_OECONF} --prefix=$prefix --openssldir=${libdir}/ssl-3 --libdir=${libdir} linux-aarch64
  perl ${B}/configdata.pm --dump
}

do_compile () {
  oe_runmake
}

do_install () {

  oe_runmake DESTDIR=${D} install

  libdirssl="${libdir}/ssl-3"
  sysconfdirssl="${sysconfdir}/ssl"

  install -d ${D}${sysconfdirssl}
  mv ${D}${libdirssl}/certs \
     ${D}${libdirssl}/private \
     ${D}${libdirssl}/openssl.cnf \
     ${D}${sysconfdirssl}/

  # Although absolute symlinks would be OK for the target, they become
  # invalid if native or nativesdk are relocated from sstate.
  ln -sf ${@oe.path.relative('${libdir}/ssl-3', '${sysconfdir}/ssl/certs')} ${D}${libdirssl}/certs
  ln -sf ${@oe.path.relative('${libdir}/ssl-3', '${sysconfdir}/ssl/private')} ${D}${libdirssl}/private
  ln -sf ${@oe.path.relative('${libdir}/ssl-3', '${sysconfdir}/ssl/openssl.cnf')} ${D}${libdirssl}/openssl.cnf
}

do_install:append() {
  ${OBJDUMP_COMMAND} ${D}${libdir}/libssl.so >  ${D}${PURECAP_DEBUGDIR}/libssl.dump
  ${READELF_COMMAND} ${D}${libdir}/libssl.so >  ${D}${PURECAP_DEBUGDIR}/libssl.readelf
}

PTEST_BUILD_HOST_FILES  += "configdata.pm"
PTEST_BUILD_HOST_PATTERN = "perl_version ="
do_install_ptest () {

  local ptest_path = "${D}${PURECAP_SYSROOT_DIR}${PTEST_PATH}"
  install -d ${ptest_path}
  install -d ${ptest_path}/test
  install -m755 ${B}/test/p_test.so ${ptest_path}/test
  install -m755 ${B}/test/provider_internal_test.cnf ${ptest_path}/test

  # Prune the build tree
  rm -f ${B}/fuzz/*.* ${B}/test/*.*
  cp ${S}/Configure ${B}/configdata.pm ${ptest_path}
  sed 's|${S}|${ptest_path}|g' -i ${ptest_path}/configdata.pm
  cp -r ${S}/external ${B}/test ${S}/test ${B}/fuzz ${S}/util ${B}/util ${ptest_path}

  # For test_shlibload
  ln -s ${libdir}/libcrypto.so.1.1 ${ptest_path}/
  ln -s ${libdir}/libssl.so.1.1 ${ptest_path}/
  install -d ${ptest_path}/apps
  ln -s ${bindir}/openssl ${ptest_path}/apps
  install -m644 ${S}/apps/*.pem ${S}/apps/*.srl ${S}/apps/openssl.cnf ${ptest_path}/apps
  install -m755 ${B}/apps/CA.pl ${ptest_path}/apps
  install -d ${ptest_path}/engines
  install -m755 ${B}/engines/dasync.so ${ptest_path}/engines
  install -m755 ${B}/engines/loader_attic.so ${ptest_path}/engines
  install -m755 ${B}/engines/ossltest.so ${ptest_path}/engines
  install -d ${ptest_path}/providers
  install -m755 ${B}/providers/legacy.so ${ptest_path}/providers
  install -d ${ptest_path}/Configurations
  cp -rf ${S}/Configurations/* ${ptest_path}/Configurations/

  # seems to be needed with perl 5.32.1
  install -d ${ptest_path}/util/perl/recipes
  cp ${ptest_path}/test/recipes/tconversion.pl ${ptest_path}/util/perl/recipes/
  sed 's|${S}|${ptest_path}|g' -i ${ptest_path}/util/wrap.pl
}

# Add the openssl.cnf file to the openssl-conf package. Make the libcrypto
# package RRECOMMENDS on this package. This will enable the configuration
# file to be installed for both the openssl-bin package and the libcrypto
# package since the openssl-bin package depends on the libcrypto package.

PACKAGES =+ "libcrypto-morello libssl-morello openssl-morello-conf ${PN}-engines ${PN}-misc ${PN}-ossl-module-legacy"

FILES:${PN} += "${libdir}/ssl-3/* \
                ${libdir}/ossl-modules/ \
                ${sysconfdir}/ssl \
                 "

FILES:${PN}-bin            = "${bindir}/openssl"

FILES:libcrypto-morello    = "${libdir}/libcrypto${SOLIBS}"
FILES:libssl-morello       = "${libdir}/libssl${SOLIBS}"
FILES:openssl-morello-conf = "${sysconfdir}/ssl/openssl.cnf \
                              ${libdir}/ssl-3/openssl.cnf* \
                             "

FILES:${PN}-engines = "${libdir}/engines-3"

FILES:${PN}-misc               = "${libdir}/ssl-3/misc ${bindir}/c_rehash"
FILES:${PN}-ossl-module-legacy = "${libdir}/ossl-modules/legacy.so"


CONFFILES:openssl-morello-conf  = "${sysconfdir}/ssl/openssl.cnf"

RRECOMMENDS:libcrypto-mmorello += "openssl-morello-conf ${PN}-ossl-module-legacy"

RDEPENDS:${PN}-misc    = "perl"
RDEPENDS:${PN}-ptest  += "openssl-morello-bin perl perl-modules bash sed"

RDEPENDS:${PN}-bin    += "openssl-morello-conf"

CVE_PRODUCT        = "openssl:openssl"
CVE_VERSION_SUFFIX = "alphabetical"

# Only affects OpenSSL >= 1.1.1 in combination with Apache < 2.4.37
# Apache in meta-webserver is already recent enough
CVE_CHECK_IGNORE += "CVE-2019-0190"
inherit autotools binconfig-disabled ptest purecap-sysroot

MORELLO_SRC = "poky/meta/recipes-support/libpcre/libpcre_8.45.bb"

DESCRIPTION = "The PCRE library is a set of functions that implement regular \
expression pattern matching using the same syntax and semantics as Perl 5. PCRE \
has its own native API, as well as a set of wrapper functions that correspond \
to the POSIX regular expression API."
SUMMARY = "Perl Compatible Regular Expressions"
HOMEPAGE = "http://www.pcre.org"
SECTION = "devel"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENCE;md5=b5d5d1a69a24ea2718263f1ff85a1c58"

FILESEXTRAPATHS:prepend := "${THISDIR}/cheri-patches:"

TOOLCHAIN  = "${MORELLO_TOOLCHAIN}"

SRC_URI = "${SOURCEFORGE_MIRROR}/pcre/pcre-${PV}.tar.bz2 \
           file://run-ptest \
           file://Makefile \
           "

SRC_URI += "file://0001-pcre_jit_compile-cheri-provenance.patch \
            file://0002-sljitNativeARM_64-cheri-provenance.patch \
            file://0003-sljitUtils-cheri-provenance.patch \
            "

SRC_URI[sha256sum] = "4dae6fdcd2bb0bb6c37b5f97c33c2be954da743985369cddac3546e3218bffb8"

CVE_PRODUCT = "pcre"

S = "${WORKDIR}/pcre-${PV}"

PROVIDES += "pcre-morello"
DEPENDS  += "bzip2-morello zlib-morello"

PACKAGECONFIG ??= "pcre8 unicode-properties jit"

PACKAGECONFIG[pcre8] = "--enable-pcre8,--disable-pcre8"
PACKAGECONFIG[pcre16] = "--enable-pcre16,--disable-pcre16"
PACKAGECONFIG[pcre32] = "--enable-pcre32,--disable-pcre32"
PACKAGECONFIG[pcretest-readline] = "--enable-pcretest-libreadline,--disable-pcretest-libreadline,readline-morello,"
PACKAGECONFIG[unicode-properties] = "--enable-unicode-properties,--disable-unicode-properties"
PACKAGECONFIG[jit] = "--enable-jit=auto,--disable-jit"

BINCONFIG = "${bindir}/pcre-config"

EXTRA_OECONF = "--enable-utf --disable-cpp"

PACKAGES =+ "libpcrecpp-morello libpcreposix-morello pcregrep-morello pcregrep-doc-morello pcretest-morello pcretest-doc-morello"

SUMMARY:libpcrecpp-morello = "${SUMMARY} - C++ wrapper functions"
SUMMARY:libpcreposix-morello = "${SUMMARY} - C wrapper functions based on the POSIX regex API"
SUMMARY:pcregrep-morello = "grep utility that uses perl 5 compatible regexes"
SUMMARY:pcregrep-doc-morello = "grep utility that uses perl 5 compatible regexes - docs"
SUMMARY:pcretest-morello = "program for testing Perl-comatible regular expressions"
SUMMARY:pcretest-doc-morello = "program for testing Perl-comatible regular expressions - docs"


FILES:libpcrecpp-morello = "${libdir}/libpcrecpp.so.*"
FILES:libpcreposix-morello = "${libdir}/libpcreposix.so.*"
FILES:pcregrep-morello = "${bindir}/pcregrep"
FILES:pcregrep-doc-morello = "${mandir}/man1/pcregrep.1"
FILES:pcretest-morello = "${bindir}/pcretest"
FILES:pcretest-doc-morello = "${mandir}/man1/pcretest.1"

do_install:append() {
    ${OBJDUMP_COMMAND} ${D}${libdir}/libpcre.so.1.2.13 >  ${D}${PURECAP_DEBUGDIR}/libpcre.dump
    ${READELF_COMMAND} ${D}${libdir}/libpcre.so.1.2.13 >  ${D}${PURECAP_DEBUGDIR}/libpcre.readelf
}

PTEST_PATH = "${libdir}/libpcre/ptest"

do_install_ptest() {
    t=${D}${PTEST_PATH}
    cp ${WORKDIR}/Makefile $t
    cp -r ${S}/testdata $t
    for i in pcre_stringpiece_unittest pcregrep pcretest; \
      do cp ${B}/.libs/$i $t; \
    done
    for i in RunTest RunGrepTest test-driver; \
      do cp ${S}/$i $t; \
    done
    # Skip the fr_FR locale test. If the locale fr_FR is found, it is tested.
    # If not found, the test is skipped. The test program assumes fr_FR is non-UTF-8
    # locale so the test fails if fr_FR is UTF-8 locale.
    sed -i -e 's:do3=yes:do3=no:g' ${D}${PTEST_PATH}/RunTest
}

RDEPENDS:${PN}-ptest += "make"
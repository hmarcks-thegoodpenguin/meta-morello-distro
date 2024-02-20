inherit ptest purecap-sysroot

MORELLO_SRC = "poky/meta/recipes-core/zlib/zlib_1.2.11.bb"

SUMMARY     = "Zlib Compression Library"
DESCRIPTION = "Zlib is a general-purpose, patent-free, lossless data compression \
library which is used by many different programs."

HOMEPAGE         = "http://zlib.net/"
SECTION          = "libs"
LICENSE          = "Zlib"
LIC_FILES_CHKSUM = "file://zlib.h;beginline=6;endline=23;md5=5377232268e952e9ef63bc555f7aa6c0"

TOOLCHAIN = "${MORELLO_TOOLCHAIN}"

SRC_URI = "git://github.com/madler/zlib;protocol=https;branch=${SRCBRANCH} \
           file://run-ptest \
           "
SRCREV    = "04f42ceca40f73e2978b50e93806c2a18c1281fc"
SRCBRANCH = "master"

CFLAGS += "-D_REENTRANT"

RDEPENDS:${PN}-ptest += "make"

S = "${WORKDIR}/git"

do_configure() {
  LDCONFIG=true ./configure --prefix=${prefix} --libdir=${libdir} --uname=GNU
}

do_compile() {
  oe_runmake shared
}

do_install() {
	oe_runmake DESTDIR=${D} install
}

do_install_ptest() {
  install -d ${D}${PURECAP_SYSROOT_DIR}${PTEST_PATH}
  install ${B}/examplesh ${D}${PURECAP_SYSROOT_DIR}${PTEST_PATH}
}
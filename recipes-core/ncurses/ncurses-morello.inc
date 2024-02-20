inherit autotools pkgconfig purecap-sysroot

MORELLO_SRC = "poky/meta/recipes-core/ncurses/*"

SUMMARY     = "The New Curses library"
DESCRIPTION = "SVr4 and XSI-Curses compatible curses library and terminfo tools including tic, infocmp, captoinfo. Supports color, multiple highlights, forms-drawing characters, and automatic recognition of keypad and function-key sequences. Extensions include resizable windows and mouse support on both xterm and Linux console using the gpm library."
HOMEPAGE         = "http://www.gnu.org/software/ncurses/ncurses.html"
LICENSE          = "MIT"
LIC_FILES_CHKSUM = "file://COPYING;md5=c5a4600fdef86384c41ca33ecc70a4b8;endline=27"
SECTION          = "libs"

TOOLCHAIN = "${MORELLO_TOOLCHAIN}"

DEPENDS  += "ncurses-native"

do_configure() {
    oe_runconf \
    --without-debug \
    --without-ada \
    --without-gpm \
    --enable-termcap \
    --enable-echo \
    --enable-warnings \
    --with-shared \
    --disable-big-core \
    --program-prefix= \
    --with-manpage-format=normal \
    --without-manpage-renames \
    --disable-stripping \
    --disable-rpath-hack \
    --without-cxx-binding \
    --without-cxx \
    --enable-overwrite
}

do_compile() {
    oe_runmake all
}

do_install:append() {
  ${OBJDUMP_COMMAND} ${D}${libdir}/libcurses.so >  ${D}${PURECAP_DEBUGDIR}/libcurses.dump
  ${READELF_COMMAND} ${D}${libdir}/libcurses.so >  ${D}${PURECAP_DEBUGDIR}/libcurses.readelf
}

PACKAGES += " \
        ${PN}-tools \
        ${PN}-terminfo-base \
        ${PN}-terminfo \
"

FILES:${PN} += "\
  ${bindir}/tput \
  ${bindir}/tset \
  ${bindir}/ncurses5-config \
  ${bindir}/ncursesw5-config \
  ${bindir}/ncurses6-config \
  ${bindir}/ncursesw6-config \
  ${datadir}/tabset \
"

# This keeps only tput/tset in ncurses
# clear/reset are in already busybox
FILES:${PN}-tools = "\
        ${bindir}/tic \
        ${bindir}/toe \
        ${bindir}/infotocap \
        ${bindir}/captoinfo \
        ${bindir}/infocmp \
        ${bindir}/tack \
        ${bindir}/tabs \
        ${bindir}/clear \
        ${bindir}/reset \
        "

FILES:${PN}-terminfo = "\
        ${datadir}/terminfo \
        ${libdir}/terminfo \
"

FILES:${PN}-terminfo-base = "\
        ${sysconfdir}/terminfo \
"

# Putting terminfo into the sysroot adds around 2800 files to
# each recipe specific sysroot. We can live without this, particularly
# as many recipes may have native and target copies.
SYSROOT_DIRS:remove = "${datadir}"
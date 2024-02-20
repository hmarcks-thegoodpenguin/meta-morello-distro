require recipes-core/util-linux/util-linux.inc

inherit autotools gettext pkgconfig pure-cap-kheaders purecap-sysroot

MORELLO_SRC = "poky/meta/recipes-core/util-linux/util-linux_2.37.4.bb"

SUMMARY = "A suite of basic system administration utilities"

TOOLCHAIN = "${MORELLO_TOOLCHAIN}"

PV = "2.37.4"

S = "${WORKDIR}/util-linux-${PV}"
EXTRA_OECONF += "--disable-all-programs --enable-libuuid"
LICENSE = "BSD-3-Clause"

do_install() {
	install_dir="${D}"
	install -d ${install_dir}
	oe_runmake DESTDIR=${install_dir} install
	rm -rf ${install_dir}${datadir} ${install_dir}${bindir} ${install_dir}${base_bindir} \
	${install_dir}${sbindir} ${install_dir}${base_sbindir} ${install_dir}${exec_prefix}/sbin
}
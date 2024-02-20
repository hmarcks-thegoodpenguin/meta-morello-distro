# Prefix all of the paths (root AND usr) with ${PURECAP_SYSROOT_DIR}, apart from the systemd.
# We allow non purecap systemd app to manage purecap packages for now (which with further hacks is achievable)

# Path prefixes
export base_prefix = "${PURECAP_SYSROOT_DIR}"
export prefix = "${PURECAP_SYSROOT_DIR}/usr"
export exec_prefix = "${prefix}"

root_prefix = "${@bb.utils.contains('DISTRO_FEATURES', 'usrmerge', '${exec_prefix}', '${base_prefix}', d)}"

# Base paths
export base_bindir = "${root_prefix}/bin"
export base_sbindir = "${root_prefix}/sbin"
export base_libdir = "${root_prefix}/${baselib}"
export nonarch_base_libdir = "${root_prefix}/lib"

# Architecture independent paths
export sysconfdir = "${base_prefix}/etc"
export servicedir = "${base_prefix}/srv"
export sharedstatedir = "${base_prefix}/com"
export localstatedir = "${base_prefix}/var"
export datadir = "${prefix}/share"
export infodir = "${datadir}/info"
export mandir = "${datadir}/man"
export docdir = "${datadir}/doc"

export nonarch_libdir = "${exec_prefix}/lib"

export systemd_user_unitdir = "/lib/systemd/user"
export systemd_unitdir = "/lib/systemd"
export systemd_system_unitdir = "/lib/systemd/system"

# Architecture dependent paths
export bindir = "${exec_prefix}/bin"
export sbindir = "${exec_prefix}/sbin"
export libdir = "${exec_prefix}/${baselib}"
export libexecdir = "${exec_prefix}/libexec"
export includedir = "${exec_prefix}/include"
export oldincludedir = "${exec_prefix}/include"

# Disable QA for now
INSANE_SKIP:${PN}    += "file-rdeps"
EXCLUDE_FROM_SHLIBS   = "1"
do_package_qa[noexec] = "1"

# Stop debian class creating pkg duplicates
AUTO_LIBNAME_PKGS = ""

# Debug purecap, worth double checking linkage etc.
PURECAP_DEBUGDIR = "/morello-debug"

do_install:append:class-target() {
  install -d ${D}${PURECAP_DEBUGDIR}
}

FILES:${PN}-dbg  += "${PURECAP_DEBUGDIR}"

OBJDUMP_COMMAND = "${OBJDUMP} -D"
READELF_COMMAND = "${READELF} -a"

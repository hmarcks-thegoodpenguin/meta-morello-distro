FILESEXTRAPATHS:prepend := "${THISDIR}/postgresql:"

SYSTEMD_AUTO_ENABLE:${PN} = "disable"

PROVIDES += "libpq"

FILES:libpq = "${libdir}/libpq*${SOLIBS} ${includedir}"

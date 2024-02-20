FILESEXTRAPATHS:prepend := "${THISDIR}:${THISDIR}/cheri-patches:"

SRC_URI += "\
    file://0001-mman-allow-for-storing-caps-in-shared-memory.patch \
    file://files/0003-defconfig-modify.patch \
    "

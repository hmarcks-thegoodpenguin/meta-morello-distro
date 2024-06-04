require postgresql-morello.inc

FILESEXTRAPATHS:prepend := "${THISDIR}/postgresql:${THISDIR}/cheri-patches:${THISDIR}/files:"

PVBASE = "9.6"
LIC_FILES_CHKSUM = "file://COPYRIGHT;md5=87da2b84884860b71f5f24ab37e7da78"

SRC_URI = "git://github.com/CTSRD-CHERI/postgres;protocol=https;branch=${SRCBRANCH} \
           file://0003-configure.in-bypass-autoconf-2.69-version-check.patch \
           file://postgresql-morello.init \
           file://postgresql-profile \
           file://postgresql.pam \
           file://postgresql-setup \
           file://pg_config \
"

SRC_URI += "\
   file://0001-port.h-change-argument-order-to-qsort_r-to-match-pos.patch \
   file://0002-qsort-change-defines-from-freebsd-to-cheri.patch \
"

SRC_URI += " \
    file://postgresql-init \
    file://postgresql-init.service \
    file://postgresql-morello.service \
    "

SRC_URI += "\
      file://postgres-test \
      file://postgres-bench \
      file://test-schedule \
      "

SRC_URI[sha256sum] = "d4f72cb5fb857c9a9f75ec8cf091a1771272802f2178f0b2e65b7b6ff64f4a30"

SRCBRANCH = "96-cheri"
SRCREV    = "e94e514cac6a8ae2277b3e44970c734c9a066f34"

S = "${WORKDIR}/git"

CFLAGS:remove = "-O2"

SYSTEMD_AUTO_ENABLE:${PN} = "enable"

DB_DATADIR = "/var/lib/postgresql/data"

PACKAGECONFIG[tcl] = "--with-tcl --with-tclconfig=${STAGING_BINDIR_CROSS},--without-tcl,tcl-morello tcl-native,"
# PACKAGECONFIG[perl] = "--with-perl,--without-perl,perl,perl"
# PACKAGECONFIG[python] = "--with-python,--without-python,python3,python3"
# PACKAGECONFIG[gssapi] = "--with-gssapi,--without-gssapi,krb5"
# PACKAGECONFIG[pam] = "--with-pam,--without-pam,libpam"
PACKAGECONFIG[ldap] = "--with-ldap,--without-ldap,openldap-morello"
#PACKAGECONFIG[systemd] = "--with-systemd,--without-systemd,systemd systemd-systemctl-native"
#PACKAGECONFIG[uuid] = "--with-uuid=e2fs,--without-uuid,util-linux"
#PACKAGECONFIG[libxml] = "--with-libxml,--without-libxml,libxml2,libxml2"
# PACKAGECONFIG[libxslt] = "--with-libxslt,--without-libxslt,libxslt"
PACKAGECONFIG[zlib] = "--with-zlib,--without-zlib,zlib-morello"
PACKAGECONFIG[openssl] = "--with-openssl,--without-openssl,openssl-morello,"

export PRINTF_SIZE_T_SUPPORT="yes"

PG_INIT_SERVICE_FILE = "${D}${systemd_unitdir}/system/postgresql-init.service"
PG_SERVICE_FILE = "${D}${systemd_unitdir}/system/postgresql.service"

do_install:append() {

  install_dir="${D}"

  D_DEST_DIR=${install_dir}${sysconfdir}/${BPN_POSTGRESQL}

  install -d ${D_DEST_DIR}
  install -m 0755 ${WORKDIR}/postgresql-init ${D_DEST_DIR}/postgresql-init

  sed -e "s:%DB_DATADIR%:${DB_DATADIR}:g" -i ${D_DEST_DIR}/postgresql-init
  sed -e "s:%PGINSTALLDIR%:${prefix}:g" -i ${D_DEST_DIR}/postgresql-init
  sed -e "s:%BINDIR%:${bindir}:g" -i ${D_DEST_DIR}/postgresql-init

  sed -e "s:%SYSCONFDIR%:${sysconfdir}:g" -i ${D_DEST_DIR}/postgresql-init

  install -d ${D}${systemd_unitdir}/system/

  install -m 644 ${WORKDIR}/postgresql-init.service ${PG_INIT_SERVICE_FILE}

  sed -e "s:%PGINSTALLDIR%:${prefix}:g" -i ${PG_INIT_SERVICE_FILE}

  sed -e "s:%SYSCONFIGDIR%:${sysconfdir}:g" -i ${PG_INIT_SERVICE_FILE}
  sed -e "s:%SYSCONFIGDIR%:${sysconfdir}:g" -i ${PG_INIT_SERVICE_FILE}

  sed -e "s:%DB_USER%:${DB_USER}:g" -i ${PG_INIT_SERVICE_FILE}
  sed -e "s:%DB_PASSWORD%:${DB_PASSWORD}:g" -i ${PG_INIT_SERVICE_FILE}
  sed -e "s:%DB_ROOT_PASSWORD%:${DB_ROOT_PASSWORD}:g" -i ${PG_INIT_SERVICE_FILE}

  install -m 644 ${WORKDIR}/postgresql-morello.service ${PG_SERVICE_FILE}

  sed -e 's,%BINDIR%,${bindir},g' -i ${PG_SERVICE_FILE}
  sed -e "s:%PGINSTALLDIR%:${prefix}:g" -i ${PG_SERVICE_FILE}

  # Update PGDATA throughout
  files="${install_dir}${localstatedir}/lib/${BPN_POSTGRESQL}/.profile"
  files="$files ${D}${systemd_unitdir}/system/postgresql.service"
  files="$files ${install_dir}${bindir}/${BPN}-setup"
  files="$files ${install_dir}${sysconfdir}/init.d/${BPN}-server"
  for f in $files
  do
      sed -e "s:\(PGDATA=\).*$:\1${DB_DATADIR}:g" -i $f
  done

  # Ensure DB is initialize before we attempt to start the service
  FILE=${D}${systemd_unitdir}/system/postgresql.service
  sed -e '/ExecStart=.*/i ExecStartPre=${sysconfdir}/${BPN_POSTGRESQL}/postgresql-init initdb' -i $FILE
  sed -e '/ExecStartPre=.*/i PermissionsStartOnly=true' -i $FILE

  # Install test scripts
  BENCH_SCRIPT=${D}/postgres-bench.sh
  install -m 0755 ${WORKDIR}/postgres-bench ${BENCH_SCRIPT}

  sed -e "s:%BINDIR%:${bindir}:g" -i ${BENCH_SCRIPT}
  sed -e "s:%LIBDIR%:${libdir}:g" -i ${BENCH_SCRIPT}

  TEST_SCRIPT=${D}/postgres-test.sh
  install -m 0755 ${WORKDIR}/postgres-test ${TEST_SCRIPT}

  sed -e "s:%BINDIR%:${bindir}:g" -i ${TEST_SCRIPT}
  sed -e "s:%LIBDIR%:${libdir}:g" -i ${TEST_SCRIPT}

  install -d ${install_dir}${libdir}/${BPN_POSTGRESQL}/pgxs/src/test/regress
  install -m 644 ${WORKDIR}/test-schedule ${install_dir}${libdir}/${BPN_POSTGRESQL}/pgxs/src/test/regress/test_schedule
}

do_install:append () {

  install -d "${D}${libdir}"
  cp ${B}/src/test/regress/*.so ${D}${libdir}
}

do_install:append() {
  ${OBJDUMP_COMMAND} ${D}${bindir}/pg_ctl >  ${D}${PURECAP_DEBUGDIR}/pgctl.dump
  ${READELF_COMMAND} ${D}${bindir}/pg_ctl >  ${D}${PURECAP_DEBUGDIR}/pgctl.readelf
  ${OBJDUMP_COMMAND} ${D}${bindir}/postgres >  ${D}${PURECAP_DEBUGDIR}/postgres.dump
  ${READELF_COMMAND} ${D}${bindir}/postgres >  ${D}${PURECAP_DEBUGDIR}/postgres.readelf
}

PACKAGES += " ${PN}-setup"

SYSTEMD_PACKAGES += "${PN}-setup"
SYSTEMD_SERVICE:${PN}-setup = "postgresql-init.service"

FILES:${PN}-setup = " \
    ${systemd_unitdir}/system \
"

FILES:${PN}-dbg += " \
    postgres-test.sh \
    postgres-bench.sh \
    ${libdir}/${BPN_POSTGRESQL}/pgxs/src/test/regress/test-schedule \
"

FILES:${PN}-gdb-debug += "/gdb_debug"
RPROVIDES:${PN} += "postgresql-setup"

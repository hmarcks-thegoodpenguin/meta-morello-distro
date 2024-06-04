inherit autotools pkgconfig perlnative python3native python3targetconfig
inherit useradd update-rc.d systemd gettext cpan-base pure-cap-kheaders purecap-sysroot purecap-useradd
inherit perl-hacks

MORELLO_SRC  = "meta-openembedded/meta-oe/recipes-dbs/postgresql/postgresql_14.5"
MORELLO_SRC += "meta-cloud-services/meta-openstack/recipes-dbs/postgresql/*"

SUMMARY = "PostgreSQL is a powerful, open source relational database system"
DESCRIPTION = "\
    PostgreSQL is an advanced Object-Relational database management system \
    (DBMS) that supports almost all SQL constructs (including \
    transactions, subselects and user-defined types and functions). The \
    postgresql package includes the client programs and libraries that \
    you'll need to access a PostgreSQL DBMS server.  These PostgreSQL \
    client programs are programs that directly manipulate the internal \
    structure of PostgreSQL databases on a PostgreSQL server. These client \
    programs can be located on the same machine with the PostgreSQL \ 
    server, or may be on a remote machine which accesses a PostgreSQL \
    server over a network connection. This package contains the docs \
    in HTML for the whole package, as well as command-line utilities for \
    managing PostgreSQL databases on a PostgreSQL server. \
    \
    If you want to manipulate a PostgreSQL database on a local or remote \
    PostgreSQL server, you need this package. You also need to install \
    this package if you're installing the postgresql-server package. \
    \
"
HOMEPAGE = "http://www.postgresql.com"
LICENSE = "0BSD"

DEPENDS += "readline-morello zlib-morello openssl-morello util-linux-morello tcl-morello"
DEPENDS += "libnsl2 tzcode-native"

TOOLCHAIN = "${MORELLO_TOOLCHAIN}"

BPN_POSTGRESQL="postgresql"

ARM_INSTRUCTION_SET = "arm"

# used for hacking
MORELLO = "-morello"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:${THISDIR}/postgresql:"

S = "${WORKDIR}/postgresql-${PV}"

LEAD_SONAME = "libpq.so"

# LDFLAGS for shared libraries
export LDFLAGS_SL = "${LDFLAGS}"

CFLAGS  += "-I${STAGING_DIR_HOST}/usr/include/${PYTHON_DIR} -I${STAGING_INCDIR}/tcl8.6 -Wno-unused-function -pipe -msoft-float -integrated-as -O0 -g"
CLAGS   += "-Wno-deprecated-declarations -Wno-compound-token-split-by-macro"
LDFLAGS += "-pthread"
INHIBIT_PACKAGE_STRIP:toolchain-llvm-morello = "1"

SYSTEMD_SERVICE:${PN}     = "postgresql.service"
SYSTEMD_AUTO_ENABLE:${PN} = "disable"

pkg_postinst:${PN} () {
    if ${@bb.utils.contains('DISTRO_FEATURES', 'systemd sysvinit', 'true', 'false', d)}; then
        if [ -n "$D" ]; then
            OPTS="--root=$D"
        fi
        systemctl $OPTS mask postgresql-server.service
    fi
}

PACKAGECONFIG ??= " \
    ${@bb.utils.filter('DISTRO_FEATURES', 'pam systemd', d)} \
    openssl uuid tcl perl zlib \
"

EXTRA_OECONF += "--enable-thread-safety \
    --datadir=${datadir}/${BPN_POSTGRESQL} \
    --sysconfdir=${sysconfdir}/${BPN_POSTGRESQL} \
    --enable-debug \
    --without-systemd \
"

CFLAGS += "-DUSE_ASSERT_CHECKING"

PACKAGES_DYNAMIC += "^${PN}-plperl \
    ^${PN}-pltcl \
    ^${PN}-plpython \
"

python populate_packages:prepend() {

    def fill_more(name):
        if name is None or name.strip() == "":
            return

        fpack=d.getVar('PACKAGES', False) or ""
        fpack="${PN}-" + name + " " + fpack
        d.setVar('PACKAGES', fpack)

    conf=(d.getVar('PACKAGECONFIG') or "").split()
    pack=d.getVar('PACKAGES', False) or ""
    bb.debug(1, "PACKAGECONFIG=%s" % conf)
    bb.debug(1, "PACKAGES1=%s" % pack )

    if "perl" in conf :
        fill_more("plperl")

    if "tcl" in conf:
        fill_more("pltcl")

    if "python" in conf:
        fill_more("plpython")

    pack=d.getVar('PACKAGES') or ""
    bb.debug(1, "PACKAGES2=%s" % pack)

}

# This will make native perl use target settings (for include dirs etc.)
# there is no perl in purecap, so yet another hack: hardcode the version and target
export PERLCONFIGTARGET = "${@is_target(d)}"
export PERL_ARCHLIB = "${STAGING_LIBDIR_HACK}${PERL_OWN_DIR}/perl5/${@get_perl_version(d)}/${@get_perl_arch(d)}"

do_configure() {

    # do_configure
    autotools_do_configure

    # do_configure:append
    # workaround perl package related bugs
    sed -i -e "s:-L/usr/local/lib:-L=/usr/local/lib:g" \
        ${B}/src/Makefile.global
    LIBPNA="\${STAGING_LIBDIR_NATIVE}/perl-native"
    LIBNA="\${STAGING_LIBDIR_NATIVE}"
    BLIBNA="\${STAGING_BASE_LIBDIR_NATIVE}"
    sed -i -e "/^perl_archlibexp/s:${LIBPNA}:${STAGING_LIBDIR_HACK}:g" \
        ${B}/src/Makefile.global
    sed -i -e "/^perl_privlibexp/s:/usr/lib:${STAGING_LIBDIR_HACK}:g" \
        ${B}/src/Makefile.global
    # remove the rpath, replace with correct lib path
    sed -i \
        -e "/^perl_embed_ldflags/s:-Wl,-rpath,${LIBNA}::g" \
        -e "/^perl_embed_ldflags/s:-Wl,-rpath,${BLIBNA}::g" \
        -e "/^perl_embed_ldflags/s:-Wl,-rpath-link,${LIBNA}::g" \
        -e "/^perl_embed_ldflags/s:-Wl,-rpath-link,${BLIBNA}::g" \
        -e "/^perl_embed_ldflags/s:${LIBPNA}:${STAGING_LIBDIR_HACK}:g" \
        -e "/^perl_embed_ldflags/s:${LIBNA}:${STAGING_LIBDIR_HACK}:g"  \
        -e "/^perl_embed_ldflags/s:${BLIBNA}:${STAGING_BASELIBDIR_HACK}:g" \
        -e "/^TCLSH/s:=.*:= /usr/bin/tclsh:g" \
    ${B}/src/Makefile.global

    if ${@bb.utils.contains('PACKAGECONFIG', 'perl', 'true', 'false', d)}; then
        # workaround perl package's libperl.so problem
        # we are using perlnative so this perl should have same version
        perl_version=`perl -v 2>/dev/null | \
            sed -n 's/This is perl.*v[a-z ]*\([0-9]\.[0-9][0-9.]*\).*$/\1/p'`
        if [ ! -h "${STAGING_LIBDIR_HACK}/perl/$perl_version/CORE/libperl.so" -a \
             ! -h "${STAGING_LIBDIR_HACK}/libperl.so" ]; then
            ln -sf ../../../libperl.so.5 \
                ${STAGING_LIBDIR_HACK}/perl/$perl_version/CORE/libperl.so
        fi
    fi
}

do_compile:append() {
    oe_runmake -C contrib all
}

# server needs to configure user and group
usernum = "28"
groupnum = "28"
USERADD_PACKAGES = "${PN}"
USERADD_PARAM:${PN} = "-M -g postgres -o -r -d ${localstatedir}/lib/${BPN_POSTGRESQL} \
    -s /bin/sh -c 'PostgreSQL Server' -u ${usernum} postgres"
GROUPADD_PARAM:${PN} = "-g ${groupnum} -o -r postgres"

INITSCRIPT_PACKAGES = "${PN}"
INITSCRIPT_NAME = "${BPN}-server"
INITSCRIPT_PARAMS = "start 64 . stop 36 0 1 2 3 4 5 6 ."

do_install() {

    install_dir="${D}"

    install -d ${install_dir}
    oe_runmake DESTDIR=${install_dir} install

    # install contrib
    oe_runmake DESTDIR=${install_dir} -C contrib install
    # install tutorial
    install -d -m 0755 ${install_dir}${libdir}/${BPN_POSTGRESQL}/tutorial
    install ${B}/src/tutorial/* ${install_dir}${libdir}/${BPN_POSTGRESQL}/tutorial

    # install COPYRIGHT README HISTORY
    install -d -m 0755 ${install_dir}${docdir}/${BPN_POSTGRESQL}
    for i in ${B}/COPYRIGHT ${B}/README ${B}/HISTORY ${B}/doc/KNOWN_BUGS ${B}/doc/MISSING_FEATURES ${B}/doc/README* ${B}/doc/bug.template; do
        [ -f $i ] && install $i ${install_dir}${docdir}/${BPN_POSTGRESQL}
    done

    # install dirs and server init
    install -d ${install_dir}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/postgresql-morello.init ${install_dir}${sysconfdir}/init.d/${BPN}-server
    sed -i -e "s/^PGVERSION=.*$/PGVERSION=${PVBASE}/g" ${install_dir}${sysconfdir}/init.d/${BPN}-server
    install -m 0755 ${WORKDIR}/postgresql-setup ${install_dir}${bindir}/${BPN}-setup

    install -d -m 700 ${install_dir}${localstatedir}/lib/${BPN_POSTGRESQL}/data
    install -d -m 700 ${install_dir}${localstatedir}/lib/${BPN_POSTGRESQL}/backups
    install -m 644 ${WORKDIR}/postgresql-profile ${install_dir}${localstatedir}/lib/${BPN_POSTGRESQL}/.profile

    chown -R postgres:postgres ${install_dir}${localstatedir}/lib/${BPN_POSTGRESQL}

    # multiple server config directory
    install -d -m 700 ${install_dir}${sysconfdir}/default/${BPN_POSTGRESQL}

    if ${@bb.utils.contains('DISTRO_FEATURES', 'pam', 'true', 'false', d)}; then
        install -d ${install_dir}${sysconfdir}/pam.d
        install -m 644 ${WORKDIR}/postgresql.pam ${install_dir}${sysconfdir}/pam.d/postgresql
    fi

    # Remove the build path
    if [ -f ${install_dir}${libdir}/${BPN_POSTGRESQL}/pgxs/src/Makefile.global ]; then
        sed -i -e 's#${RECIPE_SYSROOT}##g' \
               -e 's#${RECIPE_SYSROOT_NATIVE}##g' \
               -e 's#${WORKDIR}##g' \
               -e 's#${TMPDIR}##g' \
             ${install_dir}${libdir}/${BPN_POSTGRESQL}/pgxs/src/Makefile.global
    fi

  install -d ${install_dir}${libdir}/${BPN_POSTGRESQL}/pgxs/src/test/regress
  cp -rf ${S}/src/test/regress ${install_dir}${libdir}/${BPN_POSTGRESQL}/pgxs/src/test
}

do_install:append() {
  # we need a fake pg_config as some lib configs will try to run pg_config...
  # probably could do with a native flavour

  install -d ${D}${bindir}/pgconfig
  install -m 755 ${WORKDIR}/pg_config ${D}${bindir}/pgconfig/pg_config
}

SSTATE_SCAN_FILES += "Makefile.global"
SSTATE_SCAN_FILES:remove = "*_config"

PACKAGES =+ "${PN}-client ${PN}-server-dev ${PN}-timezone \
    libecpg${MORELLO}compat libecpg${MORELLO}-compat-dev \
    libecpg${MORELLO} libecpg${MORELLO}-dev libecpg${MORELLO}-staticdev libecpg${MORELLO}-doc \
    libpq${MORELLO} libpq${MORELLO}-dev libpq${MORELLO}-staticdev \
    libpgtypes${MORELLO} libpgtypes${MORELLO}-staticdev libpgtypes${MORELLO}-dev \
    ${PN}-contrib \
"

RPROVIDES:${PN}-dbg += "libecpg${MORELLO}-compat-dbg \
                        libecpg${MORELLO}-dbg \
                        libpq${MORELLO}-dbg \
                        libpgtypes${MORELLO}-dbg \
                        ${PN}-contrib-dbg \
                        ${PN}-pltcl-dbg \
                        ${PN}-plpython-dbg \
                        ${PN}-plperl-dbg \
                       "

FILES:${PN} += "${sysconfdir}/${BPN_POSTGRESQL} ${libdir} ${bindir}/pg_config_fake \
    ${sysconfdir}/init.d/${BPN}-server \
    ${localstatedir}/lib/${BPN_POSTGRESQL}/data ${localstatedir}/lib/${BPN_POSTGRESQL}/backups \
    ${localstatedir}/lib/${BPN_POSTGRESQL}/.profile ${sysconfdir}/default/${BPN_POSTGRESQL} \
    ${libdir}/${BPN_POSTGRESQL}/dict_snowball.so ${libdir}/${BPN_POSTGRESQL}/plpgsql.so \
    ${libdir}/${BPN_POSTGRESQL}/euc2004_sjis2004.so \
    ${libdir}/${BPN_POSTGRESQL}/libpqwalreceiver.so \
    ${libdir}/${BPN_POSTGRESQL}/*_and_*.so \
    ${@'${sysconfdir}/pam.d/postgresql-morello' \
       if 'pam' == d.getVar('enable_pam') \
       else ''} \
    ${datadir} \
"

FILES:${PN}-client = "${bindir}/clusterdb \
    ${bindir}/createdb \
    ${bindir}/createuser \
    ${bindir}/dropdb \
    ${bindir}/dropuser \
    ${bindir}/pg_dump \
    ${bindir}/pg_dumpall \
    ${bindir}/pg_restore \
    ${bindir}/psql \
    ${bindir}/reindexdb \
    ${bindir}/vacuumdb \
    ${bindir}/vacuumlo \
    ${datadir}/${BPN_POSTGRESQL}/psqlrc.sample \
"
FILES:${PN}-client-doc = "${mandir}/man1/clusterdb.* \
    ${mandir}/man1/createdb.*   ${mandir}/man1/createlang.* \
    ${mandir}/man1/createuser.* ${mandir}/man1/dropdb.* \
    ${mandir}/man1/droplang.*   ${mandir}/man1/dropuser.* \
    ${mandir}/man1/pg_dump.*    ${mandir}/man1/pg_dumpall.* \
    ${mandir}/man1/pg_restore.* ${mandir}/man1/psql.* \
    ${mandir}/man1/reindexdb.*  ${mandir}/man1/vacuumdb.* \
    ${mandir}/man7/* \
"
FILES:${PN}-doc += "${docdir}/${BPN_POSTGRESQL}/html ${libdir}/${BPN_POSTGRESQL}/tutorial/ \
    ${mandir}/man1/initdb.* ${mandir}/man1/pg_controldata.* \
    ${mandir}/man1/pg_ctl.* ${mandir}/man1/pg_resetxlog.* \
    ${mandir}/man1/postgres.* ${mandir}/man1/postmaster.* \
    ${mandir} ${docdir} \
"
FILES:${PN}-timezone = "${datadir}/${BPN_POSTGRESQL}/timezone \
    ${datadir}/${BPN_POSTGRESQL}/timezonesets \
"

FILES:${PN}-server-dev = "${includedir}/${BPN_POSTGRESQL}/server \
                          ${libdir}/${BPN_POSTGRESQL}/pgxs \
"

FILES:libecpg${MORELLO} = "${libdir}/libecpg*${SOLIBS}"
FILES:libecpg${MORELLO}-dev = "${libdir}/libecpg*${SOLIBSDEV} \
    ${libdir}/libpgtypes*${SOLIBSDEV} \
    ${includedir}/ecpg*.h ${includedir}/${BPN_POSTGRESQL}/ecpg*.h \
    ${includedir}/pgtypes*.h ${includedir}/${BPN_POSTGRESQL}/informix \
    ${includedir}/sql3types.h ${includedir}/sqlca.h \
"

FILES:libecpg${MORELLO}-doc = "${mandir}/man1/ecpg.*"
FILES:libecpg${MORELLO}-staticdev = "${libdir}/libecpg*.a"
SECTION:libecpg${MORELLO}-staticdev = "devel"
RDEPENDS:libecpg${MORELLO}-staticdev = "libecpg${MORELLO}-dev (= ${EXTENDPKGV})"

FILES:libpq${MORELLO} = "${libdir}/libpq*${SOLIBS}"
FILES:libpq${MORELLO}-dev = "${libdir}/libpq*${SOLIBSDEV} \
    ${includedir} \
"
FILES:libpq${MORELLO}-staticdev = "${libdir}/libpq*.a ${libdir}/libpgport.a"
SECTION:libpq${MORELLO}-staticdev = "devel"
RDEPENDS:libpq${MORELLO}-staticdev = "libpq${MORELLO}-dev (= ${EXTENDPKGV})"

FILES:libecpg${MORELLO}-compat = "${libdir}/libecpg_compat*${SOLIBS}"
FILES:libecpg${MORELLO}-compat-dev = "${libdir}/libecpg_compat*${SOLIBS}"
FILES:libpgtypes${MORELLO} = "${libdir}/libpgtypes*${SOLIBS}"
FILES:libpgtypes${MORELLO}-staticdev = "${libdir}/libpgtypes*.a"
FILES:libpgtypes${MORELLO}-dev = "${libdir}/libpgtypes*${SOLIBS} ${includedir}/pgtypes*.h"

FILES:${PN}-contrib = " ${bindir}/oid2name ${bindir}/pg_standby \
    ${S}/contrib/spi/*.example \
    ${libdir}/${BPN_POSTGRESQL}/_int.so ${libdir}/${BPN_POSTGRESQL}/adminpack.so \
    ${libdir}/${BPN_POSTGRESQL}/autoinc.so ${libdir}/${BPN_POSTGRESQL}/auto_explain.so \
    ${libdir}/${BPN_POSTGRESQL}/auth_delay.so ${libdir}/${BPN_POSTGRESQL}/btree_gin.so \
    ${libdir}/${BPN_POSTGRESQL}/btree_gist.so ${libdir}/${BPN_POSTGRESQL}/.so \
    ${libdir}/${BPN_POSTGRESQL}/chkpass.so ${libdir}/${BPN_POSTGRESQL}/citext.so \
    ${libdir}/${BPN_POSTGRESQL}/cube.so ${libdir}/${BPN_POSTGRESQL}/dblink.so \
    ${libdir}/${BPN_POSTGRESQL}/dict_int.so ${libdir}/${BPN_POSTGRESQL}/dict_xsyn.so \
    ${libdir}/${BPN_POSTGRESQL}/dummy_seclabel.so ${libdir}/${BPN_POSTGRESQL}/earthdistance.so \
    ${libdir}/${BPN_POSTGRESQL}/file_fdw.so ${libdir}/${BPN_POSTGRESQL}/fuzzystrmatch.so \
    ${libdir}/${BPN_POSTGRESQL}/hstore.so ${libdir}/${BPN_POSTGRESQL}/insert_username.so \
    ${libdir}/${BPN_POSTGRESQL}/isn.so ${libdir}/${BPN_POSTGRESQL}/lo.so \
    ${libdir}/${BPN_POSTGRESQL}/ltree.so ${libdir}/${BPN_POSTGRESQL}/moddatetime.so \
    ${libdir}/${BPN_POSTGRESQL}/pageinspect.so ${libdir}/${BPN_POSTGRESQL}/pg_buffercache.so \
    ${libdir}/${BPN_POSTGRESQL}/pg_freespacemap.so ${libdir}/${BPN_POSTGRESQL}/pg_trgm.so \
    ${libdir}/${BPN_POSTGRESQL}/pgcrypto.so ${libdir}/${BPN_POSTGRESQL}/pgrowlocks.so \
    ${libdir}/${BPN_POSTGRESQL}/pgstattuple.so ${libdir}/${BPN_POSTGRESQL}/pg_stat_statements.so \
    ${libdir}/${BPN_POSTGRESQL}/refint.so ${libdir}/${BPN_POSTGRESQL}/seg.so \
    ${libdir}/${BPN_POSTGRESQL}/sslinfo.so \
    ${libdir}/${BPN_POSTGRESQL}/tablefunc.so \
    ${libdir}/${BPN_POSTGRESQL}/test_parser.so ${libdir}/${BPN_POSTGRESQL}/timetravel.so \
    ${libdir}/${BPN_POSTGRESQL}/uuid-ossp.so \
    ${libdir}/${BPN_POSTGRESQL}/pgxml.so ${libdir}/${BPN_POSTGRESQL}/passwordcheck.so \
    ${libdir}/${BPN_POSTGRESQL}/pg_upgrade_support.so ${libdir}/${BPN_POSTGRESQL}/.so \
    ${libdir}/${BPN_POSTGRESQL}/unaccent.so \
"
DESCRIPTION:${PN}-contrib = "The postgresql-contrib package contains \
    contributed packages that are included in the PostgreSQL distribution."

FILES:${PN}-pltcl = "${libdir}/${BPN_POSTGRESQL}/pltcl.so ${bindir}/pltcl_delmod \
    ${binddir}/pltcl_listmod ${bindir}/pltcl_loadmod  \
    ${datadir}/${BPN_POSTGRESQL}/unknown.pltcl"
SUMMARY:${PN}-pltcl = "The Tcl procedural language for PostgreSQL"
DESCRIPTION:${PN}-pltcl = "PostgreSQL is an advanced Object-Relational \
    database management system.  The postgresql-pltcl package contains the PL/Tcl \
    procedural language for the backend."

FILES:${PN}-plperl = "${libdir}/${BPN_POSTGRESQL}/plperl.so"
SUMMARY:${PN}-plperl = "The Perl procedural language for PostgreSQL"
DESCRIPTION:${PN}-plperl = "PostgreSQL is an advanced Object-Relational \
    database management system.  The postgresql-plperl package contains the \
    PL/Perl procedural language for the backend."

# In version 8, it will be plpython.so
# In version 9, it might be plpython{2,3}.so depending on python2 or 3
FILES:${PN}-plpython = "${libdir}/${BPN_POSTGRESQL}/plpython*.so"
SUMMARY:${PN}-plpython = "The Python procedural language for PostgreSQL"
DESCRIPTION:${PN}-plpython = "PostgreSQL is an advanced Object-Relational \
    database management system.  The postgresql-plpython package contains \
    the PL/Python procedural language for the backend."


FILES:${PN}-dbg += "gdb_debug \
                    ${bindir}/pgbench \
                    ${libdir}/${BPN_POSTGRESQL}/pgxs/src/test/regress/* \
                    "


CVE_CHECK_IGNORE += "\
   CVE-2017-8806 \
"

SYSROOT_DIRS += "${bindir}/pgconfig"
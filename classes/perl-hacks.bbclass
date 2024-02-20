# Sourced from poky/meta/classes/perl-version.bbclass - MIT license

# We do not have perl in the purecap sysroot, thus we use hacks.

# Determine the staged version of perl from the perl configuration file
# Assign vardepvalue, because otherwise signature is changed before and after
# perl is built (from None to real version in config.sh).
get_perl_version[vardepvalue] = "${PERL_OWN_DIR}"
def get_perl_version(d):
    import re
    cfg = d.expand('${STAGING_DIR_HOST}/usr/lib${PERL_OWN_DIR}/perl5/config.sh')
    try:
        f = open(cfg, 'r')
    except IOError:
        return None
    l = f.readlines();
    f.close();
    r = re.compile(r"^version='(\d*\.\d*\.\d*)'")
    for s in l:
        m = r.match(s)
        if m:
            return m.group(1)
    return None

PERLVERSION := "${@get_perl_version(d)}"
PERLVERSION[vardepvalue] = ""


# Determine the staged arch of perl from the perl configuration file
# Assign vardepvalue, because otherwise signature is changed before and after
# perl is built (from None to real version in config.sh).
def get_perl_arch(d):
    import re
    cfg = d.expand('${STAGING_DIR_HOST}/usr/lib${PERL_OWN_DIR}/perl5/config.sh')
    try:
        f = open(cfg, 'r')
    except IOError:
        return None
    l = f.readlines();
    f.close();
    r = re.compile("^archname='([^']*)'")
    for s in l:
        m = r.match(s)
        if m:
            return m.group(1)
    return None

PERLARCH := "${@get_perl_arch(d)}"
PERLARCH[vardepvalue] = ""


STAGING_LIBDIR_HACK="${STAGING_DIR_HOST}/usr/lib"
STAGING_BASELIBDIR_HACK="${STAGING_DIR_HOST}/lib"
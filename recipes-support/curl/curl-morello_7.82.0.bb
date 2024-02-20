inherit autotools pkgconfig binconfig multilib_header
inherit purecap-sysroot

MORELLO_SRC = "poky/meta/recipes-support/curl/curl_7.82.0.bb"

SUMMARY = "Command line tool and library for client-side URL transfers"
DESCRIPTION = "It uses URL syntax to transfer data to and from servers. \
curl is a widely used because of its ability to be flexible and complete \
complex tasks. For example, you can use curl for things like user authentication, \
HTTP post, SSL connections, proxy support, FTP uploads, and more!"
HOMEPAGE = "https://curl.se/"
BUGTRACKER = "https://github.com/curl/curl/issues"
SECTION = "console/network"
LICENSE = "curl"
LIC_FILES_CHKSUM = "file://COPYING;md5=190c514872597083303371684954f238"

TOOLCHAIN  = "${MORELLO_TOOLCHAIN}"

SRC_URI = "https://curl.se/download/curl-${PV}.tar.xz \
           file://CVE-2022-22576.patch \
           file://CVE-2022-27775.patch \
           file://CVE-2022-27776.patch \
           file://CVE-2022-27774-1.patch \
           file://CVE-2022-27774-2.patch \
           file://CVE-2022-27774-3.patch \
           file://CVE-2022-27774-4.patch \
           file://CVE-2022-30115.patch \
           file://CVE-2022-27780.patch \
           file://CVE-2022-27781.patch \
           file://CVE-2022-27779.patch \
           file://CVE-2022-27782-1.patch \
           file://CVE-2022-27782-2.patch \
           file://0001-openssl-fix-CN-check-error-code.patch \
           file://CVE-2022-32205.patch \
           file://CVE-2022-32206.patch \
           file://CVE-2022-32207.patch \
           file://CVE-2022-32208.patch \
           file://CVE-2022-35252.patch \
           file://CVE-2022-32221.patch \
           file://CVE-2022-42916.patch \
           file://CVE-2022-42915.patch \
           file://CVE-2022-43551.patch \
           file://CVE-2022-43552.patch \
           file://CVE-2023-23914_5-1.patch \
           file://CVE-2023-23914_5-2.patch \
           file://CVE-2023-23914_5-3.patch \
           file://CVE-2023-23914_5-4.patch \
           file://CVE-2023-23914_5-5.patch \
           "
SRC_URI[sha256sum] = "0aaa12d7bd04b0966254f2703ce80dd5c38dbbd76af0297d3d690cdce58a583c"

S = "${WORKDIR}/curl-${PV}"

# Curl has used many names over the years...
CVE_PRODUCT = "haxx:curl haxx:libcurl curl:curl curl:libcurl libcurl:libcurl daniel_stenberg:curl"

# Entropy source for random PACKAGECONFIG option
RANDOM ?= "/dev/urandom"

PACKAGECONFIG = "openssl proxy random verbose zlib"

DEPENDS += "openssl-morello zlib-morello openldap-morello libidn2-morello"

# 'ares' and 'threaded-resolver' are mutually exclusive
# PACKAGECONFIG[brotli] = "--with-brotli,--without-brotli,brotli"
PACKAGECONFIG[builtinmanual] = "--enable-manual,--disable-manual"
PACKAGECONFIG[dict] = "--enable-dict,--disable-dict,"
PACKAGECONFIG[imap] = "--enable-imap,--disable-imap,"
PACKAGECONFIG[libidn] = "--with-libidn2,--without-libidn2,libidn2-morello"
PACKAGECONFIG[ipv6] = "--enable-ipv6,--disable-ipv6,"
PACKAGECONFIG[ldap] = "--enable-ldap,--disable-ldap,openldap-morello"
PACKAGECONFIG[ldaps] = "--enable-ldaps,--disable-ldaps,openldap-morello"
PACKAGECONFIG[mqtt] = "--enable-mqtt,--disable-mqtt,"
PACKAGECONFIG[openssl] = "--with-openssl,--without-openssl,openssl-morello"
PACKAGECONFIG[pop3] = "--enable-pop3,--disable-pop3,"
PACKAGECONFIG[proxy] = "--enable-proxy,--disable-proxy,"
PACKAGECONFIG[random] = "--with-random=${RANDOM},--without-random"
PACKAGECONFIG[smtp] = "--enable-smtp,--disable-smtp,"
PACKAGECONFIG[verbose] = "--enable-verbose,--disable-verbose"
PACKAGECONFIG[zlib] = "--with-zlib,--without-zlib,zlib-morello"

EXTRA_OECONF = " \
    --disable-manual \
    --enable-threaded-resolver \
    --disable-libcurl-option \
    --disable-ntlm-wb \
    --enable-crypto-auth \
    --with-ca-bundle=${sysconfdir}/ssl/certs/ca-certificates.crt \
    --without-libpsl \
    --enable-debug \
    --enable-optimize \
    --disable-curldebug \
"

do_install:append:class-target() {
  # cleanup buildpaths from curl-config
  sed -i \
      -e 's,--sysroot=${STAGING_DIR_TARGET},,g' \
      -e 's,--with-libtool-sysroot=${STAGING_DIR_TARGET},,g' \
      -e 's|${DEBUG_PREFIX_MAP}||g' \
      -e 's|${@" ".join(d.getVar("DEBUG_PREFIX_MAP").split())}||g' \
      ${D}${bindir}/curl-config
}

do_install:append() {
  ${READELF_COMMAND} ${D}${libdir}/libcurl.so >  ${D}${PURECAP_DEBUGDIR}/libcurl.so.readelf
}

PACKAGES =+ "lib${BPN}"

FILES:lib${BPN} = "${libdir}/lib*.so.*"
RRECOMMENDS:lib${BPN} += "ca-certificates"

FILES:${PN} += "${datadir}/zsh"

SYSROOT_DIRS += "${bindir}"
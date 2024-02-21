inherit core-image
inherit extrausers

SUMMARY = "Morello SDK demo image"
LICENSE = "MIT"

IMAGE_FSTYPES  += "wic tar.bz2"

IMAGE_LINGUAS = " en-us en-gb "
IMAGE_ROOTFS_SIZE ?= "8192"
IMAGE_ROOTFS_EXTRA_SPACE:append = "${@bb.utils.contains("DISTRO_FEATURES", "systemd", " + 4096", "", d)}"
IMAGE_OVERHEAD_FACTOR = "1.5"

IMAGE_INSTALL = "packagegroup-core-boot ${CORE_IMAGE_EXTRA_INSTALL}"

IMAGE_INSTALL:append = " \
                         tzdata \
                         localedef \
                         sudo \
                        "

IMAGE_INSTALL:append = " \
                         net-tools \
                         netplan \
                         iputils \
                         iproute2 \
                         openssh \
                         nginx \
                        "

IMAGE_INSTALL:append = " \
                         libpq \
                        "

IMAGE_INSTALL:append = " \
                         php \
                         php-cli \
                         php-dev \
                         php-fpm \
                       "

IMAGE_INSTALL:append = " \
                        zlib-morello \
                        ncurses-morello \
                        openssl-morello \
                        readline-morello \
                        zabbix-server-morello \
                        zabbix-agentd-morello \
                        zabbix-frontend \
                        net-snmp-morello \
                        net-snmp-morello-mibs \
                        net-snmp-morello-server-snmptrapd \
                        openldap-morello \
                        libevent-morello \
                        libpcre-morello \
                        postgresql-morello \
                        libpq-morello\
                        libpgtypes-morello \
                        postgresql-morello-contrib \
                        postgresql-morello-server-dev \
                        postgresql-morello-client \
                        postgresql-morello-timezone \
                        postgresql-morello-setup \
                        postgresql-morello-timezone \
                        postgresql-morello-dbg \
                        curl-morello \
                        libcurl-morello \
                        libidn2-morello \
                        libunistring-morello \
                        tcl-morello \
                        base-passwd-morello \
                        util-linux-morello \
                       "

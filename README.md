meta-morello-distro
===================

This layer provides user space libraries machine translated to `Morello`
and recompiled with `purecap` (c64) for the ARM Morello SDK.

It allows to run purecap and non purecap versions of the libraries
to be tested in parallel. Purecap libraries have a dedicated sysroot.

# Building images

```
$ kas build ./meta-morello-distro/kas/debug-soc.yml
```

# Limitations
The purecap libraries are tested to a limited degree.


Yocto-wise:
- package Q&A is disabled and runtime dependency checks
- ptest is disabled/ignored
- recipes and package config can be incomplete/broken

# Structure
Most of the recipes here are copies of existing recipes in other layers
postfixed with `morello`, source of the original recipe is listed
in the `MORELLO_SRC` variable, the original recipe path is preserved, ${PV}
is not. The git hashes of layers that were used as base of these recipes are
stored in `MORELLO_LAYER_SRC_REF` variable in the `layer.conf` file.

The reason for this is that the c64 versions of the libraries live in
parallel to the a64 on the system, which is a design choice, hence a
simple `*.bbappend` would not cut it.

The purecap libraries are placed in the purecap sysroot, as defined by
`${PURECAP_SYSROOT_DIR}` in meta-morello [1], which is where the
runtime linker shall look for all dependencies.

Most of the `CHERI` patches are placed in the...`cheri-patches` folder at the
root of the said recipe.

Some sources are pulled from `CHERI-BSD` repo and then patched further to
work on Linux. In some cases patches are taken from CHERI-BSD and rebased
on top of different versions of the said libraries. There are cases where
we were patching a library oblivious to the fact that a CHERI port already
exists. Some contributions were not found elsewhere.

If you see any errors or are the author of these patches/recipes and think
we have missed something (patch authors etc.) please reach out to us and
it shall be fixed !

# The current userland libraries consist of:
- postgresql 9.6
- zabbix 5.0.38 server/agent/proxy (with embedded JS)
- curl/openssl/zlib/ncurses/readline/ et al.

# Adding new recipes:
- in theory the dev process should be as simple as:
  - find existing recipe for the library you want
  - copy over its content into library-morello_X.Y.Z.bb
  - add:

  ```
  inherit pure-cap-kheaders purecap-sysroot
  ...
  TOOLCHAIN = "${MORELLO_TOOLCHAIN}"
  ```
  - compile
  - digest the logs
  - create patches and put them in the cheri-patches folder
  - repeat the last two steps until there are no errors
  - add to the image
  - watch it crash at runtime (FVP is reducing the length of the dev loop here)
  - run through gdb
  - create more cheri-patches

- if the are no obvious runtime errors you are done

We tend to stick inherit/requires at the top of the `*.bb` files for readability,
do the same where possible.

Contributing
------------

We accept patches through the mailing list only, the patch should be named [meta-morello-distro]
https://op-lists.linaro.org/mailman3/lists/linux-morello-distros.op-lists.linaro.org/


References
----------
[1] https://git.morello-project.org/morello/meta-morello

maintainers
-----------
* Pawel Zalewski <pzalewski@thegoodpenguin.co.uk>
* Harrison Carter <hcarter@thegoodpenguin.co.uk>


#!/usr/bin/env bash
# Point hev-socks5-tunnel's JNI_OnLoad at XioVPN's package/class.
#
# APP_CFLAGS in Application.mk is not always honoured by ndk-build, and a stale
# class path makes JNI_OnLoad return JNI_ERR (the TUN engine then silently fails
# to start), so patch the compile-time default in the source instead.
set -euo pipefail
SRC="${1:?usage: patch-hev-jni-class.sh <hev-socks5-tunnel source dir>}"
sed -i 's|#define PKGNAME hev/htproxy|#define PKGNAME com/xiovpn/mobile/engine/tor|' "$SRC/src/hev-jni.c"
sed -i 's|#define CLSNAME TProxyService|#define CLSNAME HevSocks5Tunnel|' "$SRC/src/hev-jni.c"
grep -n 'define PKGNAME\|define CLSNAME' "$SRC/src/hev-jni.c"

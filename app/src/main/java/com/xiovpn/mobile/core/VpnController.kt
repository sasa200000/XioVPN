package com.xiovpn.mobile.core

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.xiovpn.mobile.settings.NetworkGuardStore
import com.xiovpn.mobile.vpn.XioVpnService

object VpnController {
    fun start(context: Context) {
        val intent = Intent(context, XioVpnService::class.java).setAction(XioVpnService.ACTION_CONNECT)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, XioVpnService::class.java).setAction(XioVpnService.ACTION_DISCONNECT)
        context.startService(intent)
    }

    fun close(context: Context) {
        val intent = Intent(context, XioVpnService::class.java).setAction(XioVpnService.ACTION_CLOSE)
        context.startService(intent)
    }

    fun switchProfile(context: Context) {
        val intent = Intent(context, XioVpnService::class.java).setAction(XioVpnService.ACTION_SWITCH_PROFILE)
        context.startService(intent)
    }

    fun applyTorExit(context: Context) {
        val state = ConnectionStateStore.state.value
        if (state != ConnectionState.CONNECTED && state != ConnectionState.CONNECTING) return
        val intent = Intent(context, XioVpnService::class.java).setAction(XioVpnService.ACTION_APPLY_TOR_EXIT)
        context.startService(intent)
    }

    fun applyPowExit(context: Context) {
        val state = ConnectionStateStore.state.value
        if (state != ConnectionState.CONNECTED && state != ConnectionState.CONNECTING) return
        val intent = Intent(context, XioVpnService::class.java).setAction(XioVpnService.ACTION_APPLY_POW_EXIT)
        context.startService(intent)
    }

    fun applyNetworkGuard(context: Context) {
        val state = ConnectionStateStore.state.value
        val live = state == ConnectionState.CONNECTED ||
            state == ConnectionState.CONNECTING ||
            state == ConnectionState.DISCONNECTING ||
            NetworkGuardStore.blocking.value
        if (!live) return
        val intent = Intent(context, XioVpnService::class.java).setAction(XioVpnService.ACTION_APPLY_NETWORK_GUARD)
        context.startService(intent)
    }

    /** Rebuild routing so newly added/removed direct domains take effect without reconnect. */
    fun applyDirectWhitelist(context: Context) {
        if (ConnectionStateStore.state.value != ConnectionState.CONNECTED) return
        val intent = Intent(context, XioVpnService::class.java)
            .setAction(XioVpnService.ACTION_APPLY_DIRECT_WHITELIST)
        context.startService(intent)
    }
}

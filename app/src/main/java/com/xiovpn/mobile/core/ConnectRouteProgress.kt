package com.xiovpn.mobile.core

data class ConnectRouteProgress(
    val current: Int = 0,
    val total: Int = 0,
) {
    val isActive: Boolean get() = current > 0 && total > 0

    companion object {
        val Idle = ConnectRouteProgress()
    }
}

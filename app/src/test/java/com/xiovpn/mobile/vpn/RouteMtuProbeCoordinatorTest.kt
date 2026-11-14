package com.xiovpn.mobile.vpn

import com.xiovpn.mobile.profiles.RouteTransferProbeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteMtuProbeCoordinatorTest {
    @Test
    fun defaultProbeIncludesEverySocketBudget() {
        assertEquals(95_000L, nativeProbeTimeoutMs(RouteTransferProbeConfig()))
    }

    @Test
    fun championshipProbeGetsEnoughTimeForEveryRequest() {
        val config = RouteTransferProbeConfig(
            uploadBytes = 1_024 * 1_024,
            downloadBytes = 1_024 * 1_024,
            readTimeoutMs = 30_000,
        )

        assertTrue(nativeProbeTimeoutMs(config) >= 195_000L)
    }
}

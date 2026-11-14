package com.xiovpn.mobile.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class EnginePowIntroTest {
    @Test
    fun firstOpenAfterUpdateUsesPow() {
        assertEquals(EngineMode.XIO_POW, EnginePowIntro.resolve(EngineMode.XRAY_CF, introApplied = false))
        assertEquals(EngineMode.XIO_POW, EnginePowIntro.resolve(EngineMode.TOR_WEBTUNNEL, introApplied = false))
        assertEquals(EngineMode.XIO_POW, EnginePowIntro.resolve(EngineMode.XIO_POW, introApplied = false))
    }

    @Test
    fun laterOpensKeepUserChoice() {
        assertEquals(EngineMode.XRAY_CF, EnginePowIntro.resolve(EngineMode.XRAY_CF, introApplied = true))
        assertEquals(EngineMode.TOR_WEBTUNNEL, EnginePowIntro.resolve(EngineMode.TOR_WEBTUNNEL, introApplied = true))
        assertEquals(EngineMode.XIO_POW, EnginePowIntro.resolve(EngineMode.XIO_POW, introApplied = true))
    }
}

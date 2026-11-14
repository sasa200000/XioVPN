package com.xiovpn.mobile.ai

import com.xiovpn.mobile.mci.MciConfig
import com.xiovpn.mobile.mci.MciNativeXrayConfig
import com.xiovpn.mobile.profiles.ProxyProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCleanHopTest {
    private val plan = AiRoutePlan(
        profile = ProxyProfile.ADVANCED_SETTINGS_TEMPLATE,
        edge = MciConfig.PRIMARY_EDGE,
    )

    @Test
    fun nativeConfigIsUnchangedWithoutAPlan() {
        assertEquals(
            MciNativeXrayConfig.build(),
            MciNativeXrayConfig.build(aiRoute = null),
        )
    }

    @Test
    fun aiExitIsAdditiveAndKeepsEveryExistingRule() {
        val baseline = MciNativeXrayConfig.build()

        val withHop = MciNativeXrayConfig.build(aiRoute = plan)

        assertFalse(baseline.contains("ai-out"))
        assertTrue(withHop.contains("\"tag\":\"ai-out\""))
        // the exit reuses the proven TCP transport instead of relaying WireGuard UDP
        assertFalse(withHop.contains("wireguard"))
        assertFalse(withHop.contains("\"proxySettings\""))
        assertTrue(withHop.contains("domain:chatgpt.com"))
        assertTrue(withHop.contains("full:gemini.google.com"))
        // proxy must stay the first outbound so unmatched traffic keeps its default route
        assertTrue(withHop.indexOf("\"tag\":\"proxy\"") < withHop.indexOf("\"tag\":\"ai-out\""))
        // every rule that existed before is still present and the AI rule precedes the catch-all
        assertTrue(withHop.contains("\"outboundTag\":\"dns-out\""))
        assertTrue(withHop.contains("\"inboundTag\":[\"dns-query\"],\"outboundTag\":\"proxy\""))
        // nativeTun adds tun-in to the catch-all so the probe measures the same path the TUN uses
        assertTrue(withHop.contains("\"inboundTag\":[\"socks-in\",\"tun-in\"],\"network\":\"tcp,udp\",\"outboundTag\":\"probe-proxy\""))
        assertTrue(
            withHop.indexOf("\"outboundTag\":\"ai-out\"") <
                withHop.indexOf("\"inboundTag\":[\"socks-in\",\"tun-in\"],\"network\":\"tcp,udp\",\"outboundTag\":\"probe-proxy\""),
        )
    }

    @Test
    fun aiRuleReachesBothTunAndSocksSoTheProbeMeasuresTheSamePath() {
        val withHop = MciNativeXrayConfig.build(aiRoute = plan)

        val rule = withHop.lines().first { it.contains("\"outboundTag\":\"ai-out\"") }
        assertTrue(rule.contains("\"socks-in\""))
        assertTrue(rule.contains("\"tun-in\""))
    }

    @Test
    fun anUnusableExitIsIgnoredSoTheTunnelNeverBreaks() {
        val broken = plan.copy(domains = emptyList())

        assertFalse(broken.isUsable)
        assertEquals(
            MciNativeXrayConfig.build(),
            MciNativeXrayConfig.build(aiRoute = broken),
        )
    }

    @Test
    fun aiDomainsDoNotSwallowUnrelatedGoogleTraffic() {
        val rules = AiRouteDomains.RULES

        assertTrue(rules.none { it == "domain:google.com" || it == "domain:googleapis.com" })
        assertTrue(rules.all(String::isNotBlank))
        assertEquals(rules.distinct().size, rules.size)
    }
}

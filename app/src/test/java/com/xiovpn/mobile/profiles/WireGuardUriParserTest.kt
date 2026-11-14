package com.xiovpn.mobile.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A WireGuard link has to survive the round trip through the parser unchanged,
 * because the outbound builder reads its keys straight off the profile.
 */
class WireGuardUriParserTest {

    private val peerKey = "xTIBA5rboUvnH4htodjb6e697QjLERt1NAB4mZqp8Dg="
    private val privateKey = "yAnz5TF+lXXJte14tji3zlMNq+hd2rYUIgJBgB3fBmk="
    private val psk = "FpCyhws9cxwWoV4xELtfJvjJN+zQVRPISllRWgeopVE="

    private fun parse(line: String) = ProfileUriParser.parse(line)

    @Test
    fun `parses a full wireguard link`() {
        val profile = parse(
            "wireguard://$peerKey@engage.cloudflareclient.com:2408" +
                "?privateKey=$privateKey&presharedKey=$psk" +
                "&allowedIPs=0.0.0.0/0,::/0&keepAlive=25&name=WARP",
        )
        assertEquals(ProxyProtocol.WIREGUARD, profile.protocol)
        assertEquals("engage.cloudflareclient.com", profile.serverHost)
        assertEquals(2408, profile.serverPort)
        assertEquals(privateKey, profile.wgPrivateKey)
        assertEquals(psk, profile.wgPresharedKey)
        assertEquals(peerKey, profile.wgPeerPublicKey)
        assertEquals("0.0.0.0/0,::/0", profile.wgAllowedIps)
        assertEquals(25, profile.wgKeepAlive)
        assertEquals("WARP", profile.name)
    }

    @Test
    fun `accepts the wg shorthand scheme`() {
        val profile = parse("wg://$peerKey@1.2.3.4:51820?privateKey=$privateKey")
        assertEquals(ProxyProtocol.WIREGUARD, profile.protocol)
        assertEquals(51820, profile.serverPort)
    }

    @Test
    fun `defaults the port when it is omitted`() {
        val profile = parse("wireguard://$peerKey@1.2.3.4?privateKey=$privateKey")
        assertEquals(51820, profile.serverPort)
    }

    @Test
    fun `rejects a link with no private key`() {
        val failure = runCatching { parse("wireguard://$peerKey@1.2.3.4:51820") }
        assertTrue(failure.isFailure)
    }

    @Test
    fun `rejects a link with no peer key`() {
        val failure = runCatching { parse("wireguard://1.2.3.4:51820?privateKey=$privateKey") }
        assertTrue(failure.isFailure)
    }
}

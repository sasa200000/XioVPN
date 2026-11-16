package com.xiovpn.mobile.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An `ss://` link must survive parsing intact: the outbound builder reads
 * `method:password` straight off the profile credential, so a mis-split here
 * produces a config the core rejects at connect time, not at import time.
 */
class ShadowsocksUriParserTest {

    private fun b64(s: String): String = Base64Codec.encode(s.toByteArray(Charsets.UTF_8))

    private fun parse(line: String) = ProfileUriParser.parse(line)

    @Test
    fun `parses a SIP002 link`() {
        val profile = parse("ss://${b64("2022-blake3-aes-256-gcm:hunter2")}@example.com:8388#Tokyo")

        assertEquals(ProxyProtocol.SHADOWSOCKS, profile.protocol)
        assertEquals("example.com", profile.serverHost)
        assertEquals(8388, profile.serverPort)
        assertEquals("2022-blake3-aes-256-gcm:hunter2", profile.credential)
        assertEquals("2022-blake3-aes-256-gcm", profile.encryption)
        assertEquals("Tokyo", profile.name)
    }

    @Test
    fun `parses a legacy fully encoded link`() {
        val profile = parse("ss://${b64("aes-256-gcm:secret@1.2.3.4:8389")}")

        assertEquals(ProxyProtocol.SHADOWSOCKS, profile.protocol)
        assertEquals("1.2.3.4", profile.serverHost)
        assertEquals(8389, profile.serverPort)
        assertEquals("aes-256-gcm:secret", profile.credential)
    }

    @Test
    fun `tolerates unpadded base64 and url safe alphabet`() {
        // encode then strip padding, as many share links do
        val encoded = b64("chacha20-ietf-poly1305:p@ss:word").trimEnd('=')
        val profile = parse("ss://$encoded@host.example:443")

        assertEquals(ProxyProtocol.SHADOWSOCKS, profile.protocol)
        // password keeps its own colon: only the FIRST colon splits method
        assertEquals("chacha20-ietf-poly1305:p@ss:word", profile.credential)
    }

    @Test
    fun `drops an unsupported plugin query instead of failing`() {
        val profile = parse("ss://${b64("aes-128-gcm:pw")}@example.com:8388?plugin=obfs-local#n")

        assertEquals(ProxyProtocol.SHADOWSOCKS, profile.protocol)
        assertEquals(8388, profile.serverPort)
    }

    @Test
    fun `accepts an IPv6 literal host`() {
        val profile = parse("ss://${b64("aes-128-gcm:pw")}@[2001:db8::1]:8388")

        assertEquals("2001:db8::1", profile.serverHost)
        assertEquals(8388, profile.serverPort)
    }

    @Test
    fun `rejects a link with no method separator`() {
        assertTrue(runCatching { parse("ss://${b64("justapassword")}@h:1") }.isFailure)
    }

    @Test
    fun `rejects an out of range port`() {
        assertTrue(runCatching { parse("ss://${b64("aes-128-gcm:pw")}@h:99999") }.isFailure)
    }

    @Test
    fun `is picked up when pasting text with other links`() {
        val text = """
            ss://${b64("aes-256-gcm:pw")}@ss.example:8388#ss-node
            vless://30980fc4-8789-42df-80d1-0c8e5cd26881@origin.example:443#kept
        """.trimIndent()

        val found = ProfileUriParser.extractUris(text)
        assertEquals(2, found.size)
        assertTrue(found.any { it.startsWith("ss://") })
        assertTrue(found.any { it.startsWith("vless://") })
    }
}

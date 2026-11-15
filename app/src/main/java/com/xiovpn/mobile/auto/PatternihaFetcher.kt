package com.xiovpn.mobile.auto

import android.net.Network
import com.xiovpn.mobile.logging.AppLogRepository
import com.xiovpn.mobile.logging.LogSource
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Fetches config lists from several public sources with ETag, keeps the last good copy on disk,
 * and refreshes at most once a day. Only called during connect.
 *
 * Sources (FLUX-style multi-source, one broken list never blocks a connect):
 *  - Patterniha Free-Configs (Cloudflare-fronted; mirrors: raw GitHub, jsDelivr, contents API)
 *  - MatinGhanbari filtered v2ray lists (vless + trojan; REALITY/direct nodes for IP-filtered nets)
 */
class PatternihaFetcher(private val store: AutoConfigStore, private val network: Network?) {
    data class Result(val text: String, val source: String)

    suspend fun load(now: Long = System.currentTimeMillis()): Result? = withContext(Dispatchers.IO) {
        val cached = store.cachedList()
        if (cached != null && now - store.listFetchedAt() in 0..REFRESH_MS) return@withContext Result(cached, "cache")

        // Primary: Patterniha with its mirrors.
        var primary: Result? = null
        for (url in PatternihaList.MIRRORS) {
            coroutineContext.ensureActive()
            val fetched = runCatching { fetch(url, cached != null, now) }
                .onFailure { log("Patterniha list fetch failed via ${host(url)}: ${it.message}") }
                .getOrNull()
            if (fetched != null) { primary = fetched; break }
        }

        // Extra sources: fetched best-effort; failure of one never blocks the others.
        val extras = mutableListOf<String>()
        for (url in EXTRA_SOURCES) {
            coroutineContext.ensureActive()
            val fetched = runCatching { fetchExtra(url, now) }
                .onFailure { log("Extra source fetch failed via ${host(url)}: ${it.message}") }
                .getOrNull()
            if (fetched != null) extras += fetched
        }

        // Merge: primary first, extras interleaved, deduped. If primary failed but extras came,
        // the extras alone still give a connect.
        val merged = buildList {
            primary?.let { addAll(it.text.lines()) }
            extras.forEach { addAll(it.lines()) }
        }.map(String::trim).filter(String::isNotEmpty).distinct().joinToString("\n")

        return@withContext when {
            merged.isNotEmpty() -> {
                if (primary == null || extras.isNotEmpty()) {
                    // Save the merged list so the cached copy also carries the extras.
                    runCatching { store.saveList(merged, primary?.source ?: "multi", null, now) }
                }
                Result(merged, primary?.source ?: "multi (${extras.size} extra)")
            }
            cached != null -> Result(cached, "stale-cache")
            else -> null
        }
    }

    /** Best-effort fetch of one extra source, no ETag/mirror logic, capped size. */
    private fun fetchExtra(url: String, now: Long): String? {
        val connection = (network?.openConnection(URL(url)) ?: URL(url).openConnection()) as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "XioVPN-Android/Auto")
            connection.setRequestProperty("Accept", "text/plain,*/*")
            val code = connection.responseCode
            if (code !in 200..299) return null
            val text = connection.inputStream.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    check(out.size() + n <= MAX_BYTES) { "list too large" }
                    out.write(buffer, 0, n)
                }
                out.toString(Charsets.UTF_8.name())
            }
            val usable = text.lineSequence().count {
                it.trim().startsWith("vless://", true) || it.trim().startsWith("trojan://", true)
            }
            if (usable == 0) return null
            log("Extra source fetched via ${host(url)} ($usable configs)")
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun fetch(url: String, haveCache: Boolean, now: Long): Result? {
        val connection = (network?.openConnection(URL(url)) ?: URL(url).openConnection()) as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "XioVPN-Android/Auto")
            connection.setRequestProperty(
                "Accept",
                if (url == PatternihaList.API_URL) "application/vnd.github.raw" else "text/plain,*/*",
            )
            if (haveCache) store.listEtag(url)?.let { connection.setRequestProperty("If-None-Match", it) }
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_NOT_MODIFIED && haveCache) {
                store.touchList(now)
                log("Patterniha list unchanged (304) via ${host(url)}")
                return store.cachedList()?.let { Result(it, "304 ${host(url)}") }
            }
            check(code in 200..299) { "HTTP $code" }
            val text = connection.inputStream.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    check(out.size() + n <= MAX_BYTES) { "list too large" }
                    out.write(buffer, 0, n)
                }
                out.toString(Charsets.UTF_8.name())
            }
            check(PatternihaList.parse(text).isNotEmpty()) { "no usable configs in list" }
            store.saveList(text, url, connection.getHeaderField("ETag"), now)
            log("Patterniha list fetched via ${host(url)} (${text.length} chars)")
            return Result(text, host(url))
        } finally {
            connection.disconnect()
        }
    }

    private fun host(url: String) = url.substringAfter("//").substringBefore('/')

    private fun log(message: String) = runCatching { AppLogRepository.info(LogSource.ADAPTIVE, message) }

    companion object {
        /** FLUX-style extra public sources: REALITY/direct nodes (vless+trojan; hy2 unsupported by Xray). */
        val EXTRA_SOURCES = listOf(
            "https://raw.githubusercontent.com/MatinGhanbari/v2ray-configs/main/subscriptions/filtered/subs/vless.txt",
            "https://raw.githubusercontent.com/MatinGhanbari/v2ray-configs/main/subscriptions/filtered/subs/trojan.txt",
        )
        const val REFRESH_MS = 24L * 60 * 60 * 1000
        private const val CONNECT_TIMEOUT_MS = 6_000
        private const val READ_TIMEOUT_MS = 10_000
        private const val MAX_BYTES = 8 * 1024 * 1024
    }
}
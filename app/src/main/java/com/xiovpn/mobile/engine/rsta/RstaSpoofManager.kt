package com.xiovpn.mobile.engine.rsta

import android.content.Context
import android.util.Log
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * RSTA SNI front: a local TLS front that dials a Cloudflare edge under a forged
 * ClientHello name, carrying SNI-split / record-fragment / combined DPI-evasion
 * profiles. Native binary (librstaspoof.so, shipped in jniLibs) listens on
 * 127.0.0.1:40443 and behaves as a plain TCP proxy toward the configured edge.
 *
 * Ported from MLM VPN's engine. The levers:
 *  -method combined | fragment | fake_sni
 *  -fragment-strategy sni_split | half | chunk | record_frag
 *  -fragment-delay, -no-raw, -ttl-trick
 *
 * There is no single profile that works on every Iranian operator, so the list
 * is a ladder tried in order until one answers; the winner is remembered.
 */
object RstaSpoofManager {
    private const val TAG = "XIO-RSTA"
    const val LISTEN_HOST = "127.0.0.1"
    const val LISTEN_PORT = 40443

    data class SpoofProfile(
        val id: String,
        val method: String,
        val strategy: String? = null,
        val delaySec: Double? = null,
        val noRaw: Boolean = false,
        val ttlTrick: Boolean = false,
    ) {
        fun args(): List<String> = buildList {
            add("-method"); add(method)
            strategy?.let { add("-fragment-strategy"); add(it) }
            delaySec?.let { add("-fragment-delay"); add(it.toString()) }
            if (noRaw) add("-no-raw")
            if (ttlTrick) add("-ttl-trick")
        }

        fun describe(): String = buildString {
            append(method)
            strategy?.let { append('/').append(it) }
            delaySec?.let { append(" d=").append(it) }
            if (noRaw) append(" no-raw")
            if (ttlTrick) append(" ttl")
        }
    }

    val PROFILES = listOf(
        SpoofProfile("combined", "combined"),
        SpoofProfile("frag-chunk", "fragment", strategy = "chunk"),
        SpoofProfile("frag-record", "fragment", strategy = "record_frag", delaySec = 0.02),
        SpoofProfile("frag-half", "fragment", strategy = "half"),
        SpoofProfile("frag-sni-split", "fragment", strategy = "sni_split"),
        SpoofProfile("combined-noraw", "combined", noRaw = true),
        SpoofProfile("fake-sni-noraw", "fake_sni", noRaw = true),
    )
    const val DEFAULT_PROFILE = "combined"

    private val holders = AtomicInteger(0)
    private val lifecycleMutex = Mutex()
    private val _running = MutableStateFlow(false)
    val isRunningFlow: StateFlow<Boolean> = _running.asStateFlow()

    @Volatile private var process: Process? = null
    @Volatile private var lastConnectIp: String? = null
    @Volatile private var lastConnectPort: Int? = null
    @Volatile private var lastFakeSni: String? = null
    @Volatile private var lastProfile: String = DEFAULT_PROFILE

    fun available(context: Context): Boolean =
        File(context.applicationInfo.nativeLibraryDir, "librstaspoof.so").exists()

    fun acquire(context: Context): Boolean {
        holders.incrementAndGet()
        return ensureRunning(context)
    }

    fun release() {
        while (true) {
            val n = holders.get()
            if (n <= 0) return
            if (holders.compareAndSet(n, n - 1)) {
                if (n == 1) stop()
                return
            }
        }
    }

    fun restart(context: Context): Boolean = runBlocking {
        lifecycleMutex.withLock {
            if (holders.get() <= 0) return@withLock false
            val (ip, port, sni) = storedRoute(context)
            startLocked(context, ip, port, sni, storedProfile(context).id)
            _running.value
        }
    }

    fun stop() {
        runBlocking {
            lifecycleMutex.withLock {
                holders.set(0)
                stopLocked()
            }
        }
    }

    fun setRoute(context: Context, connectIp: String, connectPort: Int, fakeSni: String): Boolean {
        val changed = lastConnectIp != connectIp || lastConnectPort != connectPort || lastFakeSni != fakeSni
        lastConnectIp = connectIp
        lastConnectPort = connectPort
        lastFakeSni = fakeSni
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("rsta_connect_ip", connectIp)
            .putInt("rsta_connect_port", connectPort)
            .putString("rsta_fake_sni", fakeSni)
            .apply()
        return changed
    }

    fun setProfile(context: Context, id: String): Boolean {
        val next = profileOf(id).id
        val changed = lastProfile != next
        lastProfile = next
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("rsta_profile", next)
            .apply()
        return changed
    }

    fun storedProfile(context: Context): SpoofProfile = profileOf(
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("rsta_profile", lastProfile)
    )

    fun storedRoute(context: Context): Triple<String, Int, String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Triple(
            lastConnectIp ?: prefs.getString("rsta_connect_ip", null) ?: DEFAULT_EDGE_IP,
            lastConnectPort ?: prefs.getInt("rsta_connect_port", 443),
            lastFakeSni ?: prefs.getString("rsta_fake_sni", null) ?: DEFAULT_FAKE_SNI,
        )
    }

    /** Default Cloudflare edge + a name the edge accepts on its TLS front. */
    const val DEFAULT_EDGE_IP = "188.114.98.0"
    const val DEFAULT_FAKE_SNI = "security.vercel.com"

    private const val PREFS = "xio_rsta"

    private fun profileOf(id: String?): SpoofProfile =
        PROFILES.firstOrNull { it.id == id?.trim() } ?: PROFILES.first()

    private fun startLocked(context: Context, connectIp: String, connectPort: Int, fakeSni: String, profileId: String) {
        val profile = profileOf(profileId)
        stopLocked()

        val destFile = File(context.applicationInfo.nativeLibraryDir, "librstaspoof.so")
        if (!destFile.exists()) {
            Log.e(TAG, "FATAL: librstaspoof.so not found at " + destFile.absolutePath)
            return
        }

        try {
            val logFile = File(context.filesDir, "rsta.log")
            logFile.writeText("[" + System.currentTimeMillis() + "] RSTA front start\n")
            logFile.appendText("connect=$connectIp:$connectPort sni=$fakeSni via " + profile.describe() + "\n")

            val args = listOf(
                destFile.absolutePath,
                "-listen", "$LISTEN_HOST:$LISTEN_PORT",
                "-connect", "$connectIp:$connectPort",
                "-sni", fakeSni,
            ) + profile.args()

            val pb = ProcessBuilder(args)
            pb.redirectErrorStream(true)
            val proc = pb.start()
            process = proc

            lastConnectIp = connectIp
            lastConnectPort = connectPort
            lastFakeSni = fakeSni
            lastProfile = profile.id

            Thread.sleep(200)
            val alive = try { proc.exitValue(); false } catch (_: IllegalThreadStateException) { true }
            if (!alive) {
                Log.e(TAG, "Process died immediately with code " + proc.exitValue())
                _running.value = false
                return
            }
            val portOpen = isPortOpen(LISTEN_HOST, LISTEN_PORT, 1000)
            if (!portOpen) Log.w(TAG, "Port $LISTEN_PORT not listening after start")

            _running.value = true
            Log.i(TAG, "RSTA front started on $LISTEN_HOST:$LISTEN_PORT via " + profile.describe())

            Thread {
                try {
                    proc.inputStream?.bufferedReader()?.useLines { lines ->
                        lines.forEach { line ->
                            try { logFile.appendText("$line\n") } catch (_: Exception) {}
                        }
                    }
                } catch (_: java.io.InterruptedIOException) {
                } catch (e: Exception) {
                    Log.e(TAG, "log reader", e)
                } finally {
                    val code = try { proc.waitFor() } catch (_: Exception) { null }
                    if (process === proc) _running.value = false
                    Log.i(TAG, "RSTA front exited code=$code")
                }
            }.start()
        } catch (e: Exception) {
            Log.e(TAG, "failed to start rstaspoof", e)
            _running.value = false
        }
    }

    private fun stopLocked() {
        try {
            process?.destroy()
            process = null
            _running.value = false
        } catch (e: Exception) {
            Log.e(TAG, "stop", e)
        }
    }

    private fun ensureRunning(context: Context): Boolean = runBlocking {
        lifecycleMutex.withLock {
            if (_running.value && isProcessAlive() && isPortOpen(LISTEN_HOST, LISTEN_PORT, 500)) return@withLock true
            val (ip, port, sni) = storedRoute(context)
            startLocked(context, ip, port, sni, storedProfile(context).id)
            _running.value
        }
    }

    fun isPortOpen(host: String, port: Int, timeoutMs: Int): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(host, port), timeoutMs); true }
    } catch (_: Exception) { false }

    fun isProcessAlive(): Boolean = try {
        process?.exitValue(); false
    } catch (_: IllegalThreadStateException) { true } catch (_: Exception) { false }
}

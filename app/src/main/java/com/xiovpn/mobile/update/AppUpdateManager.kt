package com.xiovpn.mobile.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.xiovpn.mobile.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

internal data class AppRelease(
    val version: String,
    val tagName: String,
    val title: String,
    val notes: String,
    val apkName: String,
    val apkUrl: String,
    val releaseUrl: String,
)

internal sealed interface UpdateCheckResult {
    data object Current : UpdateCheckResult
    data class Available(val release: AppRelease) : UpdateCheckResult
}

internal sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val release: AppRelease) : UpdateUiState
    data class Downloading(val release: AppRelease, val progress: Int?) : UpdateUiState
    data class Ready(
        val release: AppRelease,
        val message: String,
        /** Absolute path of the downloaded APK, or "" when unknown. */
        val apkPath: String = "",
        /** True when the user must allow "install unknown apps" first. */
        val needsPermission: Boolean = false,
        /** True when the installed app is signed with a different key. */
        val needsReinstall: Boolean = false,
    ) : UpdateUiState
    data class Error(val message: String, val release: AppRelease? = null) : UpdateUiState
}

internal class AppUpdateManager(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        val connection = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "XioVPN-Android/${BuildConfig.VERSION_NAME}")
        }
        try {
            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) {
                error("The update service returned HTTP $status")
            }
            val release = parseRelease(
                connection.inputStream.bufferedReader().use { it.readText() },
                resolveInstalledApkVariant(),
            )
            if (isVersionNewer(release.version, BuildConfig.VERSION_NAME)) {
                UpdateCheckResult.Available(release)
            } else {
                UpdateCheckResult.Current
            }
        } finally {
            connection.disconnect()
        }
    }

    /** Downloads the release APK and returns its absolute path. */
    suspend fun downloadAndInstall(
        release: AppRelease,
        onProgress: (Int?) -> Unit,
    ): String {
        require(Uri.parse(release.apkUrl).scheme.equals("https", ignoreCase = true)) {
            "The release APK does not use HTTPS"
        }
        val downloadManager = appContext.getSystemService(DownloadManager::class.java)
            ?: error("Android Download Manager is unavailable")
        val safeVersion = release.version.replace(Regex("[^0-9A-Za-z._-]"), "_")
        val fileName = "XioVPN-$safeVersion-${System.currentTimeMillis()}.apk"
        val request = DownloadManager.Request(Uri.parse(release.apkUrl))
            .setTitle("XioVPN ${release.version}")
            .setDescription("Downloading the verified GitHub release")
            .setMimeType(APK_MIME_TYPE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, fileName)

        // Where DownloadManager will put the file; needed later for the install
        // intent, which can only read it through our FileProvider.
        val savePath = java.io.File(
            appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            fileName,
        ).absolutePath
        val downloadId = downloadManager.enqueue(request)
        withContext(Dispatchers.Main.immediate) { onProgress(0) }

        var lastProgress: Int? = -1
        while (true) {
            val snapshot = queryDownload(downloadManager, downloadId)
            when (snapshot.status) {
                DownloadManager.STATUS_SUCCESSFUL -> return savePath
                DownloadManager.STATUS_FAILED -> error("Download failed (code ${snapshot.reason})")
                DownloadManager.STATUS_PENDING,
                DownloadManager.STATUS_PAUSED,
                DownloadManager.STATUS_RUNNING -> {
                    if (snapshot.progress != lastProgress) {
                        lastProgress = snapshot.progress
                        withContext(Dispatchers.Main.immediate) { onProgress(snapshot.progress) }
                    }
                }
                else -> error("Download stopped unexpectedly")
            }
            delay(DOWNLOAD_POLL_MS)
        }
    }

    /**
     * Prompts the system installer for an APK this app downloaded.
     *
     * Returns false when Android is not allowed to install from this app yet
     * (the user must grant "install unknown apps" first), so the caller can
     * explain that instead of silently doing nothing.
     */
    fun launchInstaller(apkPath: String): Boolean {
        val file = java.io.File(apkPath)
        if (!file.isFile || !apkPath.endsWith(".apk", ignoreCase = true)) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !appContext.packageManager.canRequestPackageInstalls()
        ) {
            return false
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.updatefileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            appContext.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    /**
     * True when the downloaded APK is signed with a different key than the
     * installed app. Android refuses such an update ("App not installed") no
     * matter what we do, so the user has to uninstall the old copy once.
     */
    fun signatureMismatch(apkPath: String): Boolean {
        val archive = appContext.packageManager.getPackageArchiveInfo(
            apkPath,
            android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES,
        ) ?: return false
        val incoming = archive.signingInfo?.apkContentsSigners?.map { it.toCharsString() }.orEmpty()
        val installed = runCatching {
            val info = appContext.packageManager.getPackageInfo(
                appContext.packageName,
                android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES,
            )
            info.signingInfo?.apkContentsSigners?.map { it.toCharsString() }.orEmpty()
        }.getOrDefault(emptyList())
        if (incoming.isEmpty() || installed.isEmpty()) return false
        return incoming.sorted() != installed.sorted()
    }

    /** Opens the system "uninstall this app" confirmation. */
    fun requestUninstallSelf(): Boolean = runCatching {
        appContext.startActivity(
            Intent(Intent.ACTION_DELETE, Uri.parse("package:${appContext.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    }.getOrDefault(false)

    /** True when the OS still needs the user to allow installs from this app. */
    fun needsInstallPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !appContext.packageManager.canRequestPackageInstalls()

    /** Settings screen where the user grants "install unknown apps". */
    fun installPermissionIntent(): Intent =
        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${appContext.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun resolveInstalledApkVariant(): String {
        if (BuildConfig.TV_MODE) return APK_VARIANT_TV
        val apkPath = appContext.applicationInfo.sourceDir
        val packed = runCatching { abisPackedInApk(apkPath) }.getOrDefault(emptySet())
        if (packed.isNotEmpty()) {
            val variant = variantFromPackedAbis(packed, Build.SUPPORTED_ABIS, tvMode = false)
            preferences.edit().putString(KEY_INSTALLED_APK_VARIANT, variant).apply()
            return variant
        }
        return preferences.getString(KEY_INSTALLED_APK_VARIANT, null)
            ?: variantFromPackedAbis(emptySet(), Build.SUPPORTED_ABIS, tvMode = false)
    }

    private fun parseRelease(jsonText: String, installedVariant: String): AppRelease {
        val root = JSONObject(jsonText)
        val tagName = root.getString("tag_name")
        val version = extractVersion(tagName)
            ?: extractVersion(root.optString("name"))
            ?: error("The latest release has no readable version")
        val assets = root.getJSONArray("assets")
        val apkAssets = buildList {
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                val name = asset.optString("name")
                if (!name.endsWith(".apk", ignoreCase = true)) continue
                add(
                    ReleaseApkAsset(
                        name = name,
                        url = asset.getString("browser_download_url"),
                    ),
                )
            }
        }
        val apk = selectReleaseApk(apkAssets, installedVariant)
            ?: error("Release $tagName does not include a signed APK for $installedVariant")
        return AppRelease(
            version = version,
            tagName = tagName,
            title = root.optString("name").ifBlank { "XioVPN $version" },
            notes = root.optString("body"),
            apkName = apk.name,
            apkUrl = apk.url,
            releaseUrl = root.getString("html_url"),
        )
    }

    private data class DownloadSnapshot(val status: Int, val reason: Int, val progress: Int?)

    private fun queryDownload(downloadManager: DownloadManager, downloadId: Long): DownloadSnapshot {
        downloadManager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
            check(cursor != null && cursor.moveToFirst()) { "Downloaded file is no longer available" }
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            val received = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val progress = if (total > 0L) ((received * 100L) / total).toInt().coerceIn(0, 100) else null
            return DownloadSnapshot(status, reason, progress)
        }
    }

    companion object {
        /** Update channel: our own worker, so the app never contacts GitHub. */
        internal const val UPDATE_BASE = "https://xiovpn-bot.nimasivi.workers.dev"
        internal const val REPOSITORY_URL = "https://t.me/mohmdnini"
        internal const val RELEASES_URL = "https://t.me/mohmdnini"
        private const val LATEST_RELEASE_API = "$UPDATE_BASE/update"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val PREFERENCES_NAME = "app_update_state"
        private const val KEY_INSTALLED_APK_VARIANT = "installed_apk_variant"
        private const val NETWORK_TIMEOUT_MS = 12_000
        private const val DOWNLOAD_POLL_MS = 450L

    }
}

internal fun isVersionNewer(candidate: String, current: String): Boolean {
    val candidateParts = versionParts(candidate)
    val currentParts = versionParts(current)
    if (candidateParts.isEmpty() || currentParts.isEmpty()) return false
    for (index in 0 until max(candidateParts.size, currentParts.size)) {
        val candidatePart = candidateParts.getOrElse(index) { 0 }
        val currentPart = currentParts.getOrElse(index) { 0 }
        if (candidatePart != currentPart) return candidatePart > currentPart
    }
    return false
}

private fun versionParts(raw: String): List<Int> =
    extractVersion(raw)?.split('.')?.mapNotNull(String::toIntOrNull).orEmpty()

private fun extractVersion(raw: String): String? =
    Regex("\\d+(?:\\.\\d+)+").find(raw)?.value

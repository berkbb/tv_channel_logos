package com.berkbb.channellogos.ui

import com.berkbb.channellogos.logos.USER_AGENT
import com.berkbb.channellogos.logos.httpGet
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object AppUpdateChecker {
    private const val REPO = "berkbb/tv_channel_logos"
    private const val RELEASE_API_URL = "https://api.github.com/repos/$REPO/releases/latest"
    /** Only an asset address under this prefix (the release's `url` field) is used as the fallback download. */
    private const val ASSET_API_PREFIX = "https://api.github.com/repos/$REPO/releases/assets/"

    data class CheckResult(
        val latestVersion: String,
        val htmlUrl: String,
        val hasUpdate: Boolean,
        /** The release APK for this TV, or null when the release has none to install. */
        val apk: ApkAsset? = null
    )

    /**
     * [apiUrl] is the same asset on api.github.com, tried when the github.com download fails: on
     * 2026-11-08 one GitHub region answered 504 for every release download while the API kept working.
     */
    data class ApkAsset(val name: String, val url: String, val size: Long, val sha256: String?, val apiUrl: String? = null)

    /** Per-ABI APK names from older releases; current releases (see package.sh) ship only the universal APK. */
    private val ABI_SUFFIXES = mapOf("arm64-v8a" to "arm64", "armeabi-v7a" to "arm32", "x86_64" to "x64")

    /** GitHub answers 404: no published release, or the repository is not public. */
    class ReleasesUnavailable : Exception("No public release")

    fun check(currentVersionName: String): CheckResult {
        val jsonBytes = try {
            httpGet(RELEASE_API_URL)
        } catch (e: IllegalStateException) {
            if (e.message?.startsWith("HTTP 404") == true) throw ReleasesUnavailable()
            throw e
        }
        return parseRelease(jsonBytes.toString(Charsets.UTF_8), currentVersionName, android.os.Build.SUPPORTED_ABIS)
    }

    /** The latest-release reply as a [CheckResult]; apart from the network so it can be tested. */
    internal fun parseRelease(releaseJson: String, currentVersionName: String, supportedAbis: Array<String>): CheckResult {
        val json = JSONObject(releaseJson)
        val rawTag = json.optString("tag_name", "").trim()
        val latestVersion = rawTag.removePrefix("v").trim()
        if (latestVersion.isBlank()) throw ReleasesUnavailable()
        val htmlUrl = json.optString("html_url", "")
        val hasUpdate = isNewerVersion(latestVersion, currentVersionName)
        val assets = json.optJSONArray("assets")
        val apks = (0 until (assets?.length() ?: 0)).map { i ->
            val asset = assets!!.getJSONObject(i)
            ApkAsset(
                name = asset.optString("name", ""),
                url = asset.optString("browser_download_url", ""),
                size = asset.optLong("size", 0L),
                sha256 = parseSha256(if (asset.isNull("digest")) null else asset.optString("digest", "")),
                apiUrl = parseApiUrl(asset.optString("url", ""))
            )
        }
        return CheckResult(latestVersion, htmlUrl, hasUpdate, selectApk(apks, supportedAbis))
    }

    /** The asset's API address, kept only when it belongs to this app's repository on api.github.com. */
    internal fun parseApiUrl(url: String?): String? =
        url?.trim()?.takeIf { it.startsWith(ASSET_API_PREFIX) && it.length > ASSET_API_PREFIX.length }

    /**
     * Streams [apk] into a .part file and keeps it only when its SHA-256 (or size) matches the release.
     * When the github.com address fails with an I/O error or a 5xx answer and the release gave the
     * asset's api.github.com address, the same file is fetched once more from there; the check runs
     * on whichever download succeeded. [open] exists so tests can stand in for the network.
     */
    internal fun downloadApk(
        apk: ApkAsset,
        target: File,
        open: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
        onProgress: (Int) -> Unit
    ): Boolean {
        val part = File(target.parentFile, target.name + ".part")
        try {
            val first = runCatching { fetch(apk.url, null, apk.size, part, onProgress, open) }
            val fetched = when {
                first.getOrNull()?.code == 200 -> first.getOrThrow()
                apk.apiUrl != null && first.isRetryable() -> fetch(apk.apiUrl, "application/octet-stream", apk.size, part, onProgress, open)
                else -> first.getOrThrow()
            }
            if (fetched.code != 200) return false
            val valid = if (apk.sha256 != null) fetched.sha256 == apk.sha256 else apk.size > 0 && fetched.length == apk.size
            if (!valid) return false
            target.delete()
            return part.renameTo(target)
        } finally {
            if (part.exists()) part.delete()
        }
    }

    /** What one attempt produced: the HTTP status and, on 200, the digest and length of the bytes written. */
    private class Fetched(val code: Int, val sha256: String = "", val length: Long = 0L)

    /** A broken connection or a server-side error (500 and above) is worth a second address; 404 is not. */
    private fun Result<Fetched>.isRetryable(): Boolean =
        exceptionOrNull() is IOException || (getOrNull()?.code ?: 0) >= 500

    /** Fetches [url] into [part] (truncated first); the body is read only on 200. */
    private fun fetch(
        url: String,
        accept: String?,
        expectedSize: Long,
        part: File,
        onProgress: (Int) -> Unit,
        open: (URL) -> HttpURLConnection
    ): Fetched {
        val connection = open(URL(url)).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
            if (accept != null) {
                setRequestProperty("Accept", accept)
                setRequestProperty("User-Agent", USER_AGENT)
            }
        }
        try {
            val code = connection.responseCode
            if (code != 200) return Fetched(code)
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: expectedSize
            val digest = MessageDigest.getInstance("SHA-256")
            var done = 0L
            var last = -1
            connection.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        done += read
                        val pct = if (total > 0) ((done * 100) / total).toInt().coerceIn(0, 100) else 0
                        if (pct != last) { last = pct; onProgress(pct) }
                    }
                }
            }
            return Fetched(code, digest.digest().joinToString("") { "%02x".format(it) }, done)
        } finally {
            connection.disconnect()
        }
    }

    /** The APK for the first supported ABI the release ships, else the universal APK, else null. */
    internal fun selectApk(assets: List<ApkAsset>, supportedAbis: Array<String>): ApkAsset? {
        val apks = assets.filter { it.name.endsWith("-release.apk") && it.url.isNotBlank() }
        return supportedAbis.asSequence()
            .mapNotNull { ABI_SUFFIXES[it] }
            .mapNotNull { suffix -> apks.firstOrNull { it.name.endsWith("-$suffix-release.apk") } }
            .firstOrNull()
            ?: apks.firstOrNull { it.name.endsWith("-universal-release.apk") }
    }

    /** "sha256:abc…" (GitHub's asset digest) as lowercase hex, or null. */
    internal fun parseSha256(digest: String?): String? {
        val value = digest?.trim()?.lowercase() ?: return null
        val hex = value.removePrefix("sha256:")
        return if (value.startsWith("sha256:") && hex.length == 64 && hex.all { it in "0123456789abcdef" }) hex else null
    }

    internal fun isNewerVersion(latest: String, current: String): Boolean {
        if (latest.isBlank() || current.isBlank()) return false
        val latestParts = latest.split('.').mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }
        val currentParts = current.split('.').mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }
        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}

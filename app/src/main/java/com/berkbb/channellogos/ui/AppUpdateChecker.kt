package com.berkbb.channellogos.ui

import com.berkbb.channellogos.logos.httpGet
import org.json.JSONObject

object AppUpdateChecker {
    private const val RELEASE_API_URL = "https://api.github.com/repos/berkbb/tv_channel_logos/releases/latest"

    data class CheckResult(
        val latestVersion: String,
        val htmlUrl: String,
        val hasUpdate: Boolean,
        /** The release APK for this TV, or null when the release has none to install. */
        val apk: ApkAsset? = null
    )

    data class ApkAsset(val name: String, val url: String, val size: Long, val sha256: String?)

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
        val json = JSONObject(jsonBytes.toString(Charsets.UTF_8))
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
                sha256 = parseSha256(if (asset.isNull("digest")) null else asset.optString("digest", ""))
            )
        }
        return CheckResult(latestVersion, htmlUrl, hasUpdate, selectApk(apks, android.os.Build.SUPPORTED_ABIS))
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

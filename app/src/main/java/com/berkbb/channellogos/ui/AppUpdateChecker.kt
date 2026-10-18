package com.berkbb.channellogos.ui

import com.berkbb.channellogos.logos.httpGet
import org.json.JSONObject

object AppUpdateChecker {
    private const val RELEASE_API_URL = "https://api.github.com/repos/berkbb/tv_channel_logos/releases/latest"

    data class CheckResult(
        val latestVersion: String,
        val htmlUrl: String,
        val hasUpdate: Boolean
    )

    fun check(currentVersionName: String): CheckResult {
        val jsonBytes = httpGet(RELEASE_API_URL)
        val json = JSONObject(jsonBytes.toString(Charsets.UTF_8))
        val rawTag = json.optString("tag_name", "").trim()
        val latestVersion = rawTag.removePrefix("v").trim()
        val htmlUrl = json.optString("html_url", "")
        val hasUpdate = isNewerVersion(latestVersion, currentVersionName)
        return CheckResult(latestVersion, htmlUrl, hasUpdate)
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

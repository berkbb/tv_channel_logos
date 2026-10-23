package com.berkbb.channellogos

import com.berkbb.channellogos.ui.AppUpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun isNewerVersionComparesCorrectly() {
        assertTrue(AppUpdateChecker.isNewerVersion("1.3.1", "1.3.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.4.0", "1.3.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("2.0.0", "1.3.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.10.0", "1.9.0"))

        assertFalse(AppUpdateChecker.isNewerVersion("1.3.0", "1.3.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("1.2.9", "1.3.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("1.2.0", "1.3.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("", "1.3.0"))
    }

    private val assets = listOf("arm32", "arm64", "universal", "x64").map {
        AppUpdateChecker.ApkAsset("channel-logos-v1.4.3-$it-release.apk", "https://example.com/$it.apk", 7_500_000, null)
    } + AppUpdateChecker.ApkAsset("SHA256SUMS", "https://example.com/sums", 400, null)

    @Test
    fun selectsTheApkForTheTvAbi() {
        assertEquals("https://example.com/arm32.apk", AppUpdateChecker.selectApk(assets, arrayOf("armeabi-v7a", "armeabi"))?.url)
        assertEquals("https://example.com/arm64.apk", AppUpdateChecker.selectApk(assets, arrayOf("arm64-v8a", "armeabi-v7a"))?.url)
        assertEquals("https://example.com/universal.apk", AppUpdateChecker.selectApk(assets, arrayOf("x86"))?.url)
        assertNull(AppUpdateChecker.selectApk(assets.takeLast(1), arrayOf("armeabi-v7a")))
    }

    @Test
    fun universalOnlyReleaseServesEveryAbi() {
        // Current releases ship only the universal APK (see package.sh)
        val universalOnly = assets.filter { "-arm" !in it.name && "-x64" !in it.name }
        for (abis in listOf(arrayOf("armeabi-v7a", "armeabi"), arrayOf("arm64-v8a", "armeabi-v7a"), arrayOf("x86_64"))) {
            assertEquals("https://example.com/universal.apk", AppUpdateChecker.selectApk(universalOnly, abis)?.url)
        }
    }

    @Test
    fun parsesGitHubDigests() {
        val hex = "a".repeat(64)
        assertEquals(hex, AppUpdateChecker.parseSha256("sha256:$hex"))
        assertNull(AppUpdateChecker.parseSha256("md5:abc"))
        assertNull(AppUpdateChecker.parseSha256(null))
    }
}

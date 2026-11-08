package com.berkbb.channellogos

import com.berkbb.channellogos.ui.AppUpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class AppUpdateCheckerTest {

    @get:Rule
    val folder = TemporaryFolder()

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

    private val apiUrl = "https://api.github.com/repos/berkbb/tv_channel_logos/releases/assets/4242"

    @Test
    fun parsesTheApiAssetAddressNextToTheDownloadAddress() {
        val hex = "b".repeat(64)
        val json = """{"tag_name":"v1.4.6","html_url":"https://github.com/berkbb/tv_channel_logos/releases/tag/v1.4.6","assets":[
            {"name":"channel-logos-v1.4.6-universal-release.apk","url":"$apiUrl","browser_download_url":"https://github.com/berkbb/tv_channel_logos/releases/download/v1.4.6/channel-logos-v1.4.6-universal-release.apk","size":7500000,"digest":"sha256:$hex"},
            {"name":"SHA256SUMS","url":"https://api.github.com/repos/berkbb/tv_channel_logos/releases/assets/4243","browser_download_url":"https://github.com/x/SHA256SUMS","size":100}]}"""
        val result = AppUpdateChecker.parseRelease(json, "1.4.5", arrayOf("arm64-v8a"))
        assertTrue(result.hasUpdate)
        assertEquals("1.4.6", result.latestVersion)
        assertEquals(
            AppUpdateChecker.ApkAsset(
                "channel-logos-v1.4.6-universal-release.apk",
                "https://github.com/berkbb/tv_channel_logos/releases/download/v1.4.6/channel-logos-v1.4.6-universal-release.apk",
                7_500_000, hex, apiUrl
            ),
            result.apk
        )
    }

    @Test
    fun dropsApiAddressesOfOtherRepositoriesHostsOrSchemes() {
        assertEquals(apiUrl, AppUpdateChecker.parseApiUrl(apiUrl))
        assertNull(AppUpdateChecker.parseApiUrl("https://api.github.com/repos/someone/tv_channel_logos/releases/assets/1"))
        assertNull(AppUpdateChecker.parseApiUrl("http://api.github.com/repos/berkbb/tv_channel_logos/releases/assets/1"))
        assertNull(AppUpdateChecker.parseApiUrl("https://api.example.com/repos/berkbb/tv_channel_logos/releases/assets/1"))
        assertNull(AppUpdateChecker.parseApiUrl("https://api.github.com/repos/berkbb/tv_channel_logos/releases/assets/"))
        assertNull(AppUpdateChecker.parseApiUrl(""))
        assertNull(AppUpdateChecker.parseApiUrl(null))
        val json = """{"tag_name":"v9.0.0","assets":[{"name":"a-universal-release.apk","url":"https://api.github.com/repos/someone/else/releases/assets/1","browser_download_url":"https://x/u.apk","size":1}]}"""
        assertNull(AppUpdateChecker.parseRelease(json, "1.4.5", arrayOf("arm64-v8a")).apk?.apiUrl)
    }

    // --- download fallback, without the network ---

    private val bytes = "channel logos release apk".toByteArray()
    private val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private val pageUrl = "https://github.com/berkbb/tv_channel_logos/releases/download/v1.4.6/channel-logos-v1.4.6-universal-release.apk"

    /** One canned answer: an HTTP status with a body, or an I/O failure when [code] is null. */
    private class Answer(val code: Int?, val body: ByteArray = ByteArray(0))

    private class FakeConnection(url: URL, private val answer: Answer) : HttpURLConnection(url) {
        override fun connect() {}
        override fun disconnect() {}
        override fun usingProxy(): Boolean = false
        override fun getResponseCode(): Int = answer.code ?: throw IOException("connection reset")
        override fun getContentLengthLong(): Long = answer.body.size.toLong()
        override fun getInputStream(): InputStream = ByteArrayInputStream(answer.body)
    }

    private class Network(private val answers: Map<String, Answer>) {
        val opened = mutableListOf<FakeConnection>()
        val open: (URL) -> HttpURLConnection = { url -> FakeConnection(url, answers.getValue(url.toString())).also { opened += it } }
        val urls get() = opened.map { it.url.toString() }
        fun header(index: Int, name: String) = opened[index].getRequestProperty(name)
    }

    private fun apk(apiUrl: String?) = AppUpdateChecker.ApkAsset("channel-logos-v1.4.6-universal-release.apk", pageUrl, bytes.size.toLong(), sha, apiUrl)

    private fun download(apk: AppUpdateChecker.ApkAsset, network: Network): Pair<Boolean, File> {
        val target = folder.newFolder().resolve(apk.name)
        return AppUpdateChecker.downloadApk(apk, target, network.open) {} to target
    }

    @Test
    fun aServerErrorOnGithubFallsBackToTheApiAssetAddress() {
        val network = Network(mapOf(pageUrl to Answer(504), apiUrl to Answer(200, bytes)))
        val (ok, target) = download(apk(apiUrl), network)
        assertTrue(ok)
        assertEquals(listOf(pageUrl, apiUrl), network.urls)
        assertNull(network.header(0, "Accept"))
        assertEquals("application/octet-stream", network.header(1, "Accept"))
        assertEquals("tv-channel-logos", network.header(1, "User-Agent"))
        assertTrue(bytes.contentEquals(target.readBytes()))
        assertFalse(File(target.parentFile, target.name + ".part").exists())
    }

    @Test
    fun aBrokenConnectionAlsoFallsBack() {
        val network = Network(mapOf(pageUrl to Answer(null), apiUrl to Answer(200, bytes)))
        assertTrue(download(apk(apiUrl), network).first)
        assertEquals(listOf(pageUrl, apiUrl), network.urls)
    }

    @Test
    fun aMissingFileIsNotRetriedAndNeitherIsAReleaseWithoutAnApiAddress() {
        val notFound = Network(mapOf(pageUrl to Answer(404), apiUrl to Answer(200, bytes)))
        assertFalse(download(apk(apiUrl), notFound).first)
        assertEquals(listOf(pageUrl), notFound.urls)

        val noApi = Network(mapOf(pageUrl to Answer(503)))
        assertFalse(download(apk(null), noApi).first)
        assertEquals(listOf(pageUrl), noApi.urls)
    }

    @Test
    fun theFallbackStillHasToMatchTheDigest() {
        val network = Network(mapOf(pageUrl to Answer(502), apiUrl to Answer(200, "something else".toByteArray())))
        val (ok, target) = download(apk(apiUrl), network)
        assertFalse(ok)
        assertFalse(target.exists())
        assertFalse(File(target.parentFile, target.name + ".part").exists())
    }

    @Test
    fun theFirstAddressIsEnoughWhenItWorks() {
        val network = Network(mapOf(pageUrl to Answer(200, bytes)))
        val (ok, target) = download(apk(apiUrl), network)
        assertTrue(ok)
        assertEquals(listOf(pageUrl), network.urls)
        assertTrue(bytes.contentEquals(target.readBytes()))
    }

    @Test
    fun parsesGitHubDigests() {
        val hex = "a".repeat(64)
        assertEquals(hex, AppUpdateChecker.parseSha256("sha256:$hex"))
        assertNull(AppUpdateChecker.parseSha256("md5:abc"))
        assertNull(AppUpdateChecker.parseSha256(null))
    }
}

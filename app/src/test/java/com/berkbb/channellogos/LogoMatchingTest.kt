package com.berkbb.channellogos

import com.berkbb.channellogos.logos.LogoEntry
import com.berkbb.channellogos.logos.LogoEntry.Companion.COUNTRY
import com.berkbb.channellogos.logos.LogoEntry.Companion.INTERNATIONAL
import com.berkbb.channellogos.logos.LogoEntry.Companion.OTHER
import com.berkbb.channellogos.logos.LogoMatching
import com.berkbb.channellogos.logos.LogoMatching.Catalogs
import com.berkbb.channellogos.logos.LogoSource
import com.berkbb.channellogos.logos.NameMatcher.Strength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogoMatchingTest {

    // Shaped like the real tables: the TV's keys from its logo database, GitHub's from file names,
    // iptv-org's from channel names with links.
    private val catalogs = Catalogs(
        tv = mapOf(
            "atvhd" to LogoEntry(5209, COUNTRY), "atv" to LogoEntry(5145, COUNTRY),
            "trt1hd" to LogoEntry(5221, COUNTRY), "trt1" to LogoEntry(5226, COUNTRY),
            "startv" to LogoEntry(5196, COUNTRY), "showtv" to LogoEntry(5184, COUNTRY),
            // The international table's "ShowHD" is Discovery Showcase HD, not Turkey's Show TV
            "showhd" to LogoEntry(5306, INTERNATIONAL),
            "ntvmir" to LogoEntry(7001, OTHER)
        ),
        gitHub = mapOf(
            "atv" to LogoEntry("turkey/atv-tr.png", COUNTRY),
            "startv" to LogoEntry("turkey/star-tv-tr.png", COUNTRY),
            "trt2" to LogoEntry("turkey/trt-2-tr.png", COUNTRY),
            "trtspor" to LogoEntry("turkey/trt-spor-tr.png", COUNTRY),
            "epicdrama" to LogoEntry("united-kingdom/epic-drama-uk.png", INTERNATIONAL)
        ),
        iptv = mapOf(
            "tivibuspor2" to LogoEntry("https://i.imgur.com/tivibu2.png", COUNTRY),
            "atv" to LogoEntry("https://i.imgur.com/atv.png", COUNTRY)
        )
    )

    @Test
    fun exactTvLogoWins() {
        val best = LogoMatching.best("ATV HD", catalogs)!!
        assertEquals(LogoSource.TV, best.source)
        assertEquals("tv:5209", best.id)
        assertEquals(Strength.EXACT, best.strength)
    }

    @Test
    fun tvLogoBeatsGitHubAndIptvAtTheSameCloseness() {
        assertEquals("tv:5196", LogoMatching.best("STAR TV HD", catalogs)!!.id)
        assertEquals("tv:5145", LogoMatching.best("ATV", catalogs)!!.id)
    }

    @Test
    fun ownCountryBeatsAnExactForeignName() {
        // "SHOW HD" must get Turkey's Show TV logo, not the international Discovery Showcase HD
        val best = LogoMatching.best("SHOW HD", catalogs)!!
        assertEquals("tv:5184", best.id)
        assertEquals(Strength.LOOSE, best.strength)
    }

    @Test
    fun otherCatalogsFillTheGaps() {
        assertEquals("github:turkey/trt-2-tr.png", LogoMatching.best("TRT 2 HD", catalogs)!!.id)
        assertEquals("github:united-kingdom/epic-drama-uk.png", LogoMatching.best("EPIC DRAMA", catalogs)!!.id)
        assertEquals("tv:7001", LogoMatching.best("NTV MIR", catalogs)!!.id)
        assertEquals("iptv:https://i.imgur.com/tivibu2.png", LogoMatching.best("TİVİBU SPOR 2 HD TİCARİ", catalogs)!!.id)
    }

    @Test
    fun theUsersOwnFileWinsAndIsSafe() {
        val withFile = Catalogs(
            files = mapOf("atvhd" to LogoEntry("/storage/USB/logos/ATV HD.png", COUNTRY)),
            tv = catalogs.tv, gitHub = catalogs.gitHub, iptv = catalogs.iptv
        )
        val best = LogoMatching.best("ATV HD", withFile)!!
        assertEquals("file:/storage/USB/logos/ATV HD.png", best.id)
        assertTrue(LogoMatching.isSafe(best))
        // A similarly named file is offered by hand, not chosen
        assertEquals("tv:5196", LogoMatching.best("STAR TV HD", withFile)!!.id)
    }

    @Test
    fun noMatch() {
        assertNull(LogoMatching.best("TARIM ORMAN EKRANI HD", catalogs))
    }

    @Test
    fun onlyCloseMatchesFromTheTvsCountryInCuratedSourcesAreSafe() {
        assertTrue(LogoMatching.isSafe(LogoMatching.best("ATV HD", catalogs)!!))      // TV, exact
        assertTrue(LogoMatching.isSafe(LogoMatching.best("STAR TV HD", catalogs)!!))  // TV, without HD
        assertTrue(LogoMatching.isSafe(LogoMatching.best("TRT 2 HD", catalogs)!!))    // GitHub Turkey
        assertFalse(LogoMatching.isSafe(LogoMatching.best("SHOW HD", catalogs)!!))    // similar name
        assertFalse(LogoMatching.isSafe(LogoMatching.best("EPIC DRAMA", catalogs)!!)) // foreign folder
        assertFalse(LogoMatching.isSafe(LogoMatching.best("NTV MIR", catalogs)!!))    // foreign table
        assertFalse(LogoMatching.isSafe(LogoMatching.best("TİVİBU SPOR 2", catalogs)!!)) // iptv-org link
        assertFalse(LogoMatching.isSafe(LogoMatching.textLogo("TARAFTAR")))           // drawn
    }

    @Test
    fun candidatesListRelatedNamesClosestFirstAndEndWithADrawnLogo() {
        // "TRT HD" has no exact logo; logos named like it are offered, the closest names first
        val trt = LogoMatching.candidates("TRT HD", catalogs).map { it.id }
        assertEquals(
            listOf("tv:5226", "github:turkey/trt-2-tr.png", "tv:5221", "github:turkey/trt-spor-tr.png", "text:TRT HD"),
            trt
        )
        assertEquals(
            listOf("github:turkey/trt-spor-tr.png", "text:TRT SPOR YILDIZ"),
            LogoMatching.candidates("TRT SPOR YILDIZ", catalogs).map { it.id }
        )
    }

    @Test
    fun candidatesPutTheBestMatchFirst() {
        assertEquals("tv:5209", LogoMatching.candidates("ATV HD", catalogs).first().id)
        assertEquals("tv:5184", LogoMatching.candidates("SHOW HD", catalogs).first().id)
    }

    // picons: Türksat services ("SID_TSID_ONID") and service names from every satellite
    private val withPicons = Catalogs(
        tv = catalogs.tv, gitHub = catalogs.gitHub, iptv = catalogs.iptv,
        picons = mapOf(
            "tarimormanekrani" to LogoEntry("tarimormanekrani", INTERNATIONAL),
            "showtv" to LogoEntry("showtv", INTERNATIONAL)
        ),
        piconServices = mapOf("2BD_8_1" to "tarimormanekrani", "1A_2_1" to "kanal7")
    )

    @Test
    fun aPiconLinkedToTheChannelsServiceIsSafe() {
        val best = LogoMatching.best("TARIM ORMAN EKRANI HD", withPicons, serviceKey = "2BD_8_1")!!
        assertEquals("picons:tarimormanekrani", best.id)
        assertEquals(COUNTRY, best.tier)
        assertTrue(LogoMatching.isSafe(best))
    }

    @Test
    fun aPiconMatchedOnlyByNameIsAFallback() {
        // Without service IDs the name index finds it, but names from other satellites are not trusted
        val best = LogoMatching.best("TARIM ORMAN EKRANI HD", withPicons)!!
        assertEquals("picons:tarimormanekrani", best.id)
        assertFalse(LogoMatching.isSafe(best))
        // The TV's own Turkish logo still wins over a picon found by name
        assertEquals("tv:5184", LogoMatching.best("SHOW TV", withPicons)!!.id)
    }

    @Test
    fun aServicePiconWithAnUnrelatedNameIsOnlyOffered() {
        // The service now carries another channel than the one picons knows it for
        assertEquals("tv:5209", LogoMatching.best("ATV HD", withPicons, serviceKey = "1A_2_1")!!.id)
        val picon = LogoMatching.candidates("ATV HD", withPicons, serviceKey = "1A_2_1").single { it.id == "picons:kanal7" }
        assertFalse(LogoMatching.isSafe(picon))
    }

    @Test
    fun searchFindsDirectAndSubstringMatchesAcrossCatalogs() {
        val results = LogoMatching.search("spor", catalogs)
        assertTrue(results.any { it.name.contains("spor", ignoreCase = true) || it.id.contains("spor") })
        val trtResults = LogoMatching.search("trt", catalogs)
        assertTrue(trtResults.any { it.name.contains("trt", ignoreCase = true) || it.id.contains("trt") })
    }
}

package com.babadogan.channellogos

import com.babadogan.channellogos.logos.NameMatcher
import com.babadogan.channellogos.logos.NameMatcher.Strength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NameMatcherTest {

    @Test
    fun keysIgnoreCaseSpacesPunctuationAndTurkishLetters() {
        assertEquals("startvhd", NameMatcher.key("STAR TV HD"))
        assertEquals("trt1hd", NameMatcher.key("TRT-1 HD"))
        assertEquals("haberturkhd", NameMatcher.key("HABERTÜRK HD"))
        assertEquals("akittvhd", NameMatcher.key("AKİT TV HD"))
        assertEquals("bikanal", NameMatcher.key("Bİ KANAL"))
        assertEquals("diyanettvhd", NameMatcher.key("DİYANET TV HD"))
        assertEquals("cnbce", NameMatcher.key("CNBC-e"))
        assertEquals("tv85hd", NameMatcher.key("TV8.5 HD"))
        assertEquals("beinhandehd", NameMatcher.key("beIN H&E HD"))
    }

    @Test
    fun channelKeysTryQualityAndTvVariantsInOrder() {
        assertEquals(
            listOf("showhd" to Strength.EXACT, "show" to Strength.WITHOUT_QUALITY, "showtv" to Strength.LOOSE),
            NameMatcher.channelKeys("SHOW HD")
        )
        assertEquals(
            listOf("startvhd" to Strength.EXACT, "startv" to Strength.WITHOUT_QUALITY, "star" to Strength.LOOSE),
            NameMatcher.channelKeys("STAR TV HD")
        )
        assertEquals(listOf("tgrt" to Strength.EXACT, "tgrttv" to Strength.LOOSE), NameMatcher.channelKeys("TGRT"))
        assertEquals(emptyList<Pair<String, Strength>>(), NameMatcher.channelKeys("  -  "))
    }

    @Test
    fun shortNamesKeepTheirQualityLetters() {
        // "hd" alone, or "tv" alone, must not be stripped down to nothing
        assertNull(NameMatcher.withoutQuality("hd"))
        assertNull(NameMatcher.withoutQuality("ahd"))   // a one-letter base is not a name
        assertEquals("a2", NameMatcher.withoutQuality("a2hd"))
        assertEquals(listOf("tv" to Strength.EXACT, "tvtv" to Strength.LOOSE), NameMatcher.channelKeys("TV"))
    }

    @Test
    fun commercialFeedsUseTheChannelsLogo() {
        assertEquals("beinsports1", NameMatcher.channelKeys("TİCARİ beIN SPORTS 1").first().first)
        assertEquals("tivibuspor1hd", NameMatcher.channelKeys("TİVİBU SPOR 1 HD TİCARİ").first().first)
        // only the whole word: a name containing the letters is untouched
        assertEquals("ticaritv", NameMatcher.key("TICARITV"))
        assertEquals("ticaritv", NameMatcher.channelKeys("TICARITV").first().first)
    }

    @Test
    fun logosAreFiledWithAndWithoutTheirQualityMarker() {
        assertEquals(listOf("beinmoviespremierehd", "beinmoviespremiere"), NameMatcher.logoKeys("bein-movies-premiere-hd"))
        assertEquals(listOf("atv"), NameMatcher.logoKeys("ATV"))
    }

    @Test
    fun gitHubFileNames() {
        assertEquals("startv", NameMatcher.gitHubKey("star-tv-tr.png", "tr"))
        assertEquals("trt1hd", NameMatcher.gitHubKey("trt-1-hd-tr.png", "tr"))
        assertNull(NameMatcher.gitHubKey("bein-hd-hz-tr.png", "tr"))
        assertNull(NameMatcher.gitHubKey("0_all_logos_mosaic.md", "tr"))
    }
}

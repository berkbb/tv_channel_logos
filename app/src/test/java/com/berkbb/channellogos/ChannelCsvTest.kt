package com.berkbb.channellogos

import com.berkbb.channellogos.logos.LogoCandidate
import com.berkbb.channellogos.logos.LogoSource
import com.berkbb.channellogos.logos.NameMatcher
import com.berkbb.channellogos.tv.Channel
import com.berkbb.channellogos.ui.ChannelCsv
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelCsvTest {

    private fun channel(id: Long, name: String, hasLogo: Boolean = false, isRadio: Boolean = false) =
        Channel(id, id.toString(), name, "input", hasLogo, isRadio)

    @Test
    fun headerThenOneQuotedRowPerChannel() {
        val suggestion = LogoCandidate(LogoSource.GITHUB, "trt-1-tr", NameMatcher.Strength.EXACT, "github:turkey/trt-1-tr.png")
        val csv = ChannelCsv.build(
            listOf(channel(1, "TRT 1 HD"), channel(2, "Radyo 1", hasLogo = true, isRadio = true)),
            mapOf(1L to suggestion)
        )
        assertEquals(
            ChannelCsv.HEADER + "\n" +
                "\"1\",\"TRT 1 HD\",\"TV\",\"No\",\"trt-1-tr\",\"GITHUB\",\"EXACT\"\n" +
                "\"2\",\"Radyo 1\",\"Radio\",\"Yes\",\"\",\"\",\"\"\n",
            csv
        )
    }

    @Test
    fun quotesInsideCellsAreDoubled() {
        assertEquals("\"Kanal \"\"D\"\"\"", ChannelCsv.cell("Kanal \"D\""))
        assertEquals("\"a, b\"", ChannelCsv.cell("a, b"))
        assertEquals("\"\"", ChannelCsv.cell(""))
    }

    @Test
    fun formulasAreKeptAsText() {
        assertEquals("\"'=HYPERLINK(\"\"http://x\"\")\"", ChannelCsv.cell("=HYPERLINK(\"http://x\")"))
        assertEquals("\"'+1\"", ChannelCsv.cell("+1"))
        assertEquals("\"'-2\"", ChannelCsv.cell("-2"))
        assertEquals("\"'@SUM(A1)\"", ChannelCsv.cell("@SUM(A1)"))
        assertEquals("\"'\tTab\"", ChannelCsv.cell("\tTab"))
        // Only a leading formula character counts
        assertEquals("\"TV8 +1\"", ChannelCsv.cell("TV8 +1"))
        assertEquals("\"a=b\"", ChannelCsv.cell("a=b"))
    }

    @Test
    fun broadcasterNamesCannotInjectIntoTheRow() {
        val csv = ChannelCsv.build(listOf(channel(7, "=cmd|' /C calc'!A0")), emptyMap())
        assertEquals("\"7\",\"'=cmd|' /C calc'!A0\",\"TV\",\"No\",\"\",\"\",\"\"", csv.lines()[1])
    }
}

package com.berkbb.channellogos

import com.berkbb.channellogos.logos.PiconsSource
import com.berkbb.channellogos.tv.serviceKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PiconsSourceTest {

    @Test
    fun indexLinesAreKeyValuePairs() {
        assertEquals(
            listOf("1D5_B_1_130000" to "novipazar", "trt1" to "trt1"),
            PiconsSource.parseIndex(listOf("1D5_B_1_130000=novipazar", "", "# comment", "broken", "trt1=trt1"))
        )
    }

    @Test
    fun servicesIgnoreTheSatelliteUnlessTheyDisagree() {
        val references = listOf(
            "2BD_8_1_1A40000" to "tarimormanekrani", // Türksat only
            "1_1_1_1A40000" to "trt1",               // the same IDs on Türksat and Hotbird…
            "1_1_1_130000" to "rai1",
            "2_1_1_130000" to "rai2",                // …and on two other satellites
            "2_1_1_C00000" to "ard",
            "3_1_1_130000" to "zdf",                 // the same logo everywhere
            "3_1_1_C00000" to "zdf"
        )
        assertEquals(
            mapOf("2BD_8_1" to "tarimormanekrani", "1_1_1" to "trt1", "3_1_1" to "zdf"),
            PiconsSource.servicesFor(references, preferredNamespaces = listOf("1A40000"))
        )
        // Without a preferred satellite a disputed service is left out
        assertNull(PiconsSource.servicesFor(references, preferredNamespaces = emptyList())["1_1_1"])
    }

    @Test
    fun serviceKeysAreUppercaseHexLikePiconsReferences() {
        assertEquals("2BD_8_1", serviceKey(onid = "1", tsid = "8", sid = "701"))
        assertNull(serviceKey(onid = "0", tsid = "0", sid = "0"))    // analogue channel
        assertNull(serviceKey(onid = null, tsid = null, sid = null)) // older TvLogoTool output
    }
}

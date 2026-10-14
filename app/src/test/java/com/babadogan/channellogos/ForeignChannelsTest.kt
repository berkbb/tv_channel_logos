package com.babadogan.channellogos

import com.babadogan.channellogos.logos.ForeignChannels
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForeignChannelsTest {

    private val romanian = setOf("protv", "antena1", "digi24", "tvr1", "kanald")
    private val turkish = setOf("trt1", "atv", "kanald", "showtv")

    @Test
    fun threeRomanianChannelsAddRomania() {
        val channels = listOf("TRT 1 HD", "PRO TV HD", "Antena 1", "Digi 24 HD", "ATV")
        assertTrue(ForeignChannels.carries(channels, romanian, turkish))
    }

    @Test
    fun namesBothCountriesShareDoNotCount() {
        // "Kanal D" is Turkish and Romanian; two Romanian-only names are not enough.
        val channels = listOf("Kanal D", "KANAL D HD", "PRO TV", "TVR 1", "Show TV")
        assertFalse(ForeignChannels.carries(channels, romanian, turkish))
    }

    @Test
    fun aPlainTurkishListAddsNothing() {
        assertFalse(ForeignChannels.carries(listOf("TRT 1", "ATV", "Show TV"), romanian, turkish))
    }
}

package com.babadogan.channellogos

import com.babadogan.channellogos.tv.Channel
import com.babadogan.channellogos.ui.ChannelSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelSearchTest {

    private fun channel(number: String, name: String) = Channel(number.toLong(), number, name, "input", hasLogo = false)

    @Test
    fun emptyQueryMatchesEverything() {
        assertTrue(ChannelSearch.matches(channel("1", "TRT 1 HD"), ""))
        assertTrue(ChannelSearch.matches(channel("1", "TRT 1 HD"), "   "))
    }

    @Test
    fun ignoresCaseAccentsAndTurkishI() {
        assertTrue(ChannelSearch.matches(channel("5", "CNN TÜRK"), "cnn turk"))
        assertTrue(ChannelSearch.matches(channel("7", "TV8 İNT"), "int"))
        assertTrue(ChannelSearch.matches(channel("9", "Digi Sport 1"), "DİGİ"))
    }

    @Test
    fun everyWordMustAppear() {
        val trt = channel("1", "TRT 1 HD")
        assertTrue(ChannelSearch.matches(trt, "trt hd"))
        assertFalse(ChannelSearch.matches(trt, "trt spor"))
    }

    @Test
    fun digitsAlsoMatchChannelNumbers() {
        assertTrue(ChannelSearch.matches(channel("105", "Show TV"), "10"))
        assertFalse(ChannelSearch.matches(channel("205", "Show TV"), "10"))
        assertTrue(ChannelSearch.matches(channel("205", "Kanal 10"), "10"))
    }

    @Test
    fun filterKeepsOrder() {
        val list = listOf(channel("1", "TRT 1"), channel("2", "Show TV"), channel("3", "TRT Spor"))
        assertEquals(listOf("TRT 1", "TRT Spor"), ChannelSearch.filter(list, "trt").map { it.name })
    }
}

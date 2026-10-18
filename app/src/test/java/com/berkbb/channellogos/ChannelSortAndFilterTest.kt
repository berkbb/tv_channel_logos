package com.berkbb.channellogos

import com.berkbb.channellogos.tv.Channel
import com.berkbb.channellogos.ui.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelSortAndFilterTest {

    private fun ch(id: Long, number: String, name: String, hasLogo: Boolean = false, isRadio: Boolean = false) =
        Channel(id, number, name, "input", hasLogo = hasLogo, isRadio = isRadio)

    @Test
    fun mediaFilterSeparatesTvAndRadio() {
        val tv1 = ch(1, "1", "TRT 1", isRadio = false)
        val tv2 = ch(2, "2", "ATV", isRadio = false)
        val radio1 = ch(3, "101", "TRT FM", isRadio = true)
        val radio2 = ch(4, "102", "Kral FM", isRadio = true)
        val all = listOf(tv1, tv2, radio1, radio2)

        val stateAll = MainViewModel.UiState(channels = all, showAll = true, mediaFilter = MainViewModel.MediaFilter.ALL)
        assertEquals(listOf(1L, 2L, 3L, 4L), stateAll.visibleChannels.map { it.id })

        val stateTv = MainViewModel.UiState(channels = all, showAll = true, mediaFilter = MainViewModel.MediaFilter.TV_ONLY)
        assertEquals(listOf(1L, 2L), stateTv.visibleChannels.map { it.id })

        val stateRadio = MainViewModel.UiState(channels = all, showAll = true, mediaFilter = MainViewModel.MediaFilter.RADIO_ONLY)
        assertEquals(listOf(3L, 4L), stateRadio.visibleChannels.map { it.id })
    }

    @Test
    fun sortModesWorkAsExpected() {
        val ch1 = ch(1, "10", "Show TV", hasLogo = true)
        val ch2 = ch(2, "2", "ATV", hasLogo = false)
        val ch3 = ch(3, "1", "TRT 1", hasLogo = true)
        val all = listOf(ch1, ch2, ch3)

        // NUMBER sort preserves original broadcast list order
        val stateNumber = MainViewModel.UiState(channels = all, showAll = true, sortMode = MainViewModel.ChannelSort.NUMBER)
        assertEquals(listOf("Show TV", "ATV", "TRT 1"), stateNumber.visibleChannels.map { it.name })

        // NAME sort orders alphabetically
        val stateName = MainViewModel.UiState(channels = all, showAll = true, sortMode = MainViewModel.ChannelSort.NAME)
        assertEquals(listOf("ATV", "Show TV", "TRT 1"), stateName.visibleChannels.map { it.name })

        // MISSING_FIRST sort places channels without logo first
        val stateMissing = MainViewModel.UiState(channels = all, showAll = true, sortMode = MainViewModel.ChannelSort.MISSING_FIRST)
        assertEquals(listOf("ATV", "TRT 1", "Show TV"), stateMissing.visibleChannels.map { it.name })
    }

    @Test
    fun settingsSerializesPiconsStyleAndRestartLiveTv() {
        val settings = com.berkbb.channellogos.settings.Settings(
            piconsStyle = com.berkbb.channellogos.settings.Settings.PICONS_STYLE_LIGHT,
            restartLiveTv = false
        )
        val map = settings.toMap()
        assertEquals("light", map["picons_style"])
        assertEquals("false", map["restart_live_tv"])

        val loaded = com.berkbb.channellogos.settings.Settings.fromMap(map)
        assertEquals(com.berkbb.channellogos.settings.Settings.PICONS_STYLE_LIGHT, loaded.piconsStyle)
        assertEquals(false, loaded.restartLiveTv)
    }

    @Test
    fun dvbServiceKeyFormatting() {
        // Türksat 42E ATV HD: onid=1, tsid=8, sid=701 (0x2BD)
        val key = com.berkbb.channellogos.tv.serviceKey("1", "8", "701")
        assertEquals("2BD_8_1", key)
    }
}

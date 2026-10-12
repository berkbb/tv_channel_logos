package com.babadogan.channellogos

import com.babadogan.channellogos.backup.LogoBackups
import com.babadogan.channellogos.backup.LogoBackups.SnapshotEntry
import com.babadogan.channellogos.tv.Channel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class LogoBackupsTest {

    private fun channel(id: Long, number: String, name: String) = Channel(id, number, name, "tuner", hasLogo = false)
    private fun entry(number: String, name: String) = SnapshotEntry(number, name, File("$number-$name.png"))

    @Test
    fun restoreMatchesByNumberAndNameAfterTheIdsChanged() {
        // A reinstallation gave the channels new ids; numbers and names survive
        val channels = listOf(channel(901, "1", "TRT 1 HD"), channel(902, "2", "ATV HD"), channel(903, "3", "KANAL D HD"))
        val matched = LogoBackups.match(listOf(entry("2", "ATV HD"), entry("1", "TRT 1 HD")), channels)
        assertEquals(mapOf(902L to "2", 901L to "1"), matched.map { (c, e) -> c.id to e.number }.toMap())
    }

    @Test
    fun aRenumberedChannelIsFoundByAUniqueName() {
        val channels = listOf(channel(1, "7", "KANAL 7 HD"), channel(2, "40", "TV 2 HD"))
        val matched = LogoBackups.match(listOf(entry("5", "KANAL 7 HD")), channels)
        assertEquals(listOf(1L), matched.keys.map { it.id })
    }

    @Test
    fun anAmbiguousNameWithoutTheSameNumberIsLeftOut() {
        // Two "TNT INT" channels; the backup's number fits neither
        val channels = listOf(channel(1, "560", "TNT INT"), channel(2, "561", "TNT INT"))
        assertEquals(emptyMap<Channel, SnapshotEntry>(), LogoBackups.match(listOf(entry("999", "TNT INT")), channels))
        assertEquals(listOf(2L), LogoBackups.match(listOf(entry("561", "TNT INT")), channels).keys.map { it.id })
    }
}

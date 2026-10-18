package com.berkbb.channellogos

import com.berkbb.channellogos.backup.LogoBackups
import com.berkbb.channellogos.backup.LogoBackups.SnapshotEntry
import com.berkbb.channellogos.tv.Channel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class LogoBackupsTest {

    private fun channel(id: Long, number: String, name: String) = Channel(id, number, name, "tuner", hasLogo = false)
    private fun entry(number: String, name: String) = SnapshotEntry(number, name, File("$number-$name.png"))

    @Test
    fun aSnapshotCopiesOnlyItsManifestAndLogos() {
        val root = kotlin.io.path.createTempDirectory().toFile()
        try {
            val from = File(root, "from").apply { mkdirs() }
            File(from, "1.png").writeText("one")
            File(from, "2.png").writeText("two")
            File(from, "notes.txt").writeText("not part of the backup")
            File(from, "manifest.tsv").writeText("1\tTRT 1\t1.png\n2\tATV\t2.png\n3\tEvil\t../escape.png\n")
            val to = File(root, "to")
            LogoBackups.copySnapshot(from, to, mapOf("created" to "1"))
            assertEquals(setOf("1.png", "2.png", "manifest.tsv", "backup.properties", "checksums.sha256"), to.list()!!.toSet())
            assertEquals("two", File(to, "2.png").readText())
            assert(LogoBackups.isSnapshot(to))
            assertEquals(2, LogoBackups.readManifest(to).size) // the "../escape.png" line is dropped
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun checksumsCatchDamagedLogosAndDamagedManifests() {
        val root = kotlin.io.path.createTempDirectory().toFile()
        try {
            val backup = File(root, "backup").apply { mkdirs() }
            File(backup, "1.png").writeText("one")
            File(backup, "2.png").writeText("two")
            File(backup, "manifest.tsv").writeText("1\tTRT 1\t1.png\n2\tATV\t2.png\n")
            LogoBackups.writeChecksums(backup)
            assertEquals(emptySet<String>(), LogoBackups.verify(backup).bad)

            // A damaged logo is reported and left out of a copy
            File(backup, "2.png").writeText("tw0")
            val verification = LogoBackups.verify(backup)
            assert(verification.checked && verification.manifestOk)
            assertEquals(setOf("2.png"), verification.bad)
            val copy = File(root, "copy")
            LogoBackups.copySnapshot(backup, copy)
            assertEquals(listOf("1.png"), LogoBackups.readManifest(copy).map { it.file.name })
            assertEquals(emptySet<String>(), LogoBackups.verify(copy).bad)

            // A damaged manifest makes the whole backup unusable
            File(backup, "manifest.tsv").appendText("3\tShow\t1.png\n")
            assert(!LogoBackups.verify(backup).manifestOk)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun backupsWithoutChecksumsAreUsedUnchecked() {
        val root = kotlin.io.path.createTempDirectory().toFile()
        try {
            File(root, "1.png").writeText("one")
            File(root, "manifest.tsv").writeText("1\tTRT 1\t1.png\n")
            val verification = LogoBackups.verify(root)
            assert(!verification.checked && verification.manifestOk)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun settingsSurviveTheBackupFile() {
        val settings = com.berkbb.channellogos.settings.Settings(language = "ro", theme = "light", preselectSafe = false, iptv = false)
        assertEquals(settings, com.berkbb.channellogos.settings.Settings.fromMap(settings.toMap()))
        // Unknown values fall back to the defaults
        assertEquals("system", com.berkbb.channellogos.settings.Settings.fromMap(mapOf("language" to "xx")).language)
    }

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

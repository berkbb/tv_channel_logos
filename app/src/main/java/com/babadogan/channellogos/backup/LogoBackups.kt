package com.babadogan.channellogos.backup

import android.content.Context
import com.babadogan.channellogos.tv.Channel
import java.io.File

/**
 * Two kinds of copies of the TV's logos, kept in the app's private storage:
 *
 * - **Originals:** before the app first writes a channel's logo, the logo the channel had (or the
 *   fact that it had none) is saved once and never overwritten. "Reset to default" puts it back.
 *   Keyed by channel id: they belong to this channel installation.
 * - **Snapshot:** "Back up logos" saves every current logo with the channel's number and name.
 *   A channel update or reinstallation can renumber the channels' ids, so "Restore" matches by
 *   number and name.
 */
class LogoBackups(context: Context) {

    private val originals = File(context.filesDir, "backup/original").apply { mkdirs() }
    private val snapshot = File(context.filesDir, "backup/snapshot")
    private val manifest = File(snapshot, "manifest.tsv")

    /** Channels whose logo the app has changed (an original is kept for them). */
    fun changedIds(): Set<Long> =
        originals.listFiles()?.mapNotNull { it.nameWithoutExtension.toLongOrNull() }?.toSet().orEmpty()

    fun hasOriginal(id: Long): Boolean = changedIds().contains(id)

    /** Saves [png] (null: the channel had no logo) as [id]'s original, unless one is kept already. */
    fun saveOriginal(id: Long, png: ByteArray?) {
        if (hasOriginal(id)) return
        if (png != null) File(originals, "$id.png").writeBytes(png) else File(originals, "$id.none").createNewFile()
    }

    /** [id]'s original: Present(png), Absent (it had no logo), or null when none is kept. */
    fun original(id: Long): Original? = when {
        File(originals, "$id.png").exists() -> Original.Present(File(originals, "$id.png").readBytes())
        File(originals, "$id.none").exists() -> Original.Absent
        else -> null
    }

    /** The channel is back to its original; nothing is kept for it any more. */
    fun forgetOriginal(id: Long) {
        File(originals, "$id.png").delete()
        File(originals, "$id.none").delete()
    }

    sealed interface Original {
        class Present(val png: ByteArray) : Original
        data object Absent : Original
    }

    // ─── Snapshot ───

    class SnapshotEntry(val number: String, val name: String, val file: File)

    /** Replaces the snapshot with [logos] (channel → PNG). */
    fun saveSnapshot(logos: Map<Channel, ByteArray>) {
        snapshot.deleteRecursively()
        snapshot.mkdirs()
        manifest.bufferedWriter().use { out ->
            for ((channel, png) in logos) {
                val file = File(snapshot, "${channel.id}.png")
                file.writeBytes(png)
                out.append(channel.number).append('\t').append(channel.name.replace('\t', ' ')).append('\t')
                    .append(file.name).append('\n')
            }
        }
    }

    fun snapshotEntries(): List<SnapshotEntry> {
        if (!manifest.exists()) return emptyList()
        return manifest.readLines().mapNotNull { line ->
            val fields = line.split('\t')
            if (fields.size < 3) null else SnapshotEntry(fields[0], fields[1], File(snapshot, fields[2]))
        }.filter { it.file.exists() }
    }

    /** When the snapshot was taken (0 if there is none). */
    fun snapshotTime(): Long = if (manifest.exists()) manifest.lastModified() else 0L

    /** Copies the snapshot to [folder] (e.g. on a USB stick); returns how many logos it holds. */
    fun exportSnapshot(folder: File): Int {
        check(snapshotEntries().isNotEmpty()) { "No backup to export" }
        copySnapshot(snapshot, folder)
        return snapshotEntries().size
    }

    /** Replaces the snapshot with the one in [folder]; returns how many logos it holds. */
    fun importSnapshot(folder: File): Int {
        copySnapshot(folder, snapshot)
        return snapshotEntries().size
    }

    companion object {
        /** Name of a backup folder on a USB stick; its manifest says which channel each logo is for. */
        const val USB_FOLDER = "ChannelLogosBackup"

        /** Whether [folder] holds a backup (a manifest next to the logos). */
        fun isSnapshot(folder: File): Boolean = File(folder, "manifest.tsv").isFile

        /**
         * Replaces [to] with a copy of the snapshot in [from]: the manifest and the logos it names,
         * nothing else. The manifest is written last, so a half-finished copy is never taken for a
         * backup.
         */
        fun copySnapshot(from: File, to: File) {
            val manifest = File(from, "manifest.tsv")
            check(manifest.isFile) { "No backup in $from" }
            val names = manifest.readLines().mapNotNull { it.split('\t').getOrNull(2) }
                .filter { it.isNotBlank() && !it.contains('/') && !it.contains("..") }
            to.deleteRecursively()
            to.mkdirs()
            for (name in names) File(from, name).takeIf { it.isFile }?.copyTo(File(to, name), overwrite = true)
            manifest.copyTo(File(to, "manifest.tsv"), overwrite = true)
        }

        /**
         * Which current channel each snapshot entry belongs to: same number and name first, then a
         * name that occurs only once. Entries without a match are left out.
         */
        fun match(entries: List<SnapshotEntry>, channels: List<Channel>): Map<Channel, SnapshotEntry> {
            val byNumberAndName = channels.associateBy { it.number to it.name }
            val byName = channels.groupBy { it.name }
            val result = LinkedHashMap<Channel, SnapshotEntry>()
            for (entry in entries) {
                val channel = byNumberAndName[entry.number to entry.name]
                    ?: byName[entry.name]?.singleOrNull()
                    ?: continue
                result.putIfAbsent(channel, entry)
            }
            return result
        }
    }
}

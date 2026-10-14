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
        writeChecksums(snapshot)
    }

    fun snapshotEntries(): List<SnapshotEntry> = readManifest(snapshot)

    /** When the snapshot was taken (0 if there is none). */
    fun snapshotTime(): Long = if (manifest.exists()) manifest.lastModified() else 0L

    /**
     * Copies the snapshot with [meta] (settings, creation time) to [folder] — a USB stick or a
     * staging folder for the TV's shared storage — with SHA-256 checksums. Returns the logo count.
     */
    fun exportSnapshot(folder: File, meta: Map<String, String>): Int {
        check(snapshotEntries().isNotEmpty()) { "No backup to export" }
        copySnapshot(snapshot, folder, meta)
        return readManifest(folder).size
    }

    /**
     * Replaces the snapshot with the one in [folder]. Logos whose checksum does not match are left
     * out; a backup whose manifest is damaged is refused. Returns what was taken over.
     */
    fun importSnapshot(folder: File): Imported {
        val verification = verify(folder)
        check(verification.manifestOk) { "The backup's channel list is damaged" }
        copySnapshot(folder, snapshot)
        readMeta(snapshot)["created"]?.toLongOrNull()?.let { manifest.setLastModified(it) }
        return Imported(snapshotEntries().size, verification.bad.size, readMeta(snapshot))
    }

    /** The snapshot's checksums (old snapshots without them count as unchecked). */
    fun verifySnapshot(): Verification = verify(snapshot)

    class Imported(val logos: Int, val damaged: Int, val meta: Map<String, String>)

    /**
     * Result of checking a backup folder against its `checksums.sha256`. Without that file
     * (backups of version 1.0) nothing can be checked: [checked] is false and everything is used.
     */
    class Verification(val checked: Boolean, val manifestOk: Boolean, val bad: Set<String>)

    companion object {
        /** Name of a backup folder on a USB stick or in the TV's shared storage. */
        const val USB_FOLDER = "ChannelLogosBackup"
        const val MANIFEST = "manifest.tsv"
        const val CHECKSUMS = "checksums.sha256"
        /** Settings and creation time, as "key=value" lines. */
        const val META = "backup.properties"

        /** Whether [folder] holds a backup (a manifest next to the logos). */
        fun isSnapshot(folder: File): Boolean = File(folder, MANIFEST).isFile

        /** The manifest's entries whose logo file exists. */
        fun readManifest(folder: File): List<SnapshotEntry> {
            val manifest = File(folder, MANIFEST)
            if (!manifest.isFile) return emptyList()
            return manifest.readLines().mapNotNull { line ->
                val fields = line.split('\t')
                if (fields.size < 3 || !safeName(fields[2])) null else SnapshotEntry(fields[0], fields[1], File(folder, fields[2]))
            }.filter { it.file.isFile }
        }

        /**
         * Replaces [to] with a copy of the snapshot in [from]: the manifest, the logos it names
         * (minus those failing their checksum) and the settings — [meta] if given, else [from]'s.
         * Checksums are written anew for the copy.
         */
        fun copySnapshot(from: File, to: File, meta: Map<String, String>? = null) {
            check(isSnapshot(from)) { "No backup in $from" }
            val bad = verify(from).bad
            val entries = readManifest(from).filter { it.file.name !in bad }
            val metaOut = meta ?: readMeta(from).takeIf { META !in bad }.orEmpty()
            to.deleteRecursively()
            to.mkdirs()
            for (entry in entries) entry.file.copyTo(File(to, entry.file.name), overwrite = true)
            if (metaOut.isNotEmpty()) {
                File(to, META).writeText(metaOut.entries.joinToString("") { (key, value) -> "$key=${value.replace('\n', ' ')}\n" })
            }
            File(to, MANIFEST).bufferedWriter().use { out ->
                for (entry in entries) {
                    out.append(entry.number).append('\t').append(entry.name).append('\t').append(entry.file.name).append('\n')
                }
            }
            writeChecksums(to)
        }

        /** Writes `checksums.sha256` (the format of `sha256sum`, so it can be checked on a computer too). */
        fun writeChecksums(folder: File) {
            val names = readManifest(folder).map { it.file.name } + listOf(MANIFEST, META).filter { File(folder, it).isFile }
            File(folder, CHECKSUMS).writeText(names.joinToString("") { "${sha256(File(folder, it))}  $it\n" })
        }

        fun verify(folder: File): Verification {
            val sums = File(folder, CHECKSUMS)
            if (!sums.isFile) return Verification(checked = false, manifestOk = isSnapshot(folder), bad = emptySet())
            val expected = sums.readLines().mapNotNull { line ->
                val hash = line.substringBefore(' ')
                val name = line.substringAfter("  ", "")
                if (hash.length == 64 && safeName(name)) name to hash.lowercase() else null
            }.toMap()
            val bad = expected.filter { (name, hash) ->
                val file = File(folder, name)
                !file.isFile || sha256(file) != hash
            }.keys
            return Verification(checked = true, manifestOk = MANIFEST in expected && MANIFEST !in bad, bad = bad)
        }

        fun readMeta(folder: File): Map<String, String> {
            val file = File(folder, META)
            if (!file.isFile) return emptyMap()
            return file.readLines().mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 1)
            }.toMap()
        }

        fun sha256(file: File): String {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        /** A plain file name: no folders, no way out of the backup folder. */
        private fun safeName(name: String) = name.isNotBlank() && !name.contains('/') && !name.contains("..")

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

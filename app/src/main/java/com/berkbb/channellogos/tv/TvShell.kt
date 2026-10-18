package com.berkbb.channellogos.tv

import android.content.Context
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import dadb.AdbKeyPair
import dadb.Dadb
import java.io.File
import java.io.IOException

/** A channel of the TV's channel list, as the TV provider stores it. */
data class Channel(
    val id: Long,
    val number: String,
    val name: String,
    val inputId: String,
    val hasLogo: Boolean,
    /** A radio station (audio-only service); these rarely have logos anywhere. */
    val isRadio: Boolean = false,
    /**
     * The DVB service as "SID_TSID_ONID" in uppercase hex, the way picons' service references
     * write it ("2BD_8_1"); null when the TV provider has no broadcast IDs for the channel.
     */
    val serviceKey: String? = null
)

/** [Channel.serviceKey] from the provider's decimal IDs; null for channels without DVB IDs. */
internal fun serviceKey(onid: String?, tsid: String?, sid: String?): String? {
    val ids = listOf(sid, tsid, onid).map { it?.toIntOrNull() ?: 0 }
    // Analogue and IP channels have no service or original network ID (0 in the provider).
    if (ids[0] <= 0 || ids[2] <= 0) return null
    return ids.joinToString("_") { Integer.toHexString(it).uppercase() }
}

/**
 * Shell access to this TV through its own adbd ("network debugging", localhost:5555). The shell
 * user may read and write every channel's logo, which no ordinary app can; the work itself is
 * done by [com.berkbb.channellogos.tool.TvLogoTool], run from this app's APK.
 *
 * The first connection makes the TV ask whether to allow this app's key; after "Always allow"
 * it connects silently.
 */
class TvShell(private val context: Context) : AutoCloseable {

    private var dadb: Dadb? = null

    /** Connects; blocks while the TV shows its "Allow debugging?" prompt. */
    fun connect() {
        if (dadb != null) return
        val keyDir = File(context.filesDir, "adb")
        val privateKey = File(keyDir, "adbkey")
        val publicKey = File(keyDir, "adbkey.pub")
        if (!privateKey.exists()) AdbKeyPair.generate(privateKey, publicKey)
        val connection = Dadb.create(HOST, PORT, AdbKeyPair.read(privateKey, publicKey))
        // Dadb connects lazily: the first command performs (and waits for) the handshake.
        val probe = connection.shell("echo ok")
        check(probe.output.trim() == "ok") { "Unexpected shell reply: ${probe.allOutput}" }
        dadb = connection
    }

    /** The TV's broadcast (tuner) channels, with whether each has a logo. */
    fun listChannels(): List<Channel> {
        val response = withConnection { it.shell(tool("list")) }
        check(response.exitCode == 0) { response.allOutput.ifBlank { "TvLogoTool list failed (${response.exitCode})" } }
        val tunerInputs = tunerInputIds()
        return response.output.lineSequence()
            .mapNotNull { line ->
                val fields = line.split('\t')
                if (fields.size < 5) return@mapNotNull null
                Channel(
                    id = fields[0].toLongOrNull() ?: return@mapNotNull null,
                    number = fields[1],
                    name = fields[2],
                    inputId = fields[3],
                    hasLogo = (fields[4].toLongOrNull() ?: 0L) > 0L,
                    isRadio = fields.getOrNull(5) == "SERVICE_TYPE_AUDIO",
                    serviceKey = serviceKey(fields.getOrNull(6), fields.getOrNull(7), fields.getOrNull(8))
                )
            }
            .filter { channel ->
                if (tunerInputs.isNotEmpty()) channel.inputId in tunerInputs
                else channel.inputId.contains("/HW") && channel.number.isNotBlank()
            }
            .toList()
    }

    /**
     * Writes logos ([channel id] → PNG file). The files must be readable by the shell user, i.e.
     * in this app's external files directory ([logoDirectory]); adb's file transfer is not used
     * (on Android 8 the TV's adbd dropped the connection during every push).
     * Returns the ids that were written.
     */
    fun writeLogos(logos: Map<Long, File>, onProgress: (done: Int) -> Unit): Set<Long> {
        val written = HashSet<Long>()
        // Batches keep each command line short; a batch is redone whole after a reconnect.
        for (batch in logos.entries.chunked(BATCH_SIZE)) {
            val args = batch.joinToString(" ", prefix = "write ") { (id, file) -> "$id ${file.absolutePath}" }
            val response = withConnection { it.shell(tool(args)) }
            response.output.lineSequence()
                .filter { it.startsWith("OK ") }
                .mapNotNull { it.removePrefix("OK ").trim().toLongOrNull() }
                .forEach { written += it }
            response.output.lineSequence().filter { it.startsWith("FAIL ") }.forEach { android.util.Log.w("ChannelLogos", it) }
            onProgress(written.size)
        }
        if (written.isNotEmpty()) refreshTvUi()
        return written
    }

    /**
     * The TV's own channel screens keep logos in a memory cache keyed by the logo's address and do
     * not drop it when the logo changes (Philips: "ZapbarUtils: getBitmapFromUri() in cache"), so a
     * replaced logo kept showing the old picture. The app is restarted (this app is in front while
     * writing, so nothing being watched is interrupted; the TV app comes back on its own with a
     * fresh cache). `am kill` is not enough: it spares these system apps' service processes.
     */
    private fun refreshTvUi() {
        val command = liveTvPackages().joinToString("; ") { "am force-stop $it" }
        runCatching { withConnection { it.shell(command) } }
            .onFailure { android.util.Log.w("ChannelLogos", "Could not refresh the TV's channel screens", it) }
    }

    /**
     * Apps that show the TV's channels: Philips' live TV app, plus whatever the TV offers for
     * opening a channel (Android TV's live TV app answers "view" on a channel address on every
     * brand). This app is left out, and so are the TV inputs' own services, which tune the picture.
     */
    private fun liveTvPackages(): Set<String> {
        val packages = LinkedHashSet(TV_UI_PACKAGES)
        runCatching {
            val view = android.content.Intent(android.content.Intent.ACTION_VIEW, android.media.tv.TvContract.buildChannelUri(1))
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(view, 0).mapTo(packages) { it.activityInfo.packageName }
        }.onFailure { android.util.Log.w("ChannelLogos", "Could not look up the live TV app", it) }
        packages.remove(context.packageName)
        // Stopping a tuner input's own package could cut the picture or a recording.
        val manager = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
        runCatching { manager?.tvInputList?.map { it.serviceInfo.packageName } }.getOrNull()
            ?.filterNot { it in TV_UI_PACKAGES }
            ?.forEach(packages::remove)
        return packages
    }

    /** The current logos of [ids] (channels without a logo are missing from the result). */
    fun readLogos(ids: Collection<Long>): Map<Long, ByteArray> {
        val logos = HashMap<Long, ByteArray>()
        for (batch in ids.chunked(READ_BATCH_SIZE)) {
            val response = withConnection { it.shell(tool("dump " + batch.joinToString(" "))) }
            for (line in response.output.lineSequence()) {
                val tab = line.indexOf('\t')
                if (tab <= 0) continue
                val id = line.substring(0, tab).toLongOrNull() ?: continue
                runCatching { android.util.Base64.decode(line.substring(tab + 1).trim(), android.util.Base64.NO_WRAP) }
                    .onSuccess { logos[id] = it }
            }
        }
        return logos
    }

    /** Leaves [ids] without a logo. Returns the ids that were cleared. */
    fun removeLogos(ids: Collection<Long>): Set<Long> {
        val removed = HashSet<Long>()
        for (batch in ids.chunked(BATCH_SIZE)) {
            val response = withConnection { it.shell(tool("remove " + batch.joinToString(" "))) }
            response.output.lineSequence()
                .filter { it.startsWith("OK ") }
                .mapNotNull { it.removePrefix("OK ").trim().toLongOrNull() }
                .forEach { removed += it }
        }
        if (removed.isNotEmpty()) refreshTvUi()
        return removed
    }

    /**
     * Copies the folder [from] to [to] as the shell user, who may use the TV's shared storage
     * (where this app may not write on newer Android versions). [to] is replaced. Returns whether
     * the copy finished.
     */
    fun copyFolder(from: String, to: String): Boolean {
        val response = withConnection { it.shell("rm -rf '$to' && mkdir -p '$to' && cp -rp '$from'/. '$to'/ && echo copied") }
        return response.output.trim().endsWith("copied")
    }

    fun exists(path: String): Boolean =
        withConnection { it.shell("[ -e '$path' ] && echo yes") }.output.trim() == "yes"

    /** A folder in this app's storage that the shell user can read and write. */
    fun stagingDirectory(name: String): File =
        File(context.getExternalFilesDir(null) ?: error("No external storage"), name)

    /** Where logos to write are put: app storage the shell user can read. */
    fun logoDirectory(): File =
        File(context.getExternalFilesDir(null) ?: error("No external storage"), "logos").apply { mkdirs() }

    override fun close() {
        runCatching { dadb?.close() }
        dadb = null
    }

    /**
     * Runs [block] on the connection. adbd drops idle or interrupted connections (seen as
     * "Broken pipe"), so a failed attempt is retried once on a fresh connection.
     */
    private fun <T> withConnection(block: (Dadb) -> T): T {
        val current = dadb ?: run { connect(); dadb!! }
        return try {
            block(current)
        } catch (e: IOException) {
            close()
            connect()
            block(dadb!!)
        }
    }

    private fun tool(arguments: String): String =
        "app_process -Djava.class.path=${context.applicationInfo.sourceDir} /system/bin $TOOL_CLASS $arguments"

    /** Ids of the TV's tuner inputs (antenna, cable, satellite); app channels are left out. */
    private fun tunerInputIds(): Set<String> {
        val manager = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return emptySet()
        return runCatching {
            manager.tvInputList.filter { it.type == TvInputInfo.TYPE_TUNER }.map { it.id }.toSet()
        }.getOrDefault(emptySet())
    }

    private companion object {
        const val HOST = "127.0.0.1"
        const val PORT = 5555
        const val TOOL_CLASS = "com.berkbb.channellogos.tool.TvLogoTool"
        const val BATCH_SIZE = 40
        // Logos come back as base64 on stdout (~15 KB each); keep a reply to about 1 MB.
        const val READ_BATCH_SIZE = 60
        // Live TV with its zap bar and channel list on Philips (TP Vision) TVs; unknown names are ignored.
        val TV_UI_PACKAGES = listOf("org.droidtv.playtv")
    }
}

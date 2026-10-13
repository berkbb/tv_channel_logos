package com.babadogan.channellogos.ui

import android.app.Application
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.babadogan.channellogos.logos.BuiltInLogoSource
import com.babadogan.channellogos.logos.Country
import com.babadogan.channellogos.logos.GitHubLogoSource
import com.babadogan.channellogos.logos.IptvOrgSource
import com.babadogan.channellogos.logos.PiconsSource
import com.babadogan.channellogos.logos.UsbLogoSource
import com.babadogan.channellogos.logos.LogoEntry
import com.babadogan.channellogos.logos.LogoCandidate
import com.babadogan.channellogos.logos.LogoImage
import com.babadogan.channellogos.logos.LogoMatching
import com.babadogan.channellogos.logos.LogoSource
import com.babadogan.channellogos.backup.LogoBackups
import com.babadogan.channellogos.settings.Settings
import com.babadogan.channellogos.settings.SettingsStore
import com.babadogan.channellogos.tv.Channel
import com.babadogan.channellogos.tv.TvShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class MainViewModel(application: Application) : AndroidViewModel(application) {

    sealed interface Connection {
        data object Connecting : Connection
        data object Connected : Connection
        data class Failed(val message: String) : Connection
    }

    data class UiState(
        val connection: Connection = Connection.Connecting,
        val channels: List<Channel> = emptyList(),
        /** Suggested logo per channel id (only for channels it could find one for). */
        val suggestions: Map<Long, LogoCandidate> = emptyMap(),
        val selected: Set<Long> = emptySet(),
        val showAll: Boolean = false,
        /** Progress or result line; null when idle. */
        val status: String? = null,
        val busy: Boolean = false,
        val tvLogos: Int = 0,
        val gitHubLogos: Int = 0,
        val piconsLogos: Int = 0,
        val iptvLogos: Int = 0,
        val usbLogos: Int = 0,
        /** When the online catalogues were last downloaded (epoch ms, 0 if never). */
        val sourcesUpdatedAt: Long = 0L,
        val tvTableMissing: Boolean = false,
        val settings: Settings = Settings(),
        /** Channels whose logo this app changed (their original is kept for "Reset to default"). */
        val changed: Set<Long> = emptySet(),
        /** When the logo backup was taken (0: none) and how many logos it holds. */
        val backupTime: Long = 0L,
        val backupCount: Int = 0,
        val gitHubFailed: Boolean = false
    ) {
        val withLogo: Int get() = channels.count { it.hasLogo }
        val radios: Int get() = channels.count { it.isRadio }
        /** By default the TV channels still missing a logo; radio stations only under "All". */
        val visibleChannels: List<Channel> get() = if (showAll) channels else channels.filter { !it.hasLogo && !it.isRadio }
    }

    private val shell = TvShell(application)
    private val builtIn = BuiltInLogoSource(application)
    private val gitHub = GitHubLogoSource(application)
    private val picons = PiconsSource(application)
    private val iptv = IptvOrgSource(application)
    private val usb = UsbLogoSource()
    private val backups = LogoBackups(application)
    private val country = Country.ofDevice()
    private val shellLock = Mutex()
    private val previews = ConcurrentHashMap<String, ImageBitmap>()

    private val settingsStore = SettingsStore(application)

    private val _state = MutableStateFlow(UiState(settings = settingsStore.load()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        Strings.setLanguage(_state.value.settings.language)
        start()
    }

    /** Saves [settings]; source and suggestion choices take effect at once on the current list. */
    fun updateSettings(settings: Settings) {
        settingsStore.save(settings)
        Strings.setLanguage(settings.language)
        _state.update { it.copy(settings = settings) }
        if (_state.value.channels.isNotEmpty()) {
            viewModelScope.launch { showChannels(Result.success(_state.value.channels)) }
        }
    }

    fun start() {
        viewModelScope.launch {
            _state.update { it.copy(connection = Connection.Connecting, status = null) }
            try {
                val t0 = System.currentTimeMillis()
                withContext(Dispatchers.IO) { shellLock.withLock { shell.connect() } }
                Log.i(TAG, "Connected in ${System.currentTimeMillis() - t0} ms")
                _state.update { it.copy(connection = Connection.Connected) }
            } catch (e: Exception) {
                Log.w(TAG, "Connection failed", e)
                _state.update { it.copy(connection = Connection.Failed(e.message ?: e.javaClass.simpleName)) }
                return@launch
            }
            // One after the other: run together, the two compete for the TV's slow CPU and both slow down.
            loadSources()
            _state.update { it.copy(busy = true, status = Strings.loadingChannels) }
            showChannels(withContext(Dispatchers.IO) { runCatching { readChannels() } })
        }
    }

    fun scan() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, status = Strings.loadingChannels) }
            showChannels(withContext(Dispatchers.IO) { runCatching { readChannels() } })
        }
    }

    /** Called when the storage permission was granted: USB logos can be read now. */
    fun onStorageAccess() {
        if (_state.value.channels.isNotEmpty() && !_state.value.busy) scan()
    }

    private suspend fun readChannels(): List<Channel> {
        // A USB stick may have been plugged in since the last scan.
        runCatching { usb.load() }.onFailure { Log.w(TAG, "USB logos", it) }
        _state.update { it.copy(usbLogos = usb.size) }
        val t0 = System.currentTimeMillis()
        return shellLock.withLock { shell.listChannels() }
            .sortedWith(compareBy({ it.number.toIntOrNull() ?: Int.MAX_VALUE }, { it.name }))
            .also { Log.i(TAG, "Channels: ${it.size} in ${System.currentTimeMillis() - t0} ms") }
    }

    private suspend fun showChannels(result: Result<List<Channel>>) {
        try {
                val channels = result.getOrThrow()
                val settings = _state.value.settings
                val suggestions = withContext(Dispatchers.Default) { suggest(channels, settings) }
                _state.update { state ->
                    state.copy(
                        channels = channels,
                        suggestions = suggestions,
                        changed = backups.changedIds(),
                        backupTime = backups.snapshotTime(),
                        backupCount = backups.snapshotEntries().size,
                        // Only safe matches are preselected; the rest wait for a look (see LogoMatching.isSafe).
                        selected = if (!settings.preselectSafe) emptySet() else channels
                            .filter { !it.hasLogo && !it.isRadio }
                            .mapNotNull { channel -> suggestions[channel.id]?.takeIf(LogoMatching::isSafe)?.let { channel.id } }
                            .toSet(),
                        busy = false,
                        status = null
                    )
                }
        } catch (e: Exception) {
            Log.w(TAG, "Scan failed", e)
            _state.update { it.copy(busy = false, status = Strings.scanFailed(e.message ?: e.javaClass.simpleName)) }
        }
    }

    fun toggle(channelId: Long) {
        if (_state.value.suggestions[channelId] == null) return
        _state.update { it.copy(selected = if (channelId in it.selected) it.selected - channelId else it.selected + channelId) }
    }

    fun selectAll() = _state.update { state ->
        state.copy(selected = state.suggestions.keys.filter { id -> state.visibleChannels.any { it.id == id } }.toSet())
    }

    /** Only the safe suggestions among the visible channels (see LogoMatching.isSafe). */
    fun selectSafe() = _state.update { state ->
        state.copy(selected = state.visibleChannels.mapNotNull { channel ->
            state.suggestions[channel.id]?.takeIf(LogoMatching::isSafe)?.let { channel.id }
        }.toSet())
    }

    fun selectNone() = _state.update { it.copy(selected = emptySet()) }

    fun setShowAll(showAll: Boolean) = _state.update { it.copy(showAll = showAll) }

    /** The user picked [candidate] (or nothing) for [channelId] in the picker. */
    fun choose(channelId: Long, candidate: LogoCandidate?) = _state.update { state ->
        if (candidate == null) {
            state.copy(suggestions = state.suggestions - channelId, selected = state.selected - channelId)
        } else {
            state.copy(suggestions = state.suggestions + (channelId to candidate), selected = state.selected + channelId)
        }
    }

    fun candidatesFor(channel: Channel): List<LogoCandidate> =
        LogoMatching.candidates(channel.name, catalogs(), channel.serviceKey)

    /** Every image on the USB stick for [channel], those named like it first. */
    fun usbCandidatesFor(channel: Channel): List<LogoCandidate> {
        val keys = com.babadogan.channellogos.logos.NameMatcher.channelKeys(channel.name).map { it.first }
        val base = keys.firstOrNull()?.let { com.babadogan.channellogos.logos.NameMatcher.withoutQuality(it) ?: it }.orEmpty()
        return usb.files()
            .sortedBy { file ->
                val key = com.babadogan.channellogos.logos.NameMatcher.key(file.nameWithoutExtension)
                when {
                    key in keys -> 0
                    base.length >= 3 && (key.startsWith(base) || base.startsWith(key)) -> 1
                    else -> 2
                }
            }
            .map { file ->
                LogoCandidate(LogoSource.FILE, file.path, com.babadogan.channellogos.logos.NameMatcher.Strength.LOOSE, "file:${file.path}", LogoEntry.COUNTRY)
            }
    }

    /** Preview image of a logo, fitted like it will be written; null while it cannot be loaded. */
    suspend fun preview(candidate: LogoCandidate): ImageBitmap? {
        previews[candidate.id]?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val png = LogoImage.fit(read(candidate))
                BitmapFactory.decodeByteArray(png, 0, png.size).asImageBitmap().also { previews[candidate.id] = it }
            }.onFailure { Log.w(TAG, "Preview of ${candidate.id} failed", it) }.getOrNull()
        }
    }

    fun applySelected() {
        val state = _state.value
        val chosen = state.selected.mapNotNull { id -> state.suggestions[id]?.let { id to it } }
        if (chosen.isEmpty() || state.busy) return
        runTask(Strings.writing(0, chosen.size)) {
            val logos = LinkedHashMap<Long, ByteArray>()
            for ((id, candidate) in chosen) {
                runCatching { LogoImage.fit(read(candidate)) }
                    .onSuccess { logos[id] = it }
                    .onFailure { Log.w(TAG, "Could not prepare ${candidate.id}", it) }
            }
            val written = writeKeepingOriginals(logos) { done -> _state.update { it.copy(status = Strings.writing(done, chosen.size)) } }
            Strings.written(written, chosen.size - written)
        }
    }

    /** Puts the original logo (or no logo) back on [ids], or on every channel the app changed. */
    fun resetToDefault(ids: Collection<Long>? = null) {
        val targets = (ids ?: backups.changedIds()).filter { backups.hasOriginal(it) }
        if (targets.isEmpty()) return
        runTask(Strings.resetting) {
            val restore = LinkedHashMap<Long, ByteArray>()
            val clear = ArrayList<Long>()
            for (id in targets) {
                when (val original = backups.original(id)) {
                    is LogoBackups.Original.Present -> restore[id] = original.png
                    LogoBackups.Original.Absent -> clear += id
                    null -> Unit
                }
            }
            val done = HashSet<Long>()
            if (restore.isNotEmpty()) done += writeFiles(restore) { }
            if (clear.isNotEmpty()) done += shellLock.withLock { shell.removeLogos(clear) }
            done.forEach(backups::forgetOriginal)
            Strings.resetDone(done.size, targets.size - done.size)
        }
    }

    /** Saves every current logo of the TV, to be restored after a channel update removed them. */
    fun backupLogos() {
        val channels = _state.value.channels.filter { it.hasLogo }
        if (channels.isEmpty()) return
        runTask(Strings.backingUp) {
            val logos = shellLock.withLock { shell.readLogos(channels.map { it.id }) }
            val byChannel = channels.mapNotNull { channel -> logos[channel.id]?.let { channel to it } }.toMap()
            backups.saveSnapshot(byChannel)
            Strings.backedUp(byChannel.size)
        }
    }

    /** Writes the backed-up logos back, matching channels by number and name. */
    fun restoreBackup() {
        runTask(Strings.restoring) {
            val matched = LogoBackups.match(backups.snapshotEntries(), _state.value.channels)
            val logos = matched.entries.associate { (channel, entry) -> channel.id to entry.file.readBytes() }
            val written = writeKeepingOriginals(logos) { }
            Strings.restored(written, backups.snapshotEntries().size - written)
        }
    }

    /**
     * Runs [work] (on IO, under the busy state), then rereads the channels and shows the line it
     * returns. Failures are shown too.
     */
    private fun runTask(progress: String, work: suspend () -> String) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, status = progress) }
            val result = try {
                withContext(Dispatchers.IO) { work() }
            } catch (e: Exception) {
                Log.w(TAG, "Task failed", e)
                Strings.writeFailed(e.message ?: e.javaClass.simpleName)
            }
            showChannels(withContext(Dispatchers.IO) { runCatching { readChannels() } })
            _state.update { it.copy(busy = false, status = result) }
        }
    }

    /** Saves the originals of channels changed for the first time, then writes [logos]. */
    private suspend fun writeKeepingOriginals(logos: Map<Long, ByteArray>, onProgress: (Int) -> Unit): Int {
        val first = logos.keys.filterNot(backups::hasOriginal)
        if (first.isNotEmpty()) {
            val current = shellLock.withLock { shell.readLogos(first) }
            for (id in first) backups.saveOriginal(id, current[id])
        }
        return writeFiles(logos, onProgress).size
    }

    private suspend fun writeFiles(logos: Map<Long, ByteArray>, onProgress: (Int) -> Unit): Set<Long> {
        val dir = shell.logoDirectory().apply { listFiles()?.forEach { it.delete() } }
        val files = logos.mapValues { (id, png) -> File(dir, "$id.png").apply { writeBytes(png) } }
        return try {
            shellLock.withLock { shell.writeLogos(files, onProgress) }
        } finally {
            dir.listFiles()?.forEach { it.delete() }
        }
    }

    override fun onCleared() {
        shell.close()
    }

    /** Downloads the GitHub, picons and iptv-org catalogues again (and drops cached logo images), then re-matches. */
    fun refreshSources() {
        if (_state.value.busy) return
        viewModelScope.launch {
            previews.clear()
            loadSources(refresh = true)
            _state.update { it.copy(busy = true, status = Strings.loadingChannels) }
            showChannels(withContext(Dispatchers.IO) { runCatching { readChannels() } })
            _state.update { it.copy(status = Strings.sourcesUpdated) }
        }
    }

    private suspend fun loadSources(refresh: Boolean = false) {
        _state.update { it.copy(busy = true, status = if (refresh) Strings.updatingSources else Strings.loadingSources) }
        withContext(Dispatchers.IO) {
            var t0 = System.currentTimeMillis()
            runCatching { builtIn.load(country) }.onFailure { Log.w(TAG, "Built-in logo table", it) }
            Log.i(TAG, "Built-in logo table: ${builtIn.size} keys in ${System.currentTimeMillis() - t0} ms")
            t0 = System.currentTimeMillis()
            val gitHubLoaded = runCatching { gitHub.load(country, refresh) }.onFailure { Log.w(TAG, "GitHub logos", it) }.isSuccess
            Log.i(TAG, "GitHub logos: ${gitHub.size} keys in ${System.currentTimeMillis() - t0} ms")
            t0 = System.currentTimeMillis()
            runCatching { picons.load(country, refresh) }.onFailure { Log.w(TAG, "picons", it) }
            Log.i(TAG, "picons: ${picons.services().size} services, ${picons.entries().size} name keys in ${System.currentTimeMillis() - t0} ms")
            t0 = System.currentTimeMillis()
            runCatching { iptv.load(country, refresh) }.onFailure { Log.w(TAG, "iptv-org logos", it) }
            Log.i(TAG, "iptv-org logos: ${iptv.size} keys in ${System.currentTimeMillis() - t0} ms")
            _state.update {
                it.copy(
                    tvLogos = builtIn.size,
                    gitHubLogos = gitHub.size,
                    piconsLogos = picons.size,
                    iptvLogos = iptv.size,
                    sourcesUpdatedAt = maxOf(gitHub.updatedAt(country), picons.updatedAt(), iptv.updatedAt(country)),
                    tvTableMissing = !builtIn.available,
                    gitHubFailed = !gitHubLoaded
                )
            }
        }
    }

    private fun suggest(channels: List<Channel>, settings: Settings): Map<Long, LogoCandidate> {
        val result = HashMap<Long, LogoCandidate>()
        val catalogs = catalogs()
        for (channel in channels) {
            // A channel no catalogue knows is offered a logo drawn from its name.
            val best = LogoMatching.best(channel.name, catalogs, channel.serviceKey)
                ?: if (settings.textLogos) LogoMatching.textLogo(channel.name) else null
            best?.let { result[channel.id] = it }
        }
        return result
    }

    private fun catalogs(): LogoMatching.Catalogs {
        val settings = _state.value.settings
        return LogoMatching.Catalogs(
            files = if (settings.usb) usb.entries() else emptyMap(),
            tv = builtIn.entries(),
            gitHub = if (settings.gitHub) gitHub.entries() else emptyMap(),
            picons = if (settings.picons) picons.entries() else emptyMap(),
            piconServices = if (settings.picons) picons.services() else emptyMap(),
            iptv = if (settings.iptv) iptv.entries() else emptyMap()
        )
    }

    private fun read(candidate: LogoCandidate): ByteArray {
        val ref = candidate.id.substringAfter(':')
        return when (candidate.source) {
            LogoSource.TV -> builtIn.read(ref.toInt())
            LogoSource.GITHUB -> gitHub.read(ref)
            LogoSource.PICONS -> picons.read(ref)
            LogoSource.IPTV -> iptv.read(ref)
            LogoSource.TEXT -> LogoImage.text(candidate.name)
            LogoSource.FILE -> usb.read(ref)
        }
    }

    private companion object {
        const val TAG = "ChannelLogos"
    }
}

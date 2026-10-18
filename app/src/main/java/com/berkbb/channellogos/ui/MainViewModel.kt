package com.berkbb.channellogos.ui

import android.app.Application
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.berkbb.channellogos.logos.BuiltInLogoSource
import com.berkbb.channellogos.logos.Country
import com.berkbb.channellogos.logos.ForeignChannels
import com.berkbb.channellogos.logos.GitHubLogoSource
import com.berkbb.channellogos.logos.IptvOrgSource
import com.berkbb.channellogos.logos.PiconsSource
import com.berkbb.channellogos.logos.FileLogoSource
import com.berkbb.channellogos.logos.LogoEntry
import com.berkbb.channellogos.logos.LogoCandidate
import com.berkbb.channellogos.logos.LogoImage
import com.berkbb.channellogos.logos.LogoMatching
import com.berkbb.channellogos.logos.LogoQuality
import com.berkbb.channellogos.logos.LogoSource
import com.berkbb.channellogos.backup.LogoBackups
import com.berkbb.channellogos.settings.Settings
import com.berkbb.channellogos.settings.SettingsStore
import com.berkbb.channellogos.tv.Channel
import com.berkbb.channellogos.tv.TvShell
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

    /** The end of a long task, shown full screen until the user closes it. */
    data class Outcome(val success: Boolean, val title: String, val message: String)

    data class PendingItem(
        val channelId: Long,
        val channelNumber: String,
        val channelName: String,
        val candidate: LogoCandidate
    )

    /** Logos downloaded and ready to write, waiting for the user's go-ahead (see confirmWrite). */
    data class PendingWrite(
        val total: Int,
        /** How many of them replace a logo the channel has now. */
        val replacing: Int,
        /** How many look poor on the TV (see LogoQuality). */
        val lowQuality: Int,
        /** Chosen logos that could not be downloaded or read; they are left out. */
        val failed: Int,
        /** No backup exists yet: one is taken before writing. */
        val autoBackup: Boolean,
        /** Channels being modified for the confirmation visual preview. */
        val previewItems: List<PendingItem> = emptyList()
    )

    data class PendingRestore(
        val matched: Int,
        val total: Int,
        val channelsChanged: Boolean,
        val previewItems: List<PendingItem> = emptyList()
    )

    enum class BackupPlace { USB, INTERNAL }
    enum class ChannelSort { NUMBER, NAME, MISSING_FIRST }
    enum class MediaFilter { ALL, TV_ONLY, RADIO_ONLY }

    /** A backup found for importing, checked against its checksums and the TV's channels. */
    data class FoundBackup(
        val place: BackupPlace,
        val folder: File,
        val created: Long,
        val logos: Int,
        /** Logos whose channel (by number and name) is on this TV now. */
        val matched: Int,
        /** Whether it has checksums (backups of version 1.0 have none). */
        val checked: Boolean,
        /** Logos failing their checksum (all of them when the manifest itself is damaged). */
        val damaged: Int,
        val hasSettings: Boolean
    ) {
        val usable: Boolean get() = damaged < logos
        /** Fewer than [MATCH_SHARE] of the logos find their channel: the channel list changed a lot. */
        val channelsChanged: Boolean get() = logos > 0 && matched < logos * MATCH_SHARE
    }

    sealed interface UpdateStatus {
        data object Idle : UpdateStatus
        data object Checking : UpdateStatus
        data class UpToDate(val version: String) : UpdateStatus
        data class Available(val version: String, val url: String) : UpdateStatus
        data class Failed(val message: String) : UpdateStatus
    }

    data class UiState(
        val connection: Connection = Connection.Connecting,
        val unsupportedReason: String? = null,
        val channels: List<Channel> = emptyList(),
        /** Suggested logo per channel id (only for channels it could find one for). */
        val suggestions: Map<Long, LogoCandidate> = emptyMap(),
        val selected: Set<Long> = emptySet(),
        val showAll: Boolean = false,
        /** Search text typed over the list; blank shows every channel of the current view. */
        val query: String = "",
        /** Progress or result line; null when idle. */
        val status: String? = null,
        val busy: Boolean = false,
        val outcome: Outcome? = null,
        val pendingWrite: PendingWrite? = null,
        val pendingRestore: PendingRestore? = null,
        /** Backups found for importing; null while that list is closed. */
        val foundBackups: List<FoundBackup>? = null,
        val choosingExport: Boolean = false,
        /** Quality problems of logos seen so far, by candidate id (only logos with problems). */
        val poorLogos: Map<String, Set<LogoQuality.Issue>> = emptyMap(),
        /** The last scan found no broadcast channels in the TV's Android channel list. */
        val noTunerChannels: Boolean = false,
        /** Other countries whose catalogues were added because the TV carries their channels (ISO codes). */
        val extraCountries: List<String> = emptyList(),
        val tvLogos: Int = 0,
        val gitHubLogos: Int = 0,
        val piconsLogos: Int = 0,
        val iptvLogos: Int = 0,
        val fileLogos: Int = 0,
        /** When the online catalogues were last downloaded (epoch ms, 0 if never). */
        val sourcesUpdatedAt: Long = 0L,
        val tvTableMissing: Boolean = false,
        val settings: Settings = Settings(),
        /** Channels whose logo this app changed (their original is kept for "Reset to default"). */
        val changed: Set<Long> = emptySet(),
        /** When the logo backup was taken (0: none) and how many logos it holds. */
        val backupTime: Long = 0L,
        val backupCount: Int = 0,
        val gitHubFailed: Boolean = false,
        val updateStatus: UpdateStatus = UpdateStatus.Idle,
        val sortMode: ChannelSort = ChannelSort.NUMBER,
        val mediaFilter: MediaFilter = MediaFilter.ALL,
        val cacheSizeBytes: Long = 0L
    ) {
        val withLogo: Int get() = channels.count { it.hasLogo }
        val radios: Int get() = channels.count { it.isRadio }
        /** Channels filtered by media type (TV/Radio), logo status (All/Missing), search query, and sorting. */
        val visibleChannels: List<Channel> get() {
            var list = when (mediaFilter) {
                MediaFilter.ALL -> channels
                MediaFilter.TV_ONLY -> channels.filter { !it.isRadio }
                MediaFilter.RADIO_ONLY -> channels.filter { it.isRadio }
            }
            if (!showAll) {
                list = list.filter { !it.hasLogo && it.id !in settings.ignoredChannels }
            }
            list = ChannelSearch.filter(list, query)
            return when (sortMode) {
                ChannelSort.NUMBER -> list
                ChannelSort.NAME -> list.sortedWith(compareBy(java.text.Collator.getInstance(Strings.locale)) { it.name })
                ChannelSort.MISSING_FIRST -> list.sortedWith(compareBy<Channel> { it.hasLogo }.thenBy { runCatching { it.number.toInt() }.getOrDefault(Int.MAX_VALUE) })
            }
        }
        /** Search matches the "without logo" view hides (channels with a logo, radios). */
        val hiddenMatches: Int get() =
            if (showAll || query.isBlank()) 0 else ChannelSearch.filter(channels, query).size - visibleChannels.size
    }

    private val shell = TvShell(application)
    private val builtIn = BuiltInLogoSource(application)
    private val gitHub = GitHubLogoSource(application)
    private val picons = PiconsSource(application)
    private val iptv = IptvOrgSource(application)
    private val fileLogos = FileLogoSource()
    private val backups = LogoBackups(application)
    private val country = Country.ofDevice()
    /** See ForeignChannels: added once the channel list shows the TV carries their channels. */
    private var extraCountries: List<Country> = emptyList()
    private val shellLock = Mutex()
    private val previews = ConcurrentHashMap<String, ImageBitmap>()
    /** Fitted PNGs of the write waiting for confirmation, and which of them look poor. */
    private var prepared: Map<Long, ByteArray> = emptyMap()
    private var preparedPoor: Set<Long> = emptySet()

    private val settingsStore = SettingsStore(application)

    private val _state = MutableStateFlow(UiState(settings = settingsStore.load()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        Strings.setLanguage(_state.value.settings.language)
        refreshCacheSize()
        start()
    }

    /** Saves [settings]; source and suggestion choices take effect at once on the current list. */
    fun updateSettings(settings: Settings) {
        val oldPiconsStyle = _state.value.settings.piconsStyle
        settingsStore.save(settings)
        Strings.setLanguage(settings.language)
        if (oldPiconsStyle != settings.piconsStyle) {
            previews.clear()
        }
        _state.update { it.copy(settings = settings) }
        if (_state.value.channels.isNotEmpty()) {
            viewModelScope.launch { showChannels(Result.success(_state.value.channels)) }
        }
    }

    fun start() {
        viewModelScope.launch {
            val tvInputManager = getApplication<Application>().getSystemService(android.content.Context.TV_INPUT_SERVICE) as? android.media.tv.TvInputManager
            val hasLiveTv = getApplication<Application>().packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LIVE_TV)
            if (tvInputManager == null && !hasLiveTv) {
                _state.update { it.copy(unsupportedReason = Strings.unsupportedNoTuner) }
                return@launch
            }
            _state.update { it.copy(connection = Connection.Connecting, status = null) }
            try {
                val t0 = System.currentTimeMillis()
                withContext(Dispatchers.IO) { shellLock.withLock { shell.connect() } }
                Log.i(TAG, "Connected in ${System.currentTimeMillis() - t0} ms")
                _state.update { it.copy(connection = Connection.Connected) }
            } catch (e: Exception) {
                Log.w(TAG, "Connection failed", e)
                val msg = e.message ?: e.javaClass.simpleName
                if (NO_ACCESS_MARKERS.any { msg.contains(it, ignoreCase = true) }) {
                    _state.update { it.copy(unsupportedReason = msg) }
                } else {
                    _state.update { it.copy(connection = Connection.Failed(msg)) }
                }
                return@launch
            }
            val probeResult = withContext(Dispatchers.IO) { shellLock.withLock { shell.probe() } }
            if (probeResult.isFailure) {
                val errorMsg = probeResult.exceptionOrNull()?.message ?: "TV logo provider unsupported"
                Log.w(TAG, "TV logo probe failed: $errorMsg")
                _state.update { it.copy(unsupportedReason = errorMsg) }
                return@launch
            }
            // One after the other: run together, the two compete for the TV's slow CPU and both slow down.
            loadSources()
            _state.update { it.copy(busy = true, status = Strings.loadingChannels) }
            val error = showChannels(withContext(Dispatchers.IO) { runCatching { readChannels() } })
            if (_state.value.unsupportedReason == null) {
                _state.update { it.copy(outcome = error ?: noChannelsWarning(it)) }
            }
            refreshIptvInBackground()
            checkForUpdates()
        }
    }

    /**
     * Checks GitHub Releases for app updates quietly or when triggered manually.
     */
    fun checkForUpdates() {
        val currentVersion = runCatching {
            val app = getApplication<Application>()
            app.packageManager.getPackageInfo(app.packageName, 0).versionName
        }.getOrNull() ?: "1.3.0"
        viewModelScope.launch {
            _state.update { it.copy(updateStatus = UpdateStatus.Checking) }
            val result = withContext(Dispatchers.IO) {
                runCatching { AppUpdateChecker.check(currentVersion) }
            }
            result.onSuccess { check ->
                _state.update {
                    it.copy(
                        updateStatus = if (check.hasUpdate) UpdateStatus.Available(check.latestVersion, check.htmlUrl)
                        else UpdateStatus.UpToDate(check.latestVersion)
                    )
                }
            }.onFailure { e ->
                Log.w(TAG, "Update check failed", e)
                _state.update { it.copy(updateStatus = UpdateStatus.Failed(e.message ?: "Failed")) }
            }
        }
    }

    /**
     * The app starts from the bundled iptv-org snapshot; a fresh list is downloaded quietly once a
     * week and used from the next start (or "Update Logos from the Internet").
     */
    private fun refreshIptvInBackground() {
        if (!iptv.stale) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { iptv.refreshList() }
                .onSuccess { if (it) _state.update { s -> s.copy(sourcesUpdatedAt = maxOf(s.sourcesUpdatedAt, iptv.updatedAt())) } }
                .onFailure { Log.w(TAG, "iptv-org background refresh", it) }
        }
    }

    /**
     * Adds the catalogues of [Country.EXTRA] countries whose channels [channels] include (see
     * ForeignChannels), reloading the sources from their caches. Returns whether any was added.
     */
    private fun addForeignCatalogues(channels: List<Channel>): Boolean {
        val names = channels.map { it.name }
        val own = HashSet<String>()
        gitHub.entries().filterValues { it.tier == LogoEntry.COUNTRY }.keys.forEach { own += it }
        builtIn.entries().filterValues { it.tier == LogoEntry.COUNTRY }.keys.forEach { own += it }
        val added = Country.EXTRA.filter { it != country && it !in extraCountries }.filter { other ->
            runCatching { ForeignChannels.carries(names, gitHub.folderKeys(other), own) }
                .onFailure { Log.w(TAG, "Catalogue of ${other.isoCode}", it) }
                .getOrDefault(false)
        }
        if (added.isEmpty()) return false
        extraCountries = extraCountries + added
        Log.i(TAG, "Channels of ${added.joinToString { it.isoCode }} found; adding their catalogues")
        runCatching { gitHub.load(country, extra = extraCountries) }.onFailure { Log.w(TAG, "GitHub logos", it) }
        runCatching { picons.load(country, extra = extraCountries) }.onFailure { Log.w(TAG, "picons", it) }
        runCatching { iptv.load(country, extra = extraCountries) }.onFailure { Log.w(TAG, "iptv-org logos", it) }
        _state.update {
            it.copy(
                extraCountries = extraCountries.map { c -> c.isoCode },
                gitHubLogos = gitHub.size,
                piconsLogos = picons.size,
                iptvLogos = iptv.size
            )
        }
        return true
    }

    fun scan() {
        if (_state.value.busy || _state.value.unsupportedReason != null) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, status = Strings.loadingChannels) }
            val error = showChannels(withContext(Dispatchers.IO) { runCatching { readChannels() } })
            if (_state.value.unsupportedReason == null) {
                _state.update {
                    it.copy(outcome = error ?: noChannelsWarning(it) ?: Outcome(true, Strings.doneTitle, Strings.channelsScanned(it.channels.size)))
                }
            }
        }
    }

    fun dismissOutcome() = _state.update { it.copy(outcome = null) }

    /**
     * Some TVs keep their channel list in a private database of the manufacturer: Android's list
     * then has no broadcast channels and logos written there would never show.
     */
    private fun noChannelsWarning(state: UiState): Outcome? =
        if (state.noTunerChannels) Outcome(false, Strings.noChannelsTitle, Strings.noChannelsExplained) else null

    /** Called when the storage permission was granted: USB logos can be read now. */
    fun onStorageAccess() {
        if (_state.value.channels.isNotEmpty() && !_state.value.busy) scan()
    }

    private suspend fun readChannels(): List<Channel> {
        // A USB stick may have been plugged in, or files copied to the TV, since the last scan.
        runCatching { fileLogos.load() }.onFailure { Log.w(TAG, "File logos", it) }
        _state.update { it.copy(fileLogos = fileLogos.size) }
        val t0 = System.currentTimeMillis()
        return shellLock.withLock { shell.listChannels() }
            .sortedWith(compareBy({ it.number.toIntOrNull() ?: Int.MAX_VALUE }, { it.name }))
            .also { Log.i(TAG, "Channels: ${it.size} in ${System.currentTimeMillis() - t0} ms") }
    }

    /** Shows the channels read; returns the error to show when they could not be read. */
    private suspend fun showChannels(result: Result<List<Channel>>): Outcome? {
        try {
                val channels = result.getOrThrow()
                val settings = _state.value.settings
                withContext(Dispatchers.IO) { addForeignCatalogues(channels) }
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
                        noTunerChannels = channels.isEmpty(),
                        busy = false,
                        status = null
                    )
                }
                return null
        } catch (e: Exception) {
            Log.w(TAG, "Scan failed", e)
            _state.update { it.copy(busy = false, status = null) }
            val detail = e.message ?: e.javaClass.simpleName
            // Arçelik-style TVs: the shell user may not touch other apps' channels at all.
            if (NO_ACCESS_MARKERS.any { detail.contains(it, ignoreCase = true) }) {
                _state.update { it.copy(unsupportedReason = detail) }
                return null
            }
            return failure(Strings.scanFailed(detail))
        }
    }

    fun toggle(channelId: Long) {
        if (_state.value.suggestions[channelId] == null) return
        _state.update { it.copy(selected = if (channelId in it.selected) it.selected - channelId else it.selected + channelId) }
    }

    fun selectAll() = _state.update { state ->
        state.copy(selected = state.suggestions.keys.filter { id -> state.visibleChannels.any { it.id == id } }.toSet())
    }

    /** Only the safe suggestions among the visible channels (see LogoMatching.isSafe), minus logos known to look poor. */
    fun selectSafe() = _state.update { state ->
        state.copy(selected = state.visibleChannels.mapNotNull { channel ->
            state.suggestions[channel.id]
                ?.takeIf { LogoMatching.isSafe(it) && it.id !in state.poorLogos }
                ?.let { channel.id }
        }.toSet())
    }

    fun selectNone() = _state.update { it.copy(selected = emptySet()) }

    /** Assigns a text/drawn logo to all channels without a logo that currently have no catalogue match. */
    fun selectDrawnLogos() {
        val state = _state.value
        val newSuggestions = HashMap(state.suggestions)
        val newSelected = HashSet(state.selected)
        for (channel in state.channels) {
            if (!channel.hasLogo && channel.id !in state.suggestions) {
                val drawn = LogoMatching.textLogo(channel.name)
                newSuggestions[channel.id] = drawn
                newSelected += channel.id
            }
        }
        _state.update { it.copy(suggestions = newSuggestions, selected = newSelected) }
    }

    fun setShowAll(showAll: Boolean) = _state.update { it.copy(showAll = showAll) }

    fun setMediaFilter(filter: MediaFilter) = _state.update { it.copy(mediaFilter = filter) }

    fun setSortMode(mode: ChannelSort) = _state.update { it.copy(sortMode = mode) }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }

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

    fun searchCandidates(query: String): List<LogoCandidate> =
        LogoMatching.search(query, catalogs())

    fun toggleIgnore(channelId: Long) {
        val current = _state.value.settings.ignoredChannels
        val updated = if (channelId in current) current - channelId else current + channelId
        updateSettings(_state.value.settings.copy(ignoredChannels = updated))
    }

    fun resetIgnored() {
        updateSettings(_state.value.settings.copy(ignoredChannels = emptySet()))
    }

    /** Every image on USB sticks and in the TV's storage for [channel], those named like it first. */
    fun fileCandidatesFor(channel: Channel): List<LogoCandidate> {
        val keys = com.berkbb.channellogos.logos.NameMatcher.channelKeys(channel.name).map { it.first }
        val base = keys.firstOrNull()?.let { com.berkbb.channellogos.logos.NameMatcher.withoutQuality(it) ?: it }.orEmpty()
        return fileLogos.files()
            .sortedBy { file ->
                val key = com.berkbb.channellogos.logos.NameMatcher.key(file.nameWithoutExtension)
                when {
                    key in keys -> 0
                    base.length >= 3 && (key.startsWith(base) || base.startsWith(key)) -> 1
                    else -> 2
                }
            }
            .map { fileCandidate(it) }
    }

    /** Where the file browser starts: USB sticks, then the TV's storage. */
    fun filePlaces(): List<FileLogoSource.Place> = fileLogos.places()

    /** The folders and images of [dir], for the file browser. */
    fun browse(dir: File): List<File> = fileLogos.browse(dir)

    fun fileCandidate(file: File): LogoCandidate =
        LogoCandidate(LogoSource.FILE, file.path, com.berkbb.channellogos.logos.NameMatcher.Strength.LOOSE, "file:${file.path}", LogoEntry.COUNTRY)

    /** Preview image of a logo, fitted like it will be written; null while it cannot be loaded. */
    suspend fun preview(candidate: LogoCandidate): ImageBitmap? {
        previews[candidate.id]?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val bytes = read(candidate)
                checkQuality(candidate, bytes)
                val png = LogoImage.fit(bytes)
                BitmapFactory.decodeByteArray(png, 0, png.size).asImageBitmap().also { previews[candidate.id] = it }
            }.onFailure { Log.w(TAG, "Preview of ${candidate.id} failed", it) }.getOrNull()
        }
    }

    /** The logo [channel] has on the TV now (for comparing in the picker); null if it has none. */
    suspend fun currentLogo(channel: Channel): ImageBitmap? {
        if (!channel.hasLogo) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val png = shellLock.withLock { shell.readLogos(listOf(channel.id)) }[channel.id] ?: return@runCatching null
                BitmapFactory.decodeByteArray(png, 0, png.size)?.asImageBitmap()
            }.onFailure { Log.w(TAG, "Current logo of ${channel.id}", it) }.getOrNull()
        }
    }

    /** Records [candidate]'s quality problems (drawn logos are made for the TV and have none). */
    private fun checkQuality(candidate: LogoCandidate, bytes: ByteArray): Set<LogoQuality.Issue> {
        if (candidate.source == LogoSource.TEXT) return emptySet()
        val issues = runCatching { LogoImage.quality(bytes) }.getOrDefault(emptySet())
        if (issues.isNotEmpty()) _state.update { it.copy(poorLogos = it.poorLogos + (candidate.id to issues)) }
        return issues
    }

    fun applySelected() = prepareWrite(_state.value.selected)

    /** Writes the suggestion of one channel (the row's quick menu), with the same confirmation. */
    fun applyOne(channelId: Long) = prepareWrite(listOf(channelId))

    /**
     * Downloads and fits the chosen logos, then asks for confirmation ([UiState.pendingWrite]):
     * how many replace an existing logo, which look poor, whether a backup is taken first.
     */
    private fun prepareWrite(ids: Collection<Long>) {
        val state = _state.value
        val chosen = ids.mapNotNull { id -> state.suggestions[id]?.let { id to it } }
        if (chosen.isEmpty() || state.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, status = Strings.preparing(0, chosen.size)) }
            val logos = LinkedHashMap<Long, ByteArray>()
            val poor = HashSet<Long>()
            withContext(Dispatchers.IO) {
                chosen.forEachIndexed { index, (id, candidate) ->
                    runCatching {
                        val bytes = read(candidate)
                        val issues = checkQuality(candidate, bytes)
                        logos[id] = LogoImage.fit(bytes)
                        if (issues.isNotEmpty()) poor += id
                    }.onFailure { Log.w(TAG, "Could not prepare ${candidate.id}", it) }
                    _state.update { it.copy(status = Strings.preparing(index + 1, chosen.size)) }
                }
            }
            if (logos.isEmpty()) {
                _state.update { it.copy(busy = false, status = null, outcome = failure(Strings.written(0, chosen.size))) }
                return@launch
            }
            prepared = logos
            preparedPoor = poor
            val byId = _state.value.channels.associateBy { it.id }
            val previewItems = logos.keys.take(5).mapNotNull { id ->
                val channel = byId[id] ?: return@mapNotNull null
                val candidate = _state.value.suggestions[id] ?: return@mapNotNull null
                PendingItem(id, channel.number, channel.name, candidate)
            }
            _state.update {
                it.copy(
                    busy = false,
                    status = null,
                    pendingWrite = PendingWrite(
                        total = logos.size,
                        replacing = logos.keys.count { id -> byId[id]?.hasLogo == true },
                        lowQuality = poor.size,
                        failed = chosen.size - logos.size,
                        autoBackup = needsAutoBackup(),
                        previewItems = previewItems
                    )
                )
            }
        }
    }

    /** Writes the prepared logos; [skipLowQuality] leaves out those that look poor. */
    fun confirmWrite(skipLowQuality: Boolean) {
        val pending = _state.value.pendingWrite ?: return
        val logos = if (skipLowQuality) prepared - preparedPoor else prepared
        val notPrepared = pending.failed
        cancelWrite()
        if (logos.isEmpty()) return
        runTask(Strings.writing(0, logos.size)) {
            val backedUp = if (needsAutoBackup()) {
                _state.update { it.copy(status = Strings.backingUp) }
                takeSnapshot() > 0
            } else false
            val written = writeKeepingOriginals(logos) { done -> _state.update { it.copy(status = Strings.writing(done, logos.size)) } }
            val failed = logos.size - written + notPrepared
            val message = Strings.written(written, failed) + if (backedUp) "\n\n${Strings.autoBackedUp}" else ""
            done(message, failed = failed)
        }
    }

    fun cancelWrite() {
        prepared = emptyMap()
        preparedPoor = emptySet()
        _state.update { it.copy(pendingWrite = null) }
    }

    /** The first write gets a full backup first, so nothing depends on remembering "Back Up Logos". */
    private fun needsAutoBackup() = backups.snapshotTime() == 0L && _state.value.channels.any { it.hasLogo }

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
            done(Strings.resetDone(done.size, targets.size - done.size), failed = targets.size - done.size)
        }
    }

    /** Saves every current logo of the TV, to be restored after a channel update removed them. */
    fun backupLogos() {
        val channels = _state.value.channels.filter { it.hasLogo }
        if (channels.isEmpty()) return
        runTask(Strings.backingUp) { done(Strings.backedUp(takeSnapshot())) }
    }

    /** Replaces the backup with every current logo; returns how many were saved. */
    private suspend fun takeSnapshot(): Int {
        val channels = _state.value.channels.filter { it.hasLogo }
        val logos = shellLock.withLock { shell.readLogos(channels.map { it.id }) }
        val byChannel = channels.mapNotNull { channel -> logos[channel.id]?.let { channel to it } }.toMap()
        if (byChannel.isNotEmpty()) {
            backups.saveSnapshot(byChannel)
            runCatching {
                val targets = usbBackupTargets()
                if (targets.isNotEmpty()) {
                    val meta = backupMeta()
                    for (target in targets) {
                        backups.exportSnapshot(target, meta)
                        Log.i(TAG, "Mirrored backup to USB: ${target.path}")
                    }
                }
            }.onFailure { Log.w(TAG, "USB backup mirror", it) }
        }
        return byChannel.size
    }

    /**
     * Folders for a backup on USB sticks: this app's own folder on each stick, which the app may
     * write without any permission. Another TV running this app finds it at the same place.
     */
    private fun usbBackupTargets(): List<File> =
        getApplication<Application>().getExternalFilesDirs(null).drop(1).filterNotNull()
            .filter { runCatching { android.os.Environment.getExternalStorageState(it) == android.os.Environment.MEDIA_MOUNTED }.getOrDefault(false) }
            .map { File(it, LogoBackups.USB_FOLDER) }

    /** Backups on USB sticks: in this app's folder there, or at a stick's top level (copied there with a computer). */
    private fun usbBackupSources(): List<File> {
        val topLevel = File("/storage").listFiles()
            ?.filter { it.isDirectory && it.name != "emulated" && it.name != "self" }
            ?.map { File(it, LogoBackups.USB_FOLDER) }
            .orEmpty()
        return (usbBackupTargets() + topLevel).filter { runCatching { LogoBackups.isSnapshot(it) }.getOrDefault(false) }
    }

    /** Saves the backup with the settings to a USB stick or the TV's shared storage. */
    fun exportBackup(place: BackupPlace) {
        _state.update { it.copy(choosingExport = false) }
        runTask(Strings.exporting) {
            val meta = backupMeta()
            when (place) {
                BackupPlace.USB -> {
                    val target = usbBackupTargets().firstOrNull() ?: return@runTask failure(Strings.noUsbStick)
                    done(Strings.exported(backups.exportSnapshot(target, meta), target.path))
                }
                BackupPlace.INTERNAL -> {
                    // Written to app storage first, then copied by the shell user into shared storage,
                    // where it survives uninstalling the app (not a factory reset).
                    val staging = shell.stagingDirectory("backup-out")
                    val count = backups.exportSnapshot(staging, meta)
                    val copied = shellLock.withLock { shell.copyFolder(staging.path, INTERNAL_BACKUP) }
                    staging.deleteRecursively()
                    if (!copied) return@runTask failure(Strings.internalCopyFailed)
                    done(Strings.exported(count, INTERNAL_BACKUP))
                }
            }
        }
    }

    fun chooseExport(open: Boolean) = _state.update { it.copy(choosingExport = open) }

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val size = runCatching {
                val cache = getApplication<Application>().cacheDir
                cache.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            }.getOrDefault(0L)
            _state.update { it.copy(cacheSizeBytes = size) }
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val cache = getApplication<Application>().cacheDir
                cache.listFiles()?.forEach { it.deleteRecursively() }
            }
            val size = runCatching {
                val cache = getApplication<Application>().cacheDir
                cache.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            }.getOrDefault(0L)
            withContext(Dispatchers.Main) {
                _state.update { it.copy(cacheSizeBytes = size, outcome = Outcome(true, Strings.cacheSizeTitle, Strings.cacheCleared)) }
            }
        }
    }

    fun exportChannelsCsv() {
        if (_state.value.busy) return
        runTask(Strings.exportingChannels) {
            val channels = _state.value.channels
            val suggestions = _state.value.suggestions
            if (channels.isEmpty()) return@runTask failure(Strings.noChannelsTitle)

            val csv = buildString {
                append("Number,Name,Type,HasLogo,SuggestedLogo,SuggestedSource,Strength\n")
                for (ch in channels) {
                    val sug = suggestions[ch.id]
                    val type = if (ch.isRadio) "Radio" else "TV"
                    val hasLogo = if (ch.hasLogo) "Yes" else "No"
                    val sugName = sug?.name?.replace("\"", "\"\"") ?: ""
                    val sugSrc = sug?.source?.name ?: ""
                    val sugStr = sug?.strength?.name ?: ""
                    append("\"${ch.number}\",\"${ch.name.replace("\"", "\"\"")}\",\"$type\",\"$hasLogo\",\"$sugName\",\"$sugSrc\",\"$sugStr\"\n")
                }
            }

            val usbTarget = usbBackupTargets().firstOrNull()
            val targetFile = if (usbTarget != null) {
                File(usbTarget, "channels.csv")
            } else {
                val dir = File(getApplication<Application>().filesDir, "export")
                dir.mkdirs()
                File(dir, "channels.csv")
            }

            targetFile.parentFile?.mkdirs()
            targetFile.writeBytes(csv.toByteArray(Charsets.UTF_8))

            if (usbTarget == null) {
                shellLock.withLock {
                    runCatching { shell.copyFolder(targetFile.parentFile!!.path, INTERNAL_BACKUP) }
                }
            }
            done(Strings.exportedChannels(channels.size, targetFile.path))
        }
    }

    /** Settings and creation time stored with an exported backup. */
    private fun backupMeta(): Map<String, String> {
        val version = runCatching {
            getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0).versionName
        }.getOrNull().orEmpty()
        return mapOf("created" to System.currentTimeMillis().toString(), "app_version" to version) +
            _state.value.settings.toMap().mapKeys { "setting.${it.key}" }
    }

    /** Lists the backups on USB sticks and in the TV's shared storage, checked and matched against the channels. */
    fun findBackups() {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, status = Strings.searchingBackups) }
            val found = withContext(Dispatchers.IO) {
                val usb = usbBackupSources().map { describeBackup(BackupPlace.USB, it) }
                val internal = runCatching {
                    // The shell copies it into app storage, where the app may read it on every Android version.
                    val staging = shell.stagingDirectory("backup-in")
                    val copied = shellLock.withLock { shell.exists("$INTERNAL_BACKUP/${LogoBackups.MANIFEST}") && shell.copyFolder(INTERNAL_BACKUP, staging.path) }
                    if (copied && LogoBackups.isSnapshot(staging)) listOf(describeBackup(BackupPlace.INTERNAL, staging)) else emptyList()
                }.onFailure { Log.w(TAG, "Internal backup", it) }.getOrDefault(emptyList())
                (usb + internal).sortedByDescending { it.created }
            }
            _state.update { it.copy(busy = false, status = null, foundBackups = found) }
        }
    }

    fun closeBackups() = _state.update { it.copy(foundBackups = null) }

    private fun describeBackup(place: BackupPlace, folder: File): FoundBackup {
        val entries = LogoBackups.readManifest(folder)
        val verification = LogoBackups.verify(folder)
        val meta = LogoBackups.readMeta(folder)
        val usable = entries.filter { it.file.name !in verification.bad }
        return FoundBackup(
            place = place,
            folder = folder,
            created = meta["created"]?.toLongOrNull() ?: File(folder, LogoBackups.MANIFEST).lastModified(),
            logos = entries.size,
            matched = LogoBackups.match(usable, _state.value.channels).size,
            checked = verification.checked,
            damaged = if (verification.manifestOk) verification.bad.size else entries.size,
            hasSettings = meta.keys.any { it.startsWith("setting.") }
        )
    }

    /**
     * Takes [found] over as this TV's backup (logos whose checksum fails are left out) and applies
     * its settings; "Restore Backup" then writes the logos.
     */
    fun importBackup(found: FoundBackup) {
        _state.update { it.copy(foundBackups = null) }
        runTask(Strings.importing) {
            val imported = backups.importSnapshot(found.folder)
            val settings = imported.meta.filterKeys { it.startsWith("setting.") }.mapKeys { it.key.removePrefix("setting.") }
            if (settings.isNotEmpty()) {
                val restored = Settings.fromMap(settings)
                settingsStore.save(restored)
                Strings.setLanguage(restored.language)
                _state.update { it.copy(settings = restored) }
            }
            if (found.place == BackupPlace.INTERNAL) found.folder.deleteRecursively()
            val message = Strings.imported(imported.logos) +
                (if (settings.isNotEmpty()) "\n\n${Strings.settingsRestored}" else "") +
                (if (imported.damaged > 0) "\n\n${Strings.damagedSkipped(imported.damaged)}" else "")
            done(message, failed = imported.damaged)
        }
    }

    /** How many of the backup's logos find their channel on this TV now: (matched, total). */
    fun backupMatch(): Pair<Int, Int> {
        val entries = backups.snapshotEntries()
        return LogoBackups.match(entries, _state.value.channels).size to entries.size
    }

    /** Prepares backup restoration with a visual preview of the first matched channels. */
    fun prepareRestore() {
        val (matchedCount, totalCount) = backupMatch()
        val changed = matchedCount < totalCount * MATCH_SHARE
        val entries = backups.snapshotEntries()
        val matched = LogoBackups.match(entries, _state.value.channels)
        val preview = matched.entries.take(5).map { (channel, entry) ->
            PendingItem(
                channelId = channel.id,
                channelNumber = channel.number,
                channelName = channel.name,
                candidate = fileCandidate(entry.file)
            )
        }
        _state.update {
            it.copy(
                pendingRestore = PendingRestore(
                    matched = matchedCount,
                    total = totalCount,
                    channelsChanged = changed,
                    previewItems = preview
                )
            )
        }
    }

    fun confirmRestore() {
        _state.update { it.copy(pendingRestore = null) }
        restoreBackup()
    }

    fun cancelRestore() {
        _state.update { it.copy(pendingRestore = null) }
    }

    /** Writes the backed-up logos back, matching channels by number and name; damaged files are skipped. */
    fun restoreBackup() {
        runTask(Strings.restoring) {
            val bad = backups.verifySnapshot().bad
            val entries = backups.snapshotEntries().filter { it.file.name !in bad }
            val matched = LogoBackups.match(entries, _state.value.channels)
            val logos = matched.entries.associate { (channel, entry) -> channel.id to entry.file.readBytes() }
            val written = writeKeepingOriginals(logos) { }
            val missed = backups.snapshotEntries().size - written
            val message = Strings.restored(written, missed) + if (bad.isNotEmpty()) "\n\n${Strings.damagedSkipped(bad.size)}" else ""
            done(message, failed = missed)
        }
    }

    /**
     * Runs [work] (on IO, under the busy state), then rereads the channels and shows the outcome it
     * returns full screen. Failures are shown too.
     */
    private fun runTask(progress: String, work: suspend () -> Outcome) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, status = progress) }
            val result = try {
                withContext(Dispatchers.IO) { work() }
            } catch (e: Exception) {
                Log.w(TAG, "Task failed", e)
                failure(Strings.writeFailed(e.message ?: e.javaClass.simpleName))
            }
            val scanError = showChannels(withContext(Dispatchers.IO) { runCatching { readChannels() } })
            _state.update { it.copy(busy = false, status = null, outcome = if (result.success) scanError ?: result else result) }
        }
    }

    /** A finished task; with [failed] items it is shown as a problem, with what to do about it. */
    private fun done(message: String, failed: Int = 0) =
        if (failed > 0) failure(message) else Outcome(true, Strings.doneTitle, message)

    private fun failure(message: String) = Outcome(false, Strings.errorTitle, "$message\n\n${Strings.errorHint}")

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
            shellLock.withLock { shell.writeLogos(files, onProgress, _state.value.settings.restartLiveTv) }
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
            val scanError = showChannels(withContext(Dispatchers.IO) { runCatching { readChannels() } })
            _state.update {
                it.copy(outcome = scanError
                    ?: if (it.gitHubFailed) failure(Strings.gitHubUnavailable)
                    else Outcome(true, Strings.updatedTitle, Strings.sourcesUpdated))
            }
        }
    }

    private suspend fun loadSources(refresh: Boolean = false) {
        _state.update { it.copy(busy = true, status = if (refresh) Strings.updatingSources else Strings.loadingSources) }
        withContext(Dispatchers.IO) {
            var t0 = System.currentTimeMillis()
            runCatching { builtIn.load(country) }.onFailure { Log.w(TAG, "Built-in logo table", it) }
            Log.i(TAG, "Built-in logo table: ${builtIn.size} keys in ${System.currentTimeMillis() - t0} ms")
            t0 = System.currentTimeMillis()
            val gitHubLoaded = runCatching { gitHub.load(country, refresh, extraCountries) }.onFailure { Log.w(TAG, "GitHub logos", it) }.isSuccess
            Log.i(TAG, "GitHub logos: ${gitHub.size} keys in ${System.currentTimeMillis() - t0} ms")
            t0 = System.currentTimeMillis()
            runCatching { picons.load(country, refresh, extraCountries) }.onFailure { Log.w(TAG, "picons", it) }
            Log.i(TAG, "picons: ${picons.services().size} services, ${picons.entries().size} name keys in ${System.currentTimeMillis() - t0} ms")
            t0 = System.currentTimeMillis()
            runCatching { iptv.load(country, refresh, extraCountries) }.onFailure { Log.w(TAG, "iptv-org logos", it) }
            Log.i(TAG, "iptv-org logos: ${iptv.size} keys in ${System.currentTimeMillis() - t0} ms")
            _state.update {
                it.copy(
                    tvLogos = builtIn.size,
                    gitHubLogos = gitHub.size,
                    piconsLogos = picons.size,
                    iptvLogos = iptv.size,
                    sourcesUpdatedAt = maxOf(gitHub.updatedAt(country), picons.updatedAt(), iptv.updatedAt()),
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
            files = if (settings.usb) fileLogos.entries() else emptyMap(),
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
            LogoSource.PICONS -> picons.read(ref, _state.value.settings.piconsStyle)
            LogoSource.IPTV -> iptv.read(ref)
            LogoSource.TEXT -> LogoImage.text(candidate.name)
            LogoSource.FILE -> fileLogos.read(ref)
        }
    }

    companion object {
        /** A backup whose logos find their channel less often than this means the channels changed a lot. */
        const val MATCH_SHARE = 0.8
        private const val TAG = "ChannelLogos"
        /** The backup in the TV's shared storage; it survives uninstalling the app. */
        private const val INTERNAL_BACKUP = "/sdcard/${LogoBackups.USB_FOLDER}"
        /** Errors meaning the TV or shell user does not support or allow reading/writing channel logos. */
        private val NO_ACCESS_MARKERS = listOf(
            "ACCESS_ALL_EPG_DATA",
            "SecurityException",
            "Permission Denial",
            "TV provider not available",
            "getContentProviderExternal not found",
            "does not support channel logo",
            "TV provider channel query returned null"
        )
    }
}

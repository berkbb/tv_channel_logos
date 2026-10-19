package com.berkbb.channellogos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.berkbb.channellogos.logos.FileLogoSource
import com.berkbb.channellogos.logos.LogoCandidate
import com.berkbb.channellogos.logos.LogoMatching
import com.berkbb.channellogos.logos.LogoSource
import com.berkbb.channellogos.logos.NameMatcher
import com.berkbb.channellogos.settings.Settings
import com.berkbb.channellogos.tv.Channel
import java.io.File

/** Light or dark palette (Pitwall Updater's and AynaCast's); Compose state, so switching redraws everything. */
private object Palette {
    var dark by mutableStateOf(true)
}

private val Background get() = if (Palette.dark) Color(0xFF111318) else Color(0xFFF2F2F7)
private val Surface get() = if (Palette.dark) Color(0xFF1C1F26) else Color(0xFFFFFFFF)
private val SurfaceVariant get() = if (Palette.dark) Color(0xFF272B34) else Color(0xFFE5E5EA)
private val Accent = Color(0xFF0A84FF)
private val Green get() = if (Palette.dark) Color(0xFF30D158) else Color(0xFF248A3D)
private val Orange get() = if (Palette.dark) Color(0xFFFF9F0A) else Color(0xFFC93400)
private val TextPrimary get() = if (Palette.dark) Color(0xFFF5F5F7) else Color(0xFF1C1C1E)
private val TextSecondary get() = if (Palette.dark) Color(0xFF949498) else Color(0xFF6C6C70)
/** A focused button fills with this and its text turns [OnFocus], as in Pitwall Updater. */
private val FocusFill get() = if (Palette.dark) Color(0xFFE0E0E0) else Color(0xFF1C1C1E)
private val OnFocus get() = if (Palette.dark) Color(0xFF111318) else Color.White

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    var picking by remember { mutableStateOf<Channel?>(null) }
    var quickMenu by remember { mutableStateOf<Channel?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    // Android only reports a dark mode from version 10 on; older TVs ("false" then) get the TV-usual dark look.
    val systemDark = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q ||
        androidx.compose.foundation.isSystemInDarkTheme()
    val dark = when (state.settings.theme) {
        Settings.THEME_DARK -> true
        Settings.THEME_LIGHT -> false
        else -> systemDark
    }
    if (Palette.dark != dark) Palette.dark = dark

    // Typing a letter or digit anywhere on the main screen starts a channel search (keyboard or remote digits).
    var search by remember { mutableStateOf(TextFieldValue(state.query)) }
    var searchFocused by remember { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }
    val onSearch: (TextFieldValue) -> Unit = { search = it; viewModel.setQuery(it.text) }

    var showBackups by remember { mutableStateOf(false) }
    var focusedChannel by remember { mutableStateOf<Channel?>(null) }
    var showLogoInfo by remember { mutableStateOf<Channel?>(null) }
    var showHealthReport by remember { mutableStateOf(false) }
    val connected = state.connection == MainViewModel.Connection.Connected

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    if (state.unsupportedReason != null) {
        UnsupportedTvScreen(reason = state.unsupportedReason!!)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .onPreviewKeyEvent { event ->
                if (event.type != androidx.compose.ui.input.key.KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    android.view.KeyEvent.KEYCODE_INFO,
                    android.view.KeyEvent.KEYCODE_GUIDE -> {
                        focusedChannel?.let {
                            showLogoInfo = it
                            return@onPreviewKeyEvent true
                        }
                    }
                    android.view.KeyEvent.KEYCODE_PROG_RED -> {
                        if (state.selected.isNotEmpty()) {
                            viewModel.selectNone()
                            return@onPreviewKeyEvent true
                        } else if (search.text.isNotEmpty()) {
                            onSearch(TextFieldValue(""))
                            return@onPreviewKeyEvent true
                        }
                    }
                    android.view.KeyEvent.KEYCODE_PROG_GREEN -> {
                        if (!state.busy) {
                            if (state.selected.isEmpty()) viewModel.selectSafe() else viewModel.selectNone()
                            return@onPreviewKeyEvent true
                        }
                    }
                    android.view.KeyEvent.KEYCODE_PROG_YELLOW -> {
                        if (!state.busy) {
                            viewModel.setShowAll(!state.showAll)
                            return@onPreviewKeyEvent true
                        }
                    }
                    android.view.KeyEvent.KEYCODE_PROG_BLUE -> {
                        if (!state.busy && state.selected.isNotEmpty()) {
                            viewModel.applySelected()
                            return@onPreviewKeyEvent true
                        }
                    }
                    android.view.KeyEvent.KEYCODE_PAGE_DOWN,
                    android.view.KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                        scope.launch {
                            val nextIndex = minOf(listState.firstVisibleItemIndex + 8, state.visibleChannels.lastIndex.coerceAtLeast(0))
                            listState.animateScrollToItem(nextIndex)
                        }
                        return@onPreviewKeyEvent true
                    }
                    android.view.KeyEvent.KEYCODE_PAGE_UP,
                    android.view.KeyEvent.KEYCODE_CHANNEL_UP -> {
                        scope.launch {
                            val prevIndex = maxOf(listState.firstVisibleItemIndex - 8, 0)
                            listState.animateScrollToItem(prevIndex)
                        }
                        return@onPreviewKeyEvent true
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                    android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        val currentIdx = listState.firstVisibleItemIndex
                        val channels = state.visibleChannels
                        if (channels.isNotEmpty() && currentIdx in channels.indices) {
                            val currentLetter = channels[currentIdx].name.firstOrNull()?.uppercaseChar() ?: ' '
                            for (i in (currentIdx + 1)..channels.lastIndex) {
                                val l = channels[i].name.firstOrNull()?.uppercaseChar() ?: ' '
                                if (l != currentLetter) {
                                    scope.launch { listState.animateScrollToItem(i) }
                                    return@onPreviewKeyEvent true
                                }
                            }
                        }
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_REWIND,
                    android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        val currentIdx = listState.firstVisibleItemIndex
                        val channels = state.visibleChannels
                        if (channels.isNotEmpty() && currentIdx in channels.indices) {
                            val currentLetter = channels[currentIdx].name.firstOrNull()?.uppercaseChar() ?: ' '
                            var targetLetter: Char? = null
                            for (i in (currentIdx - 1) downTo 0) {
                                val l = channels[i].name.firstOrNull()?.uppercaseChar() ?: ' '
                                if (l != currentLetter) {
                                    targetLetter = l
                                    break
                                }
                            }
                            if (targetLetter != null) {
                                for (i in 0 until currentIdx) {
                                    if ((channels[i].name.firstOrNull()?.uppercaseChar() ?: ' ') == targetLetter) {
                                        scope.launch { listState.animateScrollToItem(i) }
                                        return@onPreviewKeyEvent true
                                    }
                                }
                            }
                        }
                    }
                }
                if (searchFocused) return@onPreviewKeyEvent false
                if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return@onPreviewKeyEvent false
                val code = event.utf16CodePoint
                if (code == 0 || !Character.isLetterOrDigit(code)) return@onPreviewKeyEvent false
                val text = search.text + String(Character.toChars(code))
                onSearch(TextFieldValue(text, TextRange(text.length)))
                if (text.all { it.isDigit() }) {
                    val matchIndex = state.visibleChannels.indexOfFirst { it.number == text }
                    if (matchIndex >= 0) {
                        scope.launch { listState.animateScrollToItem(matchIndex) }
                    }
                }
                runCatching { searchFocus.requestFocus() }
                true
            }
            .padding(horizontal = 40.dp, vertical = 28.dp)
    ) {
        Header(
            state,
            onBackups = { showBackups = true },
            onSettings = { showSettings = true },
            onAbout = { showAbout = true },
            onHealthReport = { showHealthReport = true }
        )
        Spacer(Modifier.height(16.dp))
        if (!connected) {
            ConnectionCard(state, viewModel, modifier = Modifier.weight(1f).fillMaxWidth())
            return@Column
        }
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ActionPanel(
                state, viewModel,
                onConfirm = { message, action -> confirming = message to action },
                modifier = Modifier.width(300.dp).fillMaxHeight()
            )
            Spacer(Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                SearchField(
                    value = search,
                    onValueChange = onSearch,
                    focusRequester = searchFocus,
                    onFocusChange = { searchFocused = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                if (state.query.isNotBlank() && state.visibleChannels.isEmpty() && state.hiddenMatches == 0) {
                    Text(Strings.noMatches, color = TextSecondary, fontSize = 15.sp, modifier = Modifier.padding(8.dp))
                }
                // Searching for a channel that already has a logo should not end in "nothing found".
                val hidden = state.hiddenMatches
                if (hidden > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                        Text(Strings.hiddenMatches(hidden), color = TextSecondary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(12.dp))
                        TvButton(Strings.searchAll, small = true, onClick = { viewModel.setShowAll(true) })
                    }
                }
                notes(state).forEach { (text, color) ->
                    Text(text, color = color, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                }
                val visible = state.visibleChannels
                if (visible.isNotEmpty()) {
                    Text(
                        Strings.listCounter(
                            visible.size,
                            visible.count { it.id in state.selected },
                            visible.count { it.id in state.suggestions }
                        ) + "  ·  " + Strings.longPressHint,
                        color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                } else if (state.noTunerChannels && !state.busy) {
                    Text(Strings.noChannelsExplained, color = Orange, fontSize = 15.sp, modifier = Modifier.padding(8.dp))
                }
                ChannelList(
                    state, viewModel,
                    listState = listState,
                    onFocus = { focusedChannel = it },
                    onChange = { picking = it },
                    onLongPress = { quickMenu = it },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }

    if (showBackups) {
        BackupWindow(state, viewModel, onConfirm = { message, action -> confirming = message to action }, onDismiss = { showBackups = false })
    }
    if (state.choosingExport) {
        TvDialog(title = Strings.exportTitle, onDismiss = { viewModel.chooseExport(false) }, maxWidth = 620.dp) {
            Text(Strings.exportDesc, color = TextSecondary, fontSize = 14.sp)
            val first = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
            TvButton(Strings.placeUsb, primary = true, onClick = { viewModel.exportBackup(MainViewModel.BackupPlace.USB) },
                modifier = Modifier.fillMaxWidth().focusRequester(first))
            TvButton(Strings.placeInternal, onClick = { viewModel.exportBackup(MainViewModel.BackupPlace.INTERNAL) },
                modifier = Modifier.fillMaxWidth())
        }
    }
    state.foundBackups?.let { found ->
        BackupList(found, onPick = { backup ->
            viewModel.closeBackups()
            confirming = Strings.confirmImportFound(backup.channelsChanged) to { viewModel.importBackup(backup) }
        }, onDismiss = viewModel::closeBackups)
    }
    quickMenu?.let { channel ->
        QuickMenu(
            channel = channel,
            hasSuggestion = channel.id in state.suggestions,
            selected = channel.id in state.selected,
            changed = channel.id in state.changed,
            isIgnored = channel.id in state.settings.ignoredChannels,
            onWrite = { quickMenu = null; viewModel.applyOne(channel.id) },
            onToggle = { quickMenu = null; viewModel.toggle(channel.id) },
            onChange = { quickMenu = null; picking = channel },
            onInfo = { showLogoInfo = channel; quickMenu = null },
            onToggleIgnore = {
                quickMenu = null
                viewModel.toggleIgnore(channel.id)
            },
            onReset = {
                quickMenu = null
                confirming = Strings.confirmReset(1) to { viewModel.resetToDefault(listOf(channel.id)) }
            },
            onDismiss = { quickMenu = null }
        )
    }
    showLogoInfo?.let { channel ->
        LogoInfoDialog(
            channel = channel,
            suggestion = state.suggestions[channel.id],
            poor = state.suggestions[channel.id]?.id in state.poorLogos,
            viewModel = viewModel,
            onDismiss = { showLogoInfo = null }
        )
    }
    picking?.let { channel ->
        LogoPicker(channel, viewModel, changed = channel.id in state.changed, onDismiss = { picking = null })
    }
    if (showSettings) {
        LaunchedEffect(Unit) { viewModel.refreshCacheSize() }
        val sourcesInfo = Strings.sourceStats(state.tvLogos, state.gitHubLogos, state.piconsLogos, state.iptvLogos) +
            (if (state.fileLogos > 0) "  ·  " + Strings.fileStats(state.fileLogos) else "") +
            (if (state.sourcesUpdatedAt > 0L) "\n" + Strings.sourcesUpdatedAt(Strings.dateTime(state.sourcesUpdatedAt)) else "")
        SettingsDialog(
            settings = state.settings,
            sourcesInfo = sourcesInfo,
            cacheSize = state.cacheSizeBytes,
            onClearCache = viewModel::clearCache,
            onChange = viewModel::updateSettings,
            onDismiss = { showSettings = false }
        )
    }
    if (showAbout) AboutDialog(viewModel, onDismiss = { showAbout = false })
    if (showHealthReport) HealthReportDialog(state, onDismiss = { showHealthReport = false })
    // Progress and results cover the whole screen: a line at the bottom of the panel went unseen.
    if (state.busy && state.connection == MainViewModel.Connection.Connected) BusyScreen(state.status)
    else if (state.outcome != null) OutcomeScreen(state.outcome!!, onDismiss = viewModel::dismissOutcome, onAction = { viewModel.installUpdate() })
    else if (state.pendingWrite != null) WriteConfirmScreen(state.pendingWrite!!, viewModel, onWrite = viewModel::confirmWrite, onCancel = viewModel::cancelWrite)
    else if (state.pendingRestore != null) RestoreConfirmScreen(state.pendingRestore!!, viewModel, onRestore = viewModel::confirmRestore, onCancel = viewModel::cancelRestore)
    confirming?.let { (message, action) ->
        TvDialog(
            title = Strings.confirm,
            onDismiss = { confirming = null },
            maxWidth = 560.dp,
            footer = {
                TvButton(Strings.yes, primary = true, onClick = { confirming = null; action() })
                TvButton(Strings.no, onClick = { confirming = null })
            }
        ) {
            Text(message, color = TextPrimary, fontSize = 16.sp)
        }
    }
}

/** Title with the accent bar, the sources line, and the window buttons on the right. */
@Composable
private fun Header(
    state: MainViewModel.UiState,
    onBackups: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onHealthReport: () -> Unit
) {
    val connected = state.connection == MainViewModel.Connection.Connected
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccentBar()
                Spacer(Modifier.width(14.dp))
                Text(Strings.appTitle, color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(8.dp))
            if (connected) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FocusBox(onClick = onHealthReport) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Badge("${Strings.channelCount(state.channels.size)}  ·  ${Strings.radioCount(state.radios)}")
                            Badge(Strings.withLogoCount(state.withLogo), dot = Green)
                            Badge(Strings.withoutLogoCount(state.channels.size - state.withLogo), dot = Orange)
                        }
                    }
                    Badge(Strings.connected, dot = Green)
                }
            } else {
                Text(Strings.appSubtitle, color = TextSecondary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (connected) {
                TvButton(Strings.healthReportTitle, icon = "📊", onClick = onHealthReport)
                TvButton(Strings.backupSection, icon = "💾", onClick = onBackups)
            }
            TvButton(Strings.settings, icon = "⚙️", onClick = onSettings)
            TvButton(Strings.about, icon = "ℹ️", onClick = onAbout)
        }
    }
}

/** Vertical accent bar before screen and window titles. */
@Composable
private fun AccentBar(height: androidx.compose.ui.unit.Dp = 30.dp, color: Color = Accent) {
    Box(Modifier.width(6.dp).height(height).background(color, RoundedCornerShape(3.dp)))
}

/** A small count or state under the title, with an optional coloured dot. */
@Composable
private fun Badge(text: String, dot: Color? = null) {
    Row(
        modifier = Modifier.background(Surface, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dot != null) {
            Box(Modifier.size(8.dp).background(dot, CircleShape))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun InfoCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary,
    dot: Color? = null,
    extra: String? = null
) {
    Column(
        modifier = modifier.height(76.dp).background(Surface, RoundedCornerShape(12.dp)).padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Caption(title.uppercase(Strings.locale))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (dot != null) {
                Box(Modifier.size(10.dp).background(dot, CircleShape))
                Spacer(Modifier.width(8.dp))
            }
            Text(value, color = valueColor, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (extra != null) {
                Spacer(Modifier.width(8.dp))
                Text(extra, color = TextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Notices about the sources, shown only when there is something to say. */
private fun notes(state: MainViewModel.UiState): List<Pair<String, Color>> = buildList {
    // The header's badge row is cut off by the buttons on a 1080p TV; this line is full width.
    (state.updateStatus as? MainViewModel.UpdateStatus.Available)?.let { add(Strings.updateNote(it.version) to Accent) }
    if (state.extraCountries.isNotEmpty()) {
        val names = state.extraCountries.joinToString { java.util.Locale.Builder().setRegion(it).build().getDisplayCountry(Strings.locale) }
        add(Strings.extraCountries(names) to Accent)
    }
    if (state.tvTableMissing) add(Strings.tvTableMissing to Orange)
    if (state.gitHubFailed) add(Strings.gitHubUnavailable to Orange)
}

/** Connecting, or what to do when the connection failed; fills the screen under the header. */
@Composable
private fun ConnectionCard(state: MainViewModel.UiState, viewModel: MainViewModel, modifier: Modifier) {
    Column(
        modifier = modifier.background(Surface, RoundedCornerShape(12.dp)).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val connection = state.connection) {
            is MainViewModel.Connection.Failed -> {
                val retry = remember { FocusRequester() }
                LaunchedEffect(Unit) { runCatching { retry.requestFocus() } }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).background(Orange, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text(Strings.connectFailed, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Text(connection.message, color = TextSecondary, fontSize = 13.sp, maxLines = 3, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text(Strings.networkDebuggingHint, color = TextPrimary, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                TvButton(Strings.retry, primary = true, onClick = viewModel::start, modifier = Modifier.focusRequester(retry))
            }
            else -> {
                CircularProgressIndicator(color = Accent, strokeWidth = 5.dp, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(8.dp))
                Text(Strings.connecting, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(Strings.approveHint, color = TextSecondary, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

/** The main actions, then the list and selection choices; scrolls when it does not fit. */
@Composable
private fun ActionPanel(
    state: MainViewModel.UiState,
    viewModel: MainViewModel,
    onConfirm: (message: String, action: () -> Unit) -> Unit,
    modifier: Modifier
) {
    val idle = !state.busy
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Caption(Strings.actionsSection.uppercase(Strings.locale))
        // Main actions: write the selection, or undo everything the app wrote.
        val applyFocus = remember { FocusRequester() }
        TvButton(
            Strings.apply(state.selected.size),
            icon = "✓",
            primary = true,
            enabled = state.selected.isNotEmpty() && idle,
            onClick = viewModel::applySelected,
            modifier = Modifier.fillMaxWidth().focusRequester(applyFocus)
        )
        LaunchedEffect(Unit) { runCatching { applyFocus.requestFocus() } }
        TvButton(
            Strings.resetToDefault(state.changed.size),
            icon = "↺",
            enabled = idle && state.changed.isNotEmpty(),
            onClick = { onConfirm(Strings.confirmReset(state.changed.size)) { viewModel.resetToDefault() } },
            modifier = Modifier.fillMaxWidth()
        )

        PanelSection(Strings.listSection)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvButton(Strings.showMissing, selected = !state.showAll, small = true, onClick = { viewModel.setShowAll(false) }, modifier = Modifier.weight(1f))
            TvButton(Strings.showAll, selected = state.showAll, small = true, onClick = { viewModel.setShowAll(true) }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvButton(Strings.filterAllMedia, selected = state.mediaFilter == MainViewModel.MediaFilter.ALL, small = true, onClick = { viewModel.setMediaFilter(MainViewModel.MediaFilter.ALL) }, modifier = Modifier.weight(1f))
            TvButton(Strings.filterTvOnly, selected = state.mediaFilter == MainViewModel.MediaFilter.TV_ONLY, small = true, onClick = { viewModel.setMediaFilter(MainViewModel.MediaFilter.TV_ONLY) }, modifier = Modifier.weight(1f))
            TvButton(Strings.filterRadioOnly, selected = state.mediaFilter == MainViewModel.MediaFilter.RADIO_ONLY, small = true, onClick = { viewModel.setMediaFilter(MainViewModel.MediaFilter.RADIO_ONLY) }, modifier = Modifier.weight(1f))
        }

        PanelSection(Strings.sortSection)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvButton(Strings.sortNumber, selected = state.sortMode == MainViewModel.ChannelSort.NUMBER, small = true, onClick = { viewModel.setSortMode(MainViewModel.ChannelSort.NUMBER) }, modifier = Modifier.weight(1f))
            TvButton(Strings.sortName, selected = state.sortMode == MainViewModel.ChannelSort.NAME, small = true, onClick = { viewModel.setSortMode(MainViewModel.ChannelSort.NAME) }, modifier = Modifier.weight(1f))
            TvButton(Strings.sortMissingFirst, selected = state.sortMode == MainViewModel.ChannelSort.MISSING_FIRST, small = true, onClick = { viewModel.setSortMode(MainViewModel.ChannelSort.MISSING_FIRST) }, modifier = Modifier.weight(1f))
        }

        PanelSection(Strings.selectionSection)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvButton(Strings.selectSafe, onClick = viewModel::selectSafe, enabled = idle, small = true, modifier = Modifier.weight(1f))
            TvButton(Strings.selectAll, onClick = viewModel::selectAll, enabled = idle, small = true, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvButton(Strings.selectDrawnLogos, onClick = viewModel::selectDrawnLogos, enabled = idle, small = true, modifier = Modifier.weight(1f))
            TvButton(Strings.selectNone, onClick = viewModel::selectNone, enabled = idle && state.selected.isNotEmpty(), small = true, modifier = Modifier.weight(1f))
        }

        PanelSection(Strings.updateSection)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvButton(Strings.rescan, icon = "⟳", onClick = viewModel::scan, enabled = idle, small = true, modifier = Modifier.weight(1f))
            TvButton(Strings.refreshSources, icon = "⇣", onClick = viewModel::refreshSources, enabled = idle, small = true, modifier = Modifier.weight(1f))
        }

        PanelSection(Strings.shortcutsSection)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShortcutBadge(Color(0xFFFF3B30), Strings.shortcutClear)
            ShortcutBadge(Color(0xFF34C759), Strings.shortcutSelectSafe)
            ShortcutBadge(Color(0xFFFFCC00), Strings.shortcutToggleView)
            ShortcutBadge(Color(0xFF007AFF), Strings.shortcutWrite)
        }
    }
}

@Composable
private fun ShortcutBadge(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
    }
}

/** Back up, restore, export and import the logos, in their own window. */
@Composable
private fun BackupWindow(
    state: MainViewModel.UiState,
    viewModel: MainViewModel,
    onConfirm: (message: String, action: () -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val idle = !state.busy
    TvDialog(title = Strings.backupSection, onDismiss = onDismiss, fullScreen = true) {
        val backupDate = if (state.backupTime == 0L) null else Strings.dateTime(state.backupTime)
        Column(
            modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(12.dp)).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(backupDate?.let { Strings.backupInfo(state.backupCount, it) } ?: Strings.noBackup, color = TextPrimary, fontSize = 16.sp)
            val first = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvButton(Strings.backupLogos, icon = "💾", primary = true, enabled = idle && state.withLogo > 0,
                    onClick = { onConfirm(Strings.confirmBackup) { viewModel.backupLogos() } }, modifier = Modifier.weight(1f).focusRequester(first))
                TvButton(Strings.restoreBackup, icon = "↺", enabled = idle && state.backupCount > 0,
                    onClick = viewModel::prepareRestore, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvButton(Strings.exportBackup, icon = "⇡", enabled = idle && state.backupCount > 0,
                    onClick = { viewModel.chooseExport(true) }, modifier = Modifier.weight(1f))
                TvButton(Strings.importBackup, icon = "⇣", enabled = idle,
                    onClick = viewModel::findBackups, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvButton(Strings.exportChannelsCsv, icon = "📋", enabled = idle && state.channels.isNotEmpty(),
                    onClick = viewModel::exportChannelsCsv, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Small spaced grey caption, as on the Pitwall Updater and AynaCast cards. */
@Composable
private fun Caption(text: String) {
    Text(text, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, maxLines = 1)
}

/** Small heading over a group of panel buttons. */
@Composable
private fun PanelSection(title: String) {
    Box(Modifier.padding(top = 8.dp)) { Caption(title.uppercase(Strings.locale)) }
}

/**
 * Search box over the channel list. Up/down leave it like any other control (a text field would
 * keep them for its cursor); Escape clears it.
 */
@Composable
private fun SearchField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier
) {
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = TextPrimary, fontSize = 17.sp),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused; onFocusChange(it.isFocused) }
            .onPreviewKeyEvent { event ->
                if (event.type != androidx.compose.ui.input.key.KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    androidx.compose.ui.input.key.Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                    androidx.compose.ui.input.key.Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
                    androidx.compose.ui.input.key.Key.Escape -> {
                        if (value.text.isEmpty()) false else { onValueChange(TextFieldValue("")); true }
                    }
                    else -> false
                }
            },
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (focused) SurfaceVariant else Surface, RoundedCornerShape(12.dp))
                    .border(if (focused) BorderStroke(3.dp, Accent) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔍", fontSize = 16.sp)
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.text.isEmpty()) Text(Strings.searchHint, color = TextSecondary, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    inner()
                }
            }
        }
    )
}

@Composable
private fun ChannelList(
    state: MainViewModel.UiState,
    viewModel: MainViewModel,
    listState: LazyListState,
    onFocus: (Channel) -> Unit,
    onChange: (Channel) -> Unit,
    onLongPress: (Channel) -> Unit,
    modifier: Modifier
) {
    LazyColumn(modifier = modifier, state = listState, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(state.visibleChannels, key = { it.id }) { channel ->
            val suggestion = state.suggestions[channel.id]
            ChannelRow(
                channel = channel,
                changed = channel.id in state.changed,
                suggestion = suggestion,
                poor = suggestion != null && suggestion.id in state.poorLogos,
                selected = channel.id in state.selected,
                viewModel = viewModel,
                onFocus = { onFocus(channel) },
                onToggle = { viewModel.toggle(channel.id) },
                onLongPress = { onLongPress(channel) },
                onChange = { onChange(channel) }
            )
        }
    }
}

@Composable
private fun ChannelRow(
    channel: Channel,
    changed: Boolean,
    suggestion: LogoCandidate?,
    poor: Boolean,
    selected: Boolean,
    viewModel: MainViewModel,
    onFocus: () -> Unit,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onChange: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FocusBox(onClick = onToggle, onLongClick = onLongPress, onFocused = onFocus, modifier = Modifier.weight(1f)) { focused ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionMark(selected = selected, available = suggestion != null)
                Spacer(Modifier.width(14.dp))
                Text(channel.number, color = TextSecondary, fontSize = 15.sp, modifier = Modifier.width(52.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // The whole width is the name's; long names wrap to a second line instead of being cut.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (channel.isRadio) Text("📻 ", fontSize = 14.sp)
                        Text(
                            channel.name, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (channel.isRadio) {
                            Text(Strings.radioMark, color = Accent, fontSize = 12.sp)
                            Text("  ·  ", color = TextSecondary, fontSize = 12.sp)
                        }
                        Text(
                            if (channel.hasLogo) Strings.hasLogo else Strings.noLogo,
                            color = if (channel.hasLogo) Green else TextSecondary, fontSize = 12.sp
                        )
                        if (changed) Text("  ·  ${Strings.changedMark}", color = Accent, fontSize = 12.sp)
                        if (channel.id in viewModel.state.value.settings.ignoredChannels) {
                            Text("  ·  ${Strings.ignoredMark}", color = Orange, fontSize = 12.sp)
                        }
                        if (suggestion != null) {
                            Text("  ·  ${sourceLabel(suggestion)}  ·  ", color = if (focused) TextPrimary else TextSecondary, fontSize = 12.sp)
                            // Safe matches in green; anything that deserves a look is marked "check" in orange.
                            val safe = LogoMatching.isSafe(suggestion) && !poor
                            Text(
                                when {
                                    poor -> Strings.lowQualityMark
                                    safe -> strengthLabel(suggestion.strength)
                                    else -> Strings.check
                                },
                                color = if (safe) Green else Orange,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                if (suggestion != null) {
                    LogoPreview(suggestion, viewModel)
                } else {
                    Text(Strings.noSuggestion, color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        TvButton(Strings.change, onClick = onChange)
    }
}

/**
 * The app's windows, in the AynaCast / Pitwall Updater style: an accent-bar title with a close (✕)
 * button, a body that scrolls when it does not fit, and the buttons at the bottom right. With
 * [fullScreen] the window covers the screen and ends with a Close button; otherwise it is a card
 * that keeps a margin to all four screen edges (questions and short menus).
 */
@Composable
private fun TvDialog(
    title: String,
    onDismiss: () -> Unit,
    maxWidth: androidx.compose.ui.unit.Dp = 700.dp,
    fullScreen: Boolean = false,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = null,
    scrollBody: Boolean = true,
    /**
     * For bodies of plain text (nothing to focus inside): the body itself takes focus and the
     * remote's up/down keys scroll it. Bodies with buttons scroll by following the focus instead.
     */
    textBody: Boolean = false,
    /** What the remote's Back key does, when it should not close the dialog (e.g. a folder up). */
    onBack: (() -> Unit)? = null,
    body: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onBack ?: onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val window = if (fullScreen) {
            Modifier.fillMaxSize().background(Background).padding(horizontal = 40.dp, vertical = 28.dp)
        } else {
            Modifier
        }
        Box(
            modifier = if (fullScreen) Modifier.fillMaxSize() else Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = if (fullScreen) window else Modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(12.dp))
                    .padding(start = 28.dp, end = 20.dp, top = 20.dp, bottom = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AccentBar(height = if (fullScreen) 30.dp else 24.dp)
                    Spacer(Modifier.width(14.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        header?.invoke()
                            ?: Text(
                                title, color = TextPrimary, fontSize = if (fullScreen) 28.sp else 22.sp,
                                fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                    }
                    Spacer(Modifier.width(12.dp))
                    CloseButton(onDismiss)
                }
                Spacer(Modifier.height(if (fullScreen) 20.dp else 14.dp))
                // Takes what is left of the screen height and scrolls inside it.
                val scroll = rememberScrollState()
                var bodyModifier = if (fullScreen) Modifier.weight(1f) else Modifier.weight(1f, fill = false)
                if (scrollBody) {
                    bodyModifier = bodyModifier.scrollbar(scroll).padding(end = 14.dp)
                    if (textBody) bodyModifier = bodyModifier.remoteScroll(scroll)
                    bodyModifier = bodyModifier.verticalScroll(scroll)
                }
                Column(
                    modifier = bodyModifier,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = body
                )
                if (footer != null || fullScreen) {
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                    ) {
                        footer?.invoke(this)
                        if (fullScreen) TvButton(Strings.cancel, primary = true, onClick = onDismiss)
                    }
                }
            }
        }
    }
}

/** Backups found on USB sticks and in the TV's storage, newest first, with their check results. */
@Composable
private fun BackupList(found: List<MainViewModel.FoundBackup>, onPick: (MainViewModel.FoundBackup) -> Unit, onDismiss: () -> Unit) {
    TvDialog(title = Strings.importTitle, onDismiss = onDismiss, fullScreen = true) {
        if (found.isEmpty()) {
            Text(Strings.noBackupsFound, color = TextSecondary, fontSize = 15.sp)
            return@TvDialog
        }
        val first = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
        found.forEachIndexed { index, backup ->
            val modifier = if (index == 0) Modifier.fillMaxWidth().focusRequester(first) else Modifier.fillMaxWidth()
            FocusBox(onClick = { if (backup.usable) onPick(backup) }, modifier = modifier) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val place = if (backup.place == MainViewModel.BackupPlace.USB) Strings.placeUsb else Strings.placeInternal
                    Text("$place  ·  ${Strings.dateTime(backup.created)}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        Strings.backupFoundLine(backup.logos, backup.matched) + if (backup.hasSettings) "  ·  ${Strings.settingsIncluded}" else "",
                        color = TextSecondary, fontSize = 14.sp
                    )
                    when {
                        !backup.usable -> Text(Strings.backupUnusable, color = Orange, fontSize = 14.sp)
                        backup.damaged > 0 -> Text(Strings.damagedSkipped(backup.damaged), color = Orange, fontSize = 14.sp)
                        backup.checked -> Text(Strings.verifiedOk, color = Green, fontSize = 14.sp)
                        else -> Text(Strings.notVerified, color = TextSecondary, fontSize = 14.sp)
                    }
                    if (backup.usable && backup.channelsChanged) Text(Strings.channelsChangedWarning, color = Orange, fontSize = 14.sp)
                }
            }
        }
    }
}

/** Quick actions for one channel, opened by holding OK on its row. */
@Composable
private fun QuickMenu(
    channel: Channel,
    hasSuggestion: Boolean,
    selected: Boolean,
    changed: Boolean,
    isIgnored: Boolean,
    onWrite: () -> Unit,
    onToggle: () -> Unit,
    onChange: () -> Unit,
    onInfo: () -> Unit,
    onToggleIgnore: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    TvDialog(title = channel.name, onDismiss = onDismiss, maxWidth = 520.dp) {
        val first = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
        val wide = Modifier.fillMaxWidth()
        if (hasSuggestion) {
            TvButton(Strings.writeNow, primary = true, onClick = onWrite, modifier = wide.focusRequester(first))
            TvButton(if (selected) Strings.unselect else Strings.select, onClick = onToggle, modifier = wide)
            TvButton(Strings.change, onClick = onChange, modifier = wide)
        } else {
            TvButton(Strings.change, onClick = onChange, modifier = wide.focusRequester(first))
        }
        TvButton(if (isIgnored) Strings.unignoreChannel else Strings.ignoreChannel, onClick = onToggleIgnore, modifier = wide)
        TvButton(Strings.logoInfoTitle, icon = "ℹ️", onClick = onInfo, modifier = wide)
        if (changed) TvButton(Strings.resetOne, onClick = onReset, modifier = wide)
    }
}

/** Full-screen check before writing: how many logos, which replace existing ones, which look poor. */
@Composable
private fun WriteConfirmScreen(
    pending: MainViewModel.PendingWrite,
    viewModel: MainViewModel,
    onWrite: (skipLowQuality: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val writeFocus = remember { FocusRequester() }
        LaunchedEffect(pending) { runCatching { writeFocus.requestFocus() } }
        Column(
            modifier = Modifier.fillMaxSize().background(Background).padding(horizontal = 120.dp, vertical = 48.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(Strings.confirmWriteTitle, color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            Column(modifier = Modifier.widthIn(max = 760.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(Strings.confirmWriteSummary(pending.total), color = TextPrimary, fontSize = 20.sp)
                if (pending.replacing > 0) Text(Strings.replacingWarning(pending.replacing), color = Orange, fontSize = 17.sp)
                if (pending.lowQuality > 0) Text(Strings.lowQualityWarning(pending.lowQuality), color = Orange, fontSize = 17.sp)
                if (pending.failed > 0) Text(Strings.prepareFailed(pending.failed), color = Orange, fontSize = 17.sp)
                if (pending.autoBackup) Text(Strings.autoBackupNote, color = TextSecondary, fontSize = 16.sp)
            }
            if (pending.previewItems.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Column(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxWidth()
                        .background(Surface, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Caption(Strings.previewBeforeWrite.uppercase(Strings.locale))
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (item in pending.previewItems) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Background, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "${item.channelNumber} ${item.channelName}",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(6.dp))
                                LogoPreview(
                                    candidate = item.candidate,
                                    viewModel = viewModel,
                                    modifier = Modifier.size(width = 54.dp, height = 40.dp)
                                )
                            }
                        }
                    }
                    if (pending.total > pending.previewItems.size) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            Strings.andMoreChannels(pending.total - pending.previewItems.size),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvButton(Strings.writeAll(pending.total), primary = true, onClick = { onWrite(false) }, modifier = Modifier.focusRequester(writeFocus))
                if (pending.lowQuality in 1 until pending.total) {
                    TvButton(Strings.skipLowQuality(pending.total - pending.lowQuality), onClick = { onWrite(true) })
                }
                TvButton(Strings.no, onClick = onCancel)
            }
        }
    }
}

@Composable
private fun RestoreConfirmScreen(
    pending: MainViewModel.PendingRestore,
    viewModel: MainViewModel,
    onRestore: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val restoreFocus = remember { FocusRequester() }
        LaunchedEffect(pending) { runCatching { restoreFocus.requestFocus() } }
        Column(
            modifier = Modifier.fillMaxSize().background(Background).padding(horizontal = 120.dp, vertical = 48.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(Strings.restoreBackup, color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            Column(modifier = Modifier.widthIn(max = 760.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(Strings.restorePreviewDesc(pending.matched, pending.total), color = TextPrimary, fontSize = 20.sp)
                if (pending.channelsChanged) {
                    Text(Strings.channelsChangedWarning, color = Orange, fontSize = 17.sp)
                }
            }
            if (pending.previewItems.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Column(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxWidth()
                        .background(Surface, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Caption(Strings.previewBeforeRestore.uppercase(Strings.locale))
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (item in pending.previewItems) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Background, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "${item.channelNumber} ${item.channelName}",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(6.dp))
                                LogoPreview(
                                    candidate = item.candidate,
                                    viewModel = viewModel,
                                    modifier = Modifier.size(width = 54.dp, height = 40.dp)
                                )
                            }
                        }
                    }
                    if (pending.matched > pending.previewItems.size) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            Strings.andMoreChannels(pending.matched - pending.previewItems.size),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TvButton(Strings.restoreBackup, primary = true, onClick = onRestore, modifier = Modifier.focusRequester(restoreFocus))
                TvButton(Strings.cancel, onClick = onCancel)
            }
        }
    }
}

/** Full-screen progress ring while a task runs; it cannot be dismissed (the task goes on regardless). */
@Composable
private fun BusyScreen(status: String?) {
    Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false
        )
    ) {
        // Takes focus so remote keys do not reach the screen behind.
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        Column(
            modifier = Modifier.fillMaxSize().background(Background.copy(alpha = 0.94f)).focusRequester(focus).focusable(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = Accent, strokeWidth = 6.dp, modifier = Modifier.size(84.dp))
            Spacer(Modifier.height(32.dp))
            Text(status ?: Strings.loadingChannels, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text(Strings.pleaseWait, color = TextSecondary, fontSize = 16.sp)
        }
    }
}

/** Full-screen result of a task: a green tick or an orange warning, the message and an OK button. */
@Composable
private fun OutcomeScreen(outcome: MainViewModel.Outcome, onDismiss: () -> Unit, onAction: (MainViewModel.OutcomeAction) -> Unit = {}) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val okFocus = remember { FocusRequester() }
        LaunchedEffect(outcome) { runCatching { okFocus.requestFocus() } }
        val color = if (outcome.success) Green else Orange
        Column(
            modifier = Modifier.fillMaxSize().background(Background).padding(horizontal = 120.dp, vertical = 48.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.size(96.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                // Drawn rather than typed: older Android fonts lack the glyphs.
                androidx.compose.foundation.Canvas(modifier = Modifier.size(44.dp)) {
                    val stroke = size.width * 0.14f
                    val cap = androidx.compose.ui.graphics.StrokeCap.Round
                    fun at(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(size.width * x, size.height * y)
                    if (outcome.success) {
                        drawLine(Color.White, at(0.08f, 0.55f), at(0.38f, 0.85f), stroke, cap)
                        drawLine(Color.White, at(0.38f, 0.85f), at(0.94f, 0.18f), stroke, cap)
                    } else {
                        drawLine(Color.White, at(0.5f, 0.06f), at(0.5f, 0.62f), stroke, cap)
                        drawCircle(Color.White, stroke * 0.65f, at(0.5f, 0.92f))
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(outcome.title, color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text(
                outcome.message, color = TextSecondary, fontSize = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.widthIn(max = 760.dp)
            )
            Spacer(Modifier.height(32.dp))
            val action = outcome.action
            if (action == null) {
                TvButton(Strings.ok, primary = true, onClick = onDismiss, modifier = Modifier.focusRequester(okFocus).widthIn(min = 160.dp))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TvButton(
                        Strings.updateNow, primary = true, onClick = { onAction(action) },
                        modifier = Modifier.focusRequester(okFocus).widthIn(min = 160.dp)
                    )
                    TvButton(Strings.later, onClick = onDismiss, modifier = Modifier.widthIn(min = 160.dp))
                }
            }
        }
    }
}

/**
 * Full-screen lock screen displayed when the TV does not support channel logos or restricts access.
 * Locks the app interface completely; the only remote interaction allowed is exiting the app.
 */
@Composable
private fun UnsupportedTvScreen(reason: String) {
    val context = LocalContext.current
    val exitFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { exitFocus.requestFocus() } }

    BackHandler {
        (context as? android.app.Activity)?.finishAffinity()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 48.dp, vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .background(Surface, RoundedCornerShape(16.dp))
                .border(BorderStroke(2.dp, Orange.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
                .padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(Modifier.size(80.dp).background(Orange, CircleShape), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(38.dp)) {
                    val stroke = size.width * 0.14f
                    val cap = androidx.compose.ui.graphics.StrokeCap.Round
                    fun at(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(size.width * x, size.height * y)
                    drawLine(Color.White, at(0.5f, 0.06f), at(0.5f, 0.62f), stroke, cap)
                    drawCircle(Color.White, stroke * 0.65f, at(0.5f, 0.92f))
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AccentBar(height = 32.dp, color = Orange)
                Spacer(Modifier.width(14.dp))
                Text(
                    text = Strings.unsupportedTvTitle,
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = Strings.unsupportedTvMessage,
                color = TextSecondary,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )

            if (reason.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Background, RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = Strings.technicalDetails,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = reason,
                        color = Orange,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            TvButton(
                text = Strings.exitApp,
                primary = true,
                modifier = Modifier.focusRequester(exitFocus),
                onClick = {
                    (context as? android.app.Activity)?.finishAffinity()
                }
            )
        }
    }
}

/** A thin scroll indicator on the right edge, shown while the content is longer than the box. */
private fun Modifier.scrollbar(scroll: androidx.compose.foundation.ScrollState): Modifier =
    drawWithContent {
        drawContent()
        if (scroll.maxValue <= 0) return@drawWithContent
        val visible = size.height
        val total = visible + scroll.maxValue
        val thumb = (visible * visible / total).coerceAtLeast(24.dp.toPx())
        val top = (visible - thumb) * scroll.value / scroll.maxValue
        drawRoundRect(
            color = TextSecondary.copy(alpha = 0.6f),
            topLeft = androidx.compose.ui.geometry.Offset(size.width - 4.dp.toPx(), top),
            size = androidx.compose.ui.geometry.Size(4.dp.toPx(), thumb),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
        )
    }

/**
 * Makes a text-only scroll area usable with a remote: it takes focus (with a frame) and the up/down
 * keys scroll it by a few lines. At either end the key is left to the focus system, so focus can
 * move on to the buttons.
 */
@Composable
private fun Modifier.remoteScroll(scroll: androidx.compose.foundation.ScrollState): Modifier {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val step = with(androidx.compose.ui.platform.LocalDensity.current) { 140.dp.toPx() }
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    return this
        .border(if (focused) BorderStroke(2.dp, Accent.copy(alpha = 0.6f)) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(10.dp))
        .padding(8.dp)
        .focusRequester(focus)
        .onFocusChanged { focused = it.isFocused }
        .onKeyEvent { event ->
            if (event.type != androidx.compose.ui.input.key.KeyEventType.KeyDown) return@onKeyEvent false
            when (event.key) {
                androidx.compose.ui.input.key.Key.DirectionDown -> (scroll.value < scroll.maxValue).also { more ->
                    if (more) scope.launch { scroll.animateScrollBy(step) }
                }
                androidx.compose.ui.input.key.Key.DirectionUp -> (scroll.value > 0).also { more ->
                    if (more) scope.launch { scroll.animateScrollBy(-step) }
                }
                else -> false
            }
        }
        .focusable()
}

/** ✕ drawn rather than typed: older Android fonts lack the glyph. */
@Composable
private fun CloseButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(if (focused) FocusFill else SurfaceVariant, CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val color = if (focused) OnFocus else TextPrimary
        androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
            val stroke = size.width * 0.16f
            drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, size.height), stroke, androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(color, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(0f, size.height), stroke, androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

@Composable
private fun LogoPicker(channel: Channel, viewModel: MainViewModel, changed: Boolean, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var mode by remember { mutableStateOf(PickMode.SUGGESTIONS) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(mode) {
        if (mode == PickMode.SEARCH) {
            runCatching { searchFocus.requestFocus() }
        }
    }
    // The folder shown while browsing; null lists the USB sticks and the TV's storage.
    var folder by remember { mutableStateOf<File?>(null) }
    val places = remember(mode) { if (mode == PickMode.BROWSE) viewModel.filePlaces() else emptyList() }
    val candidates = remember(channel.id, mode, searchQuery.text) {
        when (mode) {
            PickMode.SUGGESTIONS -> viewModel.candidatesFor(channel)
            PickMode.SEARCH -> if (searchQuery.text.isNotBlank()) viewModel.searchCandidates(searchQuery.text) else emptyList()
            PickMode.FILES -> viewModel.fileCandidatesFor(channel)
            PickMode.BROWSE -> emptyList()
        }
    }
    val entries = remember(mode, folder) { folder?.let { viewModel.browse(it) }.orEmpty() }
    // Back goes up a folder, then leaves the browser; it closes the dialog only from the suggestions.
    val goBack: (() -> Unit)? = when {
        mode == PickMode.SEARCH -> ({ mode = PickMode.SUGGESTIONS })
        mode != PickMode.BROWSE -> null
        folder == null -> ({ mode = PickMode.SUGGESTIONS })
        else -> ({ folder = folder?.parentFile?.takeIf { parent -> places.none { it.dir == folder } } })
    }
    TvDialog(
        title = when (mode) {
            PickMode.SUGGESTIONS -> "${Strings.pickTitle}: ${channel.name}"
            PickMode.SEARCH -> "${Strings.searchCatalogs}: ${channel.name}"
            PickMode.FILES -> Strings.filesTitle(channel.name)
            PickMode.BROWSE -> Strings.browseTitle(channel.name)
        },
        onDismiss = onDismiss,
        onBack = goBack,
        fullScreen = true,
        scrollBody = false,
        footer = {
            if (mode != PickMode.SUGGESTIONS) TvButton(Strings.backToSuggestions, onClick = { mode = PickMode.SUGGESTIONS })
            if (mode != PickMode.SEARCH) TvButton(Strings.searchCatalogs, icon = "🔍", onClick = { mode = PickMode.SEARCH })
            if (mode != PickMode.FILES) TvButton(Strings.pickFromFiles, onClick = { mode = PickMode.FILES })
            if (mode != PickMode.BROWSE) TvButton(Strings.browseFolders, onClick = { folder = null; mode = PickMode.BROWSE })
            if (mode == PickMode.SUGGESTIONS) {
                TvButton(Strings.noLogoOption, onClick = { viewModel.choose(channel.id, null); onDismiss() })
                if (changed) TvButton(Strings.resetOne, onClick = { viewModel.resetToDefault(listOf(channel.id)); onDismiss() })
            }
        }
    ) {
        // The logo the channel has now, to compare the candidates with.
        if (channel.hasLogo) {
            val current by produceState<ImageBitmap?>(null, channel.id) { value = viewModel.currentLogo(channel) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(width = 68.dp, height = 52.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF2F2F7)),
                    contentAlignment = Alignment.Center
                ) {
                    CheckerboardPattern(modifier = Modifier.fillMaxSize())
                    current?.let { Image(it, contentDescription = Strings.currentLogo, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(2.dp)) }
                }
                Spacer(Modifier.width(16.dp))
                Text(Strings.currentLogo, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Text(Strings.currentLogoNone, color = TextSecondary, fontSize = 14.sp)
        }
        if (mode == PickMode.BROWSE) {
            FolderBrowser(
                places = places,
                folder = folder,
                entries = entries,
                viewModel = viewModel,
                onOpen = { folder = it },
                onUp = { goBack?.invoke() },
                onPick = { file -> viewModel.choose(channel.id, viewModel.fileCandidate(file)); onDismiss() }
            )
            return@TvDialog
        }
        if (mode == PickMode.SEARCH) {
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
                cursorBrush = SolidColor(Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocus)
                    .background(SurfaceVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔍", fontSize = 15.sp)
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f)) {
                            if (searchQuery.text.isEmpty()) {
                                Text(Strings.searchCatalogHint, color = TextSecondary, fontSize = 15.sp)
                            }
                            inner()
                        }
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
        }
        if (candidates.isEmpty()) Text(if (mode == PickMode.FILES) Strings.filesEmpty else Strings.noCandidates, color = TextSecondary, fontSize = 14.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(candidates, key = { it.id }) { candidate ->
                CandidateRow(candidate, viewModel, state.poorLogos[candidate.id]) { viewModel.choose(channel.id, candidate); onDismiss() }
            }
        }
    }
}

private enum class PickMode { SUGGESTIONS, SEARCH, FILES, BROWSE }

@Composable
private fun CandidateRow(candidate: LogoCandidate, viewModel: MainViewModel, issues: Set<com.berkbb.channellogos.logos.LogoQuality.Issue>?, onClick: () -> Unit) {
    FocusBox(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LogoPreview(candidate, viewModel)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(candidate.name.substringAfterLast('/'), color = TextPrimary, fontSize = 15.sp)
                Row {
                    Text(sourceLabel(candidate), color = TextSecondary, fontSize = 12.sp)
                    issues?.let { Text("  ·  ${qualityLabel(it)}", color = Orange, fontSize = 12.sp) }
                }
            }
        }
    }
}

/**
 * Folders on the USB sticks and in the TV's storage, for picking any image whatever its name:
 * the places first, then one folder at a time with its subfolders and images.
 */
@Composable
private fun FolderBrowser(
    places: List<FileLogoSource.Place>,
    folder: File?,
    entries: List<File>,
    viewModel: MainViewModel,
    onOpen: (File) -> Unit,
    onUp: () -> Unit,
    onPick: (File) -> Unit
) {
    val state by viewModel.state.collectAsState()
    // Each folder opens with the remote on its first row, not on the dialog's close button.
    val first = remember { FocusRequester() }
    LaunchedEffect(folder) { runCatching { first.requestFocus() } }
    if (folder == null) {
        if (places.isEmpty()) Text(Strings.noPlaces, color = TextSecondary, fontSize = 14.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(places, key = { it.dir.path }) { place ->
                val label = if (place.internal) Strings.placeInternal else "${Strings.placeUsb} (${place.dir.name})"
                val modifier = if (place == places.first()) Modifier.focusRequester(first) else Modifier
                FolderRow(if (place.internal) "📺" else "💾", label, modifier) { onOpen(place.dir) }
            }
        }
        return
    }
    Text(folder.path, color = TextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "up") { FolderRow("↑", Strings.upFolder, Modifier.focusRequester(first), onUp) }
        if (entries.isEmpty()) item(key = "empty") { Text(Strings.folderEmpty, color = TextSecondary, fontSize = 14.sp) }
        items(entries, key = { it.path }) { file ->
            if (file.isDirectory) {
                FolderRow("📁", file.name) { onOpen(file) }
            } else {
                val candidate = viewModel.fileCandidate(file)
                CandidateRow(candidate, viewModel, state.poorLogos[candidate.id]) { onPick(file) }
            }
        }
    }
}

@Composable
private fun FolderRow(icon: String, name: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FocusBox(onClick = onClick, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, color = TextPrimary, fontSize = 20.sp)
            Spacer(Modifier.width(16.dp))
            Text(name, color = TextPrimary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SettingsDialog(
    settings: Settings,
    sourcesInfo: String,
    cacheSize: Long,
    onClearCache: () -> Unit,
    onChange: (Settings) -> Unit,
    onDismiss: () -> Unit
) {
    TvDialog(
        title = Strings.settings,
        onDismiss = onDismiss,
        fullScreen = true
    ) {
        PanelSection(Strings.appearanceSection)
        SettingRow(Strings.language, Strings.languageDesc) {
            // No "System" choice: until one is picked the TV's language is used, and it shows as chosen.
            val shown = if (settings.language == Settings.LANGUAGE_SYSTEM) Strings.locale.language else settings.language
            for ((code, label) in listOf("tr" to "Türkçe", "en" to "English", "ro" to "Română")) {
                TvButton(label, selected = shown == code, small = true, onClick = { onChange(settings.copy(language = code)) })
            }
        }
        SettingRow(Strings.theme, Strings.themeDesc) {
            for ((code, label) in listOf(
                Settings.THEME_SYSTEM to Strings.languageSystem,
                Settings.THEME_DARK to Strings.themeDark,
                Settings.THEME_LIGHT to Strings.themeLight
            )) {
                TvButton(label, selected = settings.theme == code, small = true, onClick = { onChange(settings.copy(theme = code)) })
            }
        }

        PanelSection(Strings.suggestionsSection)
        SettingRow(Strings.preselection, Strings.preselectionDesc) {
            TvButton(Strings.preselectSafe, selected = settings.preselectSafe, small = true, onClick = { onChange(settings.copy(preselectSafe = true)) })
            TvButton(Strings.preselectNone, selected = !settings.preselectSafe, small = true, onClick = { onChange(settings.copy(preselectSafe = false)) })
        }
        SettingRow(Strings.textLogosSetting, Strings.textLogosDesc) {
            TvButton(Strings.on, selected = settings.textLogos, small = true, onClick = { onChange(settings.copy(textLogos = true)) })
            TvButton(Strings.off, selected = !settings.textLogos, small = true, onClick = { onChange(settings.copy(textLogos = false)) })
        }
        // One toggle per source: selected (framed, with a tick) is on.
        SettingRow(Strings.sources, Strings.sourcesDesc + "\n" + sourcesInfo) {
            for ((label, value, update) in listOf(
                Triple("GitHub", settings.gitHub) { on: Boolean -> settings.copy(gitHub = on) },
                Triple("picons", settings.picons) { on: Boolean -> settings.copy(picons = on) },
                Triple("iptv-org", settings.iptv) { on: Boolean -> settings.copy(iptv = on) },
                Triple(Strings.filesSource, settings.usb) { on: Boolean -> settings.copy(usb = on) }
            )) {
                TvButton(if (value) "✓ $label" else label, selected = value, small = true, onClick = { onChange(update(!value)) })
            }
        }
        SettingRow(Strings.piconsStyleSetting, Strings.piconsStyleDesc) {
            for ((code, label) in listOf(
                Settings.PICONS_STYLE_DEFAULT to Strings.piconsStyleDefault,
                Settings.PICONS_STYLE_LIGHT to Strings.piconsStyleLight,
                Settings.PICONS_STYLE_DARK to Strings.piconsStyleDark
            )) {
                TvButton(label, selected = settings.piconsStyle == code, small = true, onClick = { onChange(settings.copy(piconsStyle = code)) })
            }
        }
        if (settings.ignoredChannels.isNotEmpty()) {
            SettingRow(Strings.ignoredChannelsSetting, Strings.ignoredChannelsDesc(settings.ignoredChannels.size)) {
                TvButton(Strings.resetIgnored, small = true, onClick = { onChange(settings.copy(ignoredChannels = emptySet())) })
            }
        }

        PanelSection(Strings.storageSection)
        SettingRow(Strings.cacheSizeTitle, Strings.cacheSizeDesc(formatBytes(cacheSize))) {
            TvButton(Strings.cacheClear, small = true, enabled = cacheSize > 0, onClick = onClearCache)
        }
        SettingRow(Strings.restartTvSetting, Strings.restartTvDesc) {
            TvButton(Strings.on, selected = settings.restartLiveTv, small = true, onClick = { onChange(settings.copy(restartLiveTv = true)) })
            TvButton(Strings.off, selected = !settings.restartLiveTv, small = true, onClick = { onChange(settings.copy(restartLiveTv = false)) })
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) String.format(java.util.Locale.US, "%.1f MB", mb)
    else String.format(java.util.Locale.US, "%.0f KB", kb)
}

/** One setting: its name and explanation on the left, its choices on the right. */
@Composable
private fun SettingRow(title: String, description: String, options: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(12.dp)).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(description, color = TextSecondary, fontSize = 13.sp)
        }
        Spacer(Modifier.width(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = options)
    }
}

@Composable
private fun AboutDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
    var showLicense by remember { mutableStateOf(false) }
    val state by viewModel.state.collectAsState()
    TvDialog(
        title = Strings.about,
        onDismiss = onDismiss,
        fullScreen = true,
        textBody = true,
        footer = {
            val checking = state.updateStatus is MainViewModel.UpdateStatus.Checking
            TvButton(
                if (checking) Strings.checkingUpdates else Strings.checkUpdates,
                enabled = !checking,
                onClick = { viewModel.checkForUpdates() }
            )
            TvButton(Strings.viewLicense, onClick = { showLicense = true })
        }
    ) {
        // Hero: icon, name, version and what the app does, as the first thing seen from the sofa
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Accent.copy(alpha = 0.28f), Surface)),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 28.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(com.berkbb.channellogos.R.drawable.ic_launcher),
                contentDescription = null,
                modifier = Modifier.size(88.dp).background(Background, RoundedCornerShape(20.dp)).padding(6.dp)
            )
            Spacer(Modifier.width(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Strings.appTitle, color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(14.dp))
                    Badge("v$version", dot = Accent)
                }
                Text(Strings.aboutWhat, color = TextSecondary, fontSize = 15.sp, lineHeight = 21.sp)
            }
        }
        // Key facts as cards, as in Pitwall Updater's About window
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoCard(Strings.aboutDeveloperTitle, Strings.aboutDeveloper, Modifier.weight(1f))
            InfoCard(Strings.aboutWebsiteTitle, Strings.aboutWebsite, Modifier.weight(1.5f), valueColor = Color(0xFF64B5F6))
            InfoCard(Strings.aboutLicenseTitle, "GNU GPL-3.0", Modifier.weight(1f), valueColor = Green)
        }
        // Two columns of cards: the body is wide on a TV and long lines are hard to read from the sofa.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AboutSection(Strings.aboutHowTitle, Strings.aboutHow)
                AboutSection(Strings.aboutNoteTitle, Strings.aboutNote)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AboutSection(Strings.aboutSourcesTitle, Strings.aboutSources)
                AboutSection(Strings.aboutLicenseTitle, Strings.aboutLicense)
            }
        }
        AboutSection(Strings.aboutDisclaimerTitle, Strings.aboutDisclaimer)
    }

    if (showLicense) {
        LicenseDialog(onDismiss = { showLicense = false })
    }
}

@Composable
private fun HealthReportDialog(state: MainViewModel.UiState, onDismiss: () -> Unit) {
    val total = state.channels.size
    val withLogo = state.withLogo
    val coveragePct = if (total > 0) (withLogo * 100) / total else 0
    val tvChannels = state.channels.count { !it.isRadio }
    val radios = state.radios
    val safeMatches = state.suggestions.values.count { LogoMatching.isSafe(it) }

    TvDialog(
        title = Strings.healthReportTitle,
        onDismiss = onDismiss,
        maxWidth = 600.dp,
        textBody = true,
        footer = {
            TvButton(Strings.ok, primary = true, onClick = onDismiss)
        }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoCard(
                title = Strings.coverageRate,
                value = "$coveragePct%",
                modifier = Modifier.weight(1f),
                valueColor = if (coveragePct >= 80) Green else Orange,
                extra = "$withLogo / $total"
            )
            InfoCard(
                title = Strings.safeMatchesCount,
                value = safeMatches.toString(),
                modifier = Modifier.weight(1f),
                valueColor = Accent
            )
        }
        Spacer(Modifier.height(8.dp))
        InfoRow(Strings.channelCount(total), "$total (${tvChannels} TV · ${Strings.radioCount(radios)})")
        InfoRow(Strings.withLogoCount(withLogo), "$withLogo (${coveragePct}%)")
        InfoRow(Strings.withoutLogoCount(total - withLogo), "${total - withLogo}")
        if (state.settings.ignoredChannels.isNotEmpty()) {
            InfoRow(Strings.ignoredChannelsSetting, "${state.settings.ignoredChannels.size}")
        }
        Spacer(Modifier.height(8.dp))
        PanelSection(Strings.catalogsOverview)
        InfoRow("TV", "${state.tvLogos}")
        InfoRow("GitHub", "${state.gitHubLogos}")
        InfoRow("picons", "${state.piconsLogos}")
        InfoRow("iptv-org", "${state.iptvLogos}")
        if (state.fileLogos > 0) {
            InfoRow(Strings.filesSource, "${state.fileLogos}")
        }
        if (state.sourcesUpdatedAt > 0L) {
            InfoRow(Strings.updatedTitle, Strings.dateTime(state.sourcesUpdatedAt))
        }
    }
}

@Composable
private fun LogoInfoDialog(
    channel: Channel,
    suggestion: LogoCandidate?,
    poor: Boolean,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var showZoom by remember { mutableStateOf(false) }
    val previewImage by produceState<ImageBitmap?>(null, suggestion?.id) {
        value = suggestion?.let { viewModel.preview(it) }
    }

    TvDialog(
        title = "${channel.number} ${channel.name}",
        onDismiss = onDismiss,
        maxWidth = 580.dp,
        footer = {
            if (suggestion != null) {
                TvButton(Strings.zoomPreview, icon = "🔍", onClick = { showZoom = true })
            }
            TvButton(Strings.ok, primary = true, onClick = onDismiss)
        }
    ) {
        if (suggestion != null) {
            FocusBox(onClick = { showZoom = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(SurfaceVariant, RoundedCornerShape(12.dp)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LogoPreview(suggestion, viewModel, modifier = Modifier.size(width = 80.dp, height = 58.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(sourceLabel(suggestion), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${Strings.logoStrength}: ${strengthLabel(suggestion.strength)}",
                            color = if (LogoMatching.isSafe(suggestion) && !poor) Green else Orange,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            InfoRow(Strings.logoSource, sourceLabel(suggestion))
            InfoRow(Strings.logoReference, suggestion.id)
            previewImage?.let { img ->
                InfoRow(Strings.resolution, "${img.width} × ${img.height} px")
            }
            val tierName = when (suggestion.tier) {
                com.berkbb.channellogos.logos.LogoEntry.COUNTRY -> Strings.tierCountry
                com.berkbb.channellogos.logos.LogoEntry.INTERNATIONAL -> Strings.tierInternational
                else -> Strings.tierOther
            }
            InfoRow(Strings.logoTier, tierName)
            val issues = viewModel.state.value.poorLogos[suggestion.id]
            val qualityText = if (!issues.isNullOrEmpty()) qualityLabel(issues) else Strings.logoQualityOk
            InfoRow(Strings.logoQualityStatus, qualityText)
            channel.serviceKey?.let { dvbTripletText(it) }?.let { dvb ->
                InfoRow(Strings.dvbParams, dvb)
            }
        } else {
            channel.serviceKey?.let { dvbTripletText(it) }?.let { dvb ->
                InfoRow(Strings.dvbParams, dvb)
                Spacer(Modifier.height(8.dp))
            }
            Text(Strings.noSuggestion, color = TextSecondary, fontSize = 15.sp)
        }
    }

    if (showZoom && suggestion != null) {
        TvDialog(
            title = "${channel.number} ${channel.name}",
            onDismiss = { showZoom = false },
            maxWidth = 460.dp,
            footer = {
                TvButton(Strings.cancel, primary = true, onClick = { showZoom = false })
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LogoPreview(
                    candidate = suggestion,
                    viewModel = viewModel,
                    modifier = Modifier.size(width = 280.dp, height = 210.dp)
                )
                previewImage?.let { img ->
                    Text(
                        text = "${img.width} × ${img.height} px",
                        color = Accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "${sourceLabel(suggestion)} · ${suggestion.name.substringAfterLast('/')}",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun dvbTripletText(serviceKey: String): String? {
    val parts = serviceKey.split('_')
    if (parts.size != 3) return null
    val sid = parts[0].toIntOrNull(16) ?: return null
    val tsid = parts[1].toIntOrNull(16) ?: return null
    val onid = parts[2].toIntOrNull(16) ?: return null
    return "SID: $sid (0x${parts[0]}) · TSID: $tsid (0x${parts[1]}) · ONID: $onid (0x${parts[2]})"
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Full-screen read-only License window displaying the GNU General Public License v3.0, as in Pitwall Updater. */
@Composable
private fun LicenseDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val licenseText = remember(context) {
        runCatching {
            context.assets.open("LICENSE").bufferedReader().use { it.readText() }
        }.getOrDefault("GNU General Public License v3.0\n\nhttps://www.gnu.org/licenses/gpl-3.0.txt")
    }
    TvDialog(
        title = Strings.dialogLicenseTitle,
        onDismiss = onDismiss,
        fullScreen = true,
        textBody = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface, RoundedCornerShape(12.dp))
                .padding(20.dp)
        ) {
            Text(
                text = licenseText,
                color = TextPrimary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun AboutSection(title: String, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(12.dp)).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Caption(title)
        Text(text, color = TextPrimary, fontSize = 14.sp)
    }
}

@Composable
private fun CheckerboardPattern(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val checkSize = 6.dp.toPx()
        val cols = (size.width / checkSize).toInt() + 1
        val rows = (size.height / checkSize).toInt() + 1
        val c2 = Color(0xFFDCDCE0)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if ((r + c) % 2 == 1) {
                    drawRect(
                        color = c2,
                        topLeft = androidx.compose.ui.geometry.Offset(c * checkSize, r * checkSize),
                        size = androidx.compose.ui.geometry.Size(checkSize, checkSize)
                    )
                }
            }
        }
    }
}

@Composable
private fun LogoPreview(
    candidate: LogoCandidate,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier.size(width = 60.dp, height = 44.dp)
) {
    val image by produceState<ImageBitmap?>(null, candidate.id) { value = viewModel.preview(candidate) }
    // Logos are drawn on a light tile with a subtle checkerboard pattern for transparent PNG visibility.
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF2F2F7)),
        contentAlignment = Alignment.Center
    ) {
        CheckerboardPattern(modifier = Modifier.fillMaxSize())
        image?.let { Image(it, contentDescription = candidate.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(2.dp)) }
    }
}

@Composable
private fun SelectionMark(selected: Boolean, available: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(if (selected) Accent else Color.Transparent, CircleShape)
            .border(2.dp, if (selected) Accent else if (available) TextSecondary else SurfaceVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * A focusable, clickable surface that shows a clear focus frame for the remote control. With
 * [onLongClick], holding OK (or a long touch) calls it instead of [onClick].
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FocusBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onFocused: (() -> Unit)? = null,
    content: @Composable (focused: Boolean) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    if (onFocused != null) {
        LaunchedEffect(focused) {
            if (focused) onFocused()
        }
    }
    // A held OK key repeats KeyDown; the first repeat opens the long-press action and the key's
    // release is swallowed so the plain click does not follow.
    var longPressed by remember { mutableStateOf(false) }
    val longPressKeys = if (onLongClick == null) Modifier else Modifier.onPreviewKeyEvent { event ->
        val okKey = event.key == androidx.compose.ui.input.key.Key.DirectionCenter ||
            event.key == androidx.compose.ui.input.key.Key.Enter ||
            event.key == androidx.compose.ui.input.key.Key.NumPadEnter
        if (!okKey) return@onPreviewKeyEvent false
        when (event.type) {
            androidx.compose.ui.input.key.KeyEventType.KeyDown -> {
                if (event.nativeKeyEvent.repeatCount > 0) {
                    if (!longPressed) { longPressed = true; onLongClick() }
                    true
                } else false
            }
            androidx.compose.ui.input.key.KeyEventType.KeyUp -> longPressed.also { longPressed = false }
            else -> false
        }
    }
    Box(
        modifier = modifier
            .background(if (focused) SurfaceVariant else Surface, RoundedCornerShape(12.dp))
            .border(if (focused) BorderStroke(3.dp, Accent) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(12.dp))
            .then(longPressKeys)
            .combinedClickable(interactionSource = interaction, indication = null, onLongClick = onLongClick, onClick = onClick)
    ) {
        content(focused)
    }
}

@Composable
private fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    /** Tighter, for buttons sharing a row; long labels wrap to two centred lines. */
    small: Boolean = false,
    /** A symbol before the label (⚙️, 💾, ✓ …). */
    icon: String? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    // Focus fills the button light with dark text (Pitwall Updater); the chosen option is accent-filled.
    val background = when {
        !enabled -> SurfaceVariant.copy(alpha = 0.5f)
        focused -> FocusFill
        primary || selected -> Accent
        else -> SurfaceVariant
    }
    val content = when {
        !enabled -> TextSecondary
        focused -> OnFocus
        primary || selected -> Color.White
        else -> TextPrimary
    }
    Box(
        modifier = modifier
            .heightIn(min = if (small) 44.dp else 52.dp)
            .background(background, RoundedCornerShape(8.dp))
            .border(if (focused) BorderStroke(3.dp, FocusFill) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(8.dp))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = if (small) 10.dp else 18.dp, vertical = if (small) 8.dp else 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Text(icon, color = content, fontSize = if (small) 15.sp else 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                color = content,
                fontSize = if (small) 14.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun sourceLabel(candidate: LogoCandidate) = when (candidate.source) {
    LogoSource.TV -> Strings.sourceTv
    LogoSource.GITHUB -> Strings.sourceGitHub
    LogoSource.PICONS -> Strings.sourcePicons
    LogoSource.IPTV -> Strings.sourceIptv
    LogoSource.TEXT -> Strings.sourceText
    LogoSource.FILE -> if (FileLogoSource.isInternal(candidate.name)) Strings.sourceInternal else Strings.sourceFile
}

private fun qualityLabel(issues: Set<com.berkbb.channellogos.logos.LogoQuality.Issue>) =
    if (com.berkbb.channellogos.logos.LogoQuality.Issue.SMALL in issues) Strings.lowQualitySmall else Strings.lowQualityDark

private fun strengthLabel(strength: NameMatcher.Strength) = when (strength) {
    NameMatcher.Strength.EXACT -> Strings.exact
    NameMatcher.Strength.WITHOUT_QUALITY -> Strings.close
    NameMatcher.Strength.LOOSE -> Strings.loose
}

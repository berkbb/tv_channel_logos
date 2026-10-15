package com.babadogan.channellogos.ui

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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.babadogan.channellogos.logos.FileLogoSource
import com.babadogan.channellogos.logos.LogoCandidate
import com.babadogan.channellogos.logos.LogoMatching
import com.babadogan.channellogos.logos.LogoSource
import com.babadogan.channellogos.logos.NameMatcher
import com.babadogan.channellogos.settings.Settings
import com.babadogan.channellogos.tv.Channel
import java.io.File

/** Light or dark palette (AynaCast's); Compose state, so switching redraws everything. */
private object Palette {
    var dark by mutableStateOf(true)
}

private val Background get() = if (Palette.dark) Color(0xFF0F1012) else Color(0xFFF2F2F7)
private val Surface get() = if (Palette.dark) Color(0xFF1E2024) else Color(0xFFFFFFFF)
private val SurfaceVariant get() = if (Palette.dark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
private val Accent = Color(0xFF0A84FF)
private val Green get() = if (Palette.dark) Color(0xFF30D158) else Color(0xFF248A3D)
private val Orange get() = if (Palette.dark) Color(0xFFFF9F0A) else Color(0xFFC93400)
private val TextPrimary get() = if (Palette.dark) Color(0xFFF5F5F7) else Color(0xFF1C1C1E)
private val TextSecondary get() = if (Palette.dark) Color(0xFF86868B) else Color(0xFF6C6C70)
/** Focus frame: strong contrast with the background. */
private val FocusFrame get() = if (Palette.dark) Color.White else Color(0xFF1C1C1E)

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

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .onPreviewKeyEvent { event ->
                if (searchFocused || event.type != androidx.compose.ui.input.key.KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return@onPreviewKeyEvent false
                val code = event.utf16CodePoint
                if (code == 0 || !Character.isLetterOrDigit(code)) return@onPreviewKeyEvent false
                val text = search.text + String(Character.toChars(code))
                onSearch(TextFieldValue(text, TextRange(text.length)))
                runCatching { searchFocus.requestFocus() }
                true
            }
            .padding(horizontal = 40.dp, vertical = 28.dp)
    ) {
        SidePanel(
            state, viewModel,
            onSettings = { showSettings = true },
            onAbout = { showAbout = true },
            onConfirm = { message, action -> confirming = message to action },
            modifier = Modifier.width(360.dp).fillMaxHeight()
        )
        Spacer(Modifier.width(28.dp))
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (state.connection == MainViewModel.Connection.Connected) {
                SearchField(
                    value = search,
                    onValueChange = onSearch,
                    focusRequester = searchFocus,
                    onFocusChange = { searchFocused = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                if (state.query.isNotBlank() && state.visibleChannels.isEmpty() && state.hiddenMatches == 0) {
                    Text(Strings.noMatches, color = TextSecondary, fontSize = 15.sp, modifier = Modifier.padding(8.dp))
                }
                // Searching for a channel that already has a logo should not end in "nothing found".
                val hidden = state.hiddenMatches
                if (hidden > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                        Text(Strings.hiddenMatches(hidden), color = TextSecondary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(12.dp))
                        TvButton(Strings.searchAll, small = true, onClick = { viewModel.setShowAll(true) })
                    }
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
            }
            ChannelList(
                state, viewModel,
                onChange = { picking = it },
                onLongPress = { quickMenu = it },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
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
            onWrite = { quickMenu = null; viewModel.applyOne(channel.id) },
            onToggle = { quickMenu = null; viewModel.toggle(channel.id) },
            onChange = { quickMenu = null; picking = channel },
            onReset = {
                quickMenu = null
                confirming = Strings.confirmReset(1) to { viewModel.resetToDefault(listOf(channel.id)) }
            },
            onDismiss = { quickMenu = null }
        )
    }
    picking?.let { channel ->
        LogoPicker(channel, viewModel, changed = channel.id in state.changed, onDismiss = { picking = null })
    }
    if (showSettings) SettingsDialog(state.settings, onChange = viewModel::updateSettings, onDismiss = { showSettings = false })
    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
    // Progress and results cover the whole screen: a line at the bottom of the panel went unseen.
    if (state.busy && state.connection == MainViewModel.Connection.Connected) BusyScreen(state.status)
    else if (state.outcome != null) OutcomeScreen(state.outcome!!, onDismiss = viewModel::dismissOutcome)
    else state.pendingWrite?.let { WriteConfirmScreen(it, onWrite = viewModel::confirmWrite, onCancel = viewModel::cancelWrite) }
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

@Composable
private fun SidePanel(
    state: MainViewModel.UiState,
    viewModel: MainViewModel,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onConfirm: (message: String, action: () -> Unit) -> Unit,
    modifier: Modifier
) {
    // Scrolls: on a 1080p TV the buttons do not all fit; D-pad focus scrolls them into view.
    val connected = state.connection == MainViewModel.Connection.Connected
    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(Strings.appTitle, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        // Once connected the subtitle only takes room; the About dialog says the same.
        if (!connected) Text(Strings.appSubtitle, color = TextSecondary, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvButton(Strings.settings, onClick = onSettings, small = true, modifier = Modifier.weight(1f))
            TvButton(Strings.about, onClick = onAbout, small = true, modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (val connection = state.connection) {
                MainViewModel.Connection.Connecting -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Accent, strokeWidth = 3.dp, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(Strings.connecting, color = TextPrimary, fontSize = 16.sp)
                    }
                    Text(Strings.approveHint, color = TextSecondary, fontSize = 13.sp)
                }
                MainViewModel.Connection.Connected -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(Green, CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Text(Strings.connected, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (state.channels.isNotEmpty()) {
                        Text(Strings.channelStats(state.channels.size, state.withLogo, state.radios), color = TextPrimary, fontSize = 14.sp)
                    }
                    Text(Strings.sourceStats(state.tvLogos, state.gitHubLogos, state.piconsLogos, state.iptvLogos), color = TextSecondary, fontSize = 13.sp)
                    if (state.fileLogos > 0) Text(Strings.fileStats(state.fileLogos), color = TextSecondary, fontSize = 13.sp)
                    if (state.sourcesUpdatedAt > 0L) {
                        val date = Strings.dateTime(state.sourcesUpdatedAt)
                        Text(Strings.sourcesUpdatedAt(date), color = TextSecondary, fontSize = 13.sp)
                    }
                    if (state.extraCountries.isNotEmpty()) {
                        val names = state.extraCountries.joinToString { java.util.Locale("", it).getDisplayCountry(Strings.locale) }
                        Text(Strings.extraCountries(names), color = Accent, fontSize = 13.sp)
                    }
                    if (state.tvTableMissing) Text(Strings.tvTableMissing, color = Orange, fontSize = 13.sp)
                    if (state.gitHubFailed) Text(Strings.gitHubUnavailable, color = Orange, fontSize = 13.sp)
                }
                is MainViewModel.Connection.Failed -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(Orange, CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Text(Strings.connectFailed, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(connection.message, color = TextSecondary, fontSize = 12.sp, maxLines = 3)
                    Text(Strings.networkDebuggingHint, color = TextSecondary, fontSize = 13.sp)
                    TvButton(Strings.retry, primary = true, onClick = viewModel::start)
                }
            }
        }

        if (connected) {
            val idle = !state.busy
            // Main actions: write the selection, or undo everything the app wrote.
            val applyFocus = remember { FocusRequester() }
            TvButton(
                Strings.apply(state.selected.size),
                primary = true,
                enabled = state.selected.isNotEmpty() && idle,
                onClick = viewModel::applySelected,
                modifier = Modifier.fillMaxWidth().focusRequester(applyFocus)
            )
            LaunchedEffect(Unit) { runCatching { applyFocus.requestFocus() } }
            TvButton(
                Strings.resetToDefault(state.changed.size),
                enabled = idle && state.changed.isNotEmpty(),
                onClick = { onConfirm(Strings.confirmReset(state.changed.size)) { viewModel.resetToDefault() } },
                modifier = Modifier.fillMaxWidth()
            )

            PanelSection(Strings.listSection)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvButton(Strings.showMissing, selected = !state.showAll, small = true, onClick = { viewModel.setShowAll(false) }, modifier = Modifier.weight(1f))
                TvButton(Strings.showAll, selected = state.showAll, small = true, onClick = { viewModel.setShowAll(true) }, modifier = Modifier.weight(1f))
            }

            PanelSection(Strings.selectionSection)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvButton(Strings.selectSafe, onClick = viewModel::selectSafe, enabled = idle, small = true, modifier = Modifier.weight(1f))
                TvButton(Strings.selectAll, onClick = viewModel::selectAll, enabled = idle, small = true, modifier = Modifier.weight(1f))
                TvButton(Strings.selectNone, onClick = viewModel::selectNone, enabled = idle && state.selected.isNotEmpty(), small = true, modifier = Modifier.weight(1f))
            }

            PanelSection(Strings.updateSection)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvButton(Strings.rescan, onClick = viewModel::scan, enabled = idle, small = true, modifier = Modifier.weight(1f))
                TvButton(Strings.refreshSources, onClick = viewModel::refreshSources, enabled = idle, small = true, modifier = Modifier.weight(1f))
            }

            PanelSection(Strings.backupSection)
            val backupDate = if (state.backupTime == 0L) null else Strings.dateTime(state.backupTime)
            Text(
                backupDate?.let { Strings.backupInfo(state.backupCount, it) } ?: Strings.noBackup,
                color = TextSecondary, fontSize = 13.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvButton(Strings.backupLogos, enabled = idle && state.withLogo > 0, small = true,
                    onClick = { onConfirm(Strings.confirmBackup) { viewModel.backupLogos() } }, modifier = Modifier.weight(1f))
                TvButton(Strings.restoreBackup, enabled = idle && state.backupCount > 0, small = true,
                    onClick = {
                        val (matched, total) = viewModel.backupMatch()
                        onConfirm(Strings.confirmRestoreMatched(matched, total, matched < total * MainViewModel.MATCH_SHARE)) { viewModel.restoreBackup() }
                    }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvButton(Strings.exportBackup, enabled = idle && state.backupCount > 0, small = true,
                    onClick = { viewModel.chooseExport(true) }, modifier = Modifier.weight(1f))
                TvButton(Strings.importBackup, enabled = idle, small = true,
                    onClick = viewModel::findBackups, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Small heading over a group of side-panel buttons. */
@Composable
private fun PanelSection(title: String) {
    Text(
        title.uppercase(Strings.locale), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp, modifier = Modifier.padding(top = 8.dp)
    )
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
                    .background(if (focused) SurfaceVariant else Surface, RoundedCornerShape(14.dp))
                    .border(if (focused) BorderStroke(3.dp, FocusFrame) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(14.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
    onChange: (Channel) -> Unit,
    onLongPress: (Channel) -> Unit,
    modifier: Modifier
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.visibleChannels, key = { it.id }) { channel ->
            val suggestion = state.suggestions[channel.id]
            ChannelRow(
                channel = channel,
                changed = channel.id in state.changed,
                suggestion = suggestion,
                poor = suggestion != null && suggestion.id in state.poorLogos,
                selected = channel.id in state.selected,
                viewModel = viewModel,
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
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onChange: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FocusBox(onClick = onToggle, onLongClick = onLongPress, modifier = Modifier.weight(1f)) { focused ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionMark(selected = selected, available = suggestion != null)
                Spacer(Modifier.width(14.dp))
                Text(channel.number, color = TextSecondary, fontSize = 15.sp, modifier = Modifier.width(52.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // The whole width is the name's; long names wrap to a second line instead of being cut.
                    Text(
                        channel.name, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (channel.hasLogo) Strings.hasLogo else Strings.noLogo,
                            color = if (channel.hasLogo) Green else TextSecondary, fontSize = 12.sp
                        )
                        if (changed) Text("  ·  ${Strings.changedMark}", color = Accent, fontSize = 12.sp)
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
 * The app's pop-up window: a title with a close (✕) button, a body that scrolls when it does not
 * fit, and fixed buttons at the bottom. It keeps a margin to all four screen edges.
 */
@Composable
private fun TvDialog(
    title: String,
    onDismiss: () -> Unit,
    maxWidth: androidx.compose.ui.unit.Dp = 700.dp,
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
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(20.dp))
                    .padding(start = 28.dp, end = 20.dp, top = 20.dp, bottom = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        header?.invoke()
                            ?: Text(title, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(12.dp))
                    CloseButton(onDismiss)
                }
                Spacer(Modifier.height(14.dp))
                // Takes what is left of the screen height and scrolls inside it.
                val scroll = rememberScrollState()
                var bodyModifier = Modifier.weight(1f, fill = false)
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
                if (footer != null) {
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), content = footer)
                }
            }
        }
    }
}

/** Backups found on USB sticks and in the TV's storage, newest first, with their check results. */
@Composable
private fun BackupList(found: List<MainViewModel.FoundBackup>, onPick: (MainViewModel.FoundBackup) -> Unit, onDismiss: () -> Unit) {
    TvDialog(title = Strings.importTitle, onDismiss = onDismiss, maxWidth = 760.dp) {
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
    onWrite: () -> Unit,
    onToggle: () -> Unit,
    onChange: () -> Unit,
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
        if (changed) TvButton(Strings.resetOne, onClick = onReset, modifier = wide)
    }
}

/** Full-screen check before writing: how many logos, which replace existing ones, which look poor. */
@Composable
private fun WriteConfirmScreen(pending: MainViewModel.PendingWrite, onWrite: (skipLowQuality: Boolean) -> Unit, onCancel: () -> Unit) {
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
private fun OutcomeScreen(outcome: MainViewModel.Outcome, onDismiss: () -> Unit) {
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
            TvButton(Strings.ok, primary = true, onClick = onDismiss, modifier = Modifier.focusRequester(okFocus).widthIn(min = 160.dp))
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
            .background(if (focused) Accent else SurfaceVariant, CircleShape)
            .border(if (focused) BorderStroke(3.dp, FocusFrame) else BorderStroke(0.dp, Color.Transparent), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val color = if (focused) Color.White else TextPrimary
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
    // The folder shown while browsing; null lists the USB sticks and the TV's storage.
    var folder by remember { mutableStateOf<File?>(null) }
    val places = remember(mode) { if (mode == PickMode.BROWSE) viewModel.filePlaces() else emptyList() }
    val candidates = remember(channel.id, mode) {
        when (mode) {
            PickMode.SUGGESTIONS -> viewModel.candidatesFor(channel)
            PickMode.FILES -> viewModel.fileCandidatesFor(channel)
            PickMode.BROWSE -> emptyList()
        }
    }
    val entries = remember(mode, folder) { folder?.let { viewModel.browse(it) }.orEmpty() }
    // Back goes up a folder, then leaves the browser; it closes the dialog only from the suggestions.
    val goBack: (() -> Unit)? = when {
        mode != PickMode.BROWSE -> null
        folder == null -> ({ mode = PickMode.SUGGESTIONS })
        else -> ({ folder = folder?.parentFile?.takeIf { parent -> places.none { it.dir == folder } } })
    }
    TvDialog(
        title = when (mode) {
            PickMode.SUGGESTIONS -> "${Strings.pickTitle}: ${channel.name}"
            PickMode.FILES -> Strings.filesTitle(channel.name)
            PickMode.BROWSE -> Strings.browseTitle(channel.name)
        },
        onDismiss = onDismiss,
        onBack = goBack,
        maxWidth = 620.dp,
        scrollBody = false,
        footer = {
            if (mode != PickMode.SUGGESTIONS) TvButton(Strings.backToSuggestions, onClick = { mode = PickMode.SUGGESTIONS })
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
                    modifier = Modifier.size(width = 68.dp, height = 52.dp).background(Color(0xFFE5E5EA), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    current?.let { Image(it, contentDescription = Strings.currentLogo, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
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
        if (candidates.isEmpty()) Text(if (mode == PickMode.FILES) Strings.filesEmpty else Strings.noCandidates, color = TextSecondary, fontSize = 14.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(candidates, key = { it.id }) { candidate ->
                CandidateRow(candidate, viewModel, state.poorLogos[candidate.id]) { viewModel.choose(channel.id, candidate); onDismiss() }
            }
        }
    }
}

private enum class PickMode { SUGGESTIONS, FILES, BROWSE }

@Composable
private fun CandidateRow(candidate: LogoCandidate, viewModel: MainViewModel, issues: Set<com.babadogan.channellogos.logos.LogoQuality.Issue>?, onClick: () -> Unit) {
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
private fun SettingsDialog(settings: Settings, onChange: (Settings) -> Unit, onDismiss: () -> Unit) {
    TvDialog(
        title = Strings.settings,
        onDismiss = onDismiss,
        maxWidth = 980.dp,
        footer = { TvButton(Strings.cancel, primary = true, onClick = onDismiss) }
    ) {
        PanelSection(Strings.appearanceSection)
        SettingRow(Strings.language, Strings.languageDesc) {
            for ((code, label) in listOf(Settings.LANGUAGE_SYSTEM to Strings.languageSystem, "tr" to "Türkçe", "en" to "English", "ro" to "Română")) {
                TvButton(label, selected = settings.language == code, small = true, onClick = { onChange(settings.copy(language = code)) })
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
        SettingRow(Strings.sources, Strings.sourcesDesc) {
            for ((label, value, update) in listOf(
                Triple("GitHub", settings.gitHub) { on: Boolean -> settings.copy(gitHub = on) },
                Triple("picons", settings.picons) { on: Boolean -> settings.copy(picons = on) },
                Triple("iptv-org", settings.iptv) { on: Boolean -> settings.copy(iptv = on) },
                Triple(Strings.filesSource, settings.usb) { on: Boolean -> settings.copy(usb = on) }
            )) {
                TvButton(if (value) "✓ $label" else label, selected = value, small = true, onClick = { onChange(update(!value)) })
            }
        }
    }
}

/** One setting: its name and explanation on the left, its choices on the right. */
@Composable
private fun SettingRow(title: String, description: String, options: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Background, RoundedCornerShape(14.dp)).padding(horizontal = 18.dp, vertical = 14.dp),
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
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
    TvDialog(
        title = Strings.about,
        onDismiss = onDismiss,
        maxWidth = 980.dp,
        textBody = true,
        footer = { TvButton(Strings.cancel, primary = true, onClick = onDismiss) },
        header = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(com.babadogan.channellogos.R.drawable.ic_launcher),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).background(Background, RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(Strings.appTitle, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("${Strings.version(version)}  ·  ${Strings.aboutLicense}", color = TextSecondary, fontSize = 14.sp)
                    Text(Strings.aboutCompany, color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Text(Strings.aboutWhat, color = TextPrimary, fontSize = 16.sp)
        // Two columns of cards: the body is wide on a TV and long lines are hard to read from the sofa.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AboutSection(Strings.aboutHowTitle, Strings.aboutHow)
                AboutSection(Strings.aboutNoteTitle, Strings.aboutNote)
                AboutSection(Strings.aboutDeveloperTitle, Strings.aboutDeveloper)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AboutSection(Strings.aboutSourcesTitle, Strings.aboutSources)
            }
        }
        AboutSection(Strings.aboutDisclaimerTitle, Strings.aboutDisclaimer)
    }
}

@Composable
private fun AboutSection(title: String, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Background, RoundedCornerShape(14.dp)).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(text, color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun LogoPreview(candidate: LogoCandidate, viewModel: MainViewModel) {
    val image by produceState<ImageBitmap?>(null, candidate.id) { value = viewModel.preview(candidate) }
    // Logos are drawn on a light tile: many are dark text meant for a light background.
    Box(
        modifier = Modifier.size(width = 68.dp, height = 52.dp).background(Color(0xFFE5E5EA), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        image?.let { Image(it, contentDescription = candidate.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
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
    content: @Composable (focused: Boolean) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
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
            .background(if (focused) SurfaceVariant else Surface, RoundedCornerShape(14.dp))
            .border(if (focused) BorderStroke(3.dp, FocusFrame) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(14.dp))
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
    small: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val background = when {
        !enabled -> SurfaceVariant.copy(alpha = 0.5f)
        focused -> Accent
        primary || selected -> Accent.copy(alpha = 0.3f)
        else -> SurfaceVariant
    }
    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(12.dp))
            .border(
                when {
                    focused -> BorderStroke(3.dp, FocusFrame)
                    selected -> BorderStroke(2.dp, Accent)
                    else -> BorderStroke(0.dp, Color.Transparent)
                },
                RoundedCornerShape(12.dp)
            )
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = if (small) 10.dp else 18.dp, vertical = if (small) 10.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            // On the blue focus fill the text is white in both themes.
            color = if (!enabled) TextSecondary else if (focused) Color.White else TextPrimary,
            fontSize = if (small) 14.sp else 15.sp,
            fontWeight = if (focused || primary) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
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

private fun qualityLabel(issues: Set<com.babadogan.channellogos.logos.LogoQuality.Issue>) =
    if (com.babadogan.channellogos.logos.LogoQuality.Issue.SMALL in issues) Strings.lowQualitySmall else Strings.lowQualityDark

private fun strengthLabel(strength: NameMatcher.Strength) = when (strength) {
    NameMatcher.Strength.EXACT -> Strings.exact
    NameMatcher.Strength.WITHOUT_QUALITY -> Strings.close
    NameMatcher.Strength.LOOSE -> Strings.loose
}

package com.babadogan.channellogos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.babadogan.channellogos.logos.LogoCandidate
import com.babadogan.channellogos.logos.LogoMatching
import com.babadogan.channellogos.logos.LogoSource
import com.babadogan.channellogos.logos.NameMatcher
import com.babadogan.channellogos.settings.Settings
import com.babadogan.channellogos.tv.Channel

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

    Row(modifier = Modifier.fillMaxSize().background(Background).padding(horizontal = 40.dp, vertical = 28.dp)) {
        SidePanel(
            state, viewModel,
            onSettings = { showSettings = true },
            onAbout = { showAbout = true },
            onConfirm = { message, action -> confirming = message to action },
            modifier = Modifier.width(340.dp).fillMaxHeight()
        )
        Spacer(Modifier.width(28.dp))
        ChannelList(state, viewModel, onChange = { picking = it }, modifier = Modifier.weight(1f).fillMaxHeight())
    }

    picking?.let { channel ->
        LogoPicker(channel, viewModel, changed = channel.id in state.changed, onDismiss = { picking = null })
    }
    if (showSettings) SettingsDialog(state.settings, onChange = viewModel::updateSettings, onDismiss = { showSettings = false })
    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
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
    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(Strings.appTitle, color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(Strings.appSubtitle, color = TextSecondary, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TvButton(Strings.settings, onClick = onSettings)
            TvButton(Strings.about, onClick = onAbout)
        }

        Column(
            modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(16.dp)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Text(Strings.sourceStats(state.tvLogos, state.gitHubLogos, state.iptvLogos), color = TextSecondary, fontSize = 13.sp)
                    if (state.usbLogos > 0) Text(Strings.usbStats(state.usbLogos), color = TextSecondary, fontSize = 13.sp)
                    if (state.sourcesUpdatedAt > 0L) {
                        val date = remember(state.sourcesUpdatedAt) {
                            java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
                                .format(java.util.Date(state.sourcesUpdatedAt))
                        }
                        Text(Strings.sourcesUpdatedAt(date), color = TextSecondary, fontSize = 13.sp)
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

        if (state.connection == MainViewModel.Connection.Connected) {
            val applyFocus = remember { FocusRequester() }
            TvButton(
                Strings.apply(state.selected.size),
                primary = true,
                enabled = state.selected.isNotEmpty() && !state.busy,
                onClick = viewModel::applySelected,
                modifier = Modifier.focusRequester(applyFocus)
            )
            LaunchedEffect(Unit) { runCatching { applyFocus.requestFocus() } }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvButton(Strings.showMissing, selected = !state.showAll, onClick = { viewModel.setShowAll(false) })
                TvButton(Strings.showAll, selected = state.showAll, onClick = { viewModel.setShowAll(true) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvButton(Strings.selectAll, onClick = viewModel::selectAll, enabled = !state.busy)
                TvButton(Strings.selectSafe, onClick = viewModel::selectSafe, enabled = !state.busy)
            }
            TvButton(Strings.selectNone, onClick = viewModel::selectNone, enabled = !state.busy)
            TvButton(Strings.rescan, onClick = viewModel::scan, enabled = !state.busy)
            TvButton(Strings.refreshSources, onClick = viewModel::refreshSources, enabled = !state.busy)

            // Backup and reset
            Text(Strings.backupSection, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            val backupDate = remember(state.backupTime) {
                if (state.backupTime == 0L) null
                else java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(state.backupTime))
            }
            Text(
                backupDate?.let { Strings.backupInfo(state.backupCount, it) } ?: Strings.noBackup,
                color = TextSecondary, fontSize = 13.sp
            )
            TvButton(Strings.backupLogos, enabled = !state.busy && state.withLogo > 0,
                onClick = { onConfirm(Strings.confirmBackup) { viewModel.backupLogos() } })
            TvButton(Strings.restoreBackup, enabled = !state.busy && state.backupCount > 0,
                onClick = { onConfirm(Strings.confirmRestore) { viewModel.restoreBackup() } })
            TvButton(Strings.resetToDefault(state.changed.size), enabled = !state.busy && state.changed.isNotEmpty(),
                onClick = { onConfirm(Strings.confirmReset(state.changed.size)) { viewModel.resetToDefault() } })
        }

        state.status?.let { status ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.busy) {
                    CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(status, color = TextPrimary, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ChannelList(
    state: MainViewModel.UiState,
    viewModel: MainViewModel,
    onChange: (Channel) -> Unit,
    modifier: Modifier
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.visibleChannels, key = { it.id }) { channel ->
            ChannelRow(
                channel = channel,
                changed = channel.id in state.changed,
                suggestion = state.suggestions[channel.id],
                selected = channel.id in state.selected,
                viewModel = viewModel,
                onToggle = { viewModel.toggle(channel.id) },
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
    selected: Boolean,
    viewModel: MainViewModel,
    onToggle: () -> Unit,
    onChange: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FocusBox(onClick = onToggle, modifier = Modifier.weight(1f)) { focused ->
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
                            val safe = LogoMatching.isSafe(suggestion)
                            Text(
                                if (safe) strengthLabel(suggestion.strength) else Strings.check,
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
    body: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
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
    var fromUsb by remember { mutableStateOf(false) }
    val candidates = remember(channel.id, fromUsb) {
        if (fromUsb) viewModel.usbCandidatesFor(channel) else viewModel.candidatesFor(channel)
    }
    TvDialog(
        title = if (fromUsb) Strings.usbTitle(channel.name) else "${Strings.pickTitle}: ${channel.name}",
        onDismiss = onDismiss,
        maxWidth = 620.dp,
        scrollBody = false,
        footer = {
            if (fromUsb) TvButton(Strings.backToSuggestions, onClick = { fromUsb = false })
            else TvButton(Strings.pickFromUsb, onClick = { fromUsb = true })
            TvButton(Strings.noLogoOption, onClick = { viewModel.choose(channel.id, null); onDismiss() })
            if (changed) TvButton(Strings.resetOne, onClick = { viewModel.resetToDefault(listOf(channel.id)); onDismiss() })
        }
    ) {
        if (candidates.isEmpty()) Text(if (fromUsb) Strings.usbEmpty else Strings.noCandidates, color = TextSecondary, fontSize = 14.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(candidates, key = { it.id }) { candidate ->
                FocusBox(onClick = { viewModel.choose(channel.id, candidate); onDismiss() }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LogoPreview(candidate, viewModel)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(candidate.name.substringAfterLast('/'), color = TextPrimary, fontSize = 15.sp)
                            Text(sourceLabel(candidate), color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsDialog(settings: Settings, onChange: (Settings) -> Unit, onDismiss: () -> Unit) {
    TvDialog(title = Strings.settings, onDismiss = onDismiss) {
        SettingTitle(Strings.language)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for ((code, label) in listOf(Settings.LANGUAGE_SYSTEM to Strings.languageSystem, "tr" to "Türkçe", "en" to "English")) {
                TvButton(label, selected = settings.language == code, onClick = { onChange(settings.copy(language = code)) })
            }
        }

        SettingTitle(Strings.theme)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for ((code, label) in listOf(
                Settings.THEME_SYSTEM to Strings.languageSystem,
                Settings.THEME_DARK to Strings.themeDark,
                Settings.THEME_LIGHT to Strings.themeLight
            )) {
                TvButton(label, selected = settings.theme == code, onClick = { onChange(settings.copy(theme = code)) })
            }
        }

        SettingTitle(Strings.preselection)
        Text(Strings.preselectionDesc, color = TextSecondary, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TvButton(Strings.preselectSafe, selected = settings.preselectSafe, onClick = { onChange(settings.copy(preselectSafe = true)) })
            TvButton(Strings.preselectNone, selected = !settings.preselectSafe, onClick = { onChange(settings.copy(preselectSafe = false)) })
        }

        SettingTitle(Strings.textLogosSetting)
        Text(Strings.textLogosDesc, color = TextSecondary, fontSize = 13.sp)
        OnOff(settings.textLogos) { onChange(settings.copy(textLogos = it)) }

        SettingTitle(Strings.sources)
        for ((label, value, update) in listOf(
            Triple("GitHub", settings.gitHub) { on: Boolean -> settings.copy(gitHub = on) },
            Triple("iptv-org", settings.iptv) { on: Boolean -> settings.copy(iptv = on) },
            Triple("USB", settings.usb) { on: Boolean -> settings.copy(usb = on) }
        )) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = TextPrimary, fontSize = 15.sp, modifier = Modifier.width(110.dp))
                OnOff(value) { onChange(update(it)) }
            }
        }
    }
}

@Composable
private fun SettingTitle(text: String) {
    Text(text, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun OnOff(value: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TvButton(Strings.on, selected = value, onClick = { onChange(true) })
        TvButton(Strings.off, selected = !value, onClick = { onChange(false) })
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
        maxWidth = 780.dp,
        textBody = true,
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
                    Text(Strings.version(version), color = TextSecondary, fontSize = 14.sp)
                    Text(Strings.aboutCompany, color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Text(Strings.aboutWhat, color = TextPrimary, fontSize = 15.sp)
        AboutSection(Strings.aboutHowTitle, Strings.aboutHow)
        AboutSection(Strings.aboutSourcesTitle, Strings.aboutSources)
        AboutSection(Strings.aboutNoteTitle, Strings.aboutNote)
        AboutSection(Strings.aboutDeveloperTitle, Strings.aboutDeveloper)
        AboutSection(Strings.aboutLicenseTitle, Strings.aboutLicense)
        AboutSection(Strings.aboutDisclaimerTitle, Strings.aboutDisclaimer)
    }
}

@Composable
private fun AboutSection(title: String, text: String) {
    SettingTitle(title)
    Text(text, color = TextSecondary, fontSize = 14.sp)
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

/** A focusable, clickable surface that shows a clear focus frame for the remote control. */
@Composable
private fun FocusBox(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable (focused: Boolean) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        modifier = modifier
            .background(if (focused) SurfaceVariant else Surface, RoundedCornerShape(14.dp))
            .border(if (focused) BorderStroke(3.dp, FocusFrame) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
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
    enabled: Boolean = true
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
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            // On the blue focus fill the text is white in both themes.
            color = if (!enabled) TextSecondary else if (focused) Color.White else TextPrimary,
            fontSize = 15.sp,
            fontWeight = if (focused || primary) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

private fun sourceLabel(candidate: LogoCandidate) = when (candidate.source) {
    LogoSource.TV -> Strings.sourceTv
    LogoSource.GITHUB -> Strings.sourceGitHub
    LogoSource.IPTV -> Strings.sourceIptv
    LogoSource.TEXT -> Strings.sourceText
    LogoSource.FILE -> Strings.sourceFile
}

private fun strengthLabel(strength: NameMatcher.Strength) = when (strength) {
    NameMatcher.Strength.EXACT -> Strings.exact
    NameMatcher.Strength.WITHOUT_QUALITY -> Strings.close
    NameMatcher.Strength.LOOSE -> Strings.loose
}

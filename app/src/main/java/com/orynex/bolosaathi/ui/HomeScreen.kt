package com.orynex.bolosaathi.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.orynex.bolosaathi.core.ChatMsg
import com.orynex.bolosaathi.core.L
import com.orynex.bolosaathi.core.Lang
import com.orynex.bolosaathi.core.MsgKind
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(vm: MainViewModel, modifier: Modifier, openScam: () -> Unit, openSettings: () -> Unit) {
    val ctx = LocalContext.current
    val messages by vm.messages.collectAsState()
    val status by vm.status.collectAsState()
    val confirm by vm.confirm.collectAsState()
    val listening by vm.listening.collectAsState()
    val level by vm.level.collectAsState()
    val partial by vm.partial.collectAsState()
    val busy by vm.busy.collectAsState()
    val lang by vm.lang.collectAsState()
    val a11y by vm.a11yOn.collectAsState()
    var typing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val thinking = busy && !listening && confirm == null && status != null

    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) vm.micTap() }
    fun onMic() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.micTap()
        else micPerm.launch(Manifest.permission.RECORD_AUDIO)
    }

    val shotFile = remember { File(ctx.cacheDir, "shots").apply { mkdirs() }.let { File(it, "shot.jpg") } }
    val shotUri: Uri = remember { FileProvider.getUriForFile(ctx, ctx.packageName + ".files", shotFile) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(shotFile.path, o)
            var s = 1
            while (maxOf(o.outWidth, o.outHeight) / s > 1800) s *= 2
            BitmapFactory.decodeFile(shotFile.path, BitmapFactory.Options().apply { inSampleSize = s })?.let(vm::explainPhoto)
        }
    }

    LaunchedEffect(messages.size, thinking) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1)
    }

    Column(modifier.fillMaxSize().imePadding()) {
        Hero(
            lang = lang,
            a11y = a11y,
            showClear = messages.isNotEmpty(),
            onClear = vm::clearChat,
            onLang = vm::setLang,
            onFix = { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
        )

        Crossfade(
            targetState = messages.isEmpty(),
            animationSpec = tween(320, easing = Motion.EaseOut),
            modifier = Modifier.weight(1f),
            label = "body",
        ) { empty ->
            if (empty) {
                HomeIdle(
                    onMic = { onMic() },
                    onCamera = { camera.launch(shotUri) },
                    onYojana = { vm.sendText("मुझे कौन सी सरकारी योजना मिल सकती है?") },
                    onScam = openScam,
                    onPick = { vm.sendText(it) },
                    onDiary = { vm.diaryOpen.value = true },
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(messages, key = { _, m -> m.id }) { _, m ->
                        ChatBubble(m, Modifier.animateItem())
                    }
                    if (thinking) {
                        item(key = "thinking") { ThinkingBubble(status.orEmpty(), Modifier.animateItem()) }
                    }
                }
            }
        }

        Dock(
            listening = listening, busy = busy, level = level, partial = partial, status = status,
            confirm = confirm, typing = typing, draft = draft,
            onDraft = { draft = it },
            onSend = { vm.sendText(draft); draft = "" },
            onToggleType = { typing = !typing },
            onMic = ::onMic,
            onCamera = { camera.launch(shotUri) },
        )
    }
}

@Composable
private fun Hero(
    lang: Lang, a11y: Boolean, showClear: Boolean,
    onClear: () -> Unit, onLang: (Lang) -> Unit, onFix: () -> Unit,
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val name = remember { com.orynex.bolosaathi.BoloApp.instance.prefs.userName }
    val hello = when (hour) {
        in 4..11 -> "सुप्रभात"
        in 17..20 -> "शुभ संध्या"
        else -> "नमस्ते"
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0B1F4B), Color(0xFF1E4FA3), Color(0xFF38A3E8))))
            .statusBarsPadding()
            .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (name.isBlank()) hello else "$hello, $name जी", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
                    Text("बोलो साथी", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                AnimatedVisibility(showClear, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                    IconButton(onClick = onClear) { Icon(Icons.Outlined.DeleteSweep, "साफ़ करें", tint = Color.White.copy(alpha = 0.85f)) }
                }
                SosPill()
                Spacer(Modifier.width(8.dp))
                LangPill(lang, onLang)
            }
            Spacer(Modifier.height(12.dp))
            AnimatedContent(
                targetState = a11y,
                transitionSpec = { (fadeIn(tween(250)) + slideInVertically { it / 3 }) togetherWith fadeOut(tween(150)) },
                label = "perm",
            ) { on ->
                if (on) StatusPill(Icons.Outlined.VerifiedUser, "फ़ोन चलाने के लिए तैयार", Color(0xFFBFE3CF), null)
                else StatusPill(Icons.Outlined.TouchApp, "फ़ोन चलाने की अनुमति दें", Color(0xFFFFD699), onFix)
            }
        }
    }
}

@Composable
private fun StatusPill(icon: ImageVector, text: String, tint: Color, onClick: (() -> Unit)?) {
    val m = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.12f))
    Row(
        (if (onClick != null) m.bouncyClick(onClick = onClick) else m).padding(start = 10.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White)
        if (onClick != null) Icon(Icons.Outlined.ChevronRight, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun LangPill(lang: Lang, onPick: (Lang) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.16f))
                .bouncyClick { open = true }
                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(lang, transitionSpec = { (fadeIn() + slideInVertically { it }) togetherWith (fadeOut() + slideOutVertically { -it }) }, label = "lang") {
                Text(it.native, style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
            Icon(Icons.Outlined.ExpandMore, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = RoundedCornerShape(14.dp), containerColor = C.Surface) {
            Lang.entries.forEach { l ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(l.native, style = MaterialTheme.typography.titleMedium)
                            Text(l.label, style = MaterialTheme.typography.bodySmall, color = C.Muted)
                        }
                    },
                    trailingIcon = { if (l == lang) Icon(Icons.Outlined.CheckCircle, null, tint = C.Brand) },
                    onClick = { onPick(l); open = false },
                )
            }
        }
    }
}

@Composable
private fun ActionTile(title: String, sub: String, icon: ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(128.dp).clip(RoundedCornerShape(20.dp)).bouncyClick(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = C.Surface,
        border = BorderStroke(1.dp, C.Line),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = C.Ink, maxLines = 1)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = C.Muted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ExampleChip(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).bouncyClick(onClick = onClick),
        shape = RoundedCornerShape(20.dp), color = C.Surface, border = BorderStroke(1.dp, C.Line),
    ) {
        Text("\"$text\"", style = MaterialTheme.typography.bodyMedium, color = C.Ink, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

@Composable
private fun TrustNote(modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.BrandSoft).padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.VerifiedUser, null, tint = C.Brand, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "पैसे या संदेश भेजने से पहले मैं हमेशा पूछूँगा। पिन और ओटीपी आप खुद डालेंगे, मैं उन्हें कभी नहीं देखता।",
            style = MaterialTheme.typography.bodyMedium, color = C.Ink,
        )
    }
}

@Composable
private fun ChatBubble(m: ChatMsg, modifier: Modifier) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter = fadeIn(tween(260)) +
            slideInHorizontally(Motion.soft()) { if (m.fromUser) it / 5 else -it / 5 } +
            expandVertically(Motion.soft(), expandFrom = Alignment.Top),
    ) {
        when {
            m.fromUser -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Surface(shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp), color = C.Brand, modifier = Modifier.widthIn(max = 320.dp)) {
                    Text(m.text, style = MaterialTheme.typography.bodyLarge, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp))
                }
            }
            m.kind == MsgKind.STEP -> Row(Modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(C.Brand.copy(alpha = 0.5f)))
                Spacer(Modifier.width(10.dp))
                Text(m.text, style = MaterialTheme.typography.bodyMedium, color = C.Muted)
            }
            else -> {
                val (bg, line, icon, tint) = when (m.kind) {
                    MsgKind.WARNING -> Tone(C.WarnSoft, Color(0xFFF2C4C0), Icons.Outlined.WarningAmber, C.Warn)
                    MsgKind.SUCCESS -> Tone(C.OkSoft, Color(0xFFBFE3CF), Icons.Outlined.CheckCircle, C.Ok)
                    else -> Tone(C.Surface, C.Line, null, C.Ink)
                }
                Row(Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp), color = bg,
                        border = BorderStroke(1.dp, line), modifier = Modifier.widthIn(max = 340.dp),
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            if (icon != null) {
                                Icon(icon, null, tint = tint, modifier = Modifier.padding(top = 3.dp).size(22.dp))
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(m.text, style = MaterialTheme.typography.bodyLarge, color = C.Ink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingBubble(text: String, modifier: Modifier) {
    Row(modifier.fillMaxWidth()) {
        Surface(shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp), color = C.Surface, border = BorderStroke(1.dp, C.Line)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                ThinkingDots(C.Brand)
                Spacer(Modifier.width(12.dp))
                AnimatedContent(text, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "think") {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = C.Muted)
                }
            }
        }
    }
}

private data class Tone(val bg: Color, val line: Color, val icon: ImageVector?, val tint: Color)

@Composable
private fun Dock(
    listening: Boolean, busy: Boolean, level: Float, partial: String, status: String?,
    confirm: Confirm?, typing: Boolean, draft: String,
    onDraft: (String) -> Unit, onSend: () -> Unit, onToggleType: () -> Unit,
    onMic: () -> Unit, onCamera: () -> Unit,
) {
    Surface(
        color = C.Surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.shadow(18.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), ambientColor = Color(0x22000000), spotColor = Color(0x22000000)),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(C.Line))

            val line = when {
                listening && partial.isNotBlank() -> partial
                listening -> "बोलिए, मैं सुन रहा हूँ"
                status != null -> status
                busy -> "एक पल…"
                else -> "माइक दबाइए और बोलिए"
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp).height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                AnimatedVisibility(listening, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                    Row { VoiceBars(level, C.Accent); Spacer(Modifier.width(12.dp)) }
                }
                AnimatedContent(
                    targetState = line,
                    transitionSpec = { (fadeIn(tween(220)) + slideInVertically { it / 2 }) togetherWith (fadeOut(tween(120)) + slideOutVertically { -it / 2 }) },
                    label = "line",
                ) {
                    Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 2, textAlign = TextAlign.Center, color = if (listening) C.Brand else C.Muted)
                }
            }

            AnimatedVisibility(
                visible = confirm != null,
                enter = expandVertically(Motion.soft()) + fadeIn(tween(200)),
                exit = shrinkVertically(Motion.soft()) + fadeOut(tween(150)),
            ) {
                val c = confirm
                Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.weight(1f).height(58.dp).clip(RoundedCornerShape(29.dp)).background(Brush.horizontalGradient(listOf(C.Brand, C.Accent))).bouncyClick { c?.answer?.invoke(true) },
                        contentAlignment = Alignment.Center,
                    ) { Text("हाँ, कर दो", style = MaterialTheme.typography.labelLarge, color = Color.White) }
                    Box(
                        Modifier.weight(1f).height(58.dp).clip(RoundedCornerShape(29.dp)).background(Color(0xFFEEF3F9)).bouncyClick { c?.answer?.invoke(false) },
                        contentAlignment = Alignment.Center,
                    ) { Text("नहीं", style = MaterialTheme.typography.labelLarge, color = C.Ink) }
                }
            }

            AnimatedVisibility(typing, enter = expandVertically(Motion.soft()) + fadeIn(), exit = shrinkVertically(Motion.soft()) + fadeOut()) {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft, onValueChange = onDraft,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("यहाँ लिखिए…") },
                        shape = RoundedCornerShape(24.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = C.Line, focusedBorderColor = C.Brand),
                    )
                    val canSend = draft.isNotBlank()
                    val a by animateFloatAsState(if (canSend) 1f else 0.35f, label = "send")
                    IconButton(onClick = onSend, enabled = canSend) {
                        Icon(Icons.AutoMirrored.Outlined.Send, "भेजें", tint = C.Brand.copy(alpha = a))
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                DockButton(Icons.Outlined.Keyboard, "लिखें", active = typing, onClick = onToggleType)
                Spacer(Modifier.weight(1f))
                MicOrb(listening = listening, busy = busy && !listening, level = level, onClick = onMic)
                Spacer(Modifier.weight(1f))
                DockButton(Icons.Outlined.CameraAlt, "फ़ोटो", onClick = onCamera)
            }
        }
    }
}

@Composable
private fun DockButton(icon: ImageVector, label: String, active: Boolean = false, onClick: () -> Unit) {
    val t by animateFloatAsState(if (active) 1f else 0f, tween(200), label = "dbg")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).bouncyClick(onClick = onClick).padding(8.dp),
    ) {
        Box(
            Modifier.size(50.dp).clip(CircleShape).background(lerpColor(Color(0xFFEEF3F9), C.BrandSoft, t)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = lerpColor(C.Muted, C.Brand, t)) }
        Text(label, style = MaterialTheme.typography.labelSmall, color = C.Muted, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun MicOrb(listening: Boolean, busy: Boolean, level: Float, onClick: () -> Unit) {
    val size by animateDpAsState(if (listening) 90.dp else 82.dp, Motion.soft(), label = "orb")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(128.dp)) {
        MicHalo(listening, level, C.Accent, Modifier.fillMaxSize())
        Box(
            Modifier
                .size(size)
                .shadow(if (listening) 16.dp else 10.dp, CircleShape, spotColor = C.Brand.copy(alpha = 0.5f), ambientColor = C.Brand.copy(alpha = 0.3f))
                .clip(CircleShape)
                .background(
                    if (busy) Brush.linearGradient(listOf(Color(0xFF2B3440), C.Ink))
                    else Brush.linearGradient(listOf(Color(0xFF5CC3F5), Color(0xFF2B8FE0), Color(0xFF1A5BB8)))
                )
                .bouncyClick(pressedScale = 0.92f, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = busy,
                transitionSpec = { (scaleIn(Motion.snappy()) + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                label = "icon",
            ) { b ->
                Icon(
                    if (b) Icons.Outlined.Stop else Icons.Outlined.Mic,
                    contentDescription = if (b) "रोकें" else "बोलिए",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp),
                )
            }
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t,
)

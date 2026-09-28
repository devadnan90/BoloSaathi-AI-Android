package com.orynex.bolosaathi.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.orynex.bolosaathi.BoloApp
import com.orynex.bolosaathi.core.Lang
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val phrases = listOf(
    "मम्मी को 500 रुपये भेज दो",
    "299 वाला रिचार्ज कर दो",
    "ये मैसेज सच है या धोखा?",
    "मुझे कौन सी योजना मिलेगी?",
    "बेटा को लिखो, मैं पहुँच गया",
)

@Composable
fun WelcomeScreen(vm: MainViewModel, onDone: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF081634), Color(0xFF1E4FA3), Color(0xFF4FB3EE))))
    ) {
        FloatingGlow()
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            Column(
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                LogoOrb(Modifier.enterStagger(0))
                Spacer(Modifier.height(20.dp))
                Text(
                    "बोलो साथी", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.enterStagger(1),
                )
                Text(
                    "अपनी भाषा में बोलिए, फ़ोन काम कर देगा", color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                    modifier = Modifier.enterStagger(2).padding(top = 6.dp),
                )
                Spacer(Modifier.height(22.dp))
                PhraseTicker(Modifier.enterStagger(3))
            }

            Surface(
                color = C.Surface,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                modifier = Modifier.fillMaxWidth().enterStagger(4, rise = 60.dp).animateContentSize(Motion.soft()),
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(Motion.soft()) { it / 3 * dir } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(tween(180)) { -it / 3 * dir } + fadeOut(tween(150)))
                    },
                    label = "step",
                ) { s ->
                    Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp)) {
                        if (s == 0) IntroStep { step = 1 } else DetailsStep(vm, onBack = { step = 0 }, onDone = onDone)
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingGlow() {
    val t = rememberInfiniteTransition(label = "glow")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by t.animateFloat(1f, 0f, infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(
            Brush.radialGradient(listOf(Color(0x33FFFFFF), Color.Transparent), center = Offset(size.width * (0.15f + 0.3f * a), size.height * 0.18f), radius = size.width * 0.6f),
            radius = size.width * 0.6f, center = Offset(size.width * (0.15f + 0.3f * a), size.height * 0.18f),
        )
        drawCircle(
            Brush.radialGradient(listOf(Color(0x3387D3F8), Color.Transparent), center = Offset(size.width * (0.9f - 0.3f * b), size.height * 0.42f), radius = size.width * 0.5f),
            radius = size.width * 0.5f, center = Offset(size.width * (0.9f - 0.3f * b), size.height * 0.42f),
        )
    }
}

@Composable
private fun LogoOrb(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "orb")
    val pulse by t.animateFloat(0.15f, 0.75f, infiniteRepeatable(tween(1400, easing = Motion.EaseInOut), RepeatMode.Reverse), label = "p")
    Box(modifier.size(170.dp), contentAlignment = Alignment.Center) {
        MicHalo(listening = true, level = pulse, color = Color.White, modifier = Modifier.fillMaxSize())
        Box(
            Modifier.size(92.dp).shadow(20.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.4f))
                .clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.RecordVoiceOver, null, tint = C.Brand, modifier = Modifier.size(44.dp))
        }
    }
}

@Composable
private fun PhraseTicker(modifier: Modifier) {
    var i by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(2600); i = (i + 1) % phrases.size } }
    Box(
        modifier.clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = i,
            transitionSpec = {
                (slideInVertically(Motion.soft()) { it } + fadeIn(tween(250))) togetherWith
                    (slideOutVertically(tween(220)) { -it } + fadeOut(tween(180)))
            },
            label = "phrase",
        ) { k ->
            Text("\"${phrases[k]}\"", color = Color.White, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun IntroStep(onNext: () -> Unit) {
    Text("आपका फ़ोन वाला साथी", style = MaterialTheme.typography.titleLarge, color = C.Ink)
    Spacer(Modifier.height(16.dp))
    Point(Icons.Outlined.Mic, "बस बोलिए", "पैसे भेजना, रिचार्ज, संदेश, सब आवाज़ से", 0)
    Point(Icons.Outlined.VerifiedUser, "हर बार पूछेगा", "पैसे या संदेश भेजने से पहले आपकी हाँ ज़रूरी", 1)
    Point(Icons.Outlined.Lock, "पिन आपका, सिर्फ़ आपका", "पिन और ओटीपी मैं कभी नहीं देखता", 2)
    Spacer(Modifier.height(20.dp))
    BigButton("शुरू करें", enabled = true, onClick = onNext)
}

@Composable
private fun Point(icon: ImageVector, title: String, sub: String, i: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).enterStagger(i + 5), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(C.BrandSoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = C.Brand)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = C.Ink)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = C.Muted)
        }
    }
}

@Composable
private fun DetailsStep(vm: MainViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = BoloApp.instance.prefs
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(prefs.userName) }
    var phone by rememberSaveable { mutableStateOf(prefs.userPhone) }
    var lang by remember { mutableStateOf(prefs.lang) }
    var hearing by remember { mutableStateOf(false) }
    val valid = name.isNotBlank() && phone.length == 10

    fun speakName() {
        scope.launch {
            hearing = true
            val heard = vm.voice.listen()
            hearing = false
            if (!heard.isNullOrBlank()) {
                name = heard.replace(Regex("(?i)(मेरा नाम|mera naam|है|hai)"), "").trim().split(" ").take(3).joinToString(" ")
            }
        }
    }
    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) speakName() }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "पीछे", tint = C.Ink) }
            Text("अपने बारे में बताइए", style = MaterialTheme.typography.titleLarge, color = C.Ink)
        }
        Spacer(Modifier.height(12.dp))

        val fieldColors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = C.Line, focusedBorderColor = C.Brand, focusedLabelColor = C.Brand,
        )
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("आपका नाम") },
            placeholder = { Text(if (hearing) "बोलिए…" else "जैसे रमेश") },
            singleLine = true, shape = RoundedCornerShape(16.dp), colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                val bg by animateColorAsState(if (hearing) C.Brand else C.BrandSoft, label = "mbg")
                Box(
                    Modifier.padding(end = 6.dp).size(40.dp).clip(CircleShape).background(bg).bouncyClick {
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) speakName()
                        else micPerm.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Mic, "बोलकर नाम भरें", tint = if (hearing) Color.White else C.Brand, modifier = Modifier.size(20.dp)) }
            },
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = phone, onValueChange = { v -> phone = v.filter { it.isDigit() }.take(10) },
            label = { Text("मोबाइल नंबर") },
            prefix = { Text("+91  ", color = C.Muted) },
            singleLine = true, shape = RoundedCornerShape(16.dp), colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                val a by animateFloatAsState(if (phone.length == 10) 1f else 0f, Motion.snappy(), label = "ok")
                Icon(Icons.Outlined.CheckCircle, null, tint = C.Ok.copy(alpha = a), modifier = Modifier.size((22 * a).dp))
            },
        )

        Spacer(Modifier.height(18.dp))
        Text("आपकी भाषा", style = MaterialTheme.typography.labelMedium, color = C.Muted)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Lang.entries.forEach { l ->
                LangCard(l, selected = l == lang, modifier = Modifier.weight(1f)) { lang = l; vm.setLang(l) }
            }
        }

        Spacer(Modifier.height(22.dp))
        BigButton("चलिए शुरू करें", enabled = valid) {
            prefs.userName = name
            prefs.userPhone = phone
            vm.setLang(lang)
            prefs.onboarded = true
            onDone()
        }
        Text(
            "आपकी जानकारी सिर्फ़ इसी फ़ोन में रहती है।",
            style = MaterialTheme.typography.bodySmall, color = C.Muted, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}

@Composable
private fun LangCard(l: Lang, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) C.Brand else C.Surface, tween(220), label = "lbg")
    val fg by animateColorAsState(if (selected) Color.White else C.Ink, tween(220), label = "lfg")
    val line by animateColorAsState(if (selected) C.Brand else C.Line, tween(220), label = "lln")
    Surface(
        modifier = modifier.height(76.dp).clip(RoundedCornerShape(16.dp)).bouncyClick(onClick = onClick),
        shape = RoundedCornerShape(16.dp), color = bg, border = BorderStroke(1.dp, line),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(l.native, color = fg, style = MaterialTheme.typography.titleMedium)
            Text(l.label, color = fg.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun BigButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (enabled) C.Brand else Color(0xFFC7D3E2), tween(250), label = "bb")
    Box(
        Modifier.fillMaxWidth().height(60.dp)
            .shadow(if (enabled) 10.dp else 0.dp, RoundedCornerShape(30.dp), spotColor = C.Brand.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(30.dp))
            .background(if (enabled) Brush.horizontalGradient(listOf(C.Brand, C.Accent)) else Brush.linearGradient(listOf(bg, bg)))
            .bouncyClick(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

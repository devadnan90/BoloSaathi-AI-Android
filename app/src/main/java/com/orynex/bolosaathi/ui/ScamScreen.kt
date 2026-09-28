package com.orynex.bolosaathi.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.orynex.bolosaathi.core.MsgKind
import com.orynex.bolosaathi.core.PhoneBook

@Composable
fun ScamScreen(vm: MainViewModel, modifier: Modifier) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val result by vm.scamResult.collectAsState()
    val busy by vm.scamBusy.collectAsState()
    var text by rememberSaveable { mutableStateOf("") }
    var hasSms by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    var sms by remember { mutableStateOf<List<PhoneBook.Sms>>(emptyList()) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> hasSms = ok }
    LaunchedEffect(hasSms) { if (hasSms) sms = vm.recentSms() }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { ScamHero { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))) } }

        // check card
        item {
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .offset(y = (-28).dp)
                    .enterStagger(1)
                    .shadow(14.dp, RoundedCornerShape(24.dp), spotColor = C.Brand.copy(alpha = 0.18f), ambientColor = C.Brand.copy(alpha = 0.10f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("मैसेज जाँचिए", color = C.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Row(
                        Modifier.clip(RoundedCornerShape(16.dp)).background(C.BrandSoft)
                            .bouncyClick { clipboard.getText()?.text?.let { text = it } }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.ContentPaste, null, tint = C.Brand, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("चिपकाएँ", style = MaterialTheme.typography.labelMedium, color = C.Brand)
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = text, onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().height(128.dp),
                    placeholder = { Text("जैसे: आपका बिजली कनेक्शन आज रात कट जाएगा, इस नंबर पर कॉल करें…", color = C.Muted) },
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF1F5FA),
                        unfocusedContainerColor = Color(0xFFF1F5FA),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = C.Brand,
                    ),
                )
                Spacer(Modifier.height(14.dp))
                val can = !busy && text.isNotBlank()
                Box(
                    Modifier.fillMaxWidth().height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            if (can) Brush.horizontalGradient(listOf(C.Brand, C.Accent))
                            else Brush.linearGradient(listOf(Color(0xFFC7D3E2), Color(0xFFC7D3E2)))
                        )
                        .bouncyClick(enabled = can) { vm.checkScam(text) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (busy) Row(verticalAlignment = Alignment.CenterVertically) {
                        ThinkingDots(Color.White)
                        Spacer(Modifier.width(10.dp))
                        Text("जाँच रहा हूँ", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    } else Text("जाँचिए", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // result
        item {
            AnimatedVisibility(
                visible = result != null && !busy,
                enter = fadeIn(tween(250)) + scaleIn(Motion.soft(), initialScale = 0.92f) + expandVertically(Motion.soft()),
                exit = fadeOut(tween(150)) + shrinkVertically(),
                modifier = Modifier.offset(y = (-12).dp),
            ) {
                result?.let { r -> VerdictCard(r.kind == MsgKind.WARNING, r.text) }
            }
        }

        // recent sms
        item {
            Row(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("हाल के मैसेज", color = C.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (sms.isNotEmpty()) Text("${sms.size}", style = MaterialTheme.typography.labelMedium, color = C.Muted)
            }
        }
        if (!hasSms) {
            item {
                Column(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp)).background(C.BrandSoft).padding(18.dp)
                ) {
                    Icon(Icons.Outlined.Sms, null, tint = C.Brand)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "अनुमति देंगे तो हर मैसेज एक टैप में जाँच सकेंगे। मैसेज फ़ोन से बाहर तभी जाता है जब आप जाँचें दबाते हैं।",
                        style = MaterialTheme.typography.bodyMedium, color = C.Ink,
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(22.dp)).background(C.Brand)
                            .bouncyClick { perm.launch(Manifest.permission.READ_SMS) }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                    ) { Text("अनुमति दें", color = Color.White, style = MaterialTheme.typography.labelLarge) }
                }
            }
        } else if (sms.isEmpty()) {
            item { Text("कोई मैसेज नहीं मिला।", color = C.Muted, modifier = Modifier.padding(horizontal = 20.dp)) }
        }
        itemsIndexed(sms) { i, s ->
            SmsCard(s, busy, Modifier.padding(horizontal = 16.dp, vertical = 6.dp).enterStagger(i.coerceAtMost(8) + 2)) {
                text = s.body
                vm.checkScam("From: ${s.from}\n${s.body}")
            }
        }
    }
}

@Composable
private fun ScamHero(onHelpline: () -> Unit) {
    val t = rememberInfiniteTransition(label = "shield")
    val pulse by t.animateFloat(0.1f, 0.6f, infiniteRepeatable(tween(1500, easing = Motion.EaseInOut), RepeatMode.Reverse), label = "p")
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0B1F4B), Color(0xFF1E4FA3), Color(0xFF38A3E8))))
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 46.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("धोखा जाँच", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "कोई भी मैसेज, कॉल या ऑफ़र यहाँ पूछिए",
                    style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.16f))
                        .bouncyClick(onClick = onHelpline)
                        .padding(start = 10.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Call, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("साइबर हेल्पलाइन 1930", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                MicHalo(listening = true, level = pulse, color = Color.White, modifier = Modifier.fillMaxSize())
                Box(
                    Modifier.size(58.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Shield, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
            }
        }
    }
}

@Composable
private fun VerdictCard(danger: Boolean, text: String) {
    val tint = if (danger) C.Warn else C.Ok
    val soft = if (danger) C.WarnSoft else C.OkSoft
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = tint.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
    ) {
        Row(
            Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(soft, Color.White))).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                Icon(if (danger) Icons.Outlined.GppMaybe else Icons.Outlined.CheckCircle, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(if (danger) "सावधान!" else "ठीक लगता है", color = tint, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(if (danger) "यह धोखा हो सकता है" else "फिर भी अनजान लिंक न खोलें", style = MaterialTheme.typography.bodySmall, color = C.Muted)
            }
        }
        Text(text, style = MaterialTheme.typography.bodyLarge, color = C.Ink, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 18.dp))
    }
}

@Composable
private fun SmsCard(s: PhoneBook.Sms, busy: Boolean, modifier: Modifier, onCheck: () -> Unit) {
    Row(
        modifier.fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = C.Brand.copy(alpha = 0.12f), ambientColor = C.Brand.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(C.BrandSoft, Color(0xFFD6E8FA)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(s.from.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "#", color = C.Brand, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.from, style = MaterialTheme.typography.labelLarge, color = C.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(DateUtils.getRelativeTimeSpanString(s.date).toString(), style = MaterialTheme.typography.labelSmall, color = C.Muted)
            }
            Spacer(Modifier.height(4.dp))
            Text(s.body, style = MaterialTheme.typography.bodyMedium, color = C.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.clip(RoundedCornerShape(16.dp)).background(C.BrandSoft)
                    .bouncyClick(enabled = !busy, onClick = onCheck)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) { Text("जाँचें", style = MaterialTheme.typography.labelMedium, color = C.Brand) }
        }
    }
}

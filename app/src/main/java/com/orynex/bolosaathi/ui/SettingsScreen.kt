package com.orynex.bolosaathi.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.orynex.bolosaathi.BoloApp
import com.orynex.bolosaathi.BuildConfig
import com.orynex.bolosaathi.agent.SaathiAccessibilityService
import com.orynex.bolosaathi.core.Lang

@Composable
fun SettingsScreen(vm: MainViewModel, modifier: Modifier, onLogout: () -> Unit = {}) {
    val ctx = LocalContext.current
    val prefs = BoloApp.instance.prefs
    val lang by vm.lang.collectAsState()
    val a11y by vm.a11yOn.collectAsState()
    var key by remember { mutableStateOf(prefs.userKey) }
    var bubble by remember { mutableStateOf(prefs.bubble) }
    var guard by remember { mutableStateOf(prefs.scamGuard) }
    var askLogout by remember { mutableStateOf(false) }
    var legal by remember { mutableStateOf<Pair<String, String>?>(null) }
    var mic by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { mic = it }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ProfileHero(prefs.userName, prefs.userPhone)

        Column(Modifier.offset(y = (-26).dp).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // language
            Group(Modifier.enterStagger(1)) {
                GroupTitle("भाषा")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Lang.entries.forEach { l -> LangTile(l, l == lang, Modifier.weight(1f)) { vm.setLang(l) } }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(C.BrandSoft)
                        .bouncyClick { vm.speakTest() }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.VolumeUp, null, tint = C.Brand, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("आवाज़ सुनकर देखें", style = MaterialTheme.typography.labelLarge, color = C.Brand)
                }
            }

            // permissions
            Group(Modifier.enterStagger(2)) {
                GroupTitle("अनुमतियाँ")
                PermRow(Icons.Outlined.TouchApp, "फ़ोन चलाने की अनुमति", "दूसरे ऐप में टैप और टाइप करने के लिए", a11y) {
                    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                Divider()
                PermRow(Icons.Outlined.Mic, "माइक", "आपकी आवाज़ सुनने के लिए", mic) {
                    micPerm.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            // safety
            Group(Modifier.enterStagger(3)) {
                GroupTitle("सुरक्षा")
                ToggleRow(Icons.Outlined.BubbleChart, "तैरता माइक बटन", "हर ऐप के ऊपर, दबाकर बोलिए", bubble) {
                    bubble = it; prefs.bubble = it
                    SaathiAccessibilityService.instance?.setBubbleVisible(it)
                }
                Divider()
                ToggleRow(Icons.Outlined.Shield, "धोखा चेतावनी", "UPI में पैसे माँगने वाली रिक्वेस्ट पर सावधान करे", guard) {
                    guard = it; prefs.scamGuard = it
                }
            }

            // ai key
            Group(Modifier.enterStagger(4)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Outlined.Key)
                    Spacer(Modifier.width(12.dp))
                    Text("Gemini API key", color = C.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    val has = key.isNotBlank() || BuildConfig.GEMINI_API_KEY.isNotBlank()
                    StateChip(if (has) "मौजूद" else "नहीं है", has)
                }
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = key, onValueChange = { key = it; prefs.userKey = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text(if (BuildConfig.GEMINI_API_KEY.isNotBlank()) "ऐप में पहले से है, चाहें तो बदलें" else "AIza…") },
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF1F5FA), unfocusedContainerColor = Color(0xFFF1F5FA),
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = C.Brand,
                    ),
                )
            }

            FeaturesGroup(Modifier.enterStagger(5))

            Group(Modifier.enterStagger(5)) {
                Text("जानकारी", color = C.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                LinkRow(Icons.Outlined.PrivacyTip, "Privacy Policy", "आपकी जानकारी कैसे सुरक्षित है") { vm.legal.value = "privacy" to "Privacy Policy" }
                HorizontalDivider(color = C.Line, modifier = Modifier.padding(vertical = 6.dp))
                LinkRow(Icons.Outlined.Description, "Terms of Service", "ऐप इस्तेमाल करने की शर्तें") { vm.legal.value = "terms" to "Terms of Service" }
                HorizontalDivider(color = C.Line, modifier = Modifier.padding(vertical = 6.dp))
                LinkRow(Icons.Outlined.Storage, "डेटा और अनुमतियाँ", "कौन सा डेटा कहाँ जाता है") { vm.legal.value = "data" to "डेटा और अनुमतियाँ" }
            }

            // trust
            Row(
                Modifier.fillMaxWidth().enterStagger(5)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(C.BrandSoft, Color(0xFFE3F3FD))))
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Outlined.VerifiedUser, null, tint = C.Brand)
                Spacer(Modifier.width(12.dp))
                Text(
                    "पैसे भेजने, संदेश भेजने या फ़ॉर्म जमा करने से पहले मैं हमेशा पूछता हूँ। पिन, ओटीपी और पासवर्ड मैं कभी नहीं पढ़ता।",
                    style = MaterialTheme.typography.bodyMedium, color = C.Ink,
                )
            }

            // logout
            Row(
                Modifier.fillMaxWidth().height(58.dp).enterStagger(6)
                    .clip(RoundedCornerShape(29.dp))
                    .background(C.WarnSoft)
                    .bouncyClick { askLogout = true },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = C.Warn)
                Spacer(Modifier.width(10.dp))
                Text("लॉग आउट", color = C.Warn, style = MaterialTheme.typography.labelLarge)
            }

            Text(
                "Made with ❤️ by Team Orynex",
                style = MaterialTheme.typography.labelSmall, color = C.Muted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
            )
        }
    }


    if (askLogout) {
        LogoutDialog(
            onCancel = { askLogout = false },
            onConfirm = {
                askLogout = false
                prefs.userName = ""
                prefs.userPhone = ""
                prefs.onboarded = false
                vm.clearChat()
                onLogout()
            },
        )
    }
}

// ---------------------------------------------------------------- pieces

@Composable
private fun ProfileHero(name: String, phone: String) {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0B1F4B), Color(0xFF1E4FA3), Color(0xFF38A3E8))))
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 50.dp)
    ) {
        Column {
            Text("सेटिंग", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).padding(3.dp)
                        .clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(name.take(1).ifBlank { "स" }, color = C.Brand, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        if (name.isBlank()) "नमस्ते" else "नमस्ते, $name जी",
                        color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                    )
                    if (phone.isNotBlank()) Text("+91 $phone", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun Group(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = C.Brand.copy(alpha = 0.14f), ambientColor = C.Brand.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(18.dp),
        content = content,
    )
}

@Composable
private fun GroupTitle(t: String) {
    Text(t, color = C.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
}

@Composable
private fun Divider() = HorizontalDivider(color = C.Line, modifier = Modifier.padding(vertical = 12.dp))

@Composable
private fun IconBadge(icon: ImageVector, tint: Color = C.Brand) {
    Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(tint.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun StateChip(text: String, ok: Boolean) {
    Box(
        Modifier.clip(RoundedCornerShape(12.dp)).background(if (ok) C.OkSoft else C.WarnSoft)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) { Text(text, style = MaterialTheme.typography.labelSmall, color = if (ok) C.Ok else C.Warn) }
}

@Composable
private fun LangTile(l: Lang, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) C.Brand else Color(0xFFF1F5FA), tween(220), label = "bg")
    val fg by animateColorAsState(if (selected) Color.White else C.Ink, tween(220), label = "fg")
    Column(
        modifier.height(72.dp).clip(RoundedCornerShape(16.dp)).background(bg).bouncyClick(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(l.native, color = fg, style = MaterialTheme.typography.titleMedium)
        Text(l.label, color = fg.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PermRow(icon: ImageVector, title: String, sub: String, ok: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = C.Ink)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = C.Muted)
        }
        Spacer(Modifier.width(8.dp))
        if (ok) StateChip("चालू", true)
        else Box(
            Modifier.clip(RoundedCornerShape(16.dp)).background(C.Brand).bouncyClick(onClick = onFix)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        ) { Text("चालू करें", color = Color.White, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = C.Ink)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = C.Muted)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = on, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = C.Brand, uncheckedBorderColor = C.Line, uncheckedTrackColor = Color(0xFFEEF3F9)),
        )
    }
}

@Composable
private fun LogoutDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color.White, modifier = Modifier.enterStagger(0, rise = 24.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(C.WarnSoft),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.WarningAmber, null, tint = C.Warn, modifier = Modifier.size(32.dp)) }
                Spacer(Modifier.height(16.dp))
                Text("लॉग आउट करें?", color = C.Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "क्या आप सच में लॉग आउट करना चाहते हैं? आपका नाम, नंबर और बातचीत हट जाएगी। 'मेरे लोग' की सूची बनी रहेगी।",
                    style = MaterialTheme.typography.bodyMedium, color = C.Muted, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
                Box(
                    Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp))
                        .background(Brush.horizontalGradient(listOf(C.Brand, C.Accent)))
                        .bouncyClick(onClick = onCancel),
                    contentAlignment = Alignment.Center,
                ) { Text("नहीं, रुकिए", color = Color.White, style = MaterialTheme.typography.labelLarge) }
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(27.dp), color = Color.White, border = BorderStroke(1.dp, C.Line),
                    modifier = Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).bouncyClick(onClick = onConfirm),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("हाँ, लॉग आउट", color = C.Warn, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

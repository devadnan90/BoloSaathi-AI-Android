package com.orynex.bolosaathi.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orynex.bolosaathi.core.Extras
import com.orynex.bolosaathi.core.Store

private val HeroBrush = Brush.linearGradient(listOf(Color(0xFF0B1F4B), Color(0xFF1E4FA3), Color(0xFF38A3E8)))

// ---------------------------------------------------------------- home bits

/** Small red SOS button for the home header. */
@Composable
fun SosPill() {
    val vm: MainViewModel = viewModel()
    Box(
        Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFFE53935))
            .bouncyClick { vm.sos() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) { Text("SOS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}

/** One slim row on home that opens the medicine and expense page. */
@Composable
fun DiaryRow(modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = C.Brand.copy(alpha = 0.14f), ambientColor = C.Brand.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .bouncyClick(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF6A3FB5).copy(alpha = 0.8f), Color(0xFF6A3FB5)))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Medication, null, tint = Color.White) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("दवा और खर्च", style = MaterialTheme.typography.titleMedium, color = C.Ink)
            Text("याद और हिसाब, बस बोलकर", style = MaterialTheme.typography.bodySmall, color = C.Muted)
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = C.Muted, modifier = Modifier.size(18.dp))
    }
}

// ---------------------------------------------------------------- diary page

@Composable
fun DiaryPage(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val ctx = LocalContext.current
    val st = remember { Store(ctx) }
    var reminders by remember { mutableStateOf(st.reminders()) }
    var expenses by remember { mutableStateOf(st.expenses()) }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val today = expenses.filter { it.time >= Extras.dayStart(0) }.sumOf { it.amount }
    val month = expenses.filter { it.time >= Extras.monthStart() }.sumOf { it.amount }

    Column(
        Modifier.fillMaxSize().background(C.Bg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .enterStagger(0, rise = 40.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().background(HeroBrush).statusBarsPadding().padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "पीछे", tint = Color.White) }
            Text("दवा और खर्च", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        LazyColumn(
            Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { PageTitle("दवा की याद") }
            if (reminders.isEmpty()) item { Hint("माइक पर बोलिए: \"रोज़ सुबह 8 बजे BP की दवा याद दिलाना\"") }
            items(reminders, key = { it.id }) { r ->
                ItemRow(Icons.Outlined.Alarm, C.Brand, r.text, "रोज़ " + Extras.timeText(r.hour, r.minute)) {
                    Extras.cancel(ctx, r.id); st.removeReminder(r.id); reminders = st.reminders()
                }
            }
            item { PageTitle("खर्च", Modifier.padding(top = 10.dp)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("आज", today, true, Modifier.weight(1f))
                    StatTile("इस महीने", month, false, Modifier.weight(1f))
                }
            }
            if (expenses.isEmpty()) item { Hint("बोलिए: \"आज 200 रुपये की सब्ज़ी ली\"") }
            items(expenses.sortedByDescending { it.time }.take(30), key = { it.time }) { e ->
                ItemRow(
                    Icons.Outlined.CurrencyRupee, Color(0xFFB26A00), "₹${e.amount}  ·  ${e.note}",
                    DateUtils.getRelativeTimeSpanString(e.time).toString(),
                ) { st.removeExpense(e.time); expenses = st.expenses() }
            }
        }
    }
}

@Composable
private fun PageTitle(t: String, modifier: Modifier = Modifier) {
    Text(t, color = C.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

@Composable
private fun Hint(t: String) {
    Text(
        t, style = MaterialTheme.typography.bodyMedium, color = C.Brand,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.BrandSoft).padding(16.dp),
    )
}

@Composable
private fun StatTile(label: String, value: Int, strong: Boolean, modifier: Modifier) {
    Column(
        modifier.height(92.dp)
            .shadow(if (strong) 12.dp else 6.dp, RoundedCornerShape(20.dp), spotColor = C.Brand.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(20.dp))
            .background(if (strong) HeroBrush else Brush.linearGradient(listOf(Color.White, Color.White)))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (strong) Color.White.copy(alpha = 0.85f) else C.Muted)
        Text("₹$value", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = if (strong) Color.White else C.Ink)
    }
}

@Composable
private fun ItemRow(icon: ImageVector, tint: Color, title: String, sub: String, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp), spotColor = C.Brand.copy(alpha = 0.10f), ambientColor = C.Brand.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = C.Ink, maxLines = 1)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = C.Muted)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "हटाएँ", tint = C.Muted) }
    }
}

// ---------------------------------------------------------------- settings card

@Composable
fun FeaturesGroup(modifier: Modifier) {
    val ctx = LocalContext.current
    val st = remember { Store(ctx) }
    var guardian by remember { mutableStateOf(st.guardian) }
    var brief by remember { mutableStateOf(st.briefOn) }
    var notifs by remember { mutableStateOf(st.readNotifs) }
    var calls by remember { mutableStateOf(st.callWarn) }
    var bigPay by remember { mutableStateOf(st.bigPayAlert) }
    val perms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    fun ask(vararg p: String) {
        val need = p.filter { ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED }
        if (need.isNotEmpty()) perms.launch(need.toTypedArray())
    }
    val notifPerm = if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray()

    Column(
        modifier.fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = C.Brand.copy(alpha = 0.14f), ambientColor = C.Brand.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(18.dp)
    ) {
        Text("सुविधाएँ", color = C.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
        TextField(
            value = guardian,
            onValueChange = { v ->
                guardian = v.filter { it.isDigit() }.take(10)
                st.guardian = guardian
                if (guardian.length == 10) ask(
                    Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE,
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            },
            label = { Text("परिवार का नंबर") },
            prefix = { Text("+91  ", color = C.Muted) },
            leadingIcon = { Icon(Icons.Outlined.Groups, null, tint = C.Brand) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF1F5FA), unfocusedContainerColor = Color(0xFFF1F5FA),
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                cursorColor = C.Brand, focusedLabelColor = C.Brand,
            ),
        )
        Text(
            "SOS और बड़े पेमेंट की ख़बर इसी नंबर पर जाएगी",
            style = MaterialTheme.typography.bodySmall, color = C.Muted, modifier = Modifier.padding(top = 6.dp, start = 4.dp),
        )
        HorizontalDivider(color = C.Line, modifier = Modifier.padding(vertical = 12.dp))
        FeatureToggle(Icons.Outlined.WbSunny, "सुबह का हाल", "रोज़ 8 बजे मौसम, दवा और खर्च", brief) {
            brief = it; st.briefOn = it; Extras.scheduleBrief(ctx)
            if (it) ask(*notifPerm, Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        HorizontalDivider(color = C.Line, modifier = Modifier.padding(vertical = 12.dp))
        FeatureToggle(Icons.Outlined.NotificationsActive, "मैसेज पढ़कर सुनाओ", "व्हाट्सऐप और SMS आते ही", notifs) {
            notifs = it; st.readNotifs = it
            if (it && !NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)) {
                ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        HorizontalDivider(color = C.Line, modifier = Modifier.padding(vertical = 12.dp))
        FeatureToggle(Icons.Outlined.Call, "कॉल के बाद चेतावनी", "अनजान नंबर से बात के बाद सावधान करे", calls) {
            calls = it; st.callWarn = it
            if (it) ask(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS, *notifPerm)
        }
        HorizontalDivider(color = C.Line, modifier = Modifier.padding(vertical = 12.dp))
        FeatureToggle(Icons.Outlined.Payments, "बड़े पेमेंट पर परिवार को ख़बर", "₹2000 या ज़्यादा भेजने पर SMS", bigPay) {
            bigPay = it; st.bigPayAlert = it
            if (it) ask(Manifest.permission.SEND_SMS)
        }
    }
}

@Composable
private fun FeatureToggle(icon: ImageVector, title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(C.Brand.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = C.Brand, modifier = Modifier.size(22.dp))
        }
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

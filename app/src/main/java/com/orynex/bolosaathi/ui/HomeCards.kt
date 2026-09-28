package com.orynex.bolosaathi.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val examples = listOf(
    "मम्मी को 500 रुपये भेज दो" to Icons.Outlined.CurrencyRupee,
    "299 वाला रिचार्ज कर दो" to Icons.Outlined.PhoneAndroid,
    "बेटा को लिखो, मैं पहुँच गया" to Icons.AutoMirrored.Outlined.Chat,
    "यूट्यूब पर भजन चला दो" to Icons.Outlined.PlayCircle,
    "ये मैसेज सच है या धोखा?" to Icons.Outlined.Shield,
)

/** Everything shown on home before the first conversation. */
@Composable
fun HomeIdle(
    onMic: () -> Unit,
    onCamera: () -> Unit,
    onYojana: () -> Unit,
    onScam: () -> Unit,
    onPick: (String) -> Unit,
    onDiary: () -> Unit = {},
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 22.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp).enterStagger(0)) {
                Text("आज क्या करना है?", color = C.Ink, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Text("बोलिए, या नीचे से चुनिए", style = MaterialTheme.typography.bodyMedium, color = C.Muted)
            }
        }
        item {
            FeatureCard(Modifier.padding(horizontal = 20.dp).enterStagger(1), onMic)
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniCard("पढ़ के सुनाओ", "बिल, चिट्ठी", Icons.Outlined.CameraAlt, Color(0xFF0E8A7A), Modifier.weight(1f).enterStagger(2), onCamera)
                MiniCard("योजना साथी", "सरकारी मदद", Icons.Outlined.AccountBalance, Color(0xFFB26A00), Modifier.weight(1f).enterStagger(3), onYojana)
                MiniCard("धोखा जाँच", "सच या झूठ", Icons.Outlined.Shield, C.Warn, Modifier.weight(1f).enterStagger(4), onScam)
            }
        }
        item { DiaryRow(Modifier.padding(horizontal = 20.dp).enterStagger(5), onDiary) }
        item {
            Text(
                "बोलकर देखिए", color = C.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, top = 4.dp).enterStagger(5),
            )
        }
        item {
            LazyRow(
                modifier = Modifier.enterStagger(6),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(examples) { _, (text, icon) ->
                    SuggestionCard(text, icon) { onPick(text) }
                }
            }
        }
    }
}

/** Big main card: speak and get things done. */
@Composable
fun FeatureCard(modifier: Modifier, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "fc")
    val pulse by t.animateFloat(
        0.1f, 0.7f,
        infiniteRepeatable(tween(1300, easing = Motion.EaseInOut), RepeatMode.Reverse),
        label = "p",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(156.dp)
            .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = C.Brand.copy(alpha = 0.45f), ambientColor = C.Brand.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0B1F4B), Color(0xFF1E4FA3), Color(0xFF38A3E8))))
            .bouncyClick(pressedScale = 0.97f, onClick = onClick)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.07f), radius = size.height * 0.95f, center = Offset(size.width * 0.98f, size.height * 0.05f))
            drawCircle(Color.White.copy(alpha = 0.05f), radius = size.height * 0.55f, center = Offset(size.width * 0.62f, size.height * 1.1f))
        }
        Row(Modifier.fillMaxSize().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("सबसे आसान तरीका", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
                Spacer(Modifier.height(2.dp))
                Text("बोल के काम", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                Text("पैसे भेजना, रिचार्ज, संदेश", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.88f))
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.18f))
                        .padding(start = 12.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("दबाइए और बोलिए", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                MicHalo(listening = true, level = pulse, color = Color.White, modifier = Modifier.fillMaxSize())
                Box(
                    Modifier.size(62.dp).shadow(10.dp, CircleShape).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Mic, null, tint = C.Brand, modifier = Modifier.size(30.dp))
                }
            }
        }
    }
}

/** Small feature card with soft shadow instead of a border. */
@Composable
fun MiniCard(title: String, sub: String, icon: ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(142.dp)
            .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = C.Brand.copy(alpha = 0.16f), ambientColor = C.Brand.copy(alpha = 0.10f))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .bouncyClick(onClick = onClick)
    ) {
        Box(
            Modifier.fillMaxWidth().height(80.dp)
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.10f), Color.Transparent)))
        )
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                Modifier.size(46.dp)
                    .shadow(8.dp, RoundedCornerShape(15.dp), spotColor = accent.copy(alpha = 0.6f))
                    .clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.78f), accent))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(title, color = C.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, maxLines = 2)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = C.Muted, maxLines = 1)
            }
        }
    }
}

/** Swipeable example the user can tap instead of speaking. */
@Composable
private fun SuggestionCard(text: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        Modifier
            .width(196.dp)
            .height(112.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = C.Brand.copy(alpha = 0.14f), ambientColor = C.Brand.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .bouncyClick(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(C.BrandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = C.Brand, modifier = Modifier.size(18.dp))
        }
        Text(text, color = C.Ink, fontSize = 15.sp, lineHeight = 20.sp, maxLines = 2)
    }
}

package com.orynex.bolosaathi.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/** Full screen page drawn on top of the whole app. page = "privacy" | "terms" | "data" */
@Composable
fun LegalViewer(page: String, title: String, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F9FC))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .enterStagger(0, rise = 40.dp)
    ) {
        Row(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF0B1F4B), Color(0xFF1E4FA3), Color(0xFF38A3E8))))
                .statusBarsPadding()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "पीछे", tint = Color.White)
            }
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        AndroidView(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = false
                    setBackgroundColor(android.graphics.Color.parseColor("#F6F9FC"))
                    loadUrl("file:///android_asset/legal/$page.html")
                }
            },
        )
    }
}

/** One tappable row in the Settings "जानकारी" group. */
@Composable
fun LinkRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).bouncyClick(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(C.Brand.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = C.Brand, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = C.Ink)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = C.Muted)
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = C.Muted, modifier = Modifier.size(18.dp))
    }
}

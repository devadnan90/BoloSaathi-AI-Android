package com.orynex.bolosaathi.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object C {
    val Brand = Color(0xFF123A7A)
    val BrandSoft = Color(0xFFE6F0FB)
    val Accent = Color(0xFF2B9BE5)
    val AccentDeep = Color(0xFF1A6FC4)
    val AccentSoft = Color(0xFFE3F3FD)
    val Bg = Color(0xFFF6F9FC)
    val Surface = Color.White
    val Line = Color(0xFFDCE5EF)
    val Ink = Color(0xFF0E1726)
    val Muted = Color(0xFF4A5668)
    val Ok = Color(0xFF1E7D4F)
    val OkSoft = Color(0xFFE6F4EC)
    val Warn = Color(0xFFB71C1C)
    val WarnSoft = Color(0xFFFDECEC)
    val Amber = Color(0xFFF2A33A)
    val AmberSoft = Color(0xFFFFF4E0)
}

private val colors = lightColorScheme(
    primary = C.Brand,
    onPrimary = Color.White,
    primaryContainer = C.BrandSoft,
    onPrimaryContainer = C.Brand,
    secondary = C.AccentDeep,
    onSecondary = Color.White,
    background = C.Bg,
    onBackground = C.Ink,
    surface = C.Surface,
    onSurface = C.Ink,
    surfaceVariant = Color(0xFFEEF3F9),
    onSurfaceVariant = C.Muted,
    outline = C.Line,
    outlineVariant = C.Line,
    error = C.Warn,
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color(0xFFF0F5FA),
)

private val type = Typography(
    headlineSmall = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, lineHeight = 34.sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    bodyLarge = TextStyle(fontSize = 19.sp, lineHeight = 29.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun BoloTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = type, content = content)
}

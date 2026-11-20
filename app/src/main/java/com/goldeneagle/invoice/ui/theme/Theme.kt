package com.goldeneagle.invoice.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp

val OliveGold = Color(0xFF87743B)
val OliveGoldDark = Color(0xFF635325)
val OliveGoldLight = Color(0xFFF3EFE4)
val WarmGold = Color(0xFFC5A059)
val PaperBg = Color(0xFFFBF9F4)
val CardWhite = Color(0xFFFFFFFF)
val InkDark = Color(0xFF1D1810)
val InkMuted = Color(0xFF645D4E)
val BorderSoft = Color(0xFFE4DEC9)
val AccentDanger = Color(0xFFB3261E)
val AccentSuccess = Color(0xFF2E7D32)

private val GoldenEagleColors = lightColorScheme(
    primary = OliveGold,
    onPrimary = Color.White,
    primaryContainer = OliveGoldLight,
    onPrimaryContainer = OliveGoldDark,
    secondary = WarmGold,
    onSecondary = InkDark,
    secondaryContainer = Color(0xFFF7F0DC),
    onSecondaryContainer = OliveGoldDark,
    background = PaperBg,
    onBackground = InkDark,
    surface = CardWhite,
    onSurface = InkDark,
    surfaceVariant = Color(0xFFF2EDE0),
    onSurfaceVariant = InkMuted,
    outline = BorderSoft,
    error = AccentDanger,
    onError = Color.White
)

private val AppTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)

@Composable
fun GoldenEagleTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = GoldenEagleColors,
            typography = AppTypography,
            content = content
        )
    }
}

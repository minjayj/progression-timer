package com.example.progressiontimer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val AppColorScheme = lightColorScheme(
    primary = Color(0xFF1F6F61),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFE08E45),
    onSecondary = Color(0xFF241408),
    tertiary = Color(0xFF315D86),
    background = Color(0xFFF6F0DF),
    onBackground = Color(0xFF1E2019),
    surface = Color(0xFFFFF8E8),
    onSurface = Color(0xFF1E2019),
    surfaceVariant = Color(0xFFE7DEC9),
    onSurfaceVariant = Color(0xFF595243),
    outline = Color(0xFF8B8069),
)

private val AppTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
        ),
        headlineMedium = base.headlineMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
        ),
        titleLarge = base.titleLarge.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
        ),
    )
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(32.dp),
)

@Composable
fun ProgressionTimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

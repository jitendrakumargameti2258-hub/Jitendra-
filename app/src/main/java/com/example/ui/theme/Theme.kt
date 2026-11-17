package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = SafetyTealDarkPrimary,
    onPrimary = Color(0xFF04212E),
    primaryContainer = SafetyTealDarkContainer,
    onPrimaryContainer = Color(0xFFBBE6FA),
    secondary = SafetyAmberDark,
    onSecondary = Color(0xFF2D1600),
    secondaryContainer = SafetyAmberDarkContainer,
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = ComplianceEmerald,
    onTertiary = Color.White,
    background = DarkCharcoalBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

private val LightColorScheme = lightColorScheme(
    primary = SafetyTeal,
    onPrimary = Color.White,
    primaryContainer = SafetyTealContainer,
    onPrimaryContainer = OnSafetyTealContainer,
    secondary = SafetyAmber,
    onSecondary = Color.White,
    secondaryContainer = SafetyAmberContainer,
    onSecondaryContainer = OnSafetyAmberContainer,
    tertiary = ComplianceEmerald,
    onTertiary = Color.White,
    background = ConcreteBg,
    onBackground = SlateInk,
    surface = CardWhite,
    onSurface = SlateInk,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = SlateOutline,
    error = CriticalCrimson,
    onError = Color.White
)

val EhsShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SiteGuardEhsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = EhsShapes,
        content = content
    )
}

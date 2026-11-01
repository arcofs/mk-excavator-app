package io.github.arcofs.mkexcavator.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Always dark: easier on the eyes outdoors and it keeps the controls the brightest thing on screen.
private val ColorScheme =
  darkColorScheme(
    primary = SafetyYellow,
    onPrimary = OnSafetyYellow,
    primaryContainer = Color(0xFF3B2F00),
    onPrimaryContainer = Color(0xFFFFE08A),
    inversePrimary = SafetyYellowDeep,
    secondary = SteelLight,
    onSecondary = Graphite,
    secondaryContainer = GraphiteHighest,
    onSecondaryContainer = Concrete,
    tertiary = GoGreen,
    onTertiary = Graphite,
    error = SignalRedLight,
    onError = Color(0xFF3B0603),
    errorContainer = SignalRedDeep,
    onErrorContainer = Color(0xFFFFDAD5),
    background = Graphite,
    onBackground = Concrete,
    surface = Graphite,
    onSurface = Concrete,
    surfaceVariant = GraphiteHigh,
    onSurfaceVariant = SteelLight,
    surfaceTint = SafetyYellow,
    outline = Steel,
    outlineVariant = GraphiteHighest,
    inverseSurface = Concrete,
    inverseOnSurface = GraphiteRaised,
    surfaceBright = GraphiteHighest,
    surfaceDim = Graphite,
    surfaceContainerLowest = Color(0xFF0A0C0E),
    surfaceContainerLow = GraphiteRaised,
    surfaceContainer = GraphiteLow,
    surfaceContainerHigh = GraphiteMid,
    surfaceContainerHighest = GraphiteHigh,
    scrim = Color.Black,
  )

private val AppShapes =
  Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
  )

@Composable
fun MKExcavatorTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = ColorScheme, typography = Typography, shapes = AppShapes, content = content)
}

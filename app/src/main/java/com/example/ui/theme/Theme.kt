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

private val LightColorScheme = lightColorScheme(
  primary = AlaapBlue,
  onPrimary = Color.White,
  primaryContainer = AlaapBlueLight,
  onPrimaryContainer = AlaapBlueDark,
  secondary = AlaapGreen,
  onSecondary = Color.White,
  secondaryContainer = AlaapGreenLight,
  onSecondaryContainer = AlaapGreen,
  tertiary = AlaapPurple,
  onTertiary = Color.White,
  tertiaryContainer = AlaapPurpleLight,
  onTertiaryContainer = AlaapPurple,
  background = CanvasLight,
  onBackground = TextPrimaryLight,
  surface = CardLight,
  onSurface = TextPrimaryLight,
  surfaceVariant = Color(0xFFEDF0F7),
  onSurfaceVariant = TextSecondaryLight,
  outline = OutlineLight,
  error = AlaapRed,
  onError = Color.White,
  errorContainer = AlaapRedLight,
  onErrorContainer = AlaapRed,
)

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF6BA1FF),
  onPrimary = Color(0xFF002D6F),
  primaryContainer = Color(0xFF143C85),
  onPrimaryContainer = Color(0xFFD8E5FF),
  secondary = Color(0xFF4ADE80),
  onSecondary = Color(0xFF052E16),
  secondaryContainer = Color(0xFF14532D),
  onSecondaryContainer = Color(0xFFDCFCE7),
  tertiary = Color(0xFFC4B5FD),
  onTertiary = Color(0xFF2E1065),
  background = CanvasDark,
  onBackground = TextPrimaryDark,
  surface = CardDark,
  onSurface = TextPrimaryDark,
  surfaceVariant = Color(0xFF242E42),
  onSurfaceVariant = TextSecondaryDark,
  outline = OutlineDark,
  error = Color(0xFFF87171),
  onError = Color(0xFF450A0A),
)

val AlaapShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(24.dp),
)

@Composable
fun AlaapTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = AlaapShapes,
    content = content,
  )
}

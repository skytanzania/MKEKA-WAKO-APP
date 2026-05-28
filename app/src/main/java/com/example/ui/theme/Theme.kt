package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = LightBluePrimary,
    secondary = DarkTextMuted,
    tertiary = LightBluePrimary,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkBackground,
    onSecondary = DarkTextSlate,
    onBackground = DarkTextSlate,
    onSurface = DarkTextSlate,
    primaryContainer = MinimalBluePrimary,
    onPrimaryContainer = DarkTextSlate
  )

private val LightColorScheme =
  lightColorScheme(
    primary = MinimalBluePrimary,
    secondary = MinimalTextMuted,
    tertiary = MinimalBluePrimary,
    background = MinimalBackground,
    surface = MinimalSurface,
    onPrimary = Color.White,
    onSecondary = MinimalTextSlate,
    onBackground = MinimalTextSlate,
    onSurface = MinimalTextSlate,
    primaryContainer = MinimalBlueContainer,
    onPrimaryContainer = MinimalBluePrimary
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Force clean minimalist palette
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

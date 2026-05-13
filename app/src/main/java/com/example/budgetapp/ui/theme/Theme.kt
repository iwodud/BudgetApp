package com.example.budgetapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF00695C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    error = Color(0xFFC62828)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003069),
    primaryContainer = Color(0xFF004BA0),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF004D43),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE6E1E5),
    error = Color(0xFFEF9A9A)
)

@Composable
fun BudgetAppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

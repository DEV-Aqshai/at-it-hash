package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PlayerAtBlue,
    secondary = PlayerHashRed,
    background = NotebookPaperDark,
    surface = NotebookPaperDark,
    onPrimary = ChalkWhite,
    onSecondary = ChalkWhite,
    onBackground = ChalkWhite,
    onSurface = ChalkWhite
)

private val LightColorScheme = lightColorScheme(
    primary = PlayerAtBlue,
    secondary = PlayerHashRed,
    background = NotebookPaper,
    surface = NotebookPaper,
    onPrimary = ChalkWhite,
    onSecondary = ChalkWhite,
    onBackground = GraphitePencil,
    onSurface = GraphitePencil
)

@Composable
fun AtItHashTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NotebookTypography,
        content = content
    )
}

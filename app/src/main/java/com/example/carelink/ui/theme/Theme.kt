package com.example.carelink.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CareLinkColorScheme = lightColorScheme(
    primary = CareLinkPrimaryAction,
    onPrimary = CareLinkTextOnDark,

    secondary = CareLinkPrompt,
    onSecondary = CareLinkTextOnDark,

    tertiary = CareLinkSuccess,
    onTertiary = CareLinkTextOnDark,

    background = CareLinkBackground,
    onBackground = CareLinkTextPrimary,

    surface = CareLinkSurface,
    onSurface = CareLinkTextPrimary,

    error = CareLinkError,
    onError = CareLinkTextOnDark
)

@Composable
fun CareLinkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CareLinkColorScheme,
        typography = Typography,
        content = content
    )
}

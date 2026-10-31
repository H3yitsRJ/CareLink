package com.example.carelink.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Semantic colors let screens ask for an action, error, or surface color by purpose.
private val CareLinkColorScheme = lightColorScheme(
    primary = CareLinkPrimaryAction,
    onPrimary = CareLinkTextOnDark,
    primaryContainer = CareLinkActionContainer,
    onPrimaryContainer = CareLinkTextPrimary,

    secondary = CareLinkPrompt,
    onSecondary = CareLinkTextOnDark,
    secondaryContainer = CareLinkSurfaceMuted,
    onSecondaryContainer = CareLinkTextPrimary,

    tertiary = CareLinkSuccess,
    onTertiary = CareLinkTextOnDark,
    tertiaryContainer = CareLinkSuccessContainer,
    onTertiaryContainer = CareLinkTextPrimary,

    background = CareLinkBackground,
    onBackground = CareLinkTextPrimary,

    surface = CareLinkSurface,
    onSurface = CareLinkTextPrimary,
    surfaceVariant = CareLinkSurfaceMuted,
    onSurfaceVariant = CareLinkPrompt,
    surfaceTint = CareLinkPrimaryAction,
    outline = CareLinkPrompt,
    outlineVariant = CareLinkOutlineMuted,
    surfaceBright = CareLinkSurface,
    surfaceDim = CareLinkSurfaceMuted,
    surfaceContainerLowest = CareLinkSurface,
    surfaceContainerLow = CareLinkSurface,
    surfaceContainer = CareLinkSurface,
    surfaceContainerHigh = CareLinkSurface,
    surfaceContainerHighest = CareLinkSurfaceMuted,
    inverseSurface = CareLinkTextPrimary,
    inverseOnSurface = CareLinkTextOnDark,
    inversePrimary = CareLinkActionContainer,

    error = CareLinkError,
    onError = CareLinkTextOnDark,
    errorContainer = CareLinkErrorContainer,
    onErrorContainer = CareLinkError
)

@Composable
// Every screen enters through one theme so typography and colors stay consistent.
fun CareLinkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CareLinkColorScheme,
        typography = Typography,
        content = content
    )
}

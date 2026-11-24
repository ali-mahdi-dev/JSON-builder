package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

enum class ThemePreference {
    SYSTEM, LIGHT, DARK
}

val LocalJsonSyntaxColors = staticCompositionLocalOf { LightJsonSyntaxColors }

private val DarkColorScheme = darkColorScheme(
    primary = StudioDarkPrimary,
    onPrimary = StudioDarkOnPrimary,
    primaryContainer = StudioDarkPrimaryContainer,
    onPrimaryContainer = StudioDarkOnPrimaryContainer,
    secondary = StudioDarkSecondary,
    onSecondary = StudioDarkOnSecondary,
    secondaryContainer = StudioDarkSecondaryContainer,
    onSecondaryContainer = StudioDarkOnSecondaryContainer,
    tertiary = StudioDarkTertiary,
    onTertiary = StudioDarkOnTertiary,
    tertiaryContainer = StudioDarkTertiaryContainer,
    onTertiaryContainer = StudioDarkOnTertiaryContainer,
    background = StudioDarkBackground,
    onBackground = StudioDarkOnBackground,
    surface = StudioDarkSurface,
    onSurface = StudioDarkOnSurface,
    surfaceVariant = StudioDarkSurfaceVariant,
    onSurfaceVariant = StudioDarkOnSurfaceVariant,
    outline = StudioDarkOutline,
    outlineVariant = StudioDarkOutlineVariant,
    error = StudioDarkError,
    onError = StudioDarkOnError,
    errorContainer = StudioDarkErrorContainer,
    onErrorContainer = StudioDarkOnErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = StudioLightPrimary,
    onPrimary = StudioLightOnPrimary,
    primaryContainer = StudioLightPrimaryContainer,
    onPrimaryContainer = StudioLightOnPrimaryContainer,
    secondary = StudioLightSecondary,
    onSecondary = StudioLightOnSecondary,
    secondaryContainer = StudioLightSecondaryContainer,
    onSecondaryContainer = StudioLightOnSecondaryContainer,
    tertiary = StudioLightTertiary,
    onTertiary = StudioLightOnTertiary,
    tertiaryContainer = StudioLightTertiaryContainer,
    onTertiaryContainer = StudioLightOnTertiaryContainer,
    background = StudioLightBackground,
    onBackground = StudioLightOnBackground,
    surface = StudioLightSurface,
    onSurface = StudioLightOnSurface,
    surfaceVariant = StudioLightSurfaceVariant,
    onSurfaceVariant = StudioLightOnSurfaceVariant,
    outline = StudioLightOutline,
    outlineVariant = StudioLightOutlineVariant,
    error = StudioLightError,
    onError = StudioLightOnError,
    errorContainer = StudioLightErrorContainer,
    onErrorContainer = StudioLightOnErrorContainer
)

private val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun JsonBuilderTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val syntaxColors = if (isDark) DarkJsonSyntaxColors else LightJsonSyntaxColors

    CompositionLocalProvider(LocalJsonSyntaxColors provides syntaxColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = StudioShapes,
            content = content
        )
    }
}

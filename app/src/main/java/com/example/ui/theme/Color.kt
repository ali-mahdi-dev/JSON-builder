package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Precision Studio Light Palette
val StudioLightPrimary = Color(0xFF0284C7)
val StudioLightOnPrimary = Color(0xFFFFFFFF)
val StudioLightPrimaryContainer = Color(0xFFE0F2FE)
val StudioLightOnPrimaryContainer = Color(0xFF0C4A6E)

val StudioLightSecondary = Color(0xFF0D9488)
val StudioLightOnSecondary = Color(0xFFFFFFFF)
val StudioLightSecondaryContainer = Color(0xFFCCFBF1)
val StudioLightOnSecondaryContainer = Color(0xFF134E4A)

val StudioLightTertiary = Color(0xFF6366F1)
val StudioLightOnTertiary = Color(0xFFFFFFFF)
val StudioLightTertiaryContainer = Color(0xFFE0E7FF)
val StudioLightOnTertiaryContainer = Color(0xFF312E81)

val StudioLightBackground = Color(0xFFF6F8FB)
val StudioLightOnBackground = Color(0xFF0F172A)
val StudioLightSurface = Color(0xFFFFFFFF)
val StudioLightOnSurface = Color(0xFF0F172A)
val StudioLightSurfaceVariant = Color(0xFFE8EEF5)
val StudioLightOnSurfaceVariant = Color(0xFF475569)
val StudioLightOutline = Color(0xFFCBD5E1)
val StudioLightOutlineVariant = Color(0xFFE2E8F0)

val StudioLightError = Color(0xFFDC2626)
val StudioLightOnError = Color(0xFFFFFFFF)
val StudioLightErrorContainer = Color(0xFFFEE2E2)
val StudioLightOnErrorContainer = Color(0xFF7F1D1D)

// Precision Studio Dark Palette
val StudioDarkPrimary = Color(0xFF38BDF8)
val StudioDarkOnPrimary = Color(0xFF082F49)
val StudioDarkPrimaryContainer = Color(0xFF0C4A6E)
val StudioDarkOnPrimaryContainer = Color(0xFFE0F2FE)

val StudioDarkSecondary = Color(0xFF2DD4BF)
val StudioDarkOnSecondary = Color(0xFF042F2E)
val StudioDarkSecondaryContainer = Color(0xFF115E59)
val StudioDarkOnSecondaryContainer = Color(0xFFCCFBF1)

val StudioDarkTertiary = Color(0xFFA5B4FC)
val StudioDarkOnTertiary = Color(0xFF1E1B4B)
val StudioDarkTertiaryContainer = Color(0xFF312E81)
val StudioDarkOnTertiaryContainer = Color(0xFFE0E7FF)

val StudioDarkBackground = Color(0xFF0B1120)
val StudioDarkOnBackground = Color(0xFFF1F5F9)
val StudioDarkSurface = Color(0xFF111827)
val StudioDarkOnSurface = Color(0xFFF1F5F9)
val StudioDarkSurfaceVariant = Color(0xFF1E293B)
val StudioDarkOnSurfaceVariant = Color(0xFF94A3B8)
val StudioDarkOutline = Color(0xFF334155)
val StudioDarkOutlineVariant = Color(0xFF1E293B)

val StudioDarkError = Color(0xFFF87171)
val StudioDarkOnError = Color(0xFF450A0A)
val StudioDarkErrorContainer = Color(0xFF7F1D1D)
val StudioDarkOnErrorContainer = Color(0xFFFEE2E2)

// Semantic JSON Syntax Highlighting & Type Badge Colors
data class JsonSyntaxColors(
    val key: Color,
    val stringValue: Color,
    val numberValue: Color,
    val booleanValue: Color,
    val nullValue: Color,
    val punctuation: Color,
    val codeBackground: Color,
    val lineNumber: Color,
    val validBadge: Color,
    val warningBadge: Color
)

val LightJsonSyntaxColors = JsonSyntaxColors(
    key = Color(0xFF0369A1),
    stringValue = Color(0xFF047857),
    numberValue = Color(0xFFB45309),
    booleanValue = Color(0xFF6D28D9),
    nullValue = Color(0xFFBE123C),
    punctuation = Color(0xFF64748B),
    codeBackground = Color(0xFFF1F5F9),
    lineNumber = Color(0xFF94A3B8),
    validBadge = Color(0xFF059669),
    warningBadge = Color(0xFFD97706)
)

val DarkJsonSyntaxColors = JsonSyntaxColors(
    key = Color(0xFF38BDF8),
    stringValue = Color(0xFF34D399),
    numberValue = Color(0xFFFBBF24),
    booleanValue = Color(0xFFC084FC),
    nullValue = Color(0xFFFB7185),
    punctuation = Color(0xFF94A3B8),
    codeBackground = Color(0xFF090E1A),
    lineNumber = Color(0xFF475569),
    validBadge = Color(0xFF10B981),
    warningBadge = Color(0xFFF59E0B)
)

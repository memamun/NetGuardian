package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Centralized Material 3 Design System Colors
// ==========================================

// Primary Brand Palette (High-contrast Sapphire & Oceanic Indigo)
val SleekPrimary = Color(0xFF0061A4)
val SleekPrimaryContainer = Color(0xFFD1E4FF)
val SleekOnPrimaryContainer = Color(0xFF001D36)
val SleekPrimaryText = Color(0xFF001D36)
val SleekBodyText = Color(0xFF1A1C1E)
val SleekMutedText = Color(0xFF44474E)

// Light Surfaces & Outlines (M3 Tone-based Containers)
val SleekBackground = Color(0xFFFDFBFF)
val SleekSurface = Color(0xFFFFFFFF)
val SleekSurfaceVariant = Color(0xFFF0F4F8)
val SleekCardBackground = Color(0xFFF0F4F8)
val SleekBorder = Color(0xFFC2C7CF)
val SleekDivider = Color(0x80C2C7CF)

val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF7F9FC)
val LightSurfaceContainer = Color(0xFFF0F4F8)
val LightSurfaceContainerHigh = Color(0xFFE8EEF4)
val LightSurfaceContainerHighest = Color(0xFFDFE6EF)

// Dark Theme Surfaces & Outlines (Slate Obsidian & Deep Navy)
val DarkBackground = Color(0xFF090E17)
val DarkSurface = Color(0xFF111927)
val DarkSurfaceVariant = Color(0xFF1A2436)
val DarkSurfaceElevated = Color(0xFF1E2B3E)
val DarkCardBackground = Color(0xFF141D2C)
val DarkBorder = Color(0xFF2A394E)
val DarkDivider = Color(0xFF223044)

val DarkSurfaceContainerLowest = Color(0xFF070C13)
val DarkSurfaceContainerLow = Color(0xFF0E1522)
val DarkSurfaceContainer = Color(0xFF141D2C)
val DarkSurfaceContainerHigh = Color(0xFF1A2536)
val DarkSurfaceContainerHighest = Color(0xFF223044)

// Scrim (32% opacity per M3 specification)
val M3Scrim = Color(0x52000000)

// Dark Theme Primary & Secondary
val DarkPrimary = Color(0xFF70B6FF)
val DarkOnPrimary = Color(0xFF003058)
val DarkPrimaryContainer = Color(0xFF0D3B66)
val DarkOnPrimaryContainer = Color(0xFFCEE5FF)

val DarkSecondary = Color(0xFF8BB5E8)
val DarkOnSecondary = Color(0xFF00305F)
val DarkSecondaryContainer = Color(0xFF1E3A5F)
val DarkOnSecondaryContainer = Color(0xFFD6E4FF)

// Dark Theme Text Colors
val DarkTextPrimary = Color(0xFFE6EDF5)
val DarkTextSecondary = Color(0xFFCBD5E1)
val DarkTextMuted = Color(0xFF94A3B8)

// Semantic Security & Protection Status Tokens
// 1. Safe / Active / Allowed
val LightSafeGreen = Color(0xFF0E8A54)
val LightSuccessContainer = Color(0xFFD1FAE5)
val LightOnSuccessContainer = Color(0xFF065F46)

val DarkSafeGreen = Color(0xFF34D399)
val DarkSuccessContainer = Color(0xFF093E23)
val DarkOnSuccessContainer = Color(0xFFA7F3D0)

// 2. Blocked / Error / Attention
val SleekBlockedRed = Color(0xFFBA1A1A)
val SleekBlockedContainer = Color(0xFFFFDAD6)
val SleekOnBlockedContainer = Color(0xFF410002)

val DarkBlockedRed = Color(0xFFFF6B6B)
val DarkBlockedContainer = Color(0xFF4A1015)
val DarkOnBlockedContainer = Color(0xFFFFDAD6)

// 3. Allowed Blue (Network Active)
val SleekAllowedBlue = Color(0xFF0061A4)
val SleekAllowedContainer = Color(0xFFD1E4FF)
val SleekAllowedGreen = LightSafeGreen

val DarkAllowedContainer = Color(0xFF0E385E)
val DarkOnAllowedContainer = Color(0xFFBFDEFF)

// 4. Warning / Attention Needed
val StatusWarningAmber = Color(0xFFD97706)
val LightWarningContainer = Color(0xFFFEF3C7)
val LightOnWarningContainer = Color(0xFF78350F)

val DarkWarningAmber = Color(0xFFFBBF24)
val DarkWarningContainer = Color(0xFF4D2A00)
val DarkOnWarningContainer = Color(0xFFFEF08A)

// Compatibility & Aliases
val DarkSleekBg = DarkBackground
val DarkSleekSurface = DarkSurface
val DarkSleekCard = DarkSurfaceVariant
val DarkSleekBorder = DarkBorder
val DarkSleekPrimary = DarkPrimary
val DarkSleekPrimaryContainer = DarkPrimaryContainer

val CyanElectric = SleekPrimary
val CyanGlow = SleekPrimaryContainer
val CyanDeep = Color(0xFF004880)

val IndigoSoft = Color(0xFF9ECAFF)
val IndigoDeep = SleekPrimary
val IndigoMuted = Color(0xFF00325B)

val StatusSafeGreen = LightSafeGreen
val StatusBlockedRed = SleekBlockedRed
val SleekInactiveContainer = Color(0xFFEEF2F6)




// My design decision (not in M3): brand-specific control outlines, distinct from
// the lower-contrast card/divider palette.
val LightControlOutline = Color(0xFF74777F)
val DarkControlOutline = Color(0xFF8993A3)

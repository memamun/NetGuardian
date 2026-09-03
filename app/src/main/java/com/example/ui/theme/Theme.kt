package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class NetGuardianColors(
    val isDark: Boolean,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val blocked: Color,
    val onBlocked: Color,
    val blockedContainer: Color,
    val onBlockedContainer: Color,
    val allowed: Color,
    val onAllowed: Color,
    val allowedContainer: Color,
    val onAllowedContainer: Color,
    val surfaceElevated: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val neutralContainer: Color,
    val onNeutralContainer: Color
)

private val DarkNetGuardianColors = NetGuardianColors(
    isDark = true,
    success = DarkSafeGreen,
    onSuccess = Color(0xFF022C16),
    successContainer = DarkSuccessContainer,
    onSuccessContainer = DarkOnSuccessContainer,
    warning = DarkWarningAmber,
    onWarning = Color(0xFF451A03),
    warningContainer = DarkWarningContainer,
    onWarningContainer = DarkOnWarningContainer,
    blocked = DarkBlockedRed,
    onBlocked = Color(0xFF4C0006),
    blockedContainer = DarkBlockedContainer,
    onBlockedContainer = DarkOnBlockedContainer,
    allowed = DarkPrimary,
    onAllowed = DarkOnPrimary,
    allowedContainer = DarkAllowedContainer,
    onAllowedContainer = DarkOnAllowedContainer,
    surfaceElevated = DarkSurfaceElevated,
    cardBackground = DarkCardBackground,
    cardBorder = DarkBorder,
    divider = DarkDivider,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    neutralContainer = Color(0xFF1E2B3E),
    onNeutralContainer = DarkTextSecondary
)

private val LightNetGuardianColors = NetGuardianColors(
    isDark = false,
    success = LightSafeGreen,
    onSuccess = Color.White,
    successContainer = LightSuccessContainer,
    onSuccessContainer = LightOnSuccessContainer,
    warning = StatusWarningAmber,
    onWarning = Color.White,
    warningContainer = LightWarningContainer,
    onWarningContainer = LightOnWarningContainer,
    blocked = SleekBlockedRed,
    onBlocked = Color.White,
    blockedContainer = SleekBlockedContainer,
    onBlockedContainer = SleekOnBlockedContainer,
    allowed = SleekPrimary,
    onAllowed = Color.White,
    allowedContainer = SleekAllowedContainer,
    onAllowedContainer = SleekOnPrimaryContainer,
    surfaceElevated = SleekSurface,
    cardBackground = SleekCardBackground,
    cardBorder = SleekBorder,
    divider = SleekDivider,
    textPrimary = SleekBodyText,
    textSecondary = SleekPrimaryText,
    textMuted = SleekMutedText,
    neutralContainer = SleekInactiveContainer,
    onNeutralContainer = SleekBodyText
)

val LocalNetGuardianColors = staticCompositionLocalOf { LightNetGuardianColors }

val MaterialTheme.netGuardian: NetGuardianColors
    @Composable
    @ReadOnlyComposable
    get() = LocalNetGuardianColors.current

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkSafeGreen,
    onTertiary = Color(0xFF022C16),
    tertiaryContainer = DarkSuccessContainer,
    onTertiaryContainer = DarkOnSuccessContainer,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextMuted,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceElevated,
    outline = DarkBorder,
    outlineVariant = DarkDivider,
    error = DarkBlockedRed,
    onError = Color(0xFF4C0006),
    errorContainer = DarkBlockedContainer,
    onErrorContainer = DarkOnBlockedContainer
)

private val LightColorScheme = lightColorScheme(
    primary = SleekPrimary,
    onPrimary = Color.White,
    primaryContainer = SleekPrimaryContainer,
    onPrimaryContainer = SleekOnPrimaryContainer,
    secondary = SleekPrimary,
    onSecondary = Color.White,
    secondaryContainer = SleekPrimaryContainer,
    onSecondaryContainer = SleekOnPrimaryContainer,
    tertiary = LightSafeGreen,
    onTertiary = Color.White,
    tertiaryContainer = LightSuccessContainer,
    onTertiaryContainer = LightOnSuccessContainer,
    background = SleekBackground,
    onBackground = SleekBodyText,
    surface = SleekSurface,
    onSurface = SleekBodyText,
    surfaceVariant = SleekSurfaceVariant,
    onSurfaceVariant = SleekMutedText,
    surfaceContainer = SleekSurfaceVariant,
    surfaceContainerHigh = SleekSurface,
    outline = SleekBorder,
    outlineVariant = SleekDivider,
    error = SleekBlockedRed,
    onError = Color.White,
    errorContainer = SleekBlockedContainer,
    onErrorContainer = SleekOnBlockedContainer
)

@Composable
fun NetGuardianTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep cyber identity consistent
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val netGuardianColors = if (darkTheme) DarkNetGuardianColors else LightNetGuardianColors

    CompositionLocalProvider(LocalNetGuardianColors provides netGuardianColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = NetGuardianTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)



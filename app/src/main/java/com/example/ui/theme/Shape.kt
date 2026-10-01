package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Corner Radius Scale (10 Steps)
 *
 * Direct implementation of official tokens from `.antigravity/m3-expressive/references/tokens.md`:
 * - None: 0dp
 * - Extra small: 4dp
 * - Small: 8dp
 * - Medium: 12dp
 * - Large: 16dp
 * - Large increased: 20dp (M3 Expressive addition)
 * - Extra large: 28dp
 * - Extra large increased: 32dp (M3 Expressive addition)
 * - Extra extra large: 48dp (M3 Expressive addition)
 * - Full: fully rounded (CircleShape)
 */
object M3CornerRadius {
    val None: Dp = 0.dp
    val ExtraSmall: Dp = 4.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Large: Dp = 16.dp
    val LargeIncreased: Dp = 20.dp
    val ExtraLarge: Dp = 28.dp
    val ExtraLargeIncreased: Dp = 32.dp
    val ExtraExtraLarge: Dp = 48.dp
}

object M3ShapesTokens {
    val CornerNone = RoundedCornerShape(M3CornerRadius.None)
    val CornerExtraSmall = RoundedCornerShape(M3CornerRadius.ExtraSmall)
    val CornerSmall = RoundedCornerShape(M3CornerRadius.Small)
    val CornerMedium = RoundedCornerShape(M3CornerRadius.Medium)
    val CornerLarge = RoundedCornerShape(M3CornerRadius.Large)
    val CornerLargeIncreased = RoundedCornerShape(M3CornerRadius.LargeIncreased)
    val CornerExtraLarge = RoundedCornerShape(M3CornerRadius.ExtraLarge)
    val CornerExtraLargeIncreased = RoundedCornerShape(M3CornerRadius.ExtraLargeIncreased)
    val CornerExtraExtraLarge = RoundedCornerShape(M3CornerRadius.ExtraExtraLarge)
    val CornerFull = CircleShape

    // Partial-corner variants for sheets, menus, drawers
    val CornerExtraLargeTop = RoundedCornerShape(
        topStart = M3CornerRadius.ExtraLarge,
        topEnd = M3CornerRadius.ExtraLarge,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    val CornerLargeTop = RoundedCornerShape(
        topStart = M3CornerRadius.Large,
        topEnd = M3CornerRadius.Large,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    val CornerLargeStart = RoundedCornerShape(
        topStart = M3CornerRadius.Large,
        bottomStart = M3CornerRadius.Large,
        topEnd = 0.dp,
        bottomEnd = 0.dp
    )
    val CornerLargeEnd = RoundedCornerShape(
        topEnd = M3CornerRadius.Large,
        bottomEnd = M3CornerRadius.Large,
        topStart = 0.dp,
        bottomStart = 0.dp
    )
}

/**
 * Optical roundness calculation for nested containers (from M3 Expressive specification):
 * inner radius = outer radius - padding
 * Never reuse the parent's radius on nested elements.
 */
fun opticalInnerRadius(outer: Dp, padding: Dp): Dp = maxOf(0.dp, outer - padding)

fun opticalInnerShape(outer: Dp, padding: Dp): RoundedCornerShape {
    val inner = opticalInnerRadius(outer, padding)
    return if (inner == 0.dp) RoundedCornerShape(0.dp) else RoundedCornerShape(inner)
}

/**
 * Standard Compose Material 3 Shapes mapped to M3 Expressive definitions.
 */
val NetGuardianShapes = Shapes(
    extraSmall = M3ShapesTokens.CornerExtraSmall,
    small = M3ShapesTokens.CornerSmall,
    medium = M3ShapesTokens.CornerMedium,
    large = M3ShapesTokens.CornerLargeIncreased, // My design decision (not in M3): larger containers use this scale step.
    extraLarge = M3ShapesTokens.CornerExtraLarge
)

@Immutable
data class M3ExpressiveShapes(
    val none: CornerBasedShape = M3ShapesTokens.CornerNone,
    val extraSmall: CornerBasedShape = M3ShapesTokens.CornerExtraSmall,
    val small: CornerBasedShape = M3ShapesTokens.CornerSmall,
    val medium: CornerBasedShape = M3ShapesTokens.CornerMedium,
    val large: CornerBasedShape = M3ShapesTokens.CornerLarge,
    val largeIncreased: CornerBasedShape = M3ShapesTokens.CornerLargeIncreased,
    val extraLarge: CornerBasedShape = M3ShapesTokens.CornerExtraLarge,
    val extraLargeIncreased: CornerBasedShape = M3ShapesTokens.CornerExtraLargeIncreased,
    val extraExtraLarge: CornerBasedShape = M3ShapesTokens.CornerExtraExtraLarge,
    val full: CornerBasedShape = M3ShapesTokens.CornerFull,
    val extraLargeTop: CornerBasedShape = M3ShapesTokens.CornerExtraLargeTop,
    val largeTop: CornerBasedShape = M3ShapesTokens.CornerLargeTop
)

val LocalM3Shapes = staticCompositionLocalOf { M3ExpressiveShapes() }

val MaterialTheme.m3Shapes: M3ExpressiveShapes
    @Composable
    @ReadOnlyComposable
    get() = LocalM3Shapes.current

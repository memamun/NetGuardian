package com.example.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Material Design 3 Expressive motion tokens based on:
 * .antigravity/m3-expressive/references/tokens.md
 *
 * Expressive scheme:
 * - Spatial springs: expressive bounce with damping 0.6 - 0.8
 * - Effects springs: always damping 1.0 (no overshoot, for color/alpha transitions)
 */
object M3ExpressiveMotionTokens {
    // Spatial (damping, stiffness)
    const val SpatialDefaultDamping = 0.8f
    const val SpatialDefaultStiffness = 380f

    const val SpatialFastDamping = 0.6f
    const val SpatialFastStiffness = 800f

    const val SpatialSlowDamping = 0.8f
    const val SpatialSlowStiffness = 200f

    // Effects (damping 1.0, stiffness)
    const val EffectsDefaultDamping = 1.0f
    const val EffectsDefaultStiffness = 1600f

    const val EffectsFastDamping = 1.0f
    const val EffectsFastStiffness = 3800f

    const val EffectsSlowDamping = 1.0f
    const val EffectsSlowStiffness = 800f
}

/**
 * Spring spec helper methods for Compose animations
 */
object M3Motion {
    fun <T> spatialDefault(): SpringSpec<T> = spring(
        dampingRatio = M3ExpressiveMotionTokens.SpatialDefaultDamping,
        stiffness = M3ExpressiveMotionTokens.SpatialDefaultStiffness
    )

    fun <T> spatialFast(): SpringSpec<T> = spring(
        dampingRatio = M3ExpressiveMotionTokens.SpatialFastDamping,
        stiffness = M3ExpressiveMotionTokens.SpatialFastStiffness
    )

    fun <T> spatialSlow(): SpringSpec<T> = spring(
        dampingRatio = M3ExpressiveMotionTokens.SpatialSlowDamping,
        stiffness = M3ExpressiveMotionTokens.SpatialSlowStiffness
    )

    fun <T> effectsDefault(): SpringSpec<T> = spring(
        dampingRatio = M3ExpressiveMotionTokens.EffectsDefaultDamping,
        stiffness = M3ExpressiveMotionTokens.EffectsDefaultStiffness
    )

    // Alias for common naming
    fun <T> effectsMedium(): SpringSpec<T> = effectsDefault()

    fun <T> effectsFast(): SpringSpec<T> = spring(
        dampingRatio = M3ExpressiveMotionTokens.EffectsFastDamping,
        stiffness = M3ExpressiveMotionTokens.EffectsFastStiffness
    )

    fun <T> effectsSlow(): SpringSpec<T> = spring(
        dampingRatio = M3ExpressiveMotionTokens.EffectsSlowDamping,
        stiffness = M3ExpressiveMotionTokens.EffectsSlowStiffness
    )
}

@Immutable
data class M3ExpressiveMotion(
    val spatialDefaultDamping: Float = M3ExpressiveMotionTokens.SpatialDefaultDamping,
    val spatialDefaultStiffness: Float = M3ExpressiveMotionTokens.SpatialDefaultStiffness,
    val spatialFastDamping: Float = M3ExpressiveMotionTokens.SpatialFastDamping,
    val spatialFastStiffness: Float = M3ExpressiveMotionTokens.SpatialFastStiffness,
    val spatialSlowDamping: Float = M3ExpressiveMotionTokens.SpatialSlowDamping,
    val spatialSlowStiffness: Float = M3ExpressiveMotionTokens.SpatialSlowStiffness,
    val effectsDefaultDamping: Float = M3ExpressiveMotionTokens.EffectsDefaultDamping,
    val effectsDefaultStiffness: Float = M3ExpressiveMotionTokens.EffectsDefaultStiffness,
    val effectsFastDamping: Float = M3ExpressiveMotionTokens.EffectsFastDamping,
    val effectsFastStiffness: Float = M3ExpressiveMotionTokens.EffectsFastStiffness,
    val effectsSlowDamping: Float = M3ExpressiveMotionTokens.EffectsSlowDamping,
    val effectsSlowStiffness: Float = M3ExpressiveMotionTokens.EffectsSlowStiffness
)

val LocalM3Motion = staticCompositionLocalOf { M3ExpressiveMotion() }

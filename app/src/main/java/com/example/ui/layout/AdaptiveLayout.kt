package com.example.ui.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Window Breakpoints and Adaptive Layout System
 *
 * Direct implementation of official tokens and guidelines from
 * `.antigravity/m3-expressive/references/layout.md`:
 *
 * Breakpoint scale:
 * - Compact: Width < 600dp (Phone portrait) -> Single pane, 16dp margins, bottom Navigation Bar
 * - Medium: Width 600–839dp (Tablet portrait, Foldable unfolded) -> 24dp margins, Navigation Rail
 * - Expanded: Width 840–1199dp (Tablet landscape, Desktop) -> 24dp margins, Navigation Rail
 * - Large: Width 1200–1599dp -> 24dp margins, multi-pane (412dp fixed pane)
 * - ExtraLarge: Width 1600dp+ -> 24dp margins, 3 panes
 *
 * Region Color Rule:
 * - Body area: surface
 * - Navigation area: surfaceContainer (invariant across all breakpoints)
 */
enum class M3Breakpoint {
    Compact,
    Medium,
    Expanded,
    Large,
    ExtraLarge
}

@Immutable
data class M3LayoutInfo(
    val breakpoint: M3Breakpoint,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val isCompact: Boolean,
    val isMediumOrExpanded: Boolean,
    val margin: Dp,
    val spacer: Dp,
    val defaultFixedPaneWidth: Dp
)

@Composable
fun rememberM3LayoutInfo(): M3LayoutInfo {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp
    val height = configuration.screenHeightDp

    val breakpoint = when {
        width < 600 -> M3Breakpoint.Compact
        width < 840 -> M3Breakpoint.Medium
        width < 1200 -> M3Breakpoint.Expanded
        width < 1600 -> M3Breakpoint.Large
        else -> M3Breakpoint.ExtraLarge
    }

    val margin = if (breakpoint == M3Breakpoint.Compact) 16.dp else 24.dp
    val spacer = 24.dp
    val fixedPaneWidth = when (breakpoint) {
        M3Breakpoint.Compact, M3Breakpoint.Medium -> 0.dp
        M3Breakpoint.Expanded -> 360.dp
        M3Breakpoint.Large, M3Breakpoint.ExtraLarge -> 412.dp
    }

    return M3LayoutInfo(
        breakpoint = breakpoint,
        screenWidthDp = width,
        screenHeightDp = height,
        isCompact = breakpoint == M3Breakpoint.Compact,
        isMediumOrExpanded = breakpoint != M3Breakpoint.Compact,
        margin = margin,
        spacer = spacer,
        defaultFixedPaneWidth = fixedPaneWidth
    )
}

/**
 * Adaptive Navigation Scaffold
 *
 * Automatically swaps between:
 * - Compact (<600dp): Bottom NavigationBar (containerColor = surfaceContainer)
 * - Medium & Expanded (>=600dp): Leading NavigationRail (containerColor = surfaceContainer)
 */
@Composable
fun AdaptiveNavigationScaffold(
    layoutInfo: M3LayoutInfo,
    navigationBar: @Composable () -> Unit,
    navigationRail: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (M3LayoutInfo) -> Unit
) {
    if (layoutInfo.isCompact) {
        // Compact: Content with bottom NavigationBar
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content(layoutInfo)
            }
            // Navigation area uses surfaceContainer per M3 specification
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                navigationBar()
            }
        }
    } else {
        // Medium / Expanded: Leading NavigationRail with Content
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Navigation area uses surfaceContainer per M3 specification
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxHeight()
            ) {
                navigationRail()
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                content(layoutInfo)
            }
        }
    }
}

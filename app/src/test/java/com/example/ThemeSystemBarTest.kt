package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.view.WindowCompat
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesRepository
import com.example.ui.theme.NetGuardianTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeSystemBarTest {

    @get:Rule
    val compose = createComposeRule()

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    @Test
    fun preferencesRepository_defaultsToLightModeSync_andUpdatesOnSave() = runTest {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val prefs = PreferencesRepository(app)
        assertEquals("LIGHT", prefs.getThemeModeSync())

        prefs.setThemeMode("DARK")
        assertEquals("DARK", prefs.getThemeModeSync())

        prefs.setThemeMode("LIGHT")
        assertEquals("LIGHT", prefs.getThemeModeSync())
    }

    @Test
    fun netGuardianTheme_configuresLightSystemBars_whenNotDarkTheme() {
        var activity: Activity? = null
        compose.setContent {
            val view = LocalView.current
            activity = view.context.findActivity()
            NetGuardianTheme(darkTheme = false) {
                Text("Light Mode Content")
            }
        }

        val window = activity?.window
        org.junit.Assert.assertNotNull("Activity window should not be null", window)
        val insetsController = WindowCompat.getInsetsController(window!!, window.decorView)

        assertTrue(
            "Light mode must set isAppearanceLightStatusBars = true so icons are dark and readable on light background",
            insetsController.isAppearanceLightStatusBars
        )
        assertTrue(
            "Light mode must set isAppearanceLightNavigationBars = true so icons are dark and readable on light background",
            insetsController.isAppearanceLightNavigationBars
        )
    }

    @Test
    fun netGuardianTheme_configuresDarkSystemBars_whenDarkTheme() {
        var activity: Activity? = null
        compose.setContent {
            val view = LocalView.current
            activity = view.context.findActivity()
            NetGuardianTheme(darkTheme = true) {
                Text("Dark Mode Content")
            }
        }

        val window = activity?.window
        org.junit.Assert.assertNotNull("Activity window should not be null", window)
        val insetsController = WindowCompat.getInsetsController(window!!, window.decorView)

        assertFalse(
            "Dark mode must set isAppearanceLightStatusBars = false so icons are light/white on dark background",
            insetsController.isAppearanceLightStatusBars
        )
        assertFalse(
            "Dark mode must set isAppearanceLightNavigationBars = false so icons are light/white on dark background",
            insetsController.isAppearanceLightNavigationBars
        )
    }
}

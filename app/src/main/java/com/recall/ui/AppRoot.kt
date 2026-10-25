package com.recall.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.recall.data.settings.AppSettings
import com.recall.data.settings.resolveDark
import com.recall.di.AppContainer
import com.recall.ui.nav.KilnNavHost
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.KilnTheme
import com.recall.ui.theme.kilnGround

@Composable
fun AppRoot(container: AppContainer) {
    // Waiting for the stored settings avoids flashing the default palette on every launch.
    val settings: AppSettings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val current = settings ?: return
    val dark = current.resolveDark(isSystemInDarkTheme())

    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
    }

    KilnTheme(current.palette, dark, current.typePairing, current.reduceMotion) {
        val navController = rememberNavController()
        Box(Modifier.fillMaxSize().kilnGround(Kiln.colors)) {
            KilnNavHost(navController, Modifier.fillMaxSize())
        }
    }
}

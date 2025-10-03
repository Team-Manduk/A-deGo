package com.teammanduk.adego.core.designsystem.ui.theme

import android.app.Activity
import android.os.Build
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun AdegoTheme(
    content: @Composable () -> Unit,
) {
    val colors = remember { AdegoLightColor }

    val view = LocalView.current
    val window = (view.context as? Activity)?.window

    SideEffect {
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSystemBarBackground()
        }
    }

    CompositionLocalProvider(
        LocalColor provides colors,
        LocalTypography provides AdegoTypography,
        content = content
    )
}

private fun Window.setSystemBarBackground() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        this.isNavigationBarContrastEnforced = false
    }

    this.statusBarColor = Color.Transparent.toArgb()
    this.navigationBarColor = Color.Transparent.toArgb()
}

object AdegoTheme {
    val colors: AdegoColor
        @Composable get() = LocalColor.current
    val typography: AdegoTypography
        @Composable get() = LocalTypography.current
}
package com.iqodist.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary          = IqoPrimary,
    onPrimary        = androidx.compose.ui.graphics.Color.White,
    primaryContainer = IqoPrimaryLight,
    secondary        = IqoSecondary,
    onSecondary      = androidx.compose.ui.graphics.Color.White,
    error            = IqoError,
    background       = Gray50,
    surface          = androidx.compose.ui.graphics.Color.White,
    onBackground     = Gray900,
    onSurface        = Gray900
)

private val DarkColorScheme = darkColorScheme(
    primary          = IqoPrimaryDarkTheme,
    onPrimary        = Gray900,
    secondary        = IqoSecondary,
    onSecondary      = Gray900,
    error            = IqoError,
    background       = BackgroundDark,
    surface          = SurfaceDark,
    onBackground     = Gray50,
    onSurface        = Gray50
)

@Composable
fun IqodistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,   // set false agar warna brand konsisten
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = IqodistTypography,
        content     = content
    )
}

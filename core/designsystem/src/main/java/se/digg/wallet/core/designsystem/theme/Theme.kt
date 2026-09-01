// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    secondary = Orange50,
    tertiary = Pink80,
    surface = ContainerDarkMode,
    background = DefaultBackgroundDarkMode,
    onSurface = TextColorDarkMode,
    errorContainer = ErrorContainerDarkMode,
    primaryContainer = ButtonContainerPrimaryDark,
    onPrimaryContainer = TextColor,

)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    secondary = Orange50,
    tertiary = Pink40,
    surface = Container,
    background = DefaultBackground,
    onSurface = TextColor,
    errorContainer = ErrorContainer,
    primaryContainer = ButtonContainerPrimary,
    onPrimaryContainer = TextColorDarkMode,

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
     */
)

/**
 * Whether the app is effectively in dark mode right now — [WalletTheme]'s [darkTheme][WalletTheme]
 * value, honoring the user's in-app theme override, not just the OS setting. Read this instead of
 * [isSystemInDarkTheme] anywhere a component picks a color by dark/light, so hand-picked colors
 * stay in sync with [se.digg.wallet.core.theme.ThemePreference] the same way [MaterialTheme.colorScheme]
 * already does.
 */
val LocalIsDarkTheme = staticCompositionLocalOf<Boolean> {
    error("LocalIsDarkTheme not provided — wrap content in WalletTheme")
}

@Composable
fun isWalletInDarkTheme(): Boolean = LocalIsDarkTheme.current

@Composable
fun WalletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> {
            DarkColorScheme
        }

        else -> {
            LightColorScheme
        }
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = UbuntuTypography,
            content = content,
        )
    }
}

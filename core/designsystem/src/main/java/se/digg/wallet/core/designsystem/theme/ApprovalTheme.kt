// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val ApprovalSurfaceLight = Color(0xFFFFFFFF)
private val ApprovalSurfaceDark = Color(0xFF121411)
private val ApprovalTextLight = Color(0xFF2B2A29)
private val ApprovalTextDark = Color(0xFFECEDEA)
private val ApprovalTextSecondaryLight = Color(0xFF747372)
private val ApprovalTextSecondaryDark = Color(0xFFA3A6A1)
private val ApprovalLabelLight = Color(0xFF4F5F4B)
private val ApprovalLabelDark = Color(0xFFA9BDA3)
private val ApprovalAccentLight = Color(0xFF556951)
private val ApprovalAccentDark = Color(0xFFA8C49F)
private val ApprovalOnAccentLight = Color(0xFFFFFFFF)
private val ApprovalOnAccentDark = Color(0xFF14200F)
private val ApprovalTintLight = Color(0xFFEEF2ED)
private val ApprovalTintDark = Color(0xFF253023)
private val ApprovalDividerLight = Color(0xFFE8E8E6)
private val ApprovalDividerDark = Color(0xFF333532)
private val ApprovalBorderLight = Color(0xFFD5DAD4)
private val ApprovalBorderDark = Color(0xFF3B403A)
private val ApprovalBorderStrongLight = Color(0xFF7E897B)
private val ApprovalBorderStrongDark = Color(0xFF7C8879)
private val ApprovalBarLight = Color(0xFFF3F4F0)
private val ApprovalBarDark = Color(0xFF1E201D)

private val ApprovalLightColorScheme = lightColorScheme(
    primary = ApprovalAccentLight,
    onPrimary = ApprovalOnAccentLight,
    primaryContainer = ApprovalTintLight,
    onPrimaryContainer = ApprovalTextLight,
    tertiary = ApprovalLabelLight,
    background = ApprovalSurfaceLight,
    onBackground = ApprovalTextLight,
    surface = ApprovalSurfaceLight,
    onSurface = ApprovalTextLight,
    onSurfaceVariant = ApprovalTextSecondaryLight,
    surfaceContainer = ApprovalBarLight,
    surfaceContainerHighest = ApprovalDividerLight,
    outline = ApprovalBorderStrongLight,
    outlineVariant = ApprovalBorderLight,
    errorContainer = ErrorContainer,
)

private val ApprovalDarkColorScheme = darkColorScheme(
    primary = ApprovalAccentDark,
    onPrimary = ApprovalOnAccentDark,
    primaryContainer = ApprovalTintDark,
    onPrimaryContainer = ApprovalTextDark,
    tertiary = ApprovalLabelDark,
    background = ApprovalSurfaceDark,
    onBackground = ApprovalTextDark,
    surface = ApprovalSurfaceDark,
    onSurface = ApprovalTextDark,
    onSurfaceVariant = ApprovalTextSecondaryDark,
    surfaceContainer = ApprovalBarDark,
    surfaceContainerHighest = ApprovalDividerDark,
    outline = ApprovalBorderStrongDark,
    outlineVariant = ApprovalBorderDark,
    errorContainer = ErrorContainerDarkMode,
)

private val ApprovalTypography = with(UbuntuTypography) {
    copy(
        headlineSmall = headlineSmall.withMetrics(24, 32, FontWeight.Medium),
        titleLarge = titleLarge.withMetrics(22, 28, FontWeight.Medium),
        titleMedium = titleMedium.withMetrics(16, 24, FontWeight.Bold),
        titleSmall = titleSmall.withMetrics(14, 20, FontWeight.Medium),
        bodyLarge = bodyLarge.withMetrics(16, 24, FontWeight.Normal),
        bodyMedium = bodyMedium.withMetrics(14, 20, FontWeight.Normal),
        labelLarge = labelLarge.withMetrics(16, 24, FontWeight.Medium),
        labelMedium = labelMedium.withMetrics(12, 16, FontWeight.Medium),
    )
}

private fun TextStyle.withMetrics(size: Int, lineHeight: Int, weight: FontWeight) = copy(
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = 0.sp,
)

@Composable
fun ApprovalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isWalletInDarkTheme()) {
            ApprovalDarkColorScheme
        } else {
            ApprovalLightColorScheme
        },
        typography = ApprovalTypography,
        content = content,
    )
}

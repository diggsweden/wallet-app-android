// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import se.digg.wallet.R

val ubuntuFontFamily = FontFamily(
    Font(R.font.ubuntu_light, FontWeight.Light),
    Font(R.font.ubuntu_light_italic, FontWeight.Light, FontStyle.Italic),
    Font(R.font.ubuntu_regular, FontWeight.Normal),
    Font(R.font.ubuntu_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.ubuntu_medium, FontWeight.Medium),
    Font(R.font.ubuntu_medium_italic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.ubuntu_bold, FontWeight.Bold),
    Font(R.font.ubuntu_bold_italic, FontWeight.Bold, FontStyle.Italic),
)

private val DefaultM3 = Typography()

// https://developer.android.com/develop/ui/compose/designsystems/material2-material3#typography
val UbuntuTypography = Typography(
    displayLarge = DefaultM3.displayLarge.copy(fontFamily = ubuntuFontFamily),
    displayMedium = DefaultM3.displayMedium.copy(fontFamily = ubuntuFontFamily),
    displaySmall = DefaultM3.displaySmall.copy(fontFamily = ubuntuFontFamily),

    headlineLarge = DefaultM3.headlineLarge.copy(fontFamily = ubuntuFontFamily),
    headlineMedium = DefaultM3.headlineMedium.copy(fontFamily = ubuntuFontFamily),
    headlineSmall = DefaultM3.headlineSmall.copy(fontFamily = ubuntuFontFamily),

    titleLarge = DefaultM3.titleLarge.copy(fontFamily = ubuntuFontFamily),
    titleMedium = DefaultM3.titleMedium.copy(fontFamily = ubuntuFontFamily),
    titleSmall = DefaultM3.titleSmall.copy(fontFamily = ubuntuFontFamily),

    bodyLarge = DefaultM3.bodyLarge.copy(fontFamily = ubuntuFontFamily),
    bodyMedium = DefaultM3.bodyMedium.copy(fontFamily = ubuntuFontFamily),
    bodySmall = DefaultM3.bodySmall.copy(fontFamily = ubuntuFontFamily),

    labelLarge = DefaultM3.labelLarge.copy(fontFamily = ubuntuFontFamily),
    labelMedium = DefaultM3.labelMedium.copy(fontFamily = ubuntuFontFamily),
    labelSmall = DefaultM3.labelSmall.copy(fontFamily = ubuntuFontFamily),
)

object DiggTextStyle {
    val H1 = TextStyle(
        fontFamily = ubuntuFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 32.sp,
    )
    val H2 = TextStyle(
        fontFamily = ubuntuFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 24.sp,
    )
    val H3 = TextStyle(
        fontFamily = ubuntuFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 20.sp,
    )
    val H4 = TextStyle(
        fontFamily = ubuntuFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 18.sp,
    )
    val H5 = TextStyle(
        fontFamily = ubuntuFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 16.sp,
    )
    val H6 = TextStyle(
        fontFamily = ubuntuFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 16.sp,
    )
    val BodyPreamble = TextStyle(
        fontFamily = ubuntuFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 20.sp,
    )
    val BodyLG = TextStyle(
        fontFamily = ubuntuFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 32.sp,
    )
    val BodyMD = TextStyle(
        fontFamily = ubuntuFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )
    val BodySM = TextStyle(
        fontFamily = ubuntuFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
}

// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.utils

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "1 Dark Mode - Text size: Normal",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:parent=pixel_9,orientation=portrait",
    showBackground = true,
)
@Preview(
    name = "1 Light Mode - Text size: Normal",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    device = "spec:parent=pixel_9,orientation=portrait",
    showBackground = true,
)
@Preview(
    name = "Dark Mode - Landscape - Text size: Normal",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:parent=pixel_9,orientation=landscape",
    showBackground = true,
)
@Preview(
    name = "Light Mode - Landscape -  Text size: Normal",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    device = "spec:parent=pixel_9,orientation=landscape",
    showBackground = true,
)
annotation class PreviewsWallet

@Preview(
    name = "Dark Mode - Text size: Normal",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Preview(
    name = "Light Mode - Text size: Normal",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
annotation class PreviewsWalletAccessibility

@Preview(
    name = "1 Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    device = "spec:parent=pixel_9,orientation=portrait",
    showBackground = true,
)
@Preview(
    name = "2 Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:parent=pixel_9,orientation=portrait",
    showBackground = true,
)
@Preview(
    name = "3 Text size: Largest (2.0)",
    fontScale = 2f,
    device = "spec:parent=pixel_9,orientation=portrait",
    showBackground = true,
)
@Preview(
    name = "4 Swedish",
    locale = "sv",
    device = "spec:parent=pixel_9,orientation=portrait",
    showBackground = true,
)
@Preview(
    name = "5 Small phone",
    device = "spec:width=360dp,height=640dp",
    showBackground = true,
)
@Preview(
    name = "6 Tablet",
    device = Devices.TABLET,
    showBackground = true,
)
annotation class PreviewsWalletScreen

// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.utils

import android.content.res.Configuration
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

// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import android.content.ClipData
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import se.digg.wallet.core.designsystem.R
import se.digg.wallet.core.designsystem.theme.Brown100
import se.digg.wallet.core.designsystem.theme.Brown30
import se.digg.wallet.core.designsystem.theme.Brown50
import se.digg.wallet.core.designsystem.theme.Brown70
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.NetworkType
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.core.designsystem.utils.getDeviceInfo

data class ErrorDetail(val title: String, val value: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericErrorScreen(
    modifier: Modifier = Modifier,
    @DrawableRes image: Int = R.drawable.phone_error_1,
    errorTitle: String? = null,
    errorMessage: String? = null,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    supportLabel: String? = null,
    onSupportClick: (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    errorDetails: List<ErrorDetail> = emptyList(),
    detailsButtonLabel: String? = null,
) {
    val textColor = if (isSystemInDarkTheme()) Brown30 else Brown100
    var showDetails by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (onNavigateBack != null) {
            IconButton(
                modifier = Modifier.align(Alignment.TopStart),
                onClick = onNavigateBack,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_left),
                    contentDescription = stringResource(R.string.error_navigate_back),
                    tint = textColor,
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                modifier = Modifier.size(180.dp),
                painter = painterResource(image),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.height(23.dp))
            Text(
                modifier = Modifier.width(300.dp),
                textAlign = TextAlign.Start,
                text = errorTitle ?: stringResource(R.string.generic_error_screen_title),
                style = WalletTextStyle.H1,
                color = textColor,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                modifier = Modifier.width(300.dp),
                textAlign = TextAlign.Start,
                text = errorMessage
                    ?: stringResource(R.string.generic_error_screen_message),
                style = WalletTextStyle.BodyMD,
                color = textColor,
            )
            if (onSupportClick != null) {
                Spacer(modifier = Modifier.height(7.dp))
                TextWithLink(
                    modifier = Modifier.width(300.dp),
                    text = supportLabel ?: stringResource(R.string.error_contact_support),
                    onClick = onSupportClick,
                )
            }
            if (errorDetails.isNotEmpty()) {
                Spacer(modifier = Modifier.height(7.dp))
                TextWithLink(
                    modifier = Modifier.width(300.dp),
                    text = detailsButtonLabel ?: stringResource(R.string.error_details_button),
                    onClick = { showDetails = true },
                )
            }
            Spacer(modifier = Modifier.height(50.dp))
            if (onPrimaryAction != null) {
                PrimaryButton(
                    text = primaryActionLabel ?: stringResource(R.string.generic_retry),
                    onClick = onPrimaryAction,
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
            if (onSecondaryAction != null) {
                SecondaryButton(
                    text = secondaryActionLabel ?: stringResource(R.string.generic_home),
                    onClick = onSecondaryAction,
                )
            }
        }
    }

    if (showDetails) {
        ModalBottomSheet(onDismissRequest = { showDetails = false }) {
            ErrorDetailsSheetContent(errorDetails)
        }
    }
}

private const val ERROR_DETAILS_CLIPBOARD_LABEL = "error-details"
private const val COPIED_LABEL_DURATION_MS = 1_500L

@Composable
private fun ErrorDetailsSheetContent(details: List<ErrorDetail>, modifier: Modifier = Modifier) {
    val labelColor = if (isSystemInDarkTheme()) Brown50 else Brown70
    val valueColor = if (isSystemInDarkTheme()) Brown30 else Brown100
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    val deviceDetails = rememberDeviceErrorDetails()
    val allDetails = details + deviceDetails

    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_LABEL_DURATION_MS)
            copied = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.error_details_title),
                style = WalletTextStyle.H3,
                color = valueColor,
            )
            AssistChip(
                onClick = {
                    scope.launch {
                        val text =
                            allDetails.joinToString(separator = "\n") { "${it.title}: ${it.value}" }
                        clipboard.setClipEntry(
                            ClipEntry(ClipData.newPlainText(ERROR_DETAILS_CLIPBOARD_LABEL, text)),
                        )
                    }
                    copied = true
                },
                label = {
                    Text(
                        text = if (copied) {
                            stringResource(R.string.error_details_copied)
                        } else {
                            stringResource(R.string.error_details_copy)
                        },
                        style = WalletTextStyle.BodySM,
                    )
                },
                leadingIcon = {
                    Image(
                        painter = painterResource(R.drawable.copy_icon),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        ErrorDetailRows(details = details, labelColor = labelColor, valueColor = valueColor)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.device_info_title),
            style = WalletTextStyle.H4,
            color = valueColor,
        )
        Spacer(modifier = Modifier.height(16.dp))
        ErrorDetailRows(details = deviceDetails, labelColor = labelColor, valueColor = valueColor)
    }
}

@Composable
private fun ErrorDetailRows(details: List<ErrorDetail>, labelColor: Color, valueColor: Color) {
    details.forEachIndexed { index, detail ->
        Text(
            text = detail.title,
            style = WalletTextStyle.BodySM,
            color = labelColor,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = detail.value,
            style = WalletTextStyle.BodyMD,
            color = valueColor,
        )
        if (index != details.lastIndex) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun rememberDeviceErrorDetails(): List<ErrorDetail> {
    val context = LocalContext.current
    val deviceInfo = remember { getDeviceInfo(context) }
    return listOf(
        ErrorDetail(title = stringResource(R.string.device_info_brand), value = deviceInfo.brand),
        ErrorDetail(title = stringResource(R.string.device_info_model), value = deviceInfo.model),
        ErrorDetail(
            title = stringResource(R.string.device_info_version),
            value = deviceInfo.sdkVersion.toString(),
        ),
        ErrorDetail(
            title = stringResource(R.string.device_info_android_version),
            value = deviceInfo.osVersion,
        ),
        ErrorDetail(
            title = stringResource(R.string.device_info_network_type),
            value = deviceInfo.networkType.toLabel(),
        ),
        ErrorDetail(
            title = stringResource(R.string.device_info_app_version),
            value = "${deviceInfo.appVersionName} (${deviceInfo.appVersionCode})",
        ),
    )
}

@Composable
private fun NetworkType.toLabel(): String = when (this) {
    NetworkType.WIFI -> stringResource(R.string.device_info_network_wifi)
    NetworkType.CELLULAR -> stringResource(R.string.device_info_network_cellular)
    NetworkType.ETHERNET -> stringResource(R.string.device_info_network_ethernet)
    NetworkType.VPN -> stringResource(R.string.device_info_network_vpn)
    NetworkType.OTHER -> stringResource(R.string.device_info_network_other)
    NetworkType.OFFLINE -> stringResource(R.string.device_info_network_offline)
    NetworkType.UNKNOWN -> stringResource(R.string.device_info_network_unknown)
}

@Composable
@PreviewsWallet
private fun GenericErrorScreenDefaultPreview() {
    WalletPreview {
        GenericErrorScreen(
            onPrimaryAction = {},
        )
    }
}

@Composable
@PreviewsWallet
private fun GenericErrorScreenNetworkPreview() {
    WalletPreview {
        GenericErrorScreen(
            errorTitle = "No connection",
            errorMessage = "Check your internet connection and try again.",
            onPrimaryAction = {},
            secondaryActionLabel = "Go back",
            onSecondaryAction = {},
        )
    }
}

@Composable
@PreviewsWallet
private fun GenericErrorScreenWithDetailsPreview() {
    WalletPreview {
        GenericErrorScreen(
            errorTitle = "Account already exists",
            errorMessage = "This device is already registered to an account.",
            onPrimaryAction = {},
            secondaryActionLabel = "Cancel",
            onSecondaryAction = {},
            errorDetails = listOf(
                ErrorDetail(title = "Status", value = "409"),
                ErrorDetail(title = "Type", value = "/problem-details/device-key-duplicate"),
                ErrorDetail(title = "Transaction ID", value = "b66dca70-0908-4b07-a1af-29d0c18b89b6"),
            ),
        )
    }
}

@Composable
@PreviewsWallet
private fun GenericErrorScreenMessageOnlyPreview() {
    WalletPreview {
        GenericErrorScreen(
            errorTitle = "Nothing to show",
            errorMessage = "We couldn't find what you were looking for.",
            onSupportClick = {},
        )
    }
}

@Composable
@PreviewsWallet
private fun GenericErrorScreenFullPreview() {
    WalletPreview {
        GenericErrorScreen(
            errorTitle = null,
            errorMessage = null,
            primaryActionLabel = "Try again",
            onPrimaryAction = {},
            onSecondaryAction = {},
            onSupportClick = {},
        )
    }
}

@Composable
@PreviewsWallet
private fun GenericErrorScreenWithBackPreview() {
    WalletPreview {
        GenericErrorScreen(
            errorTitle = "No connection",
            errorMessage = "Check your internet connection and try again.",
            onPrimaryAction = {},
            onNavigateBack = {},
        )
    }
}

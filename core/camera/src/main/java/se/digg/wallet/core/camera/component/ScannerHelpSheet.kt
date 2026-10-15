// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.camera.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import se.digg.wallet.core.camera.R
import se.digg.wallet.core.designsystem.theme.Brown100
import se.digg.wallet.core.designsystem.theme.Brown30
import se.digg.wallet.core.designsystem.theme.DarkPrimary
import se.digg.wallet.core.designsystem.theme.DefaultLink
import se.digg.wallet.core.designsystem.theme.DefaultLinkDarkMode
import se.digg.wallet.core.designsystem.theme.Green10
import se.digg.wallet.core.designsystem.theme.Green100
import se.digg.wallet.core.designsystem.theme.Green25
import se.digg.wallet.core.designsystem.theme.Pink10
import se.digg.wallet.core.designsystem.theme.Pink175
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.theme.isWalletInDarkTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

private val CardShape = RoundedCornerShape(size = 20.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScannerHelpSheet(onDismissRequest: () -> Unit, modifier: Modifier = Modifier) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        ScannerHelpContent(
            onCloseClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) onDismissRequest()
                }
            },
        )
    }
}

@Composable
private fun ScannerHelpContent(onCloseClick: () -> Unit, modifier: Modifier = Modifier) {
    val accentColor = if (isWalletInDarkTheme()) DarkPrimary else Green10
    val consentColor = if (isWalletInDarkTheme()) Pink175 else Pink10

    Column(modifier = modifier.fillMaxWidth()) {
        IconButton(
            onClick = onCloseClick,
            modifier = Modifier.padding(horizontal = 18.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.close),
                contentDescription = stringResource(id = R.string.qr_scanner_help_close),
            )
        }
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 30.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            ScannerHelpHeader(accentColor = accentColor)
            ScannerHelpSection(
                title = stringResource(id = R.string.qr_scanner_help_scannable_title),
            ) {
                ScannerHelpCard(color = accentColor) {
                    ScannerHelpInfoRow(
                        iconRes = R.drawable.id_card_24px,
                        title = stringResource(id = R.string.qr_scanner_help_identify_title),
                        detail = stringResource(id = R.string.qr_scanner_help_identify_detail),
                    )
                    ScannerHelpInfoRow(
                        iconRes = R.drawable.library_add,
                        title = stringResource(id = R.string.qr_scanner_help_add_document_title),
                        detail = stringResource(id = R.string.qr_scanner_help_add_document_detail),
                    )
                }
            }
            ScannerHelpCard(color = consentColor) {
                ScannerHelpInfoRow(
                    iconRes = R.drawable.verified_user,
                    title = stringResource(id = R.string.qr_scanner_help_consent_title),
                    detail = stringResource(id = R.string.qr_scanner_help_consent_detail),
                )
            }
            ScannerHelpSection(title = stringResource(id = R.string.qr_scanner_help_tips_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ScannerHelpTip(
                        iconRes = R.drawable.crop_free,
                        text = stringResource(id = R.string.qr_scanner_help_tip_frame),
                    )
                    ScannerHelpTip(
                        iconRes = R.drawable.light_mode,
                        text = stringResource(id = R.string.qr_scanner_help_tip_light),
                    )
                    ScannerHelpTip(
                        iconRes = R.drawable.back_hand,
                        text = stringResource(id = R.string.qr_scanner_help_tip_steady),
                    )
                    ScannerHelpTip(
                        iconRes = R.drawable.verified,
                        text = stringResource(id = R.string.qr_scanner_help_tip_supported),
                    )
                    ScannerHelpTip(
                        iconRes = R.drawable.smartphone,
                        text = stringResource(id = R.string.qr_scanner_help_tip_same_device),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannerHelpHeader(accentColor: Color, modifier: Modifier = Modifier) {
    val iconColor = if (isWalletInDarkTheme()) Green25 else Green100
    val descriptionColor = if (isWalletInDarkTheme()) Brown30 else Brown100

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .background(color = accentColor, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.qr_code_scanner),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(40.dp),
            )
        }
        Text(
            text = stringResource(id = R.string.qr_scanner_help_title),
            style = WalletTextStyle.H1,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(id = R.string.qr_scanner_help_description),
            style = WalletTextStyle.BodyMD,
            color = descriptionColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ScannerHelpSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = title,
            style = WalletTextStyle.H4,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun ScannerHelpCard(
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = color, shape = CardShape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = content,
    )
}

@Composable
private fun ScannerHelpInfoRow(
    @DrawableRes iconRes: Int,
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    val iconColor = if (isWalletInDarkTheme()) Green25 else Green100

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(width = 32.dp, height = 24.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = WalletTextStyle.H5)
            Text(text = detail, style = WalletTextStyle.BodySM, color = iconColor)
        }
    }
}

@Composable
private fun ScannerHelpTip(@DrawableRes iconRes: Int, text: String, modifier: Modifier = Modifier) {
    val iconColor = if (isWalletInDarkTheme()) DefaultLinkDarkMode else DefaultLink

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(width = 32.dp, height = 20.dp),
        )
        Text(text = text, style = WalletTextStyle.BodyMD)
    }
}

@Composable
@PreviewsWallet
private fun ScannerHelpContentPreview() {
    WalletPreview {
        ScannerHelpContent(onCloseClick = {})
    }
}

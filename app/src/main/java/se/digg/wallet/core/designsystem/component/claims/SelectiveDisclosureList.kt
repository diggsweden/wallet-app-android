// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component.claims

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.data.PresentationItem

@Composable
fun SelectiveDisclosureList(
    onClaimClick: (String, Boolean) -> Unit,
    presentationItems: List<PresentationItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.presentation_selective_disclosure_title),
            style = WalletTextStyle.H3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 20.dp),
        )
        presentationItems.forEach { item ->
            SelectiveDisclosureItem(
                item = item,
                onClaimClick = { id, checked ->
                    onClaimClick.invoke(id, checked)
                },
            )
        }
    }
}

@Composable
fun SelectiveDisclosureItem(
    item: PresentationItem,
    onClaimClick: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onClaimClick.invoke(item.id, !item.isChecked) },
        )
        item.claims.forEach {
            ClaimItem(
                claim = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .padding(end = 16.dp),
            )
        }
    }
}

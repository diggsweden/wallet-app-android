// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component.claims

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.time.LocalDate
import se.digg.wallet.core.designsystem.component.DetailList
import se.digg.wallet.core.designsystem.component.DetailRow
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.core.model.ClaimUiModel
import se.digg.wallet.core.model.ClaimValue

@Composable
fun ClaimDetailRow(
    claim: ClaimUiModel,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
) {
    DetailRow(
        label = claim.displayName ?: claim.id,
        modifier = modifier,
        showDivider = showDivider,
    ) {
        ClaimValueContent(value = claim.value)
    }
}

@PreviewsWallet
@Composable
private fun ClaimDetailRowPreview() {
    val claims = listOf(
        ClaimUiModel("given_name", "Förnamn", ClaimValue.TextValue("Elin")),
        ClaimUiModel("birth_date", "Födelsedatum", ClaimValue.DateValue(LocalDate.of(1994, 3, 18))),
        ClaimUiModel("age_over_18", "Över 18 år", ClaimValue.BooleanValue(true)),
    )
    WalletPreview {
        DetailList {
            claims.forEachIndexed { index, claim ->
                ClaimDetailRow(claim = claim, showDivider = index > 0)
            }
        }
    }
}

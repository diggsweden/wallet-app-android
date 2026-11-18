// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.R
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun RoundCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val toggleModifier = if (onCheckedChange != null) {
        Modifier
            .minimumInteractiveComponentSize()
            .toggleable(
                value = checked,
                interactionSource = null,
                indication = ripple(bounded = false, radius = 20.dp),
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
    } else {
        Modifier
    }
    val stateModifier = if (checked) {
        Modifier.background(color = colors.primary, shape = CircleShape)
    } else {
        Modifier.border(width = 2.dp, color = colors.outline, shape = CircleShape)
    }

    Box(
        modifier = modifier
            .then(toggleModifier)
            .alpha(if (enabled) 1f else 0.38f)
            .size(24.dp)
            .then(stateModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painter = painterResource(R.drawable.check),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@PreviewsWallet
@Composable
private fun RoundCheckboxPreview() {
    WalletPreview {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundCheckbox(checked = false, onCheckedChange = {})
            RoundCheckbox(checked = true, onCheckedChange = {})
            RoundCheckbox(checked = false, onCheckedChange = null)
            RoundCheckbox(checked = true, onCheckedChange = null, enabled = false)
        }
    }
}

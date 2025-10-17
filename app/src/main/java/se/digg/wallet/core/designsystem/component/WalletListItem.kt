package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.WalletPreview

private val NONE = 0.dp
private val EXTRA_SMALL = 4.dp
private val SMALL = 8.dp
private val MEDIUM = 12.dp
private val LARGE = 16.dp
private val LARGE_INCREASED = 20.dp
private val EXTRA_LARGE = 28.dp
private val EXTRA_LARGE_INCREASED = 32.dp
private val EXTRA_EXTRA_LARGE = 48.dp
private val FULL = 50.dp

private val cornerRadius = EXTRA_LARGE

@Composable
@WalletPreview
private fun ListItemPreview() {
    WalletTheme {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            ListItem(
                modifier = Modifier.clip(
                    RoundedCornerShape(
                        topStart = cornerRadius,
                        topEnd = cornerRadius,
                        bottomStart = NONE,
                        bottomEnd = NONE
                    )
                ),
                headlineContent = { Text("Headline") },
                supportingContent = { Text("Supporting") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null
                    )
                }
            )
            ListItem(
                headlineContent = { Text("Headline") },
                supportingContent = { Text("Supporting") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null
                    )
                }
            )
            ListItem(
                modifier = Modifier.clip(
                    RoundedCornerShape(
                        topStart = NONE,
                        topEnd = NONE,
                        bottomStart = cornerRadius,
                        bottomEnd = cornerRadius
                    )
                ),
                headlineContent = { Text("Headline") },
                supportingContent = { Text("Supporting") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null
                    )
                }
            )
        }
    }
}
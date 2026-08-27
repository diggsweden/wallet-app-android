// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.R
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

/**
 * Scaffold for a titled, scrollable screen with a back button: the large [title] in the
 * scrollable content fades out as it scrolls under the top bar, while the same title fades in
 * in the [WalletTopAppBar]. Used by settings and its subpages so they all share one title
 * behavior instead of each wiring up [rememberCollapsingTitleState] by hand.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsingTitleScaffold(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val titleState = rememberCollapsingTitleState(scrollState)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            WalletTopAppBar(
                title = {
                    Text(
                        text = title,
                        modifier = Modifier.collapsingAppBarTitle(titleState),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_left),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .collapsingContentTitle(titleState),
            )
            content()
        }
    }
}

@Composable
@PreviewsWallet
private fun CollapsingTitleScaffoldPreview() {
    WalletPreview {
        CollapsingTitleScaffold(title = "Title", onBackClick = {}) {
            Text(text = "Content", modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

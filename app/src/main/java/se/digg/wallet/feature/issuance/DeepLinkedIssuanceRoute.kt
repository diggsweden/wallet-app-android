package se.digg.wallet.feature.issuance

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.github.skydoves.navgraph.annotations.NavDestination
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.WalletTopAppBar
import se.digg.wallet.core.navigation.IssuanceDeepLinkKey

@NavDestination(route = IssuanceDeepLinkKey::class)
@Composable
fun DeepLinkedIssuanceRoute(
    onBackClick: () -> Unit,
    onFinishClick: () -> Unit,
    credentialOfferUri: String,
    modifier: Modifier = Modifier,
) {
    var backVisible by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            WalletTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.issuance_app_bar_title),
                    )
                },
                navigationIcon = {
                    if (backVisible) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_left),
                                contentDescription = null,
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            IssuanceRoute(
                credentialOfferUri = credentialOfferUri,
                onComplete = onFinishClick,
                onDismissibleChange = { backVisible = it },
            )
        }
    }
}

// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.auth.AuthTabIntent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import se.digg.wallet.core.deeplink.DeepLinkHandler
import se.digg.wallet.core.deeplink.DeepLinkResult
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.navigation.IntroKey
import se.digg.wallet.core.navigation.WalletNavDisplay
import se.digg.wallet.core.navigation.WalletNavigator
import se.digg.wallet.core.navigation.toNavKey
import se.digg.wallet.core.oauth.OAuthCoordinator
import se.digg.wallet.core.oauth.ProvideAuthTabLauncher
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var oAuthCoordinator: OAuthCoordinator

    @Inject
    lateinit var deepLinkHandler: DeepLinkHandler

    private val authLauncher =
        AuthTabIntent.registerActivityResultLauncher(this) { result ->
            oAuthCoordinator.onResult(result)
        }

    private fun launchAuthTab(url: Uri, redirectScheme: String) {
        val authTabIntent = AuthTabIntent.Builder().build()
        authTabIntent.launch(authLauncher, url, redirectScheme)
    }

    private var walletNavigator: WalletNavigator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialDeepLinkKey = intent.data?.toNavKey()

        setContent {
            val backStack = rememberNavBackStack(IntroKey)
            val nav = remember { WalletNavigator(backStack) }
            SideEffect { walletNavigator = nav }

            ProvideAuthTabLauncher(launcher = ::launchAuthTab) {
                WalletTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                        AppRoot(
                            navigator = nav,
                            initialDeepLinkKey = initialDeepLinkKey,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (val result = deepLinkHandler.handle(intent)) {
            DeepLinkResult.Consumed -> {
                Timber.d("MainActivity: Deeplink handled by OauthCoordinator")
            }

            is DeepLinkResult.Unhandled -> {
                Timber.d("MainActivity: Deeplink handled by WalletNavigator")
                result.uri.toNavKey()?.let { walletNavigator?.navigate(it) }
            }
        }
    }
}

@Composable
fun AppRoot(
    navigator: WalletNavigator,
    initialDeepLinkKey: NavKey?,
    viewModel: MainActivityViewModel = hiltViewModel(),
) {
    val app by viewModel.onboardingState.collectAsState()
    var prevFlowName by rememberSaveable { mutableStateOf<String?>(null) }
    var initialDeepLinkApplied by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(app.flow) {
        if (app.flow.name == prevFlowName) return@LaunchedEffect
        prevFlowName = app.flow.name
        when (app.flow) {
            AppFlow.Onboarding -> {
                navigator.resetToOnboarding()
            }

            AppFlow.Dashboard -> {
                navigator.resetToDashboard()
                if (!initialDeepLinkApplied && initialDeepLinkKey != null) {
                    navigator.navigate(initialDeepLinkKey)
                    initialDeepLinkApplied = true
                }
            }
        }
    }

    WalletNavDisplay(navigator = navigator)
}

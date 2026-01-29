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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.navigation.WalletNavHost
import se.digg.wallet.core.oauth.OAuthCoordinator
import se.digg.wallet.core.oauth.ProvideAuthTabLauncher
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var OAuthCoordinator: OAuthCoordinator

    private val authLauncher =
        AuthTabIntent.registerActivityResultLauncher(this) { result ->
            OAuthCoordinator.onResult(result)
        }

    private fun launchAuthTab(url: Uri) {
        val authTabIntent = AuthTabIntent.Builder().build()

        authTabIntent.launch(
            authLauncher,
            url,
            "wallet-app"
        )
    }

    lateinit var navHostController: NavHostController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            navHostController = rememberNavController()
            ProvideAuthTabLauncher(launcher = ::launchAuthTab) {
                WalletTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        AppRoot(navHostController = navHostController)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent()
    }

    private fun handleIntent() {
        intent.data?.let { uri ->
            val request = NavDeepLinkRequest.Builder
                .fromUri(uri)
                .build()

            navHostController.navigate(
                request
            )
        }
    }
}

@Composable
fun AppRoot(
    navHostController: NavHostController,
    viewModel: MainActivityViewModel = hiltViewModel(),
) {
    val app by viewModel.enrollmentState.collectAsState()

    when (app.flow) {
        AppFlow.Enrollment -> WalletNavHost(
            navController = navHostController,
            isEnrolled = false
        ) { viewModel.goToDashboard() }

        AppFlow.Dashboard -> WalletNavHost(
            navController = navHostController,
            isEnrolled = true
        ) { viewModel.goToEnrollment() }
    }
}
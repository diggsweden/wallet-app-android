// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.auth.AuthTabIntent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.navigation.WalletNavHost


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    lateinit var navHostController: NavHostController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            navHostController = rememberNavController()
            WalletTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppRoot(navHostController = navHostController, onLaunchAuthTab = { url ->
                        launchAuthTab(url)
                    })
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

    private fun launchAuthTab(uri: String) {
        val authTabIntent = AuthTabIntent.Builder().build()

        authTabIntent.launch(
            mLauncher,
            uri.toUri(),
            "wallet-app"
        )
    }

    private val mLauncher =
        AuthTabIntent.registerActivityResultLauncher(this, this::handleAuthResult)

    private fun handleAuthResult(result: AuthTabIntent.AuthResult) {
        var message = when (result.resultCode) {
            AuthTabIntent.RESULT_OK -> "Received auth result."
            AuthTabIntent.RESULT_CANCELED -> "AuthTab canceled."
            AuthTabIntent.RESULT_VERIFICATION_FAILED -> "Verification failed."
            AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT -> "Verification timed out."
            else -> "Unknown auth result."
        }

        if (result.resultCode == AuthTabIntent.RESULT_OK) {
            message += " Uri: ${result.resultUri}"
        }
    }
}

@Composable
fun AppRoot(
    navHostController: NavHostController,
    viewModel: MainActivityViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onLaunchAuthTab: (String) -> Unit
) {
    val app by viewModel.enrollmentState.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.effects.collect { effect ->
            when (effect) {
                is AuthEffect.LaunchAuthTab ->
                    onLaunchAuthTab.invoke(effect.uri)
            }
        }
    }

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
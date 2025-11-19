package se.digg.wallet

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.navigation.WalletNavHost

class MainActivity : ComponentActivity() {

    lateinit var navHostController: NavHostController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            navHostController = rememberNavController()
            WalletTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppRoot(navHostController)
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
                request,
                //navOptions = NavOptions.Builder().setLaunchSingleTop(true).build()
            )
        }
    }
}

@Composable
fun AppRoot(
    navHostController: NavHostController,
    viewModel: MainActivityViewModel = viewModel(
        factory = MainActivityViewModel.Factory(
            LocalContext.current
        )
    )
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
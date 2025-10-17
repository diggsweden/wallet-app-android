package se.digg.wallet

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
import androidx.navigation.compose.rememberNavController
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.navigation.AppNavHost
import se.digg.wallet.core.navigation.EnrollmentNavHost
import se.digg.wallet.feature.enrollment.pin.PinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WalletTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    AppRoot()
                }
            }
        }
    }
}


@Composable
fun AppRoot(viewModel: MainActivityViewModel = viewModel(factory = MainActivityViewModel.Factory(LocalContext.current))) {
    val app by viewModel.state.collectAsState()

    when (app.flow) {
        AppFlow.Onboarding -> EnrollmentNavHost(
            navController = rememberNavController(),
            onFinish = {
                viewModel.goToDashboard() })

        AppFlow.Dashboard -> AppNavHost(
            navController = rememberNavController(),
            onLogout = {
                viewModel.goToOnboarding() })

        AppFlow.PIN -> TODO()
    }
}
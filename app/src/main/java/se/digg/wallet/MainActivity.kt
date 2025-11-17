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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.navigation.WalletNavHost

class MainActivity : ComponentActivity() {

    lateinit var navigation : NavHostController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            navigation = rememberNavController()
            WalletTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppRoot()
                }
            }
        }
    }
}


@Composable
fun AppRoot(
    viewModel: MainActivityViewModel = viewModel(
        factory = MainActivityViewModel.Factory(
            LocalContext.current
        )
    )
) {
    val app by viewModel.enrollmentState.collectAsState()

    /*
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
     */


    when (app.flow) {
        AppFlow.Enrollment -> WalletNavHost(
            navController = rememberNavController(),
            isEnrolled = false
        ) { viewModel.goToDashboard() }

        AppFlow.Dashboard -> WalletNavHost(
            navController = rememberNavController(),
            isEnrolled = true
        ) { viewModel.goToEnrollment() }

        AppFlow.PIN -> TODO()
    }
}
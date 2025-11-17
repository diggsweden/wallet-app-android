package se.digg.wallet.feature.enrollment

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController

@Composable
fun EnrollmentGraphScopedViewModel(
    navController: NavHostController,
    graphRoute: String = "enrollment_graph"
): OnboardingViewModel {
    val parentEntry = remember(navController, graphRoute) {
        navController.getBackStackEntry(graphRoute)
    }
    return viewModel(parentEntry)

}

@Composable
inline fun <reified VM : ViewModel> graphViewModel(
    navController: NavHostController,
    graphRoute: String
): VM {
    val parentEntry = remember(navController, graphRoute) {
        navController.getBackStackEntry(graphRoute)
    }
    return viewModel(parentEntry) // or hiltViewModel(parentEntry)
}

@Composable
fun OnboardingGraphScopedViewModel(
    parentEntry: NavBackStackEntry
): OnboardingViewModel {
    return viewModel(parentEntry)
}

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun rememberGraphBackStackEntry(
    navController: NavHostController,
    graphRoute: String
): NavBackStackEntry {
    return remember(navController, graphRoute) {
        navController.getBackStackEntry(graphRoute)
    }
}


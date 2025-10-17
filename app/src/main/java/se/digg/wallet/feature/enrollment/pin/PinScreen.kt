package se.digg.wallet.feature.enrollment.pin

import PinInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.feature.presentation.UiEffect
import timber.log.Timber

@Composable
fun PinScreen(
    navController: NavController,
    viewModel: PinViewModel = viewModel(factory = PinViewModel.Factory(LocalContext.current)),
    onContinue: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UiEffect.Verified -> {
                    onContinue.invoke()
                }
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            imageVector = Icons.Filled.Lock,
            contentDescription = "Favorite",
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text( when(uiState){
            PinState.Pin -> "Ange ny PIN-kod"
            PinState.VerifyPin -> "Verifiera din nya PIN-kod"
        }, style = MaterialTheme.typography.headlineMedium)
        PinInput(minLengthToSubmit = 6, onSubmit = {
            viewModel.onPinSubmit(it)
        })
    }
}

@Composable
@WalletPreview
private fun Preview() {
    WalletTheme {
        Surface {
            PinScreen(navController = rememberNavController(), onContinue = {})
        }
    }
}
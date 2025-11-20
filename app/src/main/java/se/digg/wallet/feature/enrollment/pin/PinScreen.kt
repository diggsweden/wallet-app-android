package se.digg.wallet.feature.enrollment.pin

import PinInput
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun PinScreen(
    navController: NavController,
    viewModel: PinViewModel = hiltViewModel(),
    onContinue: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == ORIENTATION_PORTRAIT

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UiEffect.Verified -> {
                    onContinue.invoke()
                }
            }
        }
    }
    if (isPortrait) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Content(uiState = uiState)
            T9Numeric(onPinSubmit = { viewModel.onPinSubmit(it) })
        }
    } else {
        Row {
            Content(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(), uiState = uiState
            )
            T9Numeric(modifier = Modifier.weight(1f), onPinSubmit = { viewModel.onPinSubmit(it) })
        }
    }
}

@Composable
private fun Content(modifier: Modifier = Modifier, uiState: PinState) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            painter = when (uiState) {
                PinState.Pin -> painterResource(R.drawable.lock_open_right_24px)
                PinState.VerifyPin -> painterResource(R.drawable.lock_24px)
            },
            contentDescription = "Lock",
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            when (uiState) {
                PinState.Pin -> stringResource(R.string.enrollment_pin_title)
                PinState.VerifyPin -> stringResource(R.string.enrollment_pin_confirm)
            }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "6 siffror", fontSize = 24.sp
        )
    }
}

@Composable
private fun T9Numeric(modifier: Modifier = Modifier, onPinSubmit: (String) -> Unit) {
    PinInput(modifier = modifier, minLengthToSubmit = 6, onSubmit = {
        onPinSubmit.invoke(it)
    })
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
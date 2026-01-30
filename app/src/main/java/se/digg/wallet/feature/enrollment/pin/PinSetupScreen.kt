package se.digg.wallet.feature.enrollment.pin

import PinInput
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.DiggTextStyle
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet

@Composable
fun PinSetupScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    verifyPin: Boolean = false,
    viewModel: PinSetupViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PinSetupUiEffect.OnNext -> onNext.invoke()
                PinSetupUiEffect.OnGoBack -> onBack.invoke()
            }
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PinSetupScreen(
        uiState = uiState,
        verifyPin = verifyPin,
        onNext = { viewModel.setPin(it) },
        onVerify = { viewModel.checkIfValid(isVerifyScreen = verifyPin, code = it) },
    )
}

@Composable
private fun PinSetupScreen(
    uiState: PinSetupUiState,
    verifyPin: Boolean,
    onNext: (String) -> Unit,
    onVerify: (String) -> Unit,
) {
    var pinCode by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            if (verifyPin) {
                "8. Bekräfta pinkod för identifiering"
            } else {
                "7. Ange pinkod för identifiering"
            },
            style = DiggTextStyle.H1,
        )
        Spacer(Modifier.weight(1f))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = "Pinkod används när du ska identifiera dig",
            style = DiggTextStyle.BodyLG,
        )
        Spacer(Modifier.height(16.dp))
        PinInput(
            onPinChange = {
                pinCode = it
            },
        )

        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(R.string.generic_next),
            onClick = {
                if (verifyPin) {
                    onVerify.invoke(pinCode)
                } else {
                    onNext.invoke(pinCode)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
@PreviewsWallet
private fun PinSetupScreenPreview() {
    WalletTheme {
        Surface {
            PinSetupScreen(
                uiState = PinSetupUiState(),
                verifyPin = true,
                onNext = {},
                onVerify = {},
            )
        }
    }
}

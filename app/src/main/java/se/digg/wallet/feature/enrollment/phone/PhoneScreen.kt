package se.digg.wallet.feature.enrollment.phone

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.OutLinedInput
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.DiggTextStyle
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun PhoneScreen(
    onNext: () -> Unit,
    onSkip: () -> Unit,
    viewModel: PhoneViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PhoneUiEffect.OnNext -> onNext.invoke()
                PhoneUiEffect.OnSkip -> onSkip.invoke()
            }
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PhoneScreen(
        uiState = uiState,
        onInputChanged = { viewModel.onEvent(PhoneUiEvent.PhoneChanged(it)) },
        onNextClicked = { viewModel.onEvent(PhoneUiEvent.NextClicked) },
        onSkipClicked = { viewModel.onEvent(PhoneUiEvent.SkipClicked) }
    )
}

@Composable
private fun PhoneScreen(
    uiState: PhoneUiState,
    onInputChanged: (String) -> Unit,
    onNextClicked: () -> Unit,
    onSkipClicked: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "2. Ditt telefonnummer", textAlign = TextAlign.Center, style = DiggTextStyle.H1,
        )
        Spacer(Modifier.height(70.dp))
        OutLinedInput(
            modifier = Modifier.onFocusChanged { state ->

            },
            value = uiState.phone,
            onValueChange = { onInputChanged.invoke(it) },
            isError = uiState.showError,
            errorText = "Ogiltigt telefonnummer",
            labelText = stringResource(R.string.contact_info_phone_label),
            hintText = stringResource(R.string.contact_info_phone_placeholder),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.generic_next),
            onClick = { onNextClicked.invoke() }
        )
        Spacer(Modifier.height(16.dp))
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onSkipClicked.invoke()
                },
            text = "Hoppa över",
            textDecoration = TextDecoration.Underline,
            style = DiggTextStyle.BodyMD,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
@WalletPreview
private fun PhoneScreenPreview() {
    WalletTheme {
        Surface {
            PhoneScreen(
                uiState = PhoneUiState(),
                onInputChanged = {},
                onNextClicked = {},
                onSkipClicked = {})
        }
    }
}
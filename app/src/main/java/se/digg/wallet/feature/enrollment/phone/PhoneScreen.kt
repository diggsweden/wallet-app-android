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
import se.digg.wallet.core.designsystem.component.OnboardingHeader
import se.digg.wallet.core.designsystem.component.OutLinedInput
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.DiggTextStyle
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet

@Composable
fun PhoneScreen(
    pageNumber: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    viewModel: PhoneViewModel = hiltViewModel(),
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
        pageNumber = pageNumber,
        onInputChange = { viewModel.onEvent(PhoneUiEvent.PhoneChanged(it)) },
        onNext = { viewModel.onEvent(PhoneUiEvent.NextClicked) },
        onSkip = { viewModel.onEvent(PhoneUiEvent.SkipClicked) },
    )
}

@Composable
private fun PhoneScreen(
    uiState: PhoneUiState,
    pageNumber: Int,
    onInputChange: (String) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        OnboardingHeader(pageNumber = pageNumber, pageTitle = "Ditt telefonnummer")
        OutLinedInput(
            value = uiState.phone,
            labelText = stringResource(R.string.contact_info_phone_label),
            onValueChange = { onInputChange.invoke(it) },
            modifier = Modifier.onFocusChanged { state ->
            },
            hintText = stringResource(R.string.contact_info_phone_placeholder),
            errorText = "Ogiltigt telefonnummer",
            isError = uiState.showError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(R.string.generic_next),
            onClick = { onNext.invoke() },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onSkip.invoke()
                },
            text = "Hoppa över",
            textDecoration = TextDecoration.Underline,
            style = DiggTextStyle.BodyMD,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
@PreviewsWallet
private fun PhoneScreenPreview() {
    WalletTheme {
        Surface {
            PhoneScreen(
                uiState = PhoneUiState(),
                pageNumber = 2,
                onInputChange = {},
                onNext = {},
                onSkip = {},
            )
        }
    }
}

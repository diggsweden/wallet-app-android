package se.digg.wallet.feature.enrollment.email

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
fun EmailScreen(onNext: () -> Unit, viewModel: EmailViewModel = hiltViewModel()) {

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EmailUiEffect.OnNext -> onNext.invoke()
            }
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EmailScreen(
        uiState = uiState,
        onNextClicked = { viewModel.onEvent(EmailUiEvent.NextClicked) },
        onEmailChanged = { viewModel.onEvent(EmailUiEvent.EmailChanged(it)) },
        onEmailFocusedChanged = { viewModel.onEvent(EmailUiEvent.EmailFocusedChanged(it)) },
        onVerifyEmailChanged = { viewModel.onEvent(EmailUiEvent.VerifyEmailChanged(it)) },
        onVerifyEmailFocusedChanged = { viewModel.onEvent(EmailUiEvent.VerifyEmailFocusedChanged(it)) })
}

@Composable
private fun EmailScreen(
    uiState: EmailUiState,
    onNextClicked: () -> Unit,
    onEmailChanged: (String) -> Unit,
    onEmailFocusedChanged: (Boolean) -> Unit,
    onVerifyEmailChanged: (String) -> Unit,
    onVerifyEmailFocusedChanged: (Boolean) -> Unit
) {
    val emailErrorCopy = when (uiState.emailError) {
        EmailValidationError.EMPTY -> "Tom epost ej giltig"
        EmailValidationError.NOT_VALID_EMAIL -> "Inte valid epost"
        EmailValidationError.NOT_SAME -> "Inte samma"
        null -> ""
    }

    val verifyEmailErrorCopy = when (uiState.verifyEmailError) {
        EmailValidationError.EMPTY -> "Tom epost ej giltig"
        EmailValidationError.NOT_VALID_EMAIL -> "Inte valid epost"
        EmailValidationError.NOT_SAME -> "Inte samma"
        null -> ""
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "4. Din e-postadress", textAlign = TextAlign.Center, style = DiggTextStyle.H1,
        )
        Spacer(Modifier.height(70.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Image(
                painter = painterResource(R.drawable.pinphone),
                contentDescription = "",
                modifier = Modifier
                    .width(135.dp)
                    .height(161.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Vi behöver din e-postadress för att skapa ett konto. \n" +
                        "Kontot används för att administrera din plånbok om du till exempel skulle tappa din enhet.",
                style = DiggTextStyle.BodyMD,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(48.dp))
        OutLinedInput(
            modifier = Modifier.onFocusChanged { state ->
                onEmailFocusedChanged.invoke(state.isFocused)
            },
            value = uiState.email,
            onValueChange = { onEmailChanged.invoke(it) },
            isError = uiState.emailError != null,
            errorText = emailErrorCopy,
            labelText = "Din e-postadress",
            hintText = stringResource(R.string.contact_info_email_placeholder),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutLinedInput(
            modifier = Modifier.onFocusChanged { state ->
                onVerifyEmailFocusedChanged.invoke(state.isFocused)
            },
            value = uiState.verifyEmail,
            onValueChange = { onVerifyEmailChanged.invoke(it) },
            isError = uiState.verifyEmailError != null,
            errorText = verifyEmailErrorCopy,
            labelText = "Din e-postadress igen",
            hintText = stringResource(R.string.contact_info_email_placeholder),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.generic_next),
            onClick = { onNextClicked.invoke() }
        )
    }
}

@Composable
@WalletPreview
private fun PhoneScreenPreview() {
    WalletTheme {
        Surface {
            EmailScreen(
                uiState = EmailUiState(),
                onNextClicked = {},
                onEmailChanged = {},
                onEmailFocusedChanged = {},
                onVerifyEmailChanged = {},
                onVerifyEmailFocusedChanged = {})
        }
    }
}
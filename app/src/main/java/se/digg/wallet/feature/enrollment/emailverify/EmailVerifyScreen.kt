package se.digg.wallet.feature.enrollment.emailverify

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.ConfirmCode
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.DiggTextStyle
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun EmailVerifyScreen(onNext: () -> Unit, viewModel: EmailVerifyViewModel = hiltViewModel()) {

    LaunchedEffect(Unit) {
        viewModel.setEmail()
        viewModel.effects.collect { effect ->
            when (effect) {
                EmailVerifyUiEffect.OnNext -> onNext.invoke()
            }
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EmailVerifyScreen(
        uiState = uiState,
        onNextClicked = { viewModel.onEvent(EmailVerifyUiEvent.NextClicked) },
        onCodeChanged = { viewModel.onEvent(EmailVerifyUiEvent.CodeChanged(it)) })
}

@Composable
private fun EmailVerifyScreen(
    uiState: EmailVerifyUiState,
    onNextClicked: () -> Unit,
    onCodeChanged: (String) -> Unit
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
            modifier = Modifier.fillMaxWidth(),
            text = "6. Bekräfta e-postadress",
            style = DiggTextStyle.H1,
        )
        Spacer(Modifier.height(70.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                text = "En kod för att bekräfta din e-postadress har skickats till ${uiState.email}",
                style = DiggTextStyle.BodyMD,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                painter = painterResource(R.drawable.pinphone),
                contentDescription = "",
                modifier = Modifier
                    .width(135.dp)
                    .height(161.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Det kan ta några minuter innan du får din kod, den är aktiv i en timme. \n\n" +
                    "Kom inte koden gå ett steg tillbaka.",
            style = DiggTextStyle.BodySM
        )
        Spacer(Modifier.height(64.dp))

        ConfirmCode(
            value = uiState.code,
            onValueChange = { onCodeChanged.invoke(it) },
            length = 6,
            onDone = { /* submit code */ }
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.generic_next),
            onClick = {
                onNextClicked.invoke()
            }
        )
    }
}

@Composable
@WalletPreview
private fun PhoneScreenPreview() {
    WalletTheme {
        Surface {
            EmailVerifyScreen(
                uiState = EmailVerifyUiState(email = "test@digg.se"),
                onNextClicked = {},
                onCodeChanged = {})
        }
    }
}
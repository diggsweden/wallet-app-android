package se.digg.wallet.feature.enrollment.login

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.DiggTextStyle
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.core.oauth.LocalAuthTabLauncher

@Composable
fun LoginScreen(
    onLoginSuccessful: (String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {

    val launchAuthTab = LocalAuthTabLauncher.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LoginUiEffect.OnLoginSuccessful -> {
                    onLoginSuccessful.invoke(effect.sessionId)
                }
            }
        }
    }

    LoginScreen(onLoginClicked = { viewModel.authorize(launchAuthTab) })
}

@Composable
private fun LoginScreen(onLoginClicked: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "2. Logga in", style = DiggTextStyle.H1,
        )
        Spacer(Modifier.height(70.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "För att kunna registrera ett konto behöver du först logga in",
            style = DiggTextStyle.BodyMD
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Logga in",
            onClick = {
                onLoginClicked.invoke()
            }
        )
    }
}

@Composable
@WalletPreview
private fun LoginScreenPreview() {
    WalletTheme {
        Surface {
            LoginScreen(onLoginClicked = {})
        }
    }
}


package se.digg.wallet.feature.enrollment.consent

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.DiggTextStyle
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet

@Composable
fun ConsentScreen(onNext: () -> Unit, viewModel: ConsentViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ConsentUiEffect.OnNext -> onNext.invoke()
            }
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ConsentScreen(
        uiState = uiState,
        onNext = { viewModel.onEvent(ConsentUiEvent.NextClicked) },
        onConsent = {
            viewModel.onEvent(
                ConsentUiEvent.ConsentChanged(it),
            )
        },
    )
}

@Composable
private fun ConsentScreen(
    uiState: ConsentUiState,
    onNext: () -> Unit,
    onConsent: (Boolean) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "1. Tillåt behörigheter",
            textAlign = TextAlign.Center,
            style = DiggTextStyle.H1,
        )
        Spacer(Modifier.height(70.dp))
        Row(Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(R.drawable.handshake),
                contentDescription = "",
            )
            Spacer(Modifier.width(10.dp))
            Checkbox(
                modifier = Modifier.size(20.dp),
                checked = uiState.hasConsent,
                onCheckedChange = { isChecked ->
                    onConsent.invoke(isChecked)
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    disabledCheckedColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledUncheckedColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "Samtycke",
                    style = DiggTextStyle.BodyMD,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Ja, jag samtycker till att Digg får lagra mina användar-uppgifter såsom telefonnummer och e-postadress",
                    style = DiggTextStyle.BodyMD,
                )
                if (uiState.showError) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Samtycke krävs för att du ska kunna använda plånboken",
                        style = DiggTextStyle.BodySM,
                        color = Color(0xFFB50000),
                    )
                }
            }
        }
        Spacer(Modifier.height(40.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Läs mer om användarvillkor",
                style = DiggTextStyle.BodyMD,
                textDecoration = TextDecoration.Underline,
                color = Color(0xFF556951),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.open_in_new),
                contentDescription = null,
                tint = Color(0xFF556951),
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Så behandlar vi dina personuppgifter",
                style = DiggTextStyle.BodyMD,
                textDecoration = TextDecoration.Underline,
                color = Color(0xFF556951),

            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.open_in_new),
                contentDescription = null,
                tint = Color(0xFF556951),
            )
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(R.string.generic_next),
            onClick = { onNext.invoke() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
@PreviewsWallet
private fun ConsentScreenPreview() {
    WalletTheme {
        Surface {
            ConsentScreen(uiState = ConsentUiState(), onNext = {}, onConsent = {})
        }
    }
}

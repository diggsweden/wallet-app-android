package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import se.digg.wallet.core.designsystem.theme.DiggTextStyle

@Composable
fun CredentialOfferHeader(issuer: CredentialIssuerMetadata?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .wrapContentSize()
                    .heightIn(max = 200.dp, min = 100.dp),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = issuer
                    ?.display
                    ?.first()
                    ?.logo
                    ?.uri
                    .toString(),
                contentDescription = "-",
            )
        }
        Spacer(modifier = Modifier.height(36.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = "Utfärdare:",
            style = DiggTextStyle.H3,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = issuer?.display?.first()?.name ?: "-",
            style = DiggTextStyle.BodyMD,
        )
    }
}

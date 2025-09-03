package se.digg.wallet.feature.dashboard

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import se.digg.wallet.R
import se.digg.wallet.core.ui.theme.WalletTheme

const val CREDENTIAL_URL = "https://wallet.sandbox.digg.se/prepare-credential-offer"
const val PRESENTATION_URL = "https://wallet.sandbox.digg.se/strumpsorteringscentralen/"

@Composable
fun DashboardScreen(navController: NavController, viewModel: DashboardViewModel = viewModel()) {

    val credential by viewModel.credential.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Header()
                credential?.let { credentialData ->
                    CredentialCard(credentialData.jwt)
                }
                NewCredentialCard()
                PresentationCard()
            }
        }
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .padding(top = 16.dp)
            .padding(bottom = 8.dp)
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_logo_full),
            contentDescription = "App icon",
            colorFilter = null
        )
        Text(
            fontSize = 24.sp,
            text = stringResource(R.string.homescreen_welcome),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            text = stringResource(R.string.homescreen_content)
        )
    }
}

@Composable
private fun CredentialCard(credentialJwt: String) {
    Column(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            onClick = {

            },
            colors = CardDefaults.cardColors(
                containerColor = colorResource(id = R.color.digg_primary).copy(
                    alpha = 0.2f
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .height(150.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(credentialJwt)
            }
        }
    }
}

@Composable
private fun NewCredentialCard() {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val context = LocalContext.current
        Card(
            onClick = {
                val context = context
                val intent = Intent(Intent.ACTION_VIEW, CREDENTIAL_URL.toUri())
                context.startActivity(intent)
            },
            colors = CardDefaults.cardColors(
                containerColor = colorResource(id = R.color.digg_primary).copy(
                    alpha = 0.2f
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .height(150.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Add new credential")
            }
        }
    }
}

@Composable
private fun PresentationCard(){
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val context = LocalContext.current
        Card(
            onClick = {
                val context = context
                val intent = Intent(Intent.ACTION_VIEW, PRESENTATION_URL.toUri())
                context.startActivity(intent)
            },
            colors = CardDefaults.cardColors(
                containerColor = colorResource(id = R.color.digg_primary).copy(
                    alpha = 0.2f
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .height(150.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Presentation")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    WalletTheme {

    }
}
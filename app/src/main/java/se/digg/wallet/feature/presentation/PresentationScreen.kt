package se.digg.wallet.feature.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import se.digg.wallet.R
import se.digg.wallet.core.ui.theme.WalletTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentationScreen(
    navController: NavController,
    fullUri: String,
    viewModel: PresentationViewModel = viewModel()
) {
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        viewModel.init(fullUri)

        viewModel.effects.collect { effect ->
            when (effect) {
                is UiEffect.OpenUrl -> {
                    uriHandler.openUri(effect.url)
                    navController.popBackStack()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        {
            TopAppBar(title = {
                Text(
                    text = "Presenting"
                )
            }, navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = ""
                    )
                }
            })
        }) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Header()
                Spacer(Modifier.height(4.dp))
                Disclosures()
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
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
                    .fillMaxWidth(),
            ) {
                Text("Do you want to share data with EUDI Wallet Reference Implementation?")
            }
        }
    }
}

@Composable
private fun Disclosures() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
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
                    .fillMaxWidth(),
            ) {
                Text("Disclosures to share:")
                LockedFieldWithCheckbox(modifier = Modifier,label ="Birth Date", value = "FC", checked = true, onCheckedChange = {})
                LockedFieldWithCheckbox(modifier = Modifier,label ="Issuing Country", value = "adasda", checked = true, onCheckedChange = {})
                LockedFieldWithCheckbox(modifier = Modifier,label ="Family Name(s)", value = "AFASDSD", checked = true, onCheckedChange = {})
                LockedFieldWithCheckbox(modifier = Modifier,label ="Issuance Authority", value = "GASdASD", checked = true, onCheckedChange = {})
                LockedFieldWithCheckbox(modifier = Modifier,label ="Given Name(s)", value = "gasdadfa", checked = true, onCheckedChange = {})
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PresentationPreview() {
    WalletTheme {

    }
}

@Composable
fun LockedFieldWithCheckbox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = value,
            label = {Text(label)},
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
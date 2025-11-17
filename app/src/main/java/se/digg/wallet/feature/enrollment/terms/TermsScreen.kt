package se.digg.wallet.feature.enrollment.terms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun TermsScreen(
    navController: NavController,
    viewModel: TermsViewModel = viewModel(),
    onContinue: () -> Unit
) {

    var isChecked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.enrollment_terms_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            stringResource(R.string.enrollment_terms_description),
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Checkbox(onCheckedChange = { isChecked = it }, checked = isChecked)
            Text(stringResource(R.string.enrollment_terms_checkbox_description))
        }
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.generic_continue),
            enabled = isChecked,
            onClick = {
                onContinue.invoke()
            })
    }
}

@Composable
@WalletPreview
private fun Preview() {
    WalletTheme {
        Surface {
            TermsScreen(rememberNavController(), onContinue = {})
        }
    }
}
package se.digg.wallet.feature.enrollment.pid

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collectLatest
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.DiggBlack
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.feature.dashboard.CREDENTIAL_URL

@Composable
fun PidScreen(
    navController: NavController,
    onContinue: () -> Unit,
    viewModel: PidViewModel = hiltViewModel()

) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.credential.collectLatest { credential ->
            if (credential!=null){
                onContinue.invoke()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            modifier = Modifier.size(48.dp),
            painter = painterResource(R.drawable.id_card_24px),
            contentDescription = "Lock",
            tint = DiggBlack
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            stringResource(R.string.enrollment_pid_title),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.enrollment_pid_description))
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            modifier = Modifier.padding(bottom = 16.dp),
            text = stringResource(R.string.generic_next),
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, CREDENTIAL_URL.toUri())
                context.startActivity(intent)
            })
    }
}

@Composable
@WalletPreview
private fun Preview() {
    WalletTheme {
        Surface {
            PidScreen(
                navController = rememberNavController(),
                onContinue = {})
        }
    }
}
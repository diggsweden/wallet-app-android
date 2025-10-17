package se.digg.wallet.feature.enrollment.contactinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collectLatest
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun ContactInfoScreen(
    navController: NavController,
    onContinue: () -> Unit,
    viewModel: ContactInfoViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.done.collectLatest { onContinue() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(modifier = Modifier.size(48.dp),
            imageVector = Icons.Filled.Person,
            contentDescription = "Favorite",
            tint = MaterialTheme.colorScheme.primary
        )

        Text("Kontaktuppgifter", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.email,
            onValueChange = { viewModel.onEvent(ContactEvent.EmailChanged(it)) },
            label = { Text("E-post") },
            isError = state.emailError != null,
            supportingText = { if (state.emailError != null) Text(state.emailError!!) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focus.clearFocus()
                    viewModel.onEvent(ContactEvent.SubmitClicked)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.verifyEmail,
            onValueChange = { viewModel.onEvent(ContactEvent.VerifyEmailChanged(it)) },
            label = { Text("Verifiera e-post") },
            isError = state.verifyEmailError != null,
            supportingText = { if (state.verifyEmailError != null) Text(state.verifyEmailError!!) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focus.clearFocus()
                    viewModel.onEvent(ContactEvent.SubmitClicked)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.phone,
            onValueChange = { viewModel.onEvent(ContactEvent.PhoneChanged(it)) },
            label = { Text("Telefonnummer") },
            isError = state.phoneError != null,
            supportingText = { if (state.phoneError != null) Text(state.phoneError!!) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )


    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Button(
            onClick = {
                focus.clearFocus()
                viewModel.onEvent(ContactEvent.SubmitClicked)
            },
            enabled = state.isValid && !state.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp)
                )
            }
            Text("Fortsätt")
        }
    }
}

@Composable
@WalletPreview
private fun Preview() {
    WalletTheme {
        Surface {
            ContactInfoScreen(
                navController = rememberNavController(),
                onContinue = {})
        }
    }
}


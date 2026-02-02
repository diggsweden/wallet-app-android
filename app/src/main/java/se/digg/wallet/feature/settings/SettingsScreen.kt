// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.settings

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collectLatest
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                SettingsViewModel.UiEvent.LocalStorageCleared -> {
                    onLogout.invoke()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_left),
                            contentDescription = "",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            ) {
                SettingsHeader()
                SettingsContent(onLogoutClick = { viewModel.onLogout() })
            }
        }
    }
}

@Composable
private fun SettingsHeader() {
    val context = LocalContext.current
    val info = remember { getAppVersionInfoCompat(context) }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 64.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.playstore_icon),
            contentDescription = "Logo",
            modifier = Modifier
                .width(160.dp)
                .height(160.dp),
        )
        Text(
            stringResource(R.string.settings_app_version, info.versionName, info.versionCode),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SettingsContent(onLogoutClick: () -> Unit) {
    PrimaryButton(
        text = stringResource(R.string.settings_logout),
        onClick = {
            onLogoutClick.invoke()
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

data class AppVersionInfo(val versionName: String, val versionCode: Long)

fun getAppVersionInfoCompat(context: Context): AppVersionInfo {
    val pm = context.packageManager
    val packageName = context.packageName

    val pkgInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(packageName, 0)
    }
    val name = pkgInfo.versionName ?: "Unknown"
    val code = pkgInfo.longVersionCode
    return AppVersionInfo(name, code)
}

@Composable
@PreviewsWallet
private fun Preview() {
    WalletTheme {
        Surface {
            SettingsScreen(navController = rememberNavController(), onLogout = {})
        }
    }
}

// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.settings

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.WalletPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        {
            TopAppBar(title = {
                Text(
                    text = "Settings"
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
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                SettingsHeader()
                SettingsContent()
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.playstore_icon),
            contentDescription = "Logo",
            modifier = Modifier
                .width(160.dp)
                .height(160.dp)
        )
        Text(
            "App version: ${info.versionName}:${info.versionCode}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SettingsContent() {
    ListItem(
        modifier = Modifier.clickable(true, onClick = {}),
        headlineContent = { Text("Licenses") })
    Spacer(Modifier.height(12.dp))
    Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Logout")}
}

data class AppVersionInfo(
    val versionName: String,
    val versionCode: Long
)

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
@WalletPreview
private fun Preview() {
    WalletTheme {
        Surface {
            SettingsScreen(navController = rememberNavController())
        }
    }
}
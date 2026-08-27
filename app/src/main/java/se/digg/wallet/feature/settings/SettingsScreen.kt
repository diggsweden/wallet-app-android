// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import se.digg.wallet.BuildConfig
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.AppVersionText
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.component.collapsingAppBarTitle
import se.digg.wallet.core.designsystem.component.collapsingContentTitle
import se.digg.wallet.core.designsystem.component.rememberCollapsingTitleState
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.core.designsystem.utils.getDeviceInfo

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onAbout: () -> Unit,
    onLanguage: () -> Unit,
    onTheme: () -> Unit,
    onHelp: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                SettingsUiEvent.LocalStorageCleared -> {
                    onLogout.invoke()
                }
            }
        }
    }

    SettingsScreen(
        onBackClick = onBack,
        onLogoutClick = { viewModel.onLogout() },
        onFeedbackClick = { openFeedbackEmail(context) },
        onDevicePermissionsClick = { openAppPermissionsSettings(context) },
        onHelpClick = onHelp,
        onAboutClick = onAbout,
        onLanguageClick = onLanguage,
        onThemeClick = onTheme,
    )
}

private const val FEEDBACK_EMAIL_ADDRESS = "test@test.com"

// ACTION_SENDTO (not ACTION_SEND) so only email apps offer to handle this, not every
// text/plain-sharing app.
private fun openFeedbackEmail(context: Context) {
    val deviceInfo = getDeviceInfo(context)
    val body = context.getString(
        R.string.settings_feedback_email_body,
        deviceInfo.appVersionName,
        deviceInfo.appVersionCode,
        deviceInfo.brand,
        deviceInfo.model,
        deviceInfo.osVersion,
        deviceInfo.sdkVersion,
        deviceInfo.networkType.name,
    )
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:".toUri()
        putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL_ADDRESS))
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.settings_feedback_email_subject))
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No email app installed; nothing to fall back to.
    }
}

// There is no public API to open directly on the Permissions sub-screen; App Info is the
// standard, documented entry point and puts "Permissions" one tap away.
private fun openAppPermissionsSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
    context.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onDevicePermissionsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onAboutClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onThemeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val titleState = rememberCollapsingTitleState(scrollState)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        modifier = Modifier.collapsingAppBarTitle(titleState),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBackClick.invoke() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_left),
                            contentDescription = null,
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
                    .verticalScroll(scrollState)
                    .padding(innerPadding),
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .collapsingContentTitle(titleState),
                )
                SettingsHeader()
                SettingsMenu(
                    onFeedbackClick = onFeedbackClick,
                    onDevicePermissionsClick = onDevicePermissionsClick,
                    onHelpClick = onHelpClick,
                    onAboutClick = onAboutClick,
                    onLanguageClick = onLanguageClick,
                    onThemeClick = onThemeClick,
                )
                SettingsContent(onLogoutClick = { onLogoutClick.invoke() })
            }
        }
    }
}

@Composable
private fun SettingsHeader() {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 32.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.playstore_icon),
            contentDescription = null,
            modifier = Modifier
                .width(160.dp)
                .height(160.dp),
        )
        AppVersionText(variant = BuildConfig.FLAVOR.takeIf { BuildConfig.DEBUG })
    }
}

@Composable
private fun SettingsMenu(
    onFeedbackClick: () -> Unit,
    onDevicePermissionsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onAboutClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onThemeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsDetailedListItem(
            iconRes = R.drawable.feedback_24px,
            title = stringResource(R.string.settings_feedback_title),
            description = stringResource(R.string.settings_feedback_description),
            onClick = onFeedbackClick,
            trailingIconRes = R.drawable.mail_24px,
        )
        HorizontalDivider()
        SettingsDetailedListItem(
            iconRes = R.drawable.admin_panel_settings_24px,
            title = stringResource(R.string.settings_app_info_title),
            description = stringResource(R.string.settings_app_info_description),
            onClick = onDevicePermissionsClick,
        )
        HorizontalDivider()
        SettingsRows(
            listOf(
                SettingsRowSpec(
                    iconRes = R.drawable.language_24px,
                    label = stringResource(R.string.settings_language),
                    onClick = onLanguageClick,
                ),
                SettingsRowSpec(
                    iconRes = R.drawable.contrast_24px,
                    label = stringResource(R.string.settings_theme),
                    onClick = onThemeClick,
                ),
            ),
        )
        HorizontalDivider()
        SettingsRows(
            listOf(
                SettingsRowSpec(
                    iconRes = R.drawable.help_24px,
                    label = stringResource(R.string.settings_help),
                    onClick = onHelpClick,
                ),
                SettingsRowSpec(
                    iconRes = R.drawable.info_24px,
                    label = stringResource(R.string.settings_about),
                    onClick = onAboutClick,
                ),
            ),
        )
    }
}

private data class SettingsRowSpec(
    @DrawableRes val iconRes: Int,
    val label: String,
    val onClick: () -> Unit,
)

@Composable
private fun SettingsRows(items: List<SettingsRowSpec>) {
    items.forEachIndexed { index, item ->
        SettingsListItem(iconRes = item.iconRes, label = item.label, onClick = item.onClick)
        if (index != items.lastIndex) {
            HorizontalDivider()
        }
    }
}

@Composable
private fun SettingsCategoryHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingsListItem(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.arrow_right),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsDetailedListItem(
    @DrawableRes iconRes: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes trailingIconRes: Int = R.drawable.arrow_right,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            painter = painterResource(trailingIconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun SettingsContent(onLogoutClick: () -> Unit) {
    var showSignOutDialog by remember { mutableStateOf(false) }

    PrimaryButton(
        text = stringResource(R.string.settings_logout),
        onClick = { showSignOutDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 32.dp, bottom = 32.dp),
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
    )

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(text = stringResource(R.string.settings_logout_dialog_title)) },
            text = { Text(text = stringResource(R.string.settings_logout_dialog_description)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onLogoutClick.invoke()
                    },
                ) {
                    Text(text = stringResource(R.string.settings_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(text = stringResource(R.string.generic_cancel))
                }
            },
        )
    }
}

@Composable
@PreviewsWallet
private fun SettingsScreenPreview() {
    WalletPreview {
        SettingsScreen(
            onBackClick = {},
            onLogoutClick = {},
            onFeedbackClick = {},
            onDevicePermissionsClick = {},
            onHelpClick = {},
            onAboutClick = {},
            onLanguageClick = {},
            onThemeClick = {},
        )
    }
}

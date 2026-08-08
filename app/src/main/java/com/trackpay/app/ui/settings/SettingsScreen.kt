package com.trackpay.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackpay.app.BuildConfig
import com.trackpay.app.R
import com.trackpay.app.ui.util.CurrencyFormat

@Composable
fun SettingsRoute(
    onOpenJobs: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onOpenJobs = onOpenJobs,
        onOpenThemes = onOpenThemes,
        onOpenPrivacy = onOpenPrivacy,
        onOpenAbout = onOpenAbout,
        onCurrencySelected = viewModel::onCurrencySelected,
        onGeoMasterChanged = viewModel::onGeoMasterChanged,
        onLiveNotificationChanged = viewModel::onLiveNotificationChanged,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpenJobs: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    onCurrencySelected: (String) -> Unit,
    onGeoMasterChanged: (Boolean) -> Unit,
    onLiveNotificationChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var currencyExpanded by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background == androidx.compose.ui.graphics.Color(0xFF1A1110)
    val surfaceBg = if (isDark) androidx.compose.ui.graphics.Color(0xFF1A1110) else MaterialTheme.colorScheme.background

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = androidx.compose.ui.text.font.FontWeight.Medium) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = surfaceBg),
            )
        },
        containerColor = surfaceBg,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSectionHeader(stringResource(R.string.settings_section_work))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_jobs_link)) },
                supportingContent = {
                    val supporting = when {
                        state.jobCount == 0 -> stringResource(R.string.settings_jobs_none)
                        state.defaultJobName != null -> stringResource(
                            R.string.settings_jobs_summary,
                            state.jobCount,
                            state.defaultJobName,
                        )
                        else -> stringResource(R.string.settings_jobs_count, state.jobCount)
                    }
                    Text(supporting)
                },
                leadingContent = {
                    Icon(Icons.Default.Work, contentDescription = null)
                },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenJobs),
            )
            Text(
                text = stringResource(R.string.settings_ot_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SettingsSectionHeader(stringResource(R.string.settings_section_preferences))

            ExposedDropdownMenuBox(
                expanded = currencyExpanded,
                onExpandedChange = { currencyExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                OutlinedTextField(
                    value = CurrencyFormat.displayName(state.currencyCode),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_currency)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = currencyExpanded,
                    onDismissRequest = { currencyExpanded = false },
                ) {
                    state.currencyCodes.forEach { code ->
                        DropdownMenuItem(
                            text = { Text(CurrencyFormat.displayName(code)) },
                            onClick = {
                                currencyExpanded = false
                                onCurrencySelected(code)
                            },
                        )
                    }
                }
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_themes_link)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_themes_summary))
                },
                leadingContent = {
                    Icon(Icons.Default.Palette, contentDescription = null)
                },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenThemes),
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SettingsSectionHeader(stringResource(R.string.settings_section_notifications))

            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.settings_live_notification))
                },
                supportingContent = {
                    Text(stringResource(R.string.settings_live_notification_summary))
                },
                leadingContent = {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = state.liveNotificationEnabled,
                        onCheckedChange = onLiveNotificationChanged,
                    )
                },
            )
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.settings_system_notifications))
                },
                supportingContent = {
                    Text(stringResource(R.string.settings_system_notifications_summary))
                },
                leadingContent = {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent().apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            } else {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        }
                        context.startActivity(intent)
                    },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SettingsSectionHeader(stringResource(R.string.settings_section_location))

            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.settings_geo_master))
                },
                supportingContent = {
                    Text(stringResource(R.string.settings_geo_master_summary))
                },
                leadingContent = {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = state.geoMasterEnabled,
                        onCheckedChange = onGeoMasterChanged,
                    )
                },
            )
            Text(
                text = stringResource(R.string.settings_geo_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.settings_geo_per_job))
                },
                supportingContent = {
                    Text(stringResource(R.string.settings_geo_per_job_summary))
                },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenJobs),
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SettingsSectionHeader(stringResource(R.string.settings_section_support))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_privacy)) },
                leadingContent = {
                    Icon(Icons.Default.Policy, contentDescription = null)
                },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPrivacy),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_about)) },
                supportingContent = {
                    Text(
                        stringResource(
                            R.string.settings_about_summary,
                            BuildConfig.VERSION_NAME,
                        ),
                    )
                },
                leadingContent = {
                    Icon(Icons.Default.Info, contentDescription = null)
                },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAbout),
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

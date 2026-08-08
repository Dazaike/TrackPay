package com.trackpay.app.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackpay.app.R
import com.trackpay.app.domain.model.GoalTemplate
import com.trackpay.app.ui.util.CurrencyFormat
import com.trackpay.app.ui.util.currencySymbol
import com.trackpay.app.ui.util.formatMoney

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) {
        if (state.finished) onFinished()
    }
    OnboardingScreen(
        state = state,
        onJobNameChange = viewModel::onJobNameChange,
        onHourlyRateChange = viewModel::onHourlyRateChange,
        onCurrencySelected = viewModel::onCurrencySelected,
        onSelectTemplate = viewModel::onSelectTemplate,
        onSelectCustomGoal = viewModel::onSelectCustomGoal,
        onCustomGoalNameChange = viewModel::onCustomGoalNameChange,
        onCustomGoalTargetChange = viewModel::onCustomGoalTargetChange,
        onNextWelcome = viewModel::nextFromWelcome,
        onNextJob = viewModel::nextFromJob,
        onNextGoal = viewModel::nextFromGoal,
        onSkipGoal = viewModel::skipGoal,
        onBack = viewModel::back,
        onFinish = viewModel::finish,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onJobNameChange: (String) -> Unit,
    onHourlyRateChange: (String) -> Unit,
    onCurrencySelected: (String) -> Unit,
    onSelectTemplate: (GoalTemplate?) -> Unit,
    onSelectCustomGoal: () -> Unit,
    onCustomGoalNameChange: (String) -> Unit,
    onCustomGoalTargetChange: (String) -> Unit,
    onNextWelcome: () -> Unit,
    onNextJob: () -> Unit,
    onNextGoal: () -> Unit,
    onSkipGoal: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* optional — finish either way */ }

    val stepIndex = when (state.step) {
        OnboardingStep.Welcome -> 0
        OnboardingStep.CreateJob -> 1
        OnboardingStep.OptionalGoal -> 2
        OnboardingStep.Permissions -> 3
    }
    val progress = (stepIndex + 1) / 4f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.onboarding_title)) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            when (state.step) {
                OnboardingStep.Welcome -> WelcomeStep(onNext = onNextWelcome)
                OnboardingStep.CreateJob -> CreateJobStep(
                    name = state.jobName,
                    rate = state.hourlyRateText,
                    currencyCode = state.currencyCode,
                    currencyCodes = state.currencyCodes,
                    error = state.errorMessage,
                    onNameChange = onJobNameChange,
                    onRateChange = onHourlyRateChange,
                    onCurrencySelected = onCurrencySelected,
                    onBack = onBack,
                    onNext = onNextJob,
                )
                OnboardingStep.OptionalGoal -> OptionalGoalStep(
                    templates = state.templates,
                    selected = state.selectedTemplate,
                    customSelected = state.customGoalSelected,
                    customName = state.customGoalName,
                    customTarget = state.customGoalTargetText,
                    hasSelection = state.hasGoalSelection,
                    error = state.errorMessage,
                    onSelect = onSelectTemplate,
                    onSelectCustom = onSelectCustomGoal,
                    onCustomNameChange = onCustomGoalNameChange,
                    onCustomTargetChange = onCustomGoalTargetChange,
                    onBack = onBack,
                    onSkip = onSkipGoal,
                    onNext = onNextGoal,
                )
                OnboardingStep.Permissions -> PermissionsStep(
                    saving = state.saving,
                    error = state.errorMessage,
                    onBack = onBack,
                    onRequestPermissions = {
                        val perms = buildList {
                            if (Build.VERSION.SDK_INT >= 33) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            add(Manifest.permission.ACCESS_COARSE_LOCATION)
                            add(Manifest.permission.ACCESS_FINE_LOCATION)
                        }.toTypedArray()
                        if (perms.isNotEmpty()) {
                            permissionLauncher.launch(perms)
                        }
                    },
                    onFinish = onFinish,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.onboarding_welcome_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = stringResource(R.string.onboarding_welcome_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = onNext,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.onboarding_get_started))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateJobStep(
    name: String,
    rate: String,
    currencyCode: String,
    currencyCodes: List<String>,
    error: String?,
    onNameChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onCurrencySelected: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    var currencyExpanded by remember { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.onboarding_job_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = stringResource(R.string.onboarding_job_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.jobs_name)) },
        singleLine = true,
    )
    ExposedDropdownMenuBox(
        expanded = currencyExpanded,
        onExpandedChange = { currencyExpanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = CurrencyFormat.displayName(currencyCode),
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
            currencyCodes.forEach { code ->
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
    OutlinedTextField(
        value = rate,
        onValueChange = onRateChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.jobs_hourly_rate)) },
        singleLine = true,
        prefix = { Text(currencySymbol()) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.action_back))
        }
        Button(onClick = onNext) {
            Text(stringResource(R.string.action_continue))
        }
    }
}

@Composable
private fun OptionalGoalStep(
    templates: List<GoalTemplate>,
    selected: GoalTemplate?,
    customSelected: Boolean,
    customName: String,
    customTarget: String,
    hasSelection: Boolean,
    error: String?,
    onSelect: (GoalTemplate?) -> Unit,
    onSelectCustom: () -> Unit,
    onCustomNameChange: (String) -> Unit,
    onCustomTargetChange: (String) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    Text(
        text = stringResource(R.string.onboarding_goal_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = stringResource(R.string.onboarding_goal_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    templates.forEach { template ->
        val isSelected = !customSelected && selected?.name == template.name
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onSelect(if (isSelected) null else template)
                },
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = formatMoney(template.defaultTargetMinor),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectCustom),
        colors = CardDefaults.cardColors(
            containerColor = if (customSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_goal_custom),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.onboarding_goal_custom_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (customSelected) {
                OutlinedTextField(
                    value = customName,
                    onValueChange = onCustomNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.onboarding_goal_custom_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = customTarget,
                    onValueChange = onCustomTargetChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.onboarding_goal_custom_target)) },
                    singleLine = true,
                    prefix = { Text(currencySymbol()) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }
    }

    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.action_back))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.onboarding_skip))
            }
            Button(onClick = onNext, enabled = hasSelection) {
                Text(stringResource(R.string.action_continue))
            }
        }
    }
}

@Composable
private fun PermissionsStep(
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRequestPermissions: () -> Unit,
    onFinish: () -> Unit,
) {
    Text(
        text = stringResource(R.string.onboarding_permissions_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = stringResource(R.string.onboarding_permissions_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = stringResource(R.string.onboarding_permissions_location_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Button(
        onClick = {
            onRequestPermissions()
            onFinish()
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !saving,
    ) {
        Text(
            if (saving) {
                stringResource(R.string.onboarding_saving)
            } else {
                stringResource(R.string.onboarding_allow_permissions)
            },
        )
    }
    TextButton(
        onClick = onFinish,
        modifier = Modifier.fillMaxWidth(),
        enabled = !saving,
    ) {
        Text(stringResource(R.string.onboarding_not_now))
    }
    TextButton(onClick = onBack, enabled = !saving) {
        Text(stringResource(R.string.action_back))
    }
    Button(
        onClick = onFinish,
        modifier = Modifier.fillMaxWidth(),
        enabled = !saving,
    ) {
        Text(
            if (saving) {
                stringResource(R.string.onboarding_saving)
            } else {
                stringResource(R.string.onboarding_finish)
            },
        )
    }
}

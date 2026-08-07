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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.trackpay.app.ui.util.MoneyFormat

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
        onSelectTemplate = viewModel::onSelectTemplate,
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
    onSelectTemplate: (GoalTemplate?) -> Unit,
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
                    error = state.errorMessage,
                    onNameChange = onJobNameChange,
                    onRateChange = onHourlyRateChange,
                    onBack = onBack,
                    onNext = onNextJob,
                )
                OnboardingStep.OptionalGoal -> OptionalGoalStep(
                    templates = state.templates,
                    selected = state.selectedTemplate,
                    onSelect = onSelectTemplate,
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
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = onNext,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(stringResource(R.string.onboarding_get_started))
    }
}

@Composable
private fun CreateJobStep(
    name: String,
    rate: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
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
    OutlinedTextField(
        value = rate,
        onValueChange = onRateChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.jobs_hourly_rate)) },
        singleLine = true,
        prefix = { Text("$") },
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
    onSelect: (GoalTemplate?) -> Unit,
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
        val isSelected = selected?.name == template.name
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
                    text = MoneyFormat.format(template.defaultTargetMinor),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
            Button(onClick = onNext, enabled = selected != null) {
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
    FilterChip(
        selected = false,
        onClick = onRequestPermissions,
        label = { Text(stringResource(R.string.onboarding_allow_permissions)) },
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
        TextButton(onClick = onBack, enabled = !saving) {
            Text(stringResource(R.string.action_back))
        }
        Button(onClick = onFinish, enabled = !saving) {
            Text(
                if (saving) {
                    stringResource(R.string.onboarding_saving)
                } else {
                    stringResource(R.string.onboarding_finish)
                },
            )
        }
    }
    TextButton(
        onClick = onFinish,
        enabled = !saving,
    ) {
        Text(stringResource(R.string.onboarding_not_now))
    }
}

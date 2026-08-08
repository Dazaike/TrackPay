package com.trackpay.app.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.unit.sp
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

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF1A1110)
    val surfaceBg = if (isDark) Color(0xFF1A1110) else MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = surfaceBg,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (stepIndex > 0) {
                    androidx.compose.material3.IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp),
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = if (isDark) Color(0xFF322726) else MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
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
            }
        }
    }
}
@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(32.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(Color(0xFF8FDBAE), Color(0xFF1F4230)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B3B22),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_welcome_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.onboarding_welcome_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth(0.75f)
                .align(Alignment.CenterHorizontally),
            shape = androidx.compose.foundation.shape.CircleShape,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = stringResource(R.string.onboarding_get_started),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
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

package com.trackpay.app.ui.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackpay.app.R
import com.trackpay.app.domain.model.ThemePackUi
import com.trackpay.app.ui.theme.ThemePacks
import com.trackpay.app.ui.util.MoneyFormat
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ThemesRoute(
    onBack: () -> Unit,
    viewModel: ThemesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lockedNeedMoreTemplate = stringResource(R.string.themes_locked_need_more)
    val applyFailedMessage = stringResource(R.string.themes_apply_failed)

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ThemesEvent.LockedNeedMore -> {
                    val msg = lockedNeedMoreTemplate.format(
                        MoneyFormat.format(event.remainingMinor),
                        event.themeName,
                    )
                    snackbarHostState.showSnackbar(msg)
                }
                ThemesEvent.ApplyFailed -> {
                    snackbarHostState.showSnackbar(applyFailedMessage)
                }
            }
        }
    }

    ThemesScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onThemeClick = viewModel::onThemeClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesScreen(
    state: ThemesUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onThemeClick: (ThemePackUi) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.themes_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.themes_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.themes_loading),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "wallet") {
                ThemesWalletCard(walletMinor = state.walletMinor)
            }
            items(state.themes, key = { it.id }) { theme ->
                ThemeRow(
                    theme = theme,
                    onClick = { onThemeClick(theme) },
                )
            }
        }
    }
}

@Composable
private fun ThemesWalletCard(walletMinor: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.themes_wallet_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = MoneyFormat.format(walletMinor),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.themes_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun ThemeRow(
    theme: ThemePackUi,
    onClick: () -> Unit,
) {
    val swatch = ThemePacks.swatchPrimary(theme.id)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(swatch),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = theme.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = themeSubtitle(theme),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                theme.active -> {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(stringResource(R.string.themes_active_chip)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            disabledLeadingIconContentColor =
                                MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    )
                }
                !theme.owned -> {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = stringResource(R.string.themes_locked_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    // Owned, not active — tap to apply; no trailing chrome needed
                }
            }
        }
    }
}

@Composable
private fun themeSubtitle(theme: ThemePackUi): String = when {
    theme.active -> stringResource(R.string.themes_status_active)
    theme.owned -> stringResource(R.string.themes_tap_to_apply)
    theme.unlockMinor <= 0L -> stringResource(R.string.themes_free)
    else -> stringResource(
        R.string.themes_unlock_at,
        MoneyFormat.format(theme.unlockMinor),
    )
}

package com.trackpay.app.ui.themes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackpay.app.domain.model.ThemePackUi
import com.trackpay.app.domain.usecase.ApplyThemeUseCase
import com.trackpay.app.domain.usecase.ObserveThemesUseCase
import com.trackpay.app.domain.usecase.ObserveWalletUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemesUiState(
    val loading: Boolean = true,
    val walletMinor: Long = 0L,
    val themes: List<ThemePackUi> = emptyList(),
)

sealed interface ThemesEvent {
    data class LockedNeedMore(val remainingMinor: Long, val themeName: String) : ThemesEvent
    data object ApplyFailed : ThemesEvent
}

@HiltViewModel
class ThemesViewModel @Inject constructor(
    observeThemes: ObserveThemesUseCase,
    observeWallet: ObserveWalletUseCase,
    private val applyTheme: ApplyThemeUseCase,
) : ViewModel() {

    val uiState: StateFlow<ThemesUiState> = combine(
        observeWallet(),
        observeThemes(),
    ) { walletMinor, themes ->
        ThemesUiState(
            loading = false,
            walletMinor = walletMinor,
            themes = themes,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemesUiState(),
    )

    private val _events = MutableSharedFlow<ThemesEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ThemesEvent> = _events.asSharedFlow()

    fun onThemeClick(theme: ThemePackUi) {
        if (theme.active) return
        if (!theme.owned) {
            val remaining = (theme.unlockMinor - uiState.value.walletMinor).coerceAtLeast(0L)
            viewModelScope.launch {
                _events.emit(ThemesEvent.LockedNeedMore(remaining, theme.name))
            }
            return
        }
        viewModelScope.launch {
            val ok = applyTheme(theme.id)
            if (!ok) {
                _events.emit(ThemesEvent.ApplyFailed)
            }
        }
    }
}

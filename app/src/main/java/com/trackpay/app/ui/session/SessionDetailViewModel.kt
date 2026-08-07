package com.trackpay.app.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackpay.app.domain.model.SessionDetail
import com.trackpay.app.domain.usecase.DeleteSessionUseCase
import com.trackpay.app.domain.usecase.GetSessionDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionDetailUiState(
    val loading: Boolean = true,
    val detail: SessionDetail? = null,
    val errorMessage: String? = null,
    val showDeleteConfirm: Boolean = false,
    val deleted: Boolean = false,
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val getSessionDetail: GetSessionDetailUseCase,
    private val deleteSession: DeleteSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState

    private var sessionId: String? = null

    fun load(sessionId: String) {
        this.sessionId = sessionId
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, errorMessage = null) }
            runCatching { getSessionDetail(sessionId) }
                .onSuccess { detail ->
                    if (detail == null) {
                        _uiState.update {
                            it.copy(loading = false, detail = null, errorMessage = "Session not found")
                        }
                    } else {
                        _uiState.update {
                            it.copy(loading = false, detail = detail, errorMessage = null)
                        }
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            errorMessage = err.message ?: "Failed to load session",
                        )
                    }
                }
        }
    }

    fun requestDelete() {
        _uiState.update { it.copy(showDeleteConfirm = true) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun confirmDelete() {
        val id = sessionId ?: return
        viewModelScope.launch {
            runCatching { deleteSession(id) }
                .onSuccess {
                    _uiState.update {
                        it.copy(showDeleteConfirm = false, deleted = true, errorMessage = null)
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            showDeleteConfirm = false,
                            errorMessage = err.message ?: "Delete failed",
                        )
                    }
                }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

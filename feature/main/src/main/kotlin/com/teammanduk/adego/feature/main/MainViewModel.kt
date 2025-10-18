package com.teammanduk.adego.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teammanduk.adego.core.domain.usecase.RestoreSessionUseCase
import com.teammanduk.adego.core.domain.usecase.SessionInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val restoreSession: RestoreSessionUseCase
) : ViewModel() {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val sessionInfo = restoreSession()
            _sessionState.value = if (sessionInfo != null) {
                SessionState.Restored(sessionInfo)
            } else {
                SessionState.None
            }
        }
    }
}

sealed interface SessionState {
    data object Loading : SessionState
    data object None : SessionState
    data class Restored(val sessionInfo: SessionInfo) : SessionState
}

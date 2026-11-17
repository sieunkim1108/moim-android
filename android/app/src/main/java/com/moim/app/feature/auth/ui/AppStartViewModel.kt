package com.moim.app.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moim.app.feature.auth.domain.usecase.RestoreSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 앱을 켰을 때 어느 화면부터 보여줄지 */
enum class AppStartState { Checking, LoggedIn, LoggedOut }

/**
 * S-01 동작: 자동 로그인이 켜져 있고 세션이 유효하면 시작 화면을 건너뛰고 메인(S-06)으로.
 * 확인하는 동안은 스플래시 화면이 그대로 떠 있다 (MainActivity).
 */
@HiltViewModel
class AppStartViewModel @Inject constructor(
    private val restoreSession: RestoreSessionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AppStartState.Checking)
    val state: StateFlow<AppStartState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val user = runCatching { restoreSession() }.getOrNull()
            _state.value = if (user != null) AppStartState.LoggedIn else AppStartState.LoggedOut
        }
    }
}

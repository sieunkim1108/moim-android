package com.moim.app.feature.auth.ui.login

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moim.app.feature.auth.domain.usecase.LoginUseCase
import com.moim.app.feature.auth.ui.toAuthMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 로그인 화면에 필요한 모든 상태를 한 곳에 모은 클래스 */
data class LoginUiState(
    val studentId: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val autoLogin: Boolean = true,
    val isLoading: Boolean = false,
    @param:StringRes val errorRes: Int? = null,
    val isLoggedIn: Boolean = false, // true가 되면 화면이 메인으로 이동
) {
    val canSubmit: Boolean get() = studentId.isNotBlank() && password.isNotBlank() && !isLoading
}

/**
 * 로그인 화면의 ViewModel.
 * 화면은 uiState(StateFlow)를 구독만 하고, 사용자가 무언가 하면 아래 함수들을 부른다.
 * @HiltViewModel: Hilt가 LoginUseCase를 자동으로 넣어서 만들어 준다.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val login: LoginUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onStudentIdChange(value: String) {
        // 숫자만, 최대 7자리
        _uiState.update { it.copy(studentId = value.filter(Char::isDigit).take(7), errorRes = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorRes = null) }
    }

    fun onTogglePasswordVisible() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onAutoLoginChange(checked: Boolean) {
        // 로그인에 성공하면 DataStore에 저장돼 다음 실행 때 시작 화면을 건너뛴다
        _uiState.update { it.copy(autoLogin = checked) }
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (!state.canSubmit) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorRes = null) }
            login(state.studentId, state.password, keepSignedIn = state.autoLogin)
                .onSuccess { _uiState.update { it.copy(isLoading = false, isLoggedIn = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorRes = e.toAuthMessageRes()) } }
        }
    }

    /** 화면 이동을 마친 뒤 호출해서 같은 이동이 반복되지 않게 한다 */
    fun onNavigatedToMain() {
        _uiState.update { it.copy(isLoggedIn = false) }
    }
}

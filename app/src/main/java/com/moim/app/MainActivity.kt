package com.moim.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moim.app.feature.auth.ui.AppStartState
import com.moim.app.feature.auth.ui.AppStartViewModel
import com.moim.app.navigation.MoimNavHost
import com.moim.app.navigation.MoimRoute
import com.moim.app.ui.theme.MoimTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 앱의 유일한 Activity. 화면 전환은 전부 Compose 내비게이션이 처리한다.
 * @AndroidEntryPoint: 이 Activity 안에서 Hilt로 ViewModel 등을 주입받을 수 있게 해줌.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val startViewModel: AppStartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen() // super.onCreate 전에 호출해야 함
        super.onCreate(savedInstanceState)
        // 자동 로그인 확인이 끝날 때까지 스플래시를 유지한다
        splash.setKeepOnScreenCondition { startViewModel.state.value == AppStartState.Checking }
        enableEdgeToEdge()    // 상태바 영역까지 화면을 채움
        setContent {
            MoimTheme {
                val startState by startViewModel.state.collectAsStateWithLifecycle()
                when (startState) {
                    AppStartState.Checking -> Unit // 스플래시가 덮고 있음
                    AppStartState.LoggedIn -> MoimNavHost(startDestination = MoimRoute.Main)
                    AppStartState.LoggedOut -> MoimNavHost(startDestination = MoimRoute.Start)
                }
            }
        }
    }
}

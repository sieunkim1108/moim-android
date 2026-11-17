package com.moim.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.moim.app.feature.auth.ui.done.SignUpDoneScreen
import com.moim.app.feature.auth.ui.login.LoginRoute
import com.moim.app.feature.auth.ui.signup.SignUpBasicRoute
import com.moim.app.feature.auth.ui.signup.SignUpTypeRoute
import com.moim.app.feature.auth.ui.signup.SignUpViewModel
import com.moim.app.feature.auth.ui.StartScreen
import com.moim.app.feature.calendar.ui.CalendarRoute
import com.moim.app.feature.chat.ui.MainRoute
import com.moim.app.feature.dm.domain.model.RoomType
import com.moim.app.feature.dm.ui.DmListRoute
import com.moim.app.feature.dm.ui.DmRoomRoute
import com.moim.app.feature.friend.ui.AddFriendRoute
import com.moim.app.feature.meetup.ui.MeetupRoute
import com.moim.app.feature.profile.ui.ProfileRoute

/**
 * 화면 이동 지도.
 * 흐름: 시작 → 로그인 → 메인 / 시작 → 회원가입1 → 회원가입2 → 가입 완료 → 메인
 */
@Composable
fun MoimNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: MoimRoute = MoimRoute.Start, // 자동 로그인 상태면 MainActivity가 Main을 넘긴다
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<MoimRoute.Start> {
            StartScreen(
                onLoginClick = { navController.navigate(MoimRoute.Login) },
                onSignUpClick = { navController.navigate(MoimRoute.SignUpGraph) },
            )
        }

        composable<MoimRoute.Login> {
            LoginRoute(
                onBack = { navController.popBackStack() },
                onLoginSuccess = { navController.navigateToMainClearingBackStack() },
                onSignUpClick = {
                    navController.navigate(MoimRoute.SignUpGraph) {
                        popUpTo(MoimRoute.Start) // 로그인 화면은 기록에서 빼서 뒤로 가기 시 시작 화면으로
                    }
                },
            )
        }

        // 회원가입 1·2단계: 하나의 그룹으로 묶어서 ViewModel을 공유
        navigation<MoimRoute.SignUpGraph>(startDestination = MoimRoute.SignUpBasic) {
            composable<MoimRoute.SignUpBasic> { entry ->
                SignUpBasicRoute(
                    viewModel = entry.sharedSignUpViewModel(navController),
                    onBack = { navController.popBackStack() },
                    onNext = { navController.navigate(MoimRoute.SignUpType) },
                    onLoginClick = {
                        navController.navigate(MoimRoute.Login) {
                            popUpTo(MoimRoute.Start)
                        }
                    },
                )
            }
            composable<MoimRoute.SignUpType> { entry ->
                SignUpTypeRoute(
                    viewModel = entry.sharedSignUpViewModel(navController),
                    onBack = { navController.popBackStack() },
                    onSignUpComplete = { profile ->
                        navController.navigate(
                            MoimRoute.SignUpDone(name = profile.name, college = profile.college, major = profile.major),
                        ) {
                            // 가입이 끝나면 시작~회원가입 화면은 뒤로 가기로 돌아오지 않게 모두 지운다
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                )
            }
        }

        composable<MoimRoute.SignUpDone> { entry ->
            val route = entry.toRoute<MoimRoute.SignUpDone>()
            SignUpDoneScreen(
                name = route.name,
                college = route.college,
                major = route.major,
                onStartChat = { navController.navigateToMainClearingBackStack() },
            )
        }

        composable<MoimRoute.Main> {
            MainRoute(
                onCalendarClick = { navController.navigate(MoimRoute.Calendar) },
                onDmClick = { navController.navigate(MoimRoute.DmList) },
                onProfileClick = { navController.navigate(MoimRoute.Profile) },
            )
        }

        composable<MoimRoute.Calendar> {
            CalendarRoute(
                onBack = { navController.popBackStack() },
                onBackToChat = { navController.popBackStack(MoimRoute.Main, inclusive = false) },
                onDmClick = { navController.navigate(MoimRoute.DmList) },
                onProfileClick = { navController.navigate(MoimRoute.Profile) },
                onAddFriendClick = { navController.navigate(MoimRoute.AddFriend) },
            )
        }

        composable<MoimRoute.DmList> {
            DmListRoute(
                onBack = { navController.popBackStack() },
                onRoomClick = { room ->
                    if (room.type == RoomType.BOT) {
                        // 'AI 눈송이' 방 = 메인 채팅 화면
                        navController.popBackStack(MoimRoute.Main, inclusive = false)
                    } else {
                        navController.navigate(MoimRoute.DmRoom(room.id))
                    }
                },
                onProfileClick = { navController.navigate(MoimRoute.Profile) },
                onAddFriendClick = { navController.navigate(MoimRoute.AddFriend) },
            )
        }

        composable<MoimRoute.DmRoom> {
            // roomId는 DmRoomViewModel이 SavedStateHandle에서 읽는다
            DmRoomRoute(
                onBack = { navController.popBackStack() },
                onMeetupClick = { roomId -> navController.navigate(MoimRoute.Meetup(roomId)) },
                onProfileClick = { navController.navigate(MoimRoute.Profile) },
            )
        }

        composable<MoimRoute.Meetup> {
            MeetupRoute(
                onBack = { navController.popBackStack() },
                onConfirmed = { navController.popBackStack() }, // 확정하면 채팅방으로 돌아가 봇 메시지를 보여 준다
            )
        }

        composable<MoimRoute.AddFriend> {
            AddFriendRoute(
                onBack = { navController.popBackStack() },
                onProfileClick = { navController.navigate(MoimRoute.Profile) },
            )
        }

        composable<MoimRoute.Profile> {
            ProfileRoute(
                onBack = { navController.popBackStack() },
                onSaved = {
                    // 저장하면 메인(눈송이 챗봇)으로
                    if (!navController.popBackStack(MoimRoute.Main, inclusive = false)) navController.popBackStack()
                },
                onLoggedOut = {
                    // 로그아웃: 기록을 모두 지우고 시작 화면(S-01)으로
                    navController.navigate(MoimRoute.Start) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}

/**
 * 회원가입 그룹(SignUpGraph)에 묶인 ViewModel을 가져온다.
 * 1단계와 2단계 화면이 이 함수를 쓰면 같은 ViewModel 인스턴스를 받는다.
 */
@Composable
private fun NavBackStackEntry.sharedSignUpViewModel(navController: NavHostController): SignUpViewModel {
    val parentEntry = remember(this) { navController.getBackStackEntry(MoimRoute.SignUpGraph) }
    return hiltViewModel(parentEntry)
}

/** 로그인/가입이 끝나면 메인으로 가면서 이전 화면 기록을 전부 지운다 (뒤로 가기 = 앱 종료) */
private fun NavHostController.navigateToMainClearingBackStack() {
    navigate(MoimRoute.Main) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

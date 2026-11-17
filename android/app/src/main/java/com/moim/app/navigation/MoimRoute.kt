package com.moim.app.navigation

import kotlinx.serialization.Serializable

/*
 * 앱의 모든 화면 주소(route).
 * type-safe route: 문자열 대신 @Serializable 객체로 화면을 지정해서, 오타가 나면 컴파일 단계에서 잡힌다.
 * 화면에 값을 넘겨야 하면 data class로 만든다. (예: SignUpDone)
 */
sealed interface MoimRoute {
    // 인증 흐름
    @Serializable data object Start : MoimRoute
    @Serializable data object Login : MoimRoute

    /** 회원가입 1·2단계를 묶는 그룹. 두 화면이 같은 SignUpViewModel을 공유한다. */
    @Serializable data object SignUpGraph : MoimRoute
    @Serializable data object SignUpBasic : MoimRoute
    @Serializable data object SignUpType : MoimRoute
    @Serializable data class SignUpDone(val name: String, val college: String, val major: String) : MoimRoute

    // 메인 흐름 (하단 탭바 없음: 메인 상단 앱바 아이콘으로만 이동)
    @Serializable data object Main : MoimRoute
    @Serializable data object Calendar : MoimRoute
    @Serializable data object DmList : MoimRoute
    @Serializable data object Profile : MoimRoute
    @Serializable data object AddFriend : MoimRoute

    // 단체 DM / 모임
    @Serializable data class DmRoom(val roomId: String) : MoimRoute
    @Serializable data class Meetup(val roomId: String) : MoimRoute
}

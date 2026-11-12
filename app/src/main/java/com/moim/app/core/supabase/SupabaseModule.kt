package com.moim.app.core.supabase

import com.moim.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import javax.inject.Singleton

/** local.properties에 Supabase 주소/anon key가 들어 있는지 */
object SupabaseConfig {
    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    /**
     * 학교 메일 인증을 켤지. 지금은 꺼 둔다 (메일 발송용 Resend 키 준비 전).
     * false면 [인증요청]은 메일을 보내지 않고, 인증번호는 아무 6자리 숫자나 통과한다.
     * 다시 켤 때: true로 바꾸고, DB의 handle_new_user에 인증 검사도 되살린다.
     */
    const val EMAIL_VERIFICATION_ENABLED = false

    /** Supabase Auth는 이메일로 로그인하므로 학번을 가상 이메일로 바꿔 쓴다 (실제 메일은 안 감) */
    fun loginEmail(studentId: String) = "$studentId@moim.app"
}

/**
 * SupabaseClient 하나를 앱 전체가 같이 쓴다.
 * 앱에는 anon key만 들어가고, 누가 어떤 행을 볼 수 있는지는 DB의 RLS가 정한다.
 * 로그인 세션 토큰은 supabase-kt가 기기 안에 저장했다가 앱을 다시 켜면 불러온다.
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth)
        install(Postgrest)
        install(Functions)
        install(Realtime)
    }
}

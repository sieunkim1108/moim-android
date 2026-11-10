package com.moim.app.feature.chat.di

import com.moim.app.core.ai.AiServerClient
import com.moim.app.feature.chat.data.AiChatBotRepository
import com.moim.app.feature.chat.data.FakeChatBotRepository
import com.moim.app.feature.chat.domain.ChatBotRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 눈송이 구현 선택.
 * AI 서버 주소(ai.serverUrl)와 Supabase 키가 모두 있으면 AI 서버, 아니면 규칙 기반 목업.
 * (AI 서버는 Supabase 로그인 토큰으로 사용자를 확인하므로 둘 다 필요)
 */
@Module
@InstallIn(SingletonComponent::class)
object ChatModule {
    @Provides
    @Singleton
    fun provideChatBotRepository(
        fake: Provider<FakeChatBotRepository>,
        ai: Provider<AiChatBotRepository>,
    ): ChatBotRepository =
        if (AiServerClient.isConfigured) ai.get() else fake.get()
}

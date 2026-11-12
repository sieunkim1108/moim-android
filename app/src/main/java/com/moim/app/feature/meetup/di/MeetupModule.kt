package com.moim.app.feature.meetup.di

import com.moim.app.core.ai.AiServerClient
import com.moim.app.core.supabase.SupabaseConfig
import com.moim.app.feature.meetup.data.AiMeetupExplainer
import com.moim.app.feature.meetup.data.FakeMeetupExplainer
import com.moim.app.feature.meetup.data.FakeMeetupRepository
import com.moim.app.feature.meetup.data.SupabaseMeetupRepository
import com.moim.app.feature.meetup.domain.MeetupExplainer
import com.moim.app.feature.meetup.domain.MeetupRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider

/** Supabase 키가 있으면 Supabase, AI 서버 주소까지 있으면 Claude 설명, 아니면 목업 */
@Module
@InstallIn(SingletonComponent::class)
object MeetupModule {
    @Provides
    fun provideMeetupRepository(
        fake: Provider<FakeMeetupRepository>,
        supabase: Provider<SupabaseMeetupRepository>,
    ): MeetupRepository = if (SupabaseConfig.isConfigured) supabase.get() else fake.get()

    @Provides
    fun provideMeetupExplainer(
        fake: Provider<FakeMeetupExplainer>,
        ai: Provider<AiMeetupExplainer>,
    ): MeetupExplainer = if (AiServerClient.isConfigured) ai.get() else fake.get()
}

package com.moim.app.core.di

import com.moim.app.core.data.FakeFriendRepository
import com.moim.app.core.data.FakeScheduleRepository
import com.moim.app.core.data.FriendRepository
import com.moim.app.core.data.ScheduleRepository
import com.moim.app.core.data.SupabaseFriendRepository
import com.moim.app.core.data.SupabaseScheduleRepository
import com.moim.app.core.supabase.SupabaseConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/** 일정·친구 저장소: Supabase 키가 있으면 Supabase 구현, 없으면 가짜(Fake) 구현 */
@Module
@InstallIn(SingletonComponent::class)
object ScheduleModule {
    @Provides
    @Singleton
    fun provideScheduleRepository(
        fake: Provider<FakeScheduleRepository>,
        supabase: Provider<SupabaseScheduleRepository>,
    ): ScheduleRepository = if (SupabaseConfig.isConfigured) supabase.get() else fake.get()

    @Provides
    @Singleton
    fun provideFriendRepository(
        fake: Provider<FakeFriendRepository>,
        supabase: Provider<SupabaseFriendRepository>,
    ): FriendRepository = if (SupabaseConfig.isConfigured) supabase.get() else fake.get()
}

package com.moim.app.feature.auth.di

import com.moim.app.core.supabase.SupabaseConfig
import com.moim.app.feature.auth.data.FakeAuthRepository
import com.moim.app.feature.auth.data.SupabaseAuthRepository
import com.moim.app.feature.auth.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Hilt 모듈: AuthRepository 구현 선택.
 * local.properties에 Supabase 키가 있으면 Supabase 구현, 없으면 메모리 목업(Fake)을 쓴다.
 * Provider로 받아서 고르지 않은 쪽은 아예 만들지 않는다.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        fake: Provider<FakeAuthRepository>,
        supabase: Provider<SupabaseAuthRepository>,
    ): AuthRepository = if (SupabaseConfig.isConfigured) supabase.get() else fake.get()
}

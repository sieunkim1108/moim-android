package com.moim.app.feature.dm.di

import com.moim.app.core.supabase.SupabaseConfig
import com.moim.app.feature.dm.data.FakeDmRepository
import com.moim.app.feature.dm.data.SupabaseDmRepository
import com.moim.app.feature.dm.domain.DmRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/** Supabase 키가 있으면 Supabase(Realtime), 없으면 메모리 목업 채팅방 */
@Module
@InstallIn(SingletonComponent::class)
object DmModule {
    @Provides
    @Singleton
    fun provideDmRepository(
        fake: Provider<FakeDmRepository>,
        supabase: Provider<SupabaseDmRepository>,
    ): DmRepository = if (SupabaseConfig.isConfigured) supabase.get() else fake.get()
}

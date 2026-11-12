package com.moim.app.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId
import javax.inject.Singleton

/**
 * 현재 시각을 주는 Clock. 앱 전체가 한국 시간(Asia/Seoul) 기준으로 동작한다.
 * 테스트에서는 고정된 Clock을 넣어서 "오늘"을 마음대로 정할 수 있다.
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    val SeoulZone: ZoneId = ZoneId.of("Asia/Seoul")

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.system(SeoulZone)
}

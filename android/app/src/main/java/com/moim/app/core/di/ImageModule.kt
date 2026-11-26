package com.moim.app.core.di

import com.moim.app.core.image.AndroidPhotoCompressor
import com.moim.app.core.image.PhotoCompressor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** "PhotoCompressor가 필요하면 AndroidPhotoCompressor를 넣어 줘" */
@Module
@InstallIn(SingletonComponent::class)
abstract class ImageModule {
    @Binds
    abstract fun bindPhotoCompressor(impl: AndroidPhotoCompressor): PhotoCompressor
}

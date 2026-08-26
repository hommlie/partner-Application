package com.hommlie.partner.ui.capturephoto

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindPhotoStorage(
        implementation: MediaStorePhotoStorage
    ): PhotoStorage
}
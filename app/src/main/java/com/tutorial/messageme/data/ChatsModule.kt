package com.tutorial.messageme.data

import com.tutorial.messageme.data.arch.ChatsRepository
import com.tutorial.messageme.data.arch.ChatsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatsModule {

    @Provides
    @Singleton
    fun provideRepository():ChatsRepository = ChatsRepositoryImpl()
}
package com.example.cropdrone.core

import com.example.cropdrone.data.repository.AnomalyRepositoryImpl
import com.example.cropdrone.data.repository.FieldRepositoryImpl
import com.example.cropdrone.data.repository.MissionRepositoryImpl
import com.example.cropdrone.domain.repository.AnomalyRepository
import com.example.cropdrone.domain.repository.FieldRepository
import com.example.cropdrone.domain.repository.MissionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindFieldRepository(impl: FieldRepositoryImpl): FieldRepository

    @Binds @Singleton
    abstract fun bindMissionRepository(impl: MissionRepositoryImpl): MissionRepository

    @Binds @Singleton
    abstract fun bindAnomalyRepository(impl: AnomalyRepositoryImpl): AnomalyRepository
}

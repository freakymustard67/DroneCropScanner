package com.example.cropdrone.core

import android.content.Context
import androidx.room.Room
import com.example.cropdrone.data.local.CropDroneDatabase
import com.example.cropdrone.data.local.AnomalyDao
import com.example.cropdrone.data.local.FieldDao
import com.example.cropdrone.data.local.MissionDao
import com.example.cropdrone.data.remote.GeminiApiService
import com.example.cropdrone.core.GeminiRetrofit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): CropDroneDatabase =
        Room.databaseBuilder(ctx, CropDroneDatabase::class.java, CropDroneDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideFieldDao(db: CropDroneDatabase): FieldDao = db.fieldDao()
    @Provides fun provideMissionDao(db: CropDroneDatabase): MissionDao = db.missionDao()
    @Provides fun provideAnomalyDao(db: CropDroneDatabase): AnomalyDao = db.anomalyDao()

    @Provides @Singleton
    fun provideGeminiApiService(@GeminiRetrofit retrofit: Retrofit): GeminiApiService =
        retrofit.create(GeminiApiService::class.java)
}

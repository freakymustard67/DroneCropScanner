package com.example.cropdrone.core

import android.content.Context
import com.example.cropdrone.hardware.DroneController
import com.example.cropdrone.hardware.ESP32DroneController
import com.example.cropdrone.hardware.MockDroneController
import com.example.cropdrone.hardware.tflite.CropDiseaseClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.example.cropdrone.BuildConfig
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HardwareModule {

    /**
     * Switch between Mock and real ESP32 drone controller via BuildConfig flag.
     * For dev: use MockDroneController (no hardware needed).
     * For prod: use ESP32DroneController.
     */
    @Provides @Singleton
    fun provideDroneController(@ApplicationContext ctx: Context): DroneController =
        if (BuildConfig.DEBUG) MockDroneController()
        else ESP32DroneController()

    @Provides @Singleton
    fun provideTfliteClassifier(@ApplicationContext ctx: Context): CropDiseaseClassifier =
        CropDiseaseClassifier(ctx).also { it.initialize() }
}

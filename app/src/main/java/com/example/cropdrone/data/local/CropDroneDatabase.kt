package com.example.cropdrone.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.cropdrone.data.local.entities.AnomalyEntity
import com.example.cropdrone.data.local.entities.Converters
import com.example.cropdrone.data.local.entities.FieldEntity
import com.example.cropdrone.data.local.entities.ScanMissionEntity

@Database(
    entities = [FieldEntity::class, ScanMissionEntity::class, AnomalyEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class CropDroneDatabase : RoomDatabase() {
    abstract fun fieldDao(): FieldDao
    abstract fun missionDao(): MissionDao
    abstract fun anomalyDao(): AnomalyDao

    companion object {
        const val DATABASE_NAME = "crop_drone.db"
    }
}

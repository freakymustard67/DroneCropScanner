package com.example.cropdrone.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.cropdrone.data.local.entities.AnomalyEntity
import com.example.cropdrone.data.local.entities.FieldEntity
import com.example.cropdrone.data.local.entities.ScanMissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldDao {
    @Upsert suspend fun upsert(field: FieldEntity)
    @Query("SELECT * FROM fields WHERE id = :id") suspend fun getById(id: String): FieldEntity?
    @Query("SELECT * FROM fields ORDER BY createdAt DESC") fun getAll(): Flow<List<FieldEntity>>
    @Query("DELETE FROM fields WHERE id = :id") suspend fun delete(id: String)
}

@Dao
interface MissionDao {
    @Upsert suspend fun upsert(mission: ScanMissionEntity)
    @Query("SELECT * FROM missions WHERE id = :id") suspend fun getById(id: String): ScanMissionEntity?
    @Query("SELECT * FROM missions ORDER BY startedAt DESC LIMIT :limit") fun getRecent(limit: Int = 10): Flow<List<ScanMissionEntity>>
    @Query("SELECT * FROM missions ORDER BY startedAt DESC LIMIT 1") fun getLatest(): Flow<ScanMissionEntity?>
}

@Dao
interface AnomalyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(anomaly: AnomalyEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(anomalies: List<AnomalyEntity>)
    @Query("SELECT * FROM anomalies WHERE missionId = :missionId ORDER BY detectedAt DESC") fun getForMission(missionId: String): Flow<List<AnomalyEntity>>
    @Query("SELECT * FROM anomalies WHERE analyzedAt IS NULL") suspend fun getPendingCloudAnalysis(): List<AnomalyEntity>
    @Query("UPDATE anomalies SET diseaseName = :diseaseName, severity = :severity, confidence = :confidence, treatment = :treatment, analyzedAt = :analyzedAt WHERE id = :id")
    suspend fun updateCloudResult(id: String, diseaseName: String, severity: Int, confidence: Double, treatment: String, analyzedAt: Long)
}

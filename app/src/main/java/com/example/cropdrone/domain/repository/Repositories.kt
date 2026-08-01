package com.example.cropdrone.domain.repository

import com.example.cropdrone.domain.model.Anomaly
import com.example.cropdrone.domain.model.FieldBoundary
import com.example.cropdrone.domain.model.ScanMission
import kotlinx.coroutines.flow.Flow

interface FieldRepository {
    suspend fun saveField(field: FieldBoundary)
    suspend fun getField(id: String): FieldBoundary?
    fun getAllFields(): Flow<List<FieldBoundary>>
    suspend fun deleteField(id: String)
}

interface MissionRepository {
    suspend fun saveMission(mission: ScanMission)
    suspend fun getMission(id: String): ScanMission?
    fun getRecentMissions(limit: Int = 10): Flow<List<ScanMission>>
    fun getLatestMission(): Flow<ScanMission?>
}

interface AnomalyRepository {
    suspend fun saveAnomaly(anomaly: Anomaly)
    fun getAnomaliesForMission(missionId: String): Flow<List<Anomaly>>
    fun getAnomaliesForField(fieldName: String): Flow<List<Anomaly>>
    suspend fun updateAnomaly(anomaly: Anomaly)
    suspend fun getPendingCloudAnalysis(): List<Anomaly>
}

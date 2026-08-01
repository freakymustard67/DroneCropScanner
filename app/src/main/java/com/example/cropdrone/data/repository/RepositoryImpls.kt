package com.example.cropdrone.data.repository

import com.example.cropdrone.data.local.AnomalyDao
import com.example.cropdrone.data.local.FieldDao
import com.example.cropdrone.data.local.MissionDao
import com.example.cropdrone.data.mapper.toDomain
import com.example.cropdrone.data.mapper.toEntity
import com.example.cropdrone.domain.model.Anomaly
import com.example.cropdrone.domain.model.FieldBoundary
import com.example.cropdrone.domain.model.ScanMission
import com.example.cropdrone.domain.repository.AnomalyRepository
import com.example.cropdrone.domain.repository.FieldRepository
import com.example.cropdrone.domain.repository.MissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FieldRepositoryImpl @Inject constructor(
    private val dao: FieldDao,
) : FieldRepository {
    override suspend fun saveField(field: FieldBoundary) = dao.upsert(field.toEntity())
    override suspend fun getField(id: String): FieldBoundary? = dao.getById(id)?.toDomain()
    override fun getAllFields(): Flow<List<FieldBoundary>> = dao.getAll().map { it.map { e -> e.toDomain() } }
    override suspend fun deleteField(id: String) = dao.delete(id)
}

@Singleton
class MissionRepositoryImpl @Inject constructor(
    private val dao: MissionDao,
) : MissionRepository {
    override suspend fun saveMission(mission: ScanMission) = dao.upsert(mission.toEntity())
    override suspend fun getMission(id: String): ScanMission? = dao.getById(id)?.toDomain()
    override fun getRecentMissions(limit: Int): Flow<List<ScanMission>> =
        dao.getRecent(limit).map { it.map { e -> e.toDomain() } }
    override fun getLatestMission(): Flow<ScanMission?> =
        dao.getLatest().map { it?.toDomain() }
}

@Singleton
class AnomalyRepositoryImpl @Inject constructor(
    private val dao: AnomalyDao,
) : AnomalyRepository {
    override suspend fun saveAnomaly(anomaly: Anomaly) = dao.insert(anomaly.toEntity())
    override fun getAnomaliesForMission(missionId: String): Flow<List<Anomaly>> =
        dao.getForMission(missionId).map { it.map { e -> e.toDomain() } }
    override fun getAnomaliesForField(fieldName: String): Flow<List<Anomaly>> =
        dao.getForMission(fieldName).map { it.map { e -> e.toDomain() } }
    override suspend fun updateAnomaly(anomaly: Anomaly) =
        dao.updateCloudResult(
            id = anomaly.id,
            diseaseName = anomaly.diseaseName ?: "",
            severity = anomaly.severity ?: 0,
            confidence = anomaly.confidence ?: 0.0,
            treatment = anomaly.treatment ?: "",
            analyzedAt = anomaly.analyzedAt ?: System.currentTimeMillis(),
        )
    override suspend fun getPendingCloudAnalysis(): List<Anomaly> =
        dao.getPendingCloudAnalysis().map { it.toDomain() }
}

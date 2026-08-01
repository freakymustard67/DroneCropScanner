package com.example.cropdrone.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cropdrone.domain.model.Anomaly
import com.example.cropdrone.domain.model.ScanMission
import com.example.cropdrone.domain.repository.AnomalyRepository
import com.example.cropdrone.domain.repository.MissionRepository
import com.example.cropdrone.domain.usecase.RunPass2DiagnosisUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val latestMission: ScanMission? = null,
    val anomalies: List<Anomaly> = emptyList(),
    val selectedAnomaly: Anomaly? = null,
    val healthScore: Int = 95,
    val cloudAnalysisRunning: Boolean = false,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val missionRepo: MissionRepository,
    private val anomalyRepo: AnomalyRepository,
    private val pass2UseCase: RunPass2DiagnosisUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            missionRepo.getLatestMission().collectLatest { mission ->
                _uiState.value = _uiState.value.copy(latestMission = mission, isLoading = false)
                if (mission != null) {
                    anomalyRepo.getAnomaliesForMission(mission.id).collectLatest { anomalies ->
                        val analyzed = anomalies.count { it.analyzedAt != null }
                        _uiState.value = _uiState.value.copy(
                            anomalies = anomalies,
                            healthScore = computeHealthScore(anomalies),
                        )
                    }
                }
            }
        }
    }

    fun selectAnomaly(anomaly: Anomaly?) {
        _uiState.value = _uiState.value.copy(selectedAnomaly = anomaly)
    }

    /** Trigger Pass 2 cloud analysis on all pending anomalies. */
    fun runCloudAnalysis() {
        if (_uiState.value.cloudAnalysisRunning) return
        _uiState.value = _uiState.value.copy(cloudAnalysisRunning = true)
        viewModelScope.launch {
            val pending = _uiState.value.anomalies.filter { it.analyzedAt == null }
            for (anomaly in pending) {
                val result = pass2UseCase.invoke(anomaly)
                if (result.isSuccess) {
                    val diagnosis = result.getOrThrow()
                    anomalyRepo.updateAnomaly(
                        anomaly.copy(
                            diseaseName = diagnosis.diseaseName,
                            severity = diagnosis.severity,
                            confidence = diagnosis.confidence,
                            treatment = diagnosis.treatmentRecommendation,
                            analyzedAt = System.currentTimeMillis(),
                        )
                    )
                }
            }
            _uiState.value = _uiState.value.copy(cloudAnalysisRunning = false)
        }
    }

    private fun computeHealthScore(anomalies: List<Anomaly>): Int {
        if (anomalies.isEmpty()) return 95
        val sevWeight = anomalies.sumOf { it.severity ?: 2 }
        return maxOf(20, 95 - sevWeight * 3)
    }
}

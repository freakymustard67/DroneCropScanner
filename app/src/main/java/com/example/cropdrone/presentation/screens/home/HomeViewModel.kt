package com.example.cropdrone.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cropdrone.domain.model.Anomaly
import com.example.cropdrone.domain.model.FieldBoundary
import com.example.cropdrone.domain.model.ScanMission
import com.example.cropdrone.domain.repository.AnomalyRepository
import com.example.cropdrone.domain.repository.FieldRepository
import com.example.cropdrone.domain.repository.MissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val latestField: FieldBoundary? = null,
    val latestMission: ScanMission? = null,
    val anomalyCount: Int = 0,
    val analyzedCount: Int = 0,
    val healthScore: Int = 95,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fieldRepo: FieldRepository,
    private val missionRepo: MissionRepository,
    private val anomalyRepo: AnomalyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        missionRepo.getLatestMission().onEach { mission ->
            _uiState.value = _uiState.value.copy(latestMission = mission)
            if (mission != null) {
                anomalyRepo.getAnomaliesForMission(mission.id).onEach { anomalies ->
                    val analyzed = anomalies.count { it.analyzedAt != null }
                    _uiState.value = _uiState.value.copy(
                        anomalyCount = anomalies.size,
                        analyzedCount = analyzed,
                        healthScore = computeHealthScore(anomalies, _uiState.value.latestField?.areaHectares ?: 0.0),
                    )
                }.launchIn(viewModelScope)
            }
        }.launchIn(viewModelScope)
    }

    private fun computeHealthScore(anomalies: List<Anomaly>, areaHa: Double): Int {
        if (anomalies.isEmpty()) return 95
        val sevWeight = anomalies.sumOf { it.severity ?: 2 }
        val density = sevWeight / maxOf(0.1, areaHa)
        return maxOf(20, (95 - density * 8 - sevWeight * 0.6).toInt())
    }
}

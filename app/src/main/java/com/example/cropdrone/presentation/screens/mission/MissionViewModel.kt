package com.example.cropdrone.presentation.screens.mission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cropdrone.domain.model.ConnectionState
import com.example.cropdrone.domain.model.MissionStatus
import com.example.cropdrone.domain.model.SafetyCheck
import com.example.cropdrone.domain.model.TelemetryData
import com.example.cropdrone.domain.model.Waypoint
import com.example.cropdrone.domain.usecase.EstimateMissionDurationUseCase
import com.example.cropdrone.domain.usecase.RunPreflightCheckUseCase
import com.example.cropdrone.hardware.DroneController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MissionUiState(
    val connection: ConnectionState = ConnectionState.DISCONNECTED,
    val missionStatus: MissionStatus = MissionStatus.IDLE,
    val waypoints: List<Waypoint> = emptyList(),
    val telemetry: TelemetryData? = null,
    val safetyChecks: List<SafetyCheck> = emptyList(),
    val preflightPassed: Boolean = false,
    val totalPhotos: Int = 0,
    val flaggedPhotos: Int = 0,
    val videoFramePath: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class MissionViewModel @Inject constructor(
    private val droneController: DroneController,
    private val preflightUseCase: RunPreflightCheckUseCase,
    private val estimateDurationUseCase: EstimateMissionDurationUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MissionUiState())
    val uiState: StateFlow<MissionUiState> = _uiState.asStateFlow()

    fun connect() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(connection = ConnectionState.CONNECTING)
            droneController.connect("192.168.4.1").collectLatest { state ->
                _uiState.value = _uiState.value.copy(connection = state)
                if (state == ConnectionState.CONNECTED) startTelemetryStream()
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            droneController.disconnect()
            _uiState.value = _uiState.value.copy(
                connection = ConnectionState.DISCONNECTED,
                missionStatus = MissionStatus.IDLE,
            )
        }
    }

    fun setWaypoints(waypoints: List<Waypoint>) {
        _uiState.value = _uiState.value.copy(waypoints = waypoints)
    }

    fun runPreflight(polygon: List<com.example.cropdrone.domain.model.LatLng>,
                     settings: com.example.cropdrone.domain.model.FlightPlanSettings,
                     batteryPct: Int) {
        val checks = preflightUseCase(_uiState.value.waypoints, polygon, settings, batteryPct)
        _uiState.value = _uiState.value.copy(
            safetyChecks = checks,
            preflightPassed = checks.all { it.ok },
        )
    }

    fun startMission() {
        val state = _uiState.value
        if (!state.preflightPassed || state.waypoints.size < 4 ||
            state.connection != ConnectionState.CONNECTED) return
        viewModelScope.launch {
            droneController.uploadMission(state.waypoints)
            droneController.startMission()
            _uiState.value = _uiState.value.copy(missionStatus = MissionStatus.FLYING)
        }
    }

    fun rth() {
        viewModelScope.launch {
            droneController.returnToHome()
            _uiState.value = _uiState.value.copy(missionStatus = MissionStatus.RETURNING)
        }
    }

    fun abort() {
        viewModelScope.launch {
            droneController.emergencyLand()
            _uiState.value = _uiState.value.copy(missionStatus = MissionStatus.ABORTED)
        }
    }

    private fun startTelemetryStream() {
        viewModelScope.launch {
            droneController.getTelemetry().collectLatest { telem ->
                _uiState.value = _uiState.value.copy(telemetry = telem)
            }
        }
        viewModelScope.launch {
            // Pass 1 inference loop on video frames
            // Real impl: use CropDiseaseClassifier on each Bitmap
        }
    }
}

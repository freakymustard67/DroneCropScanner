package com.example.cropdrone.presentation.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cropdrone.domain.model.BoundaryMode
import com.example.cropdrone.domain.model.FieldBoundary
import com.example.cropdrone.domain.model.FlightPlanSettings
import com.example.cropdrone.domain.model.LatLng
import com.example.cropdrone.domain.model.Waypoint
import com.example.cropdrone.domain.repository.FieldRepository
import com.example.cropdrone.domain.usecase.GenerateFlightPathUseCase
import com.google.android.gms.maps.model.LatLng as MapsLatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val boundaryMode: BoundaryMode = BoundaryMode.MANUAL,
    val drawingPolygon: List<LatLng> = emptyList(),
    val fieldName: String = "North Field A",
    val fieldSaved: FieldBoundary? = null,
    val flightSettings: FlightPlanSettings = FlightPlanSettings(),
    val waypoints: List<Waypoint> = emptyList(),
    val gpsWalkActive: Boolean = false,
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val fieldRepo: FieldRepository,
    private val generatePathUseCase: GenerateFlightPathUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun setBoundaryMode(mode: BoundaryMode) {
        _uiState.value = _uiState.value.copy(boundaryMode = mode, drawingPolygon = emptyList())
    }

    fun addPolygonPoint(p: LatLng) {
        _uiState.value = _uiState.value.copy(drawingPolygon = _uiState.value.drawingPolygon + p)
    }

    fun undoPolygonPoint() {
        _uiState.value = _uiState.value.copy(
            drawingPolygon = _uiState.value.drawingPolygon.dropLast(1)
        )
    }

    fun clearPolygon() {
        _uiState.value = _uiState.value.copy(
            drawingPolygon = emptyList(), fieldSaved = null, waypoints = emptyList(),
        )
    }

    fun closePolygon() {
        val poly = _uiState.value.drawingPolygon
        if (poly.size < 3) return
        if (poly.first() != poly.last()) {
            _uiState.value = _uiState.value.copy(drawingPolygon = poly + poly.first())
        }
    }

    fun setFieldName(name: String) {
        _uiState.value = _uiState.value.copy(fieldName = name)
    }

    fun updateFlightSettings(settings: FlightPlanSettings) {
        _uiState.value = _uiState.value.copy(flightSettings = settings)
    }

    fun generatePath() {
        val state = _uiState.value
        val poly = state.fieldSaved?.polygon ?: state.drawingPolygon
        if (poly.size < 3) return
        val wps = generatePathUseCase(poly, state.flightSettings)
        _uiState.value = _uiState.value.copy(waypoints = wps)
    }

    fun saveField() {
        val state = _uiState.value
        val poly = state.drawingPolygon
        if (poly.size < 4) return
        val closed = if (poly.first() == poly.last()) poly else poly + poly.first()
        viewModelScope.launch {
            val field = FieldBoundary(
                id = "field_${System.currentTimeMillis()}",
                name = state.fieldName,
                polygon = closed,
                areaHectares = 0.0, // computed in use case
                createdAt = System.currentTimeMillis(),
            )
            fieldRepo.saveField(field)
            _uiState.value = _uiState.value.copy(fieldSaved = field)
        }
    }

    /** GPS walk mode — add a point every 3 seconds (called by FusedLocationProvider callback). */
    fun onGpsWalkPoint(p: LatLng) {
        if (_uiState.value.gpsWalkActive) {
            _uiState.value = _uiState.value.copy(
                drawingPolygon = _uiState.value.drawingPolygon + p
            )
        }
    }

    fun startGpsWalk() { _uiState.value = _uiState.value.copy(gpsWalkActive = true, drawingPolygon = emptyList()) }
    fun stopGpsWalk() {
        val pts = _uiState.value.drawingPolygon
        val closed = if (pts.size >= 3 && pts.first() != pts.last()) pts + pts.first() else pts
        _uiState.value = _uiState.value.copy(gpsWalkActive = false, drawingPolygon = closed)
    }

    // ---- Helpers exposed to UI ----
    fun toMapsLatLng(p: LatLng): MapsLatLng = MapsLatLng(p.lat, p.lng)
    fun toLatLng(p: MapsLatLng): LatLng = LatLng(p.latitude, p.longitude)
}

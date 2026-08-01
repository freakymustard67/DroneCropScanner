package com.example.cropdrone.presentation.screens.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng as MapsLatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapScreen(
    navController: NavController,
    vm: MapViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val defaultCenter = MapsLatLng(28.61395, 77.20902)
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.Builder().target(defaultCenter).zoom(17f).build()
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            onMapClick = { latLng ->
                if (state.boundaryMode == com.example.cropdrone.domain.model.BoundaryMode.MANUAL) {
                    vm.addPolygonPoint(vm.toLatLng(latLng))
                }
            },
        ) {
            // Draw field polygon
            if (state.drawingPolygon.size >= 2) {
                Polygon(
                    points = state.drawingPolygon.map { vm.toMapsLatLng(it) },
                    strokeColor = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                    fillColor = androidx.compose.ui.graphics.Color(0x5566BB6A),
                    strokeWidth = 6f,
                )
            }
            // Draw flight path
            if (state.waypoints.size >= 2) {
                Polyline(
                    points = state.waypoints.map { vm.toMapsLatLng(com.example.cropdrone.domain.model.LatLng(it.lat, it.lng)) },
                    color = androidx.compose.ui.graphics.Color(0xFFFFC107),
                    width = 6f,
                )
            }
        }

        // Bottom panel
        Column(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.fieldName,
                onValueChange = { vm.setFieldName(it) },
                label = { Text("Field name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.undoPolygonPoint() }, modifier = Modifier.weight(1f)) { Text("Undo") }
                Button(onClick = { vm.clearPolygon() }, modifier = Modifier.weight(1f)) { Text("Clear") }
                Button(onClick = { vm.saveField() }, modifier = Modifier.weight(1f)) { Text("Save") }
            }
            Button(
                onClick = { vm.generatePath() },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.drawingPolygon.size >= 4,
            ) {
                Text(if (state.waypoints.isEmpty()) "Generate Flight Path"
                else "Regenerate Path (${state.waypoints.size} wps)")
            }
        }
    }
}

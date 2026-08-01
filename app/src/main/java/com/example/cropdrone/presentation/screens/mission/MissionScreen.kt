package com.example.cropdrone.presentation.screens.mission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.cropdrone.domain.model.ConnectionState
import com.example.cropdrone.domain.model.MissionStatus

@Composable
fun MissionScreen(
    navController: NavController,
    vm: MissionViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 16.dp, vertical = 32.dp)
        ) {
            Column {
                Text("Mission Control", color = Color.White,
                    fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (state.connection == ConnectionState.CONNECTED) "Drone connected"
                    else "Drone disconnected",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        when {
            state.connection == ConnectionState.DISCONNECTED -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Drone Not Connected", fontWeight = FontWeight.SemiBold)
                    Text("Connect to the drone's Wi-Fi network (ESP32-Drone-XXXX), then tap Connect.",
                        fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { vm.connect() }) { Text("Connect to Drone") }
                }
            }
            state.connection == ConnectionState.CONNECTING -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Connecting to ESP32…")
                }
            }
            state.missionStatus == MissionStatus.IDLE -> PreFlightPanel(state, vm)
            else -> InFlightPanel(state, vm)
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun PreFlightPanel(state: MissionUiState, vm: MissionViewModel) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Pre-flight Check", fontWeight = FontWeight.SemiBold)
                Text("${state.waypoints.size} waypoints", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TelemetryCell(Icons.Filled.BatteryFull, "Battery",
                        "${state.telemetry?.batteryPct ?: 100}%")
                    TelemetryCell(Icons.Filled.Height, "Altitude", "15m")
                    TelemetryCell(Icons.Filled.Speed, "Speed", "4m/s")
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { /* trigger preflight via callback */ },
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Run Pre-flight Safety Check")
                }
            }
        }

        if (state.safetyChecks.isNotEmpty()) {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Safety Checks", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    state.safetyChecks.forEach { check ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (check.ok) Icons.Filled.Flight else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (check.ok) Color(0xFF4CAF50) else Color(0xFFF44336),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(check.message, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { vm.startMission() },
                        enabled = state.preflightPassed,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Start Mission") }
                }
            }
        }
    }
}

@Composable
private fun InFlightPanel(state: MissionUiState, vm: MissionViewModel) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("LIVE", color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                state.telemetry?.let { t ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TelemetryCell(Icons.Filled.BatteryFull, "Battery", "${t.batteryPct}%")
                        TelemetryCell(Icons.Filled.Height, "Altitude", "${"%.1f".format(t.altitudeM)}m")
                        TelemetryCell(Icons.Filled.Speed, "Speed", "${"%.1f".format(t.speedMs)}m/s")
                        TelemetryCell(Icons.Filled.Home, "To Home", "${t.distanceToHomeM.toInt()}m")
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { vm.rth() },
                modifier = Modifier.weight(1f).height(48.dp),
            ) { Text("Return to Home") }
            Button(
                onClick = { vm.abort() },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
            ) { Text("Emergency Land") }
        }
    }
}

@Composable
private fun TelemetryCell(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}


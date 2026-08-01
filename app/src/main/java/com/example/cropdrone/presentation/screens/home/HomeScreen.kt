package com.example.cropdrone.presentation.screens.home

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
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.cropdrone.presentation.navigation.Screen

@Composable
fun HomeScreen(
    navController: NavController,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header (gradient)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 36.dp)
        ) {
            Column {
                Text("Crop Scanner", color = Color.White, fontSize = 12.sp)
                Text("Good day, Farmer", color = Color.White,
                    fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Field conditions look favorable",
                    color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Quick actions
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { navController.navigate(Screen.Map.route) },
                modifier = Modifier.weight(1f).height(80.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Map, contentDescription = null)
                    Text("New Scan", fontSize = 12.sp)
                }
            }
            Button(
                onClick = { navController.navigate(Screen.Dashboard.route) },
                modifier = Modifier.weight(1f).height(80.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Eco, contentDescription = null)
                    Text("Results", fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Health score card
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Eco, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Field Health Score", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${state.healthScore}",
                        fontSize = 36.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("/ 100", fontSize = 14.sp, color = Color.Gray)
                }
                LinearProgressIndicator(
                    progress = { state.healthScore / 100f },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                Text("Based on ${state.anomalyCount} flagged frames (${state.analyzedCount} analyzed).",
                    fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Latest mission card
        state.latestMission?.let { mission ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Latest Mission", fontWeight = FontWeight.SemiBold)
                    Text(mission.fieldName, fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCell(Icons.Filled.Flight, "Captured", "${mission.totalPhotos}")
                        StatCell(Icons.Filled.BatteryFull, "Flagged", "${mission.flaggedPhotos}")
                        StatCell(Icons.Filled.Eco, "Analyzed", "${mission.analyzedPhotos}")
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp)) // bottom nav clearance
    }
}

@Composable
private fun StatCell(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

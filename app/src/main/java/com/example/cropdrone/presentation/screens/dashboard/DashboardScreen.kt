package com.example.cropdrone.presentation.screens.dashboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.cropdrone.domain.model.Anomaly
import com.example.cropdrone.presentation.theme.SeverityCritical
import com.example.cropdrone.presentation.theme.SeverityHigh
import com.example.cropdrone.presentation.theme.SeverityLow
import com.example.cropdrone.presentation.theme.SeverityModerate

@Composable
fun DashboardScreen(
    navController: NavController,
    vm: DashboardViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()

    Column(Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 16.dp, vertical = 32.dp)
        ) {
            Column {
                Text("Post-flight Analysis", color = Color.White,
                    fontSize = 12.sp)
                Text(state.latestMission?.fieldName ?: "Field",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${state.healthScore}",
                        color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Text("/ 100", color = Color.White.copy(alpha = 0.8f))
                }
                LinearProgressIndicator(
                    progress = { state.healthScore / 100f },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = Color.White,
                )
            }
        }

        // Pending cloud analysis CTA
        val pending = state.anomalies.filter { it.analyzedAt == null }
        if (pending.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Cloud, contentDescription = null, tint = Color(0xFF1976D2))
                        Spacer(Modifier.size(8.dp))
                        Text("Cloud Analysis Pending",
                            fontWeight = FontWeight.SemiBold, color = Color(0xFF1565C0))
                    }
                    Text("${pending.size} flagged frame(s) awaiting Gemini 2.5 Flash diagnosis.",
                        fontSize = 12.sp, color = Color(0xFF1565C0))
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.runCloudAnalysis() },
                        enabled = !state.cloudAnalysisRunning,
                    ) {
                        Text(if (state.cloudAnalysisRunning) "Analyzing…" else "Run Cloud Analysis")
                    }
                }
            }
        }

        // Anomaly list
        Text("Anomalies (${state.anomalies.size})",
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp))
        LazyColumn {
            items(state.anomalies) { anomaly ->
                AnomalyCard(anomaly) { vm.selectAnomaly(anomaly) }
            }
        }
    }
}

@Composable
private fun AnomalyCard(anomaly: Anomaly, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = severityColor(anomaly.severity),
                            shape = RoundedCornerShape(50)
                        )
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    anomaly.diseaseName ?: "Pending: ${anomaly.pass1Label}",
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
            }
            Text("${"%.4f".format(anomaly.lat)}, ${"%.4f".format(anomaly.lng)}",
                fontSize = 11.sp, color = Color.Gray)
            if (anomaly.analyzedAt != null && anomaly.treatment != null) {
                Text(anomaly.treatment, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

private fun severityColor(severity: Int?): Color = when (severity) {
    null -> Color.Gray
    1, 2 -> SeverityLow
    3 -> SeverityModerate
    4 -> SeverityHigh
    else -> SeverityCritical
}

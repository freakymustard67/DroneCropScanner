package com.example.cropdrone.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun SettingsScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Drone Connection", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = "192.168.4.1",
                    onValueChange = {},
                    label = { Text("Drone IP") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("API Keys", fontWeight = FontWeight.SemiBold)
                Text("Placeholder values — inject real keys before deployment",
                    fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
                Spacer(Modifier.height(8.dp))
                ApiKeyField("Google Maps API Key", "YOUR_GOOGLE_MAPS_KEY")
                ApiKeyField("Gemini API Key", "YOUR_GEMINI_KEY")
                ApiKeyField("Copernicus Client ID", "SH_CLIENT_ID")
                ApiKeyField("Copernicus Client Secret", "SH_CLIENT_SECRET")
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("On-Device AI Model", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                SettingRow("Model file", "crop_disease_mobilenet.tflite")
                SettingRow("Architecture", "MobileNet V3 Small")
                SettingRow("Input size", "224 × 224 × 3")
                SettingRow("Classes", "4 (healthy, disease, pest, water_stress)")
                SettingRow("Inference target", "~30 FPS @ 15m altitude")
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Safety & Offline", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                ToggleRow("Auto RTH on low battery (<20%)", true)
                ToggleRow("Geofence (5m safety margin)", true)
                ToggleRow("Queue Pass 2 until internet reconnects", true)
                ToggleRow("Validate pre-flight safety checklist", true)
            }
        }
    }
}

@Composable
private fun ApiKeyField(label: String, placeholder: String) {
    var value by remember { mutableStateOf("") }
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
    )
}

@Composable
private fun SettingRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
        Text(value, fontSize = 12.sp)
    }
}

@Composable
private fun ToggleRow(label: String, defaultChecked: Boolean) {
    var checked by remember { mutableStateOf(defaultChecked) }
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 12.sp)
        Switch(checked = checked, onCheckedChange = { checked = it })
    }
}

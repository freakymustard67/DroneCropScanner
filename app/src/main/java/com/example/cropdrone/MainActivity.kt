package com.example.cropdrone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.cropdrone.presentation.navigation.CropDroneNavHost
import com.example.cropdrone.presentation.theme.CropDroneTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CropDroneTheme {
                CropDroneNavHost()
            }
        }
    }
}

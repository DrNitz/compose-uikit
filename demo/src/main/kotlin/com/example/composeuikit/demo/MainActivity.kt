package com.example.composeuikit.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.composeuikit.ComposeKitShowcase
import com.example.composeuikit.ComposeKitTopAppBar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        ComposeKitTopAppBar(
                            title = "Compose UI Kit",
                            subtitle = "Component demo",
                        )
                    },
                ) { innerPadding ->
                    Surface(modifier = Modifier.fillMaxSize()) {
                        ComposeKitShowcase(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

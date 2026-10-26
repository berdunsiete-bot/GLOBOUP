package com.aiastudio.globoup.arcade

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiastudio.globoup.arcade.ui.theme.MyApplicationTheme
import com.example.AdMobManager
import com.example.GloboUpScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun GlobUpScreen(modifier: Modifier = Modifier) {
    GloboUpScreen(modifier = modifier)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            Log.e("MainActivity", "EdgeToEdge warning: ${e.message}")
        }

        // Inicialización asíncrona de AdMob en segundo plano (Zero-Freeze)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AdsManager.getInstance(applicationContext).initializeSdk()
            } catch (e: Throwable) {
                Log.e("MainActivity", "Error inicializando AdMob en segundo plano: ${e.message}")
            }
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GlobUpScreen()
                }
            }
        }
    }
}

package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    try {
      enableEdgeToEdge()
    } catch (e: Throwable) {
      Log.w("MainActivity", "Edge-to-edge warning: ${e.message}")
    }
    super.onCreate(savedInstanceState)

    // Inicialización asíncrona y 100% segura en segundo plano para evitar congelamientos (Zero-Freeze)
    CoroutineScope(Dispatchers.IO).launch {
      try {
        AdMobManager.getInstance(applicationContext).initializeSdk()
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
          GloboUpScreen()
        }
      }
    }
  }
}


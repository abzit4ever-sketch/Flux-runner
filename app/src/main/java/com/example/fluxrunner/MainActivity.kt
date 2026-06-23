package com.example.fluxrunner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.fluxrunner.theme.FluxRunnerTheme

import android.graphics.drawable.ColorDrawable
import android.graphics.Color

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    window.setBackgroundDrawable(ColorDrawable(Color.parseColor("#090A15")))
    setContent {
      FluxRunnerTheme { Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color(0xFF090A15)) { MainNavigation() } }
    }
  }
}


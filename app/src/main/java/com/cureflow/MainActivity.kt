package com.cureflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cureflow.ui.main.MainScreen
import com.cureflow.ui.theme.CUREFLOWTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CUREFLOWTheme {
                MainScreen()
            }
        }
    }
}

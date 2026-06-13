package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.components.PlaySpaceAppContent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PlaySpaceViewModel
import com.example.ui.viewmodel.PlaySpaceViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Safely extract our preloaded database Repository instance
        val app = application as PlaySpaceApplication
        val repository = app.repository

        // Initialize viewModel
        val viewModel = ViewModelProvider(
            this,
            PlaySpaceViewModelFactory(repository)
        )[PlaySpaceViewModel::class.java]

        setContent {
            val isDark by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    PlaySpaceAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

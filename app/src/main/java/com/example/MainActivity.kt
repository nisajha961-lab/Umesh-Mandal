package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.AppDatabase
import com.example.data.AudioSynthEngine
import com.example.data.ScoreRepository
import com.example.data.SharedPrefManager
import com.example.ui.AppNavigation
import com.example.ui.AppViewModel
import com.example.ui.AppViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core Modules Init
        val sharedPrefs = SharedPrefManager(applicationContext)
        val synth = AudioSynthEngine(sharedPrefs)
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ScoreRepository(database.leaderboardDao(), sharedPrefs, applicationContext)

        // ViewModel Init using Factory Pattern
        val factory = AppViewModelFactory(application, repository, sharedPrefs, synth)
        val viewModel: AppViewModel by viewModels { factory }

        setContent {
            MyApplicationTheme {
                AppNavigation(viewModel = viewModel)
            }
        }
    }
}

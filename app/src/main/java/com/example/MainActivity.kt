package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.database.AppDatabase
import com.example.database.ComparisonRepository
import com.example.ui.CompareAnythingApp
import com.example.ui.ComparisonViewModel
import com.example.ui.ComparisonViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local Room database and repository
        val database = AppDatabase.getDatabase(this)
        val repository = ComparisonRepository(database.comparisonDao())

        setContent {
            MyApplicationTheme {
                // Instantiate our state-management ViewModel with Room repository
                val viewModel: ComparisonViewModel = viewModel(
                    factory = ComparisonViewModelFactory(repository)
                )

                // Render our high-fidelity Compare Anything Application
                CompareAnythingApp(viewModel = viewModel)
            }
        }
    }
}

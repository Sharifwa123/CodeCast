package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.db.CodeCastDatabase
import com.example.data.repository.CodeCastRepository
import com.example.ui.CodeCastApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CodeCastViewModel
import com.example.ui.viewmodel.CodeCastViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = CodeCastDatabase.getDatabase(applicationContext)
        val repository = CodeCastRepository(database.projectDao(), database.tutorialDao())
        val viewModelFactory = CodeCastViewModelFactory(repository)

        val viewModel: CodeCastViewModel by viewModels { viewModelFactory }

        setContent {
            MyApplicationTheme(darkTheme = true) {
                CodeCastApp(
                    viewModel = viewModel,
                    repository = repository
                )
            }
        }
    }
}

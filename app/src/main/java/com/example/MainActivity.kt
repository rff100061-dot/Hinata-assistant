package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.ui.WebConverterScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.WebConverterViewModel

class MainActivity : ComponentActivity() {

    private val webViewModel: WebConverterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                WebConverterScreen(
                    viewModel = webViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

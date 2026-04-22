package com.finance.manager.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.finance.manager.android.presentation.navigation.AppNavGraph
import com.finance.manager.android.ui.theme.FinanceTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinanceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}



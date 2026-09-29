package com.Mohammad.Elahi.terpsichore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.Mohammad.Elahi.terpsichore.di.AppContainer
import com.Mohammad.Elahi.terpsichore.ui.TerpsichoreApp
import com.Mohammad.Elahi.terpsichore.ui.theme.TerpsichoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = AppContainer(applicationContext)
        setContent {
            TerpsichoreTheme {
                TerpsichoreApp(container)
            }
        }
    }
}

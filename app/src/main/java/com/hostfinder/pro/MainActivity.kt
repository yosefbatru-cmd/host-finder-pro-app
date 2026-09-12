package com.hostfinder.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hostfinder.pro.ui.HostFinderApp
import com.hostfinder.pro.ui.theme.HostFinderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HostFinderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HostFinderApp()
                }
            }
        }
    }
}

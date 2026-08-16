package com.xw00232.watchchat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xw00232.watchchat.app.ui.AppNavHost
import com.xw00232.watchchat.app.ui.theme.WatchChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WatchChatTheme {
                AppNavHost((application as WatchChatApp).container)
            }
        }
    }
}

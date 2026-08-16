package com.xw00232.watchchat.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xw00232.watchchat.wear.ui.WearNavHost
import com.xw00232.watchchat.wear.ui.theme.WearTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WearTheme {
                WearNavHost((application as WearWatchChatApp).container)
            }
        }
    }
}

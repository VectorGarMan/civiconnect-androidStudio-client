package com.civiconnect.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.civiconnect.app.ui.CiviApp
import com.civiconnect.app.ui.theme.CiviTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CiviTheme {
                CiviApp()
            }
        }
    }
}

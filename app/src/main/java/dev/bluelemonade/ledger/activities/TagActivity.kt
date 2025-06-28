package dev.bluelemonade.ledger.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.bluelemonade.ledger.comm.AppSettings
import dev.bluelemonade.ledger.ui.theme.AppTheme
import dev.bluelemonade.ledger.views.TagView

class TagActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkMode by AppSettings.darkModeFlow(this).collectAsState(initial = false)
            AppTheme(darkTheme = darkMode) {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    TagView()
                } }
        }
    }
}
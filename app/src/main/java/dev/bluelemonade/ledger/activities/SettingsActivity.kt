package dev.bluelemonade.ledger.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.bluelemonade.ledger.comm.AppSettings
import dev.bluelemonade.ledger.ui.theme.AppTheme
import dev.bluelemonade.ledger.views.SettingsView

class SettingsActivity : ComponentActivity() {

    val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let {
                contentResolver.openOutputStream(uri)?.use { stream ->
                    AppSettings.pendingExportJson?.byteInputStream()?.copyTo(stream)
                    Toast.makeText(this, "파일이 성공적으로 저장되었습니다.", android.widget.Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkMode by AppSettings.darkModeFlow(this).collectAsState(initial = false)
            AppTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsView(
                        onBack = { finish() },
                        onExportRequest = { fileName ->
                            createDocumentLauncher.launch(fileName)
                        }
                    )
                }
            }
        }
    }

}
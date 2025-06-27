package dev.bluelemonade.ledger.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bluelemonade.ledger.ui.theme.AppTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("정보", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))

            SettingsItem("앱 버전 v1.0(1)") {}
            SettingsItem("개인정보 처리방침") {
                // TODO: Open WebView for policy
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("기타", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))

            SettingsItem("태그 초기화") {
                // TODO: Handle tag reset
            }
        }
    }
}

@Composable
fun SettingsItem(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewSettingsView() {
    AppTheme {
        SettingsView()
    }
}

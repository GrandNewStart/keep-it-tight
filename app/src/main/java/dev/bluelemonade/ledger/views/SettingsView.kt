package dev.bluelemonade.ledger.views

import android.content.Intent
import android.util.Log

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import dev.bluelemonade.ledger.activities.TagActivity
import dev.bluelemonade.ledger.activities.WebActivity
import dev.bluelemonade.ledger.comm.AppSettings
import dev.bluelemonade.ledger.comm.TagManager
import dev.bluelemonade.ledger.db.AppDatabase
import dev.bluelemonade.ledger.db.Expense
import dev.bluelemonade.ledger.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(onBack: () -> Unit = {}, onExportRequest: (String) -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).expenseDao() }
    var dialogData by remember { mutableStateOf<Triple<List<Expense>, List<Expense>, String>?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        "Unknown"
    }
    val versionCode = try {
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
    } catch (e: Exception) {
        0L
    }

    val scope = rememberCoroutineScope()
    val isDarkMode by AppSettings.darkModeFlow(context).collectAsState(initial = false)

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                try {
                    val input = context.contentResolver.openInputStream(uri)
                    val content = input?.bufferedReader()?.use { it.readText() } ?: ""
                    val jsonArray = JSONArray(content)

                    val imported = mutableListOf<Expense>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.optJSONObject(i)
                        if (obj != null) {
                            try {
                                imported.add(Expense.fromJSONObject(obj))
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context,
                                    "올바른 형식의 파일이 아닙니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }
                        }
                    }
                    if (imported.isEmpty()) {
                        Toast.makeText(context, "파일에 유효한 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val existing = dao.getAll().first()
                    val existingIds = existing.map { it.id }.toSet()
                    val (duplicate, original) = imported.partition { it.id in existingIds }

                    val message = "총 ${imported.size}개의 항목 중\n" +
                            "신규 항목: ${original.size}개\n" +
                            "중복 항목: ${duplicate.size}개\n\n" +
                            "어떻게 처리하시겠습니까?"

                    dialogData = Triple(imported, original, message)
                } catch (e: Exception) {
                    Toast.makeText(context, "파일을 읽을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    fun export() {
        scope.launch {
            val expenses = dao.getAll().first()
            val jsonArray = JSONArray()
            expenses.forEach {
                jsonArray.put(it.toJSONObject())
            }
            val timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            val fileName = "정신차려-$timestamp.json"
            AppSettings.pendingExportJson = jsonArray.toString() // store temporarily
            onExportRequest(fileName)
        }
    }

    fun reset() {
        scope.launch {
            TagManager.clearTags(context)
            dao.deleteAll()
            Toast.makeText(context, "데이터를 초기화하였습니다", Toast.LENGTH_SHORT).show()
        }
    }

    fun showPrivacyPolicy() {
        val intent = Intent(context, WebActivity::class.java)
        intent.putExtra(
            "url",
            "http://bluelemonade.co.kr/keep-it-tight/privacy-policy.html"
        )
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (dialogData != null) {
                AlertDialog(
                    onDismissRequest = {
                        dialogData = null
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                dao.insertAll(dialogData!!.first)
                                dialogData = null
                            }
                        }) { Text("모두 병합") }
                    },
                    dismissButton = {
                        Row {
                            TextButton(onClick = {
                                scope.launch {
                                    dao.insertAll(dialogData!!.second)
                                    dialogData = null
                                }
                            }) { Text("신규만 병합") }
                            TextButton(onClick = {
                                dialogData = null
                            }) {
                                Text("취소")
                            }
                        }
                    },
                    title = { Text("데이터 병합") },
                    text = { Text(dialogData!!.third) }
                )
            }

            if (showResetConfirm) {
                AlertDialog(
                    onDismissRequest = { showResetConfirm = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showResetConfirm = false
                            reset()
                        }) { Text("확인") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetConfirm = false }) { Text("취소") }
                    },
                    title = { Text("초기화 확인") },
                    text = { Text("정말로 모든 데이터를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.") }
                )
            }

            Text(
                "화면모드",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (isDarkMode) "다크 모드" else "라이트 모드", fontSize = 16.sp)
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = {
                        scope.launch {
                            AppSettings.setDarkMode(context, it)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color.Gray,
                        uncheckedTrackColor = Color.White,
                        checkedThumbColor = Color.White,
                        uncheckedThumbColor = Color.Gray
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "데이터 관리",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            SettingsItem("데이터 내보내기") {
                export()
            }
            SettingsItem("데이터 읽어오기") {
                importLauncher.launch(arrayOf("application/json"))
            }
            SettingsItem("태그 관리") {
                val intent = Intent(context, TagActivity::class.java)
                context.startActivity(intent)
            }
            Text(
                text = "초기화",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showResetConfirm = true }
                    .padding(vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "앱 정보",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            SettingsItem("개인정보 처리방침") {
                showPrivacyPolicy()
            }
            Text(
                text = "v$versionName ($versionCode)",
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
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
        SettingsView(onBack = {}, onExportRequest = {})
    }
}

package dev.bluelemonade.ledger.views

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bluelemonade.ledger.comm.TagManager
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

import dev.bluelemonade.ledger.db.AppDatabase
import dev.bluelemonade.ledger.db.Expense
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

// Bottom sheet for adding a new expense
@Composable
fun InputView(onSubmit: () -> Unit = {}) {
    var costText by remember { mutableStateOf("") }
    var isNegative by remember { mutableStateOf(true) }
    var nameText by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("선택") }
    var showTagMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = db.expenseDao()
    val coroutineScope = rememberCoroutineScope()
    val tags = remember { mutableStateOf(TagManager.getTags(context)) }

    val calendar = Calendar.getInstance()
    var date by remember { mutableLongStateOf(calendar.timeInMillis) }
    var time by remember { mutableLongStateOf(calendar.timeInMillis) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = costText,
                onValueChange = {
                    if (it.length <= 15 && it.all { c -> c.isDigit() }) costText = it
                },
                label = { Text("금액") },
                modifier = Modifier
                    .weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (isNegative) "지출" else "수입")
                Switch(
                    checked = !isNegative,
                    onCheckedChange = { isNegative = !it }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = nameText,
            onValueChange = {
                if (it.length <= 20) nameText = it
            },
            label = { Text("설명") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(10.dp)
        )

        Box {
            OutlinedTextField(
                value = selectedTag,
                onValueChange = {},
                label = { Text("태그") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTagMenu = true },
                shape = RoundedCornerShape(10.dp)
            )
            DropdownMenu(
                expanded = showTagMenu,
                onDismissRequest = { showTagMenu = false }
            ) {
                tags.value.forEach { tag ->
                    DropdownMenuItem(
                        text = { Text(tag) },
                        onClick = {
                            selectedTag = tag
                            showTagMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box {
                Text("날짜 선택")
            }
            Box {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateFormatter = DateFormat.getDateInstance()
                    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

                    Button(
                        onClick = {
                            DatePickerDialog(context).apply {
                                setOnDateSetListener { _, y, m, d ->
                                    val cal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, y)
                                        set(Calendar.MONTH, m)
                                        set(Calendar.DAY_OF_MONTH, d)
                                    }
                                    date = cal.timeInMillis
                                }
                            }.show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(56.dp)
                    ) {
                        Text(dateFormatter.format(Date(date)))
                    }

                    Button(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    val cal = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, h)
                                        set(Calendar.MINUTE, m)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    time = cal.timeInMillis
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(56.dp)
                    ) {
                        Text(timeFormatter.format(Date(time)))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (costText.isBlank() || nameText.isBlank()) return@Button

                val finalTag = if (selectedTag == "선택") "태그없음" else selectedTag
                val finalDate = Calendar.getInstance().apply {
                    timeInMillis = date
                    val t = Calendar.getInstance().apply { timeInMillis = time }
                    set(Calendar.HOUR_OF_DAY, t.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, t.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val expense = Expense(
                    id = UUID.randomUUID().toString(),
                    name = nameText,
                    cost = (if (isNegative) 1 else -1) * costText.toInt(),
                    tag = finalTag,
                    date = Instant.ofEpochMilli(finalDate).atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                )

                coroutineScope.launch {
                    dao.insert(expense)
                    onSubmit()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("입력")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewInputView() {
    InputView()
}
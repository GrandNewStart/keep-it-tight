package dev.bluelemonade.ledger.views

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bluelemonade.ledger.comm.TagManager
import dev.bluelemonade.ledger.db.AppDatabase
import dev.bluelemonade.ledger.db.Expense
import dev.bluelemonade.ledger.ui.theme.Red
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

@Composable
fun EditExpenseView(expense: Expense, onSubmit: () -> Unit = {}) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = db.expenseDao()
    val coroutineScope = rememberCoroutineScope()

    var costText by remember { mutableStateOf(expense.cost.absoluteValue.toString()) }
    var isNegative by remember { mutableStateOf(expense.cost < 0) }
    var nameText by remember { mutableStateOf(expense.name) }
    var selectedTag by remember { mutableStateOf(expense.tag.ifBlank { "태그없음" }) }
    var showTagMenu by remember { mutableStateOf(false) }
    val tags = remember { mutableStateOf(TagManager.getTags(context)) }

    val dateTime = remember { mutableStateOf(expense.date) }
    val date = remember { mutableStateOf(expense.date.toLocalDate()) }
    val time = remember { mutableStateOf(expense.date.toLocalTime()) }

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    Column(modifier = Modifier.padding(16.dp)) {
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
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("날짜 선택")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val localDate = date.value
                        val datePicker = DatePickerDialog(context)
                        datePicker.setOnDateSetListener { _, year, month, day ->
                            date.value = LocalDate.of(year, month + 1, day)
                        }
                        datePicker.show()
                    },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(dateFormatter.format(date.value))
                }

                Button(
                    onClick = {
                        val localTime = time.value
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                time.value = LocalTime.of(hour, minute)
                            },
                            localTime.hour,
                            localTime.minute,
                            true
                        ).show()
                    },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(timeFormatter.format(time.value))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        dao.delete(expense)
                        onSubmit()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Red),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("삭제")
            }

            Button(
                onClick = {
                    if (costText.isBlank() || nameText.isBlank()) return@Button

                    val combinedDateTime = LocalDateTime.of(date.value, time.value)
                    val finalTag = if (selectedTag == "선택") "태그없음" else selectedTag

                    val updatedExpense = expense.copy(
                        name = nameText,
                        cost = (if (isNegative) 1 else -1) * costText.toInt(),
                        tag = finalTag,
                        date = combinedDateTime
                    )

                    coroutineScope.launch {
                        dao.update(updatedExpense)
                        onSubmit()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("수정")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEditExpenseView() {
    val sampleExpense = Expense(
        id = "1",
        name = "Sample Expense",
        cost = -3000,
        tag = "Food",
        date = LocalDateTime.now()
    )
    EditExpenseView(expense = sampleExpense)
}
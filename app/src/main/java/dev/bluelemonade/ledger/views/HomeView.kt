package dev.bluelemonade.ledger.views

import android.content.Intent
import dev.bluelemonade.ledger.activities.SettingsActivity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowDropDown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import dev.bluelemonade.ledger.comm.TagManager
import dev.bluelemonade.ledger.db.Expense
import dev.bluelemonade.ledger.ui.theme.AppTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.material3.TopAppBar
import dev.bluelemonade.ledger.db.AppDatabase
import kotlin.math.absoluteValue
import androidx.compose.material3.ModalBottomSheet


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView() {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).expenseDao() }
    val expenses by dao.getAll().collectAsState(initial = emptyList())
    val displayExpenses = expenses

    var showInputSheet by remember { mutableStateOf(false) }
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }


    val grouped = displayExpenses.groupBy {
        it.date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    }

    var selectedMonth by remember { mutableStateOf("6월") }
    var selectedYear by remember { mutableStateOf("2025년") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text("정신차려")
            },
            actions = {
                IconButton(onClick = { showInputSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
                IconButton(onClick = {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondary)
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "금액",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
            Text(
                "설명",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
            Text(
                "태그",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            grouped.forEach { (date, items) ->
                item {
                    Text(
                        text = date,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                items(items) { expense ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable { selectedExpense = expense },
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val color = when {
                            expense.cost > 0 -> MaterialTheme.colorScheme.error
                            expense.cost < 0 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Text(
                            text = "%,d".format(expense.cost.absoluteValue),
                            color = color,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = expense.name,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = expense.tag,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {
            var selectedTag by remember { mutableStateOf("전체") }

            // Calculate today's total based on filtered expenses and selected tag
            val today = LocalDateTime.now().toLocalDate()
            val filteredTodayExpenses = displayExpenses.filter {
                it.date.toLocalDate() == today && (selectedTag == "전체" || it.tag == selectedTag)
            }
            val todayTotal = filteredTodayExpenses.sumOf { it.cost }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    Text(text = "오늘의 지출", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "%,d".format(todayTotal.absoluteValue),
                        color = when {
                            todayTotal > 0 -> MaterialTheme.colorScheme.error
                            todayTotal < 0 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                TagPicker(selectedTag = selectedTag, onTagSelected = { selectedTag = it })
            }

            // Calculate monthly total for selected month
            val monthInt = selectedMonth.removeSuffix("월").toIntOrNull()
            val filteredMonthlyExpenses = displayExpenses.filter {
                it.date.monthValue == monthInt
            }
            val monthlyTotal = filteredMonthlyExpenses.sumOf { it.cost }

            // Calculate yearly total for selected year
            val yearInt = selectedYear.removeSuffix("년").toIntOrNull()
            val filteredYearlyExpenses = displayExpenses.filter {
                it.date.year == yearInt
            }
            val yearlyTotal = filteredYearlyExpenses.sumOf { it.cost }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    Text(text = "이달의 지출", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "%,d".format(monthlyTotal.absoluteValue),
                        color = when {
                            monthlyTotal > 0 -> MaterialTheme.colorScheme.error
                            monthlyTotal < 0 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                MonthPicker(selectedMonth = selectedMonth, onMonthSelected = { selectedMonth = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    Text(text = "올해의 지출", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "%,d".format(yearlyTotal.absoluteValue),
                        color = when {
                            yearlyTotal > 0 -> MaterialTheme.colorScheme.error
                            yearlyTotal < 0 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                YearPicker(selectedYear = selectedYear, onYearSelected = { selectedYear = it })
            }
        }
        // Bottom Sheet for adding a new expense
        // Implementation omitted for brevity
    }
    if (showInputSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInputSheet = false },
            modifier = Modifier.fillMaxHeight(0.95f)
        ) {
            InputView(onSubmit = { showInputSheet = false })
        }
    }
    if (selectedExpense != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedExpense = null },
            modifier = Modifier.fillMaxHeight(0.95f)
        ) {
            EditExpenseView(expense = selectedExpense!!, onSubmit = { selectedExpense = null })
        }
    }
}

@Composable
fun TagPicker(
    selectedTag: String,
    onTagSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val tags = remember { mutableStateOf(TagManager.getTags(context)) }
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selectedTag)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf("전체") + tags.value.forEach { tag ->
                DropdownMenuItem(
                    text = { Text(tag) },
                    onClick = {
                        onTagSelected(tag)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MonthPicker(
    selectedMonth: String,
    onMonthSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val months = (1..12).map { "${it}월" }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selectedMonth)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            months.forEach { month ->
                DropdownMenuItem(
                    text = { Text(month) },
                    onClick = {
                        onMonthSelected(month)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun YearPicker(
    selectedYear: String,
    onYearSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentYear = LocalDateTime.now().year
    val years = (currentYear - 10..currentYear + 1).map { "${it}년" }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selectedYear)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            years.forEach { year ->
                DropdownMenuItem(
                    text = { Text(year) },
                    onClick = {
                        onYearSelected(year)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeView() {
    val expenses = listOf(
        Expense(cost = 2100, name = "콜라", tag = "아빠 카드", date = LocalDateTime.now().minusDays(1)),
        Expense(cost = 5000, name = "저녁", tag = "아빠 카드", date = LocalDateTime.now().minusDays(1)),
        Expense(cost = -500000, name = "아빠 용돈", tag = "내 카드", date = LocalDateTime.now().minusDays(1)),
        Expense(cost = 14000, name = "간식", tag = "아빠 카드", date = LocalDateTime.now().minusDays(1)),
        Expense(cost = -500000, name = "은혜 월급", tag = "내 카드", date = LocalDateTime.now().minusDays(1)),

        Expense(cost = -1000000, name = "태강 월세", tag = "내 카드", date = LocalDateTime.now().minusDays(2)),
        Expense(cost = 7600, name = "저녁", tag = "아빠 카드", date = LocalDateTime.now().minusDays(2)),
        Expense(cost = 3800, name = "커피", tag = "아빠 카드", date = LocalDateTime.now().minusDays(2)),

        Expense(cost = 14000, name = "간식", tag = "아빠 카드", date = LocalDateTime.now().minusDays(3)),
        Expense(cost = 7600, name = "저녁", tag = "아빠 카드", date = LocalDateTime.now().minusDays(3)),
        Expense(cost = 3800, name = "커피", tag = "아빠 카드", date = LocalDateTime.now().minusDays(3)),

        Expense(cost = 14000, name = "간식", tag = "아빠 카드", date = LocalDateTime.now().minusDays(4)),
        Expense(cost = 7600, name = "저녁", tag = "아빠 카드", date = LocalDateTime.now().minusDays(4)),
        Expense(cost = 3800, name = "커피", tag = "아빠 카드", date = LocalDateTime.now().minusDays(4)),

        Expense(cost = 14000, name = "간식", tag = "아빠 카드", date = LocalDateTime.now().minusDays(5)),
        Expense(cost = 7600, name = "저녁", tag = "아빠 카드", date = LocalDateTime.now().minusDays(5)),
        Expense(cost = 3800, name = "커피", tag = "아빠 카드", date = LocalDateTime.now().minusDays(5)),

        Expense(cost = 1000000, name = "월세", tag = "아빠 카드", date = LocalDateTime.now().minusDays(6)),
        Expense(cost = 100000, name = "통신비", tag = "아빠 카드", date = LocalDateTime.now().minusDays(6)),
        Expense(cost = 3800, name = "커피", tag = "아빠 카드", date = LocalDateTime.now().minusDays(6)),
    )
    AppTheme {
        HomeView()
    }
}

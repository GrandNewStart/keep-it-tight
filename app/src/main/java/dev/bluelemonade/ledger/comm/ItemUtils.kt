package dev.bluelemonade.ledger.comm

import dev.bluelemonade.ledger.db.Expense
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ItemUtils {

    fun getAllMonths(items: List<Expense>): List<String> {
        val format = SimpleDateFormat("yyyy.MM", Locale.getDefault())
        val sortedItems = items.sortedByDescending { it.date }
        val months = ArrayList<String>()
        sortedItems.forEach { item ->
            val date = Date(item.date.toLong())
            val dateStr = format.format(date)
            if (!months.contains(dateStr)) {
                months.add(dateStr)
            }
        }
        if (months.isEmpty()) {
            months.add(format.format(Date()))
        }
        return months
    }

    fun filterByMonth(items: List<Expense>, monthStr: String): List<Expense> {
        val format = SimpleDateFormat("yyyy.MM", Locale.getDefault())
        return items.filter { item ->
            val year = monthStr.substring(0, 4).toInt()
            val month = monthStr.substring(5, 7).toInt()
            val itemDate = Date(item.date.toLong())
            val itemDateStr = format.format(itemDate)
            val itemYear = itemDateStr.substring(0, 4).toInt()
            val itemMonth = itemDateStr.substring(5, 7).toInt()
            return@filter itemYear == year && itemMonth == month
        }
    }

}
package dev.bluelemonade.ledger.comm

import dev.bluelemonade.ledger.db.Expense
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
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

    fun getSum(items: List<Expense>, dateStr: String, tag: String): List<Int> {
        var dayTotal = 0
        var monthTotal = 0
        var yearTotal = 0
        val date = DateUtils.formatDateTimeToTimestamp(dateStr)
        val calendar = Calendar.getInstance()
        calendar.time = date
        val targetDay = calendar.get(Calendar.DAY_OF_MONTH)
        val targetMonth = calendar.get(Calendar.MONTH) + 1
        val targetYear = calendar.get(Calendar.YEAR)

        items
            .filter { tag == Strings.all_tag || it.tag == tag }
            .forEach {
                val expenseDate = Date(it.date.toLong())
                calendar.time = expenseDate
                if (calendar.get(Calendar.YEAR) == targetYear) {
                    yearTotal += it.cost
                    if (calendar.get(Calendar.MONTH) + 1 == targetMonth) {
                        monthTotal += it.cost
                        if (calendar.get(Calendar.DAY_OF_MONTH) == targetDay) {
                            dayTotal += it.cost
                        }
                    }
                }
            }
        return listOf(yearTotal, monthTotal, dayTotal)
    }

    fun convertToJsonArray(items: List<Expense>): List<JSONObject> {
        return items.map {
            JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("date", it.date)
                put("cost", it.cost)
                put("tag", it.tag)
            }
        }
    }

    fun parseJsonArray(jsonString: String): List<Expense> {
        val jsonArray = org.json.JSONArray(jsonString)
        val importedItems = mutableListOf<Expense>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue

            val id: String? = obj.optString("id", null)
            val name: String? = obj.optString("name", null)
            val date: String? = obj.optString("date", null)
            val cost = obj.optInt("cost", Int.MIN_VALUE)
            val tag: String? = obj.optString("tag", null)

            if (id == null || name == null || date == null || tag == null || cost == Int.MIN_VALUE) {
                continue
            }

            importedItems.add(Expense(id, date,name, cost, tag))
        }
        return importedItems
    }

}
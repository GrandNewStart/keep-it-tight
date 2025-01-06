package dev.jinwoo.reflection.expense

import dev.jinwoo.reflection.db.Expense

sealed class RecyclerViewItem {
    data class Header(val date: String) : RecyclerViewItem()
    data class ExpenseItem(val expense: Expense) : RecyclerViewItem()
}
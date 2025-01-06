package dev.bluelemonade.ledger.expense

import dev.bluelemonade.ledger.db.Expense

sealed class RecyclerViewItem {
    data class Header(val date: String) : RecyclerViewItem()
    data class ExpenseItem(val expense: Expense) : RecyclerViewItem()
}
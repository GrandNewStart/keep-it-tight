package dev.jinwoo.ledger.expense

import dev.jinwoo.ledger.db.Expense

sealed class RecyclerViewItem {
    data class Header(val date: String) : RecyclerViewItem()
    data class ExpenseItem(val expense: Expense) : RecyclerViewItem()
}
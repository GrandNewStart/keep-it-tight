package dev.jinwoo.ledger.db

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    suspend fun insert(expense: Expense) = expenseDao.insert(expense)

    suspend fun update(expense: Expense) = expenseDao.update(expense)

    suspend fun delete(expense: Expense) = expenseDao.delete(expense)

    suspend fun deleteAll() = expenseDao.deleteAll()

    suspend fun getAllExpenses() = expenseDao.getAllExpenses()

    suspend fun getExpenseById(id: Int) = expenseDao.getExpenseById(id)
}
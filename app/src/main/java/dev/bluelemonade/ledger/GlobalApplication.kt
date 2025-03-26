package dev.bluelemonade.ledger

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.room.Room
import dev.bluelemonade.ledger.comm.DateUtils
import dev.bluelemonade.ledger.comm.Storage
import dev.bluelemonade.ledger.comm.Theme
import dev.bluelemonade.ledger.db.Expense
import dev.bluelemonade.ledger.db.ExpenseDatabase
import dev.bluelemonade.ledger.db.ExpenseRepository
import dev.bluelemonade.ledger.db.MIGRATION_1_2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class GlobalApplication : Application() {

    private lateinit var storage: Storage
    private lateinit var database: ExpenseDatabase
    private lateinit var repository: ExpenseRepository
    val tagsLiveData = MutableLiveData<List<String>>(listOf())
    val themeLiveData = MutableLiveData(Theme.Light)
    val itemsLiveData = MutableLiveData<List<Expense>>(listOf())

    val items: List<Expense> get() = itemsLiveData.value!!
    val tags: List<String> get() = tagsLiveData.value!!
    val theme: Theme get() = themeLiveData.value!!

    override fun onCreate() {
        super.onCreate()
        instance = this
        storage = Storage(this)
        tagsLiveData.postValue(storage.getTags())
        themeLiveData.postValue(storage.getTheme())
        database = ExpenseDatabase.getDatabase(this)
        repository = ExpenseRepository(database.expenseDao())
        loadItems()

    }

    private fun loadItems() {
        CoroutineScope(Dispatchers.IO).launch {
            val items = repository.getAllExpenses().sortedByDescending { it.date.toLong() }
            itemsLiveData.postValue(items)
        }
    }

    fun addItem(name: String, cost: Int, tag: String) {
        CoroutineScope(Dispatchers.Default).launch {
            repository.insert(
                Expense(
                    date = DateUtils.createTimestamp().toString(),
                    cost = cost,
                    name = name,
                    tag = tag
                )
            )
            loadItems()
        }
    }

    fun addItems(items: List<Expense>) {
        CoroutineScope(Dispatchers.Default).launch {
            repository.insert(items)
            loadItems()
        }
    }

    fun updateItem(id: String, name: String, cost: Int, tag: String, date: Date) {
        CoroutineScope(Dispatchers.Default).launch {
            repository.update(
                Expense(
                    id = id,
                    date = DateUtils.createTimestamp(date).toString(),
                    cost = cost,
                    name = name,
                    tag = tag
                )
            )
            loadItems()
        }
    }

    fun deleteItem(item: Expense) {
        CoroutineScope(Dispatchers.Default).launch {
            repository.delete(item)
            loadItems()
        }
    }

    fun setTheme(theme: Theme) {
        storage.setTheme(theme)
        themeLiveData.postValue(theme)
    }

    fun setTags(tags: List<String>) {
        storage.setTags(tags)
        tagsLiveData.postValue(tags)
    }

    fun reset() {
        CoroutineScope(Dispatchers.Default).launch {
            repository.deleteAll()
            itemsLiveData.postValue(listOf())
            storage.clear()
            tagsLiveData.postValue(listOf())
        }
    }

    companion object {
        lateinit var instance: GlobalApplication
    }



}
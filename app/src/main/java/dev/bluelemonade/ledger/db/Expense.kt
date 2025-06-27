package dev.bluelemonade.ledger.db

import androidx.room.*
import java.time.LocalDateTime
import java.util.UUID

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val cost: Int,
    val name: String,
    val tag: String,
    val date: LocalDateTime,
)
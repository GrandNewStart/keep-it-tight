package dev.bluelemonade.ledger.db

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.UUID

@Entity(tableName = "expense_table")
data class Expense(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val cost: Int,
    val name: String,
    val tag: String,
    val date: LocalDateTime,
) {

    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("cost", cost)
            put("name", name)
            put("tag", tag)
            put(
                "date",
                date.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli().toString()
            )
        }
    }

    companion object {
        fun fromJSONObject(jsonObject: JSONObject): Expense {
            val id = jsonObject.getString("id")
            val name = jsonObject.getString("name")
            val cost = jsonObject.getInt("cost")
            val tag = jsonObject.getString("tag")
            val date = jsonObject.getString("date")
            val dateTime = java.time.Instant.ofEpochMilli(date.toLong())
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime()
            return Expense(id = id, cost = cost, name = name, tag = tag, date = dateTime)
        }
    }

}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Step 1: Add temporary column for UUIDs
        db.execSQL("ALTER TABLE expense_table ADD COLUMN new_id TEXT")

        // Step 2: Fill UUIDs into the temporary column
        val cursor = db.query("SELECT rowid FROM expense_table")
        while (cursor.moveToNext()) {
            val rowId = cursor.getInt(0)
            val uuid = UUID.randomUUID().toString()
            db.execSQL("UPDATE expense_table SET new_id = ? WHERE rowid = ?", arrayOf(uuid, rowId))
        }
        cursor.close()

        // Create new table with UUID as PK
        db.execSQL(
            """
            CREATE TABLE expense_table_new (
                id TEXT PRIMARY KEY NOT NULL,
                date TEXT NOT NULL,
                name TEXT NOT NULL,
                cost INTEGER NOT NULL,
                tag TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Copy data to new table
        // Step 4: Copy all data from old table into new table, renaming new_id to id
        db.execSQL(
            """
            INSERT INTO expense_table_new (id, date, name, cost, tag)
            SELECT new_id, date, name, cost, tag FROM expense_table
            """.trimIndent()
        )

        // Drop old table and rename new one
        db.execSQL("DROP TABLE expense_table")
        db.execSQL("ALTER TABLE expense_table_new RENAME TO expense_table")
    }
}
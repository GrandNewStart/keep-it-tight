package dev.bluelemonade.ledger.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Entity(tableName = "expense_table")
data class Expense(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "cost") val cost: Int,
    @ColumnInfo(name = "tag") val tag: String
)

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

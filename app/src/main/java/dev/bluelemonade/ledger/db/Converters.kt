package dev.bluelemonade.ledger.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class Converters {
    @TypeConverter
    fun fromDateTime(dateTime: LocalDateTime): String {
        val instant = dateTime.atZone(ZoneId.systemDefault()).toInstant()
        return instant.toEpochMilli().toString()
    }

    @TypeConverter
    fun toDateTime(timestamp: String): LocalDateTime {
        val millis = timestamp.toLong()
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }
}
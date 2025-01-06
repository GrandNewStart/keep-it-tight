package dev.jinwoo.reflection

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateUtils {

    companion object {
        fun formatTimestampToDateOnly(timestamp: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = Date(timestamp)
            return sdf.format(date)
        }

        fun formatTimestampToDateTime(timestamp: Long): String {
            val date = Date(timestamp)
            val format = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())
            return format.format(date)
        }
    }

}
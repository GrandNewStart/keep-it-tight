package dev.bluelemonade.ledger.comm

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import androidx.core.content.edit

object TagManager {
    private const val PREF_NAME = "ledger"
    private const val KEY_TAGS = "tags"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getTags(context: Context): List<String> {
        val json = getPrefs(context).getString(KEY_TAGS, null) ?: return emptyList()
        val jsonArray = JSONArray(json)
        return List(jsonArray.length()) { jsonArray.getString(it) }
    }

    fun addTag(context: Context, tag: String) {
        val tags = getTags(context).toMutableList()
        if (tag !in tags) {
            tags.add(tag)
            saveTags(context, tags)
        }
    }

    fun removeTag(context: Context, tag: String) {
        val tags = getTags(context).toMutableList()
        if (tags.remove(tag)) {
            saveTags(context, tags)
        }
    }

    private fun saveTags(context: Context, tags: List<String>) {
        val jsonArray = JSONArray()
        tags.forEach { jsonArray.put(it) }
        getPrefs(context).edit { putString(KEY_TAGS, jsonArray.toString()) }
    }
}
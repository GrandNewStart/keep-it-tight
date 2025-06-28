package dev.bluelemonade.ledger.comm

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import androidx.core.content.edit

object TagManager {
    private const val PREF_NAME = "정신차려"
    private const val KEY_TAGS = "tags"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getTags(context: Context): List<String> {
        val list = getPrefs(context).getStringSet(KEY_TAGS, null)?.toMutableList() ?: return emptyList()
        if (!list.contains("태그없음")) {
            list.add("태그없음")
        }
        return list
    }

    fun addTag(context: Context, tag: String) {
        if (tag == "태그없음") return
        val tags = getTags(context).toMutableList()
        if (tag !in tags) {
            tags.add(tag)
            saveTags(context, tags)
        }
    }

    fun removeTag(context: Context, tag: String) {
        if (tag == "태그없음") return
        val tags = getTags(context).toMutableList()
        if (tags.remove(tag)) {
            saveTags(context, tags)
        }
    }

    fun clearTags(context: Context) {
        saveTags(context, listOf())
    }

    fun saveTags(context: Context, tags: List<String>) {
        getPrefs(context).edit { putStringSet(KEY_TAGS, tags.toSet()) }
    }
}
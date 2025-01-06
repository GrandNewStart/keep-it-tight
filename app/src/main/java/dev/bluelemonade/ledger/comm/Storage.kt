package dev.bluelemonade.ledger.comm

import android.content.Context
import android.content.SharedPreferences
import dev.bluelemonade.ledger.R

class Storage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)

    fun getTheme(): Theme {
        val string = prefs.getString("theme", Theme.Dark.name)!!
        return if (string == Theme.Dark.name) {
            Theme.Dark
        } else {
            Theme.Light
        }
    }

    fun setTheme(theme: Theme) {
        prefs.edit().let { editor ->
            editor.putString("theme", theme.toString())
            editor.apply()
        }
    }

    fun getTags(): List<String> {
        return prefs.getStringSet("tags", emptySet())!!.toList()
    }

    fun setTags(tags: List<String>) {
        prefs.edit().let { editor ->
            editor.putStringSet("tags", tags.toSet())
            editor.apply()
        }
    }

    fun clear() {
        prefs.edit().let { editor ->
            editor.clear()
            editor.apply()
        }
    }

}
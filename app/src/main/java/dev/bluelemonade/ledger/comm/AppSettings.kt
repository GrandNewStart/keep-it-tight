package dev.bluelemonade.ledger.comm

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object AppSettings {
    private val Context.dataStore by preferencesDataStore(name = "ledger")
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

    var pendingExportJson: String? = null

    fun darkModeFlow(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }
    }

    suspend fun setDarkMode(context: Context, enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[DARK_MODE_KEY] = enabled
        }
    }

}
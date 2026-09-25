package preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(
    name = "user_preferences"
)

class PreferencesManager(
    private val context: Context
) {
    companion object {
        private val DARK_MODE =
            booleanPreferencesKey("dark_mode")

        private val LARGE_TEXT =
            booleanPreferencesKey("large_text")

        private val NOTIFICATIONS_ENABLED =
            booleanPreferencesKey("notifications_enabled")
    }

    val darkModeFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[DARK_MODE] ?: false
        }

    val largeTextFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[LARGE_TEXT] ?: false
        }

    val notificationsFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: true
        }

    suspend fun saveDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE] = enabled
        }
    }

    suspend fun saveLargeText(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LARGE_TEXT] = enabled
        }
    }

    suspend fun saveNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }
}
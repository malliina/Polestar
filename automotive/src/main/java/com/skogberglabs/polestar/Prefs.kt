package com.skogberglabs.polestar

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

data class UserPreferences(val carId: String?, val language: String?)

private val Context.dataStore by preferencesDataStore(
    name = "user_preferences"
)

interface DataSource {
    fun userPreferencesFlow(): Flow<UserPreferences>
    suspend fun saveCarId(carId: String?)
    suspend fun saveLanguage(code: String)
}

class LocalDataSource(private val context: Context) : DataSource {
    override fun userPreferencesFlow(): Flow<UserPreferences> =
        context.dataStore.data.map { preferences ->
            UserPreferences(
                preferences[PreferencesKeys.CarId],
                preferences[PreferencesKeys.LanguageCode]
            )
        }
    override suspend fun saveCarId(carId: String?) {
        context.dataStore.edit { preferences ->
            carId?.let { id ->
                preferences[PreferencesKeys.CarId] = id
                Timber.i("Using car '$id'.")
            } ?: run {
                preferences.remove(PreferencesKeys.CarId)
                Timber.i("Unselected car.")
            }
        }
    }

    override suspend fun saveLanguage(code: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LanguageCode] = code
            Timber.i("Saved language ${code}.")
        }
    }
}

private object PreferencesKeys {
    val CarId = stringPreferencesKey("carId2")
    val LanguageCode = stringPreferencesKey("language")
}

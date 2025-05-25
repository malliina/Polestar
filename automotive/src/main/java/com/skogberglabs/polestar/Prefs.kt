package com.skogberglabs.polestar

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

data class UserPreferences(val carId: String?, val language: String?, val carConf: CarConf?) {
    companion object {
        val empty = UserPreferences(null, null, null)
    }

    var lang: CarLang? = carConf?.languages?.firstOrNull { l -> l.language.code == language } ?: carConf?.languages?.firstOrNull()
}

private val Context.dataStore by preferencesDataStore(
    name = "user_preferences",
)

interface DataSource {
    fun userPreferencesFlow(): Flow<UserPreferences>

    suspend fun saveCarId(carId: String?)

    suspend fun saveLanguage(code: String)

    suspend fun saveConf(conf: CarConf): UserPreferences
}

class LocalDataSource(private val context: Context) : DataSource {
    override fun userPreferencesFlow(): Flow<UserPreferences> =
        context.dataStore.data.map { preferences ->
            parse(preferences)
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
            Timber.i("Saved language $code.")
        }
    }

    override suspend fun saveConf(conf: CarConf): UserPreferences {
        val snapshot =
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.Conf] = JsonConf.encode(conf, CarConf.serializer())
            }
        return parse(snapshot)
    }

    private fun parse(preferences: Preferences): UserPreferences =
        UserPreferences(
            preferences[PreferencesKeys.CarId],
            preferences[PreferencesKeys.LanguageCode],
            preferences[PreferencesKeys.Conf]?.let { str ->
                try {
                    JsonConf.decode(str, CarConf.serializer())
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse cached conf. This is normal if new keys have been introduced.")
                    null
                }
            },
        )
}

private object PreferencesKeys {
    val CarId = stringPreferencesKey("carId2")
    val Conf = stringPreferencesKey("conf")
    val LanguageCode = stringPreferencesKey("language")
}

package com.skogberglabs.polestar

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import timber.log.Timber

@Serializable
data class CarRef(val id: String, val token: String) {
    override fun toString(): String = "car '$id'"
}

data class UserPreferences(val selectedCar: CarRef?, val language: String?, val carConf: CarConf?) {
    val carId: String? get() = selectedCar?.id

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

    suspend fun saveCar(car: CarRef?)

    suspend fun saveLanguage(code: String)

    suspend fun saveConf(conf: CarConf): UserPreferences
}

class LocalDataSource(private val context: Context) : DataSource {
    override fun userPreferencesFlow(): Flow<UserPreferences> =
        context.dataStore.data.map { preferences ->
            parse(preferences)
        }

    override suspend fun saveCar(car: CarRef?) {
        context.dataStore.edit { preferences ->
            car?.let { ref ->
                preferences[PreferencesKeys.Car] = JsonConf.encode(ref)
                Timber.i("Using $ref.")
            } ?: run {
                preferences.remove(PreferencesKeys.Car)
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
                preferences[PreferencesKeys.Conf] = JsonConf.encode(conf)
            }
        return parse(snapshot)
    }

    private fun parse(preferences: Preferences): UserPreferences =
        UserPreferences(
            preferences[PreferencesKeys.Car]?.let { str ->
                try {
                    JsonConf.decode(str)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse selected car. This is normal if new keys have been introduced.")
                    null
                }
            },
            preferences[PreferencesKeys.LanguageCode],
            preferences[PreferencesKeys.Conf]?.let { str ->
                try {
                    JsonConf.decode(str)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse cached conf. This is normal if new keys have been introduced.")
                    null
                }
            },
        )
}

private object PreferencesKeys {
    val Car = stringPreferencesKey("car")
    val Conf = stringPreferencesKey("conf")
    val LanguageCode = stringPreferencesKey("language")
}

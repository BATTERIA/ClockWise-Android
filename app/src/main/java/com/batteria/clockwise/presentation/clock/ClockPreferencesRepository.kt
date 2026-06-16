package com.batteria.clockwise.presentation.clock

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.clockDataStore by preferencesDataStore(name = "clockwise_prefs")

@Singleton
class ClockPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store get() = context.clockDataStore

    val state: Flow<ClockUiState> = store.data.map { prefs ->
        ClockUiState(
            timeFormat   = TimeFormat.fromKey(prefs[KEY_FORMAT]),
            language     = Language.fromKey(prefs[KEY_LANG]),
            showSeconds  = prefs[KEY_SHOW_SECONDS] ?: false,
            voiceGender  = VoiceGender.fromKey(prefs[KEY_VOICE_GENDER]),
        )
    }

    suspend fun setTimeFormat(format: TimeFormat) {
        store.edit { it[KEY_FORMAT] = format.key }
    }

    suspend fun setLanguage(lang: Language) {
        store.edit { it[KEY_LANG] = lang.key }
    }

    suspend fun setShowSeconds(show: Boolean) {
        store.edit { it[KEY_SHOW_SECONDS] = show }
    }

    suspend fun setVoiceGender(gender: VoiceGender) {
        store.edit { it[KEY_VOICE_GENDER] = gender.key }
    }

    private companion object {
        val KEY_FORMAT = stringPreferencesKey("time_format")
        val KEY_LANG = stringPreferencesKey("language")
        val KEY_SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        val KEY_VOICE_GENDER = stringPreferencesKey("voice_gender")
    }
}

package com.salesgoals.app.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPrefs(private val context: Context) {

    private val keyMode = stringPreferencesKey("mode")            // "advisor" | "manager"
    private val keyDays = intPreferencesKey("working_days")
    private val keyOnboarded = booleanPreferencesKey("onboarded")

    suspend fun getMode(): String? = context.dataStore.data.map { it[keyMode] }.first()
    suspend fun setMode(mode: String) { context.dataStore.edit { it[keyMode] = mode } }

    suspend fun getWorkingDays(): Int = context.dataStore.data.map { it[keyDays] ?: 22 }.first()
    suspend fun setWorkingDays(days: Int) { context.dataStore.edit { it[keyDays] = days } }

    suspend fun isOnboarded(): Boolean =
        context.dataStore.data.map { it[keyOnboarded] ?: false }.first()
    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { it[keyOnboarded] = value }
    }
}

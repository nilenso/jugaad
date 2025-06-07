package com.nilenso.jugaad.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

// Create singleton DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferencesKeys {
    val SLACK_WEBHOOK_URL = stringPreferencesKey("slack_webhook_url")
    val SMS_MATCH_STRING = stringPreferencesKey("sms_match_string")
    val JUGAAD_ENABLED = booleanPreferencesKey("jugaad_enabled")
    val MONITORING_WEBHOOK_URL = stringPreferencesKey("monitoring_webhook_url")
} 

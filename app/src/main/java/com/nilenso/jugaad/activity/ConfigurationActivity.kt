package com.nilenso.jugaad.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nilenso.jugaad.R
import com.nilenso.jugaad.viewmodel.ConfigurationViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class ConfigurationActivity: AppCompatActivity() {

    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    val SERVER_URL = stringPreferencesKey("server_url")
    val AUTH_TOKEN = stringPreferencesKey("auth_token")
    val SMS_MATCH_STRING = stringPreferencesKey("sms_match_string")
    val JUGAAD_ENABLED = booleanPreferencesKey("jugaad_enabled")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure)
    }
}

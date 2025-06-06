package com.nilenso.jugaad.activity

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.nilenso.jugaad.R
import com.nilenso.jugaad.datastore.dataStore
import com.nilenso.jugaad.datastore.PreferencesKeys
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class ConfigurationActivity: AppCompatActivity() {
    
    private lateinit var editTextSlackWebhookUrl: EditText
    private lateinit var editTextSmsMatchString: EditText
    private lateinit var checkBoxEnabled: CheckBox
    private lateinit var buttonSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure)
        
        // Initialize UI components
        editTextSlackWebhookUrl = findViewById(R.id.editTextSlackWebhookUrl)
        editTextSmsMatchString = findViewById(R.id.editTextSmsMatchString)
        checkBoxEnabled = findViewById(R.id.checkBoxEnabled)
        buttonSave = findViewById(R.id.buttonSave)
        
        // Load current configuration
        loadConfiguration()
        
        // Set up save button click listener
        buttonSave.setOnClickListener {
            saveConfiguration()
        }
    }
    
    private fun loadConfiguration() {
        lifecycleScope.launch {
            dataStore.data.first().let { preferences ->
                editTextSlackWebhookUrl.setText(preferences[PreferencesKeys.SLACK_WEBHOOK_URL] ?: "")
                editTextSmsMatchString.setText(preferences[PreferencesKeys.SMS_MATCH_STRING] ?: "OTP")
                checkBoxEnabled.isChecked = preferences[PreferencesKeys.JUGAAD_ENABLED] ?: false
            }
        }
    }
    
    private fun saveConfiguration() {
        val webhookUrl = editTextSlackWebhookUrl.text.toString().trim()
        val smsMatchString = editTextSmsMatchString.text.toString().trim()
        val isEnabled = checkBoxEnabled.isChecked
        
        // Basic validation
        if (isEnabled && webhookUrl.isEmpty()) {
            Toast.makeText(this, "Please enter a Slack webhook URL", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isEnabled && smsMatchString.isEmpty()) {
            Toast.makeText(this, "Please enter an SMS match string", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isEnabled && !webhookUrl.startsWith("https://hooks.slack.com/")) {
            Toast.makeText(this, "Please enter a valid Slack webhook URL", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                dataStore.edit { preferences ->
                    preferences[PreferencesKeys.SLACK_WEBHOOK_URL] = webhookUrl
                    preferences[PreferencesKeys.SMS_MATCH_STRING] = smsMatchString
                    preferences[PreferencesKeys.JUGAAD_ENABLED] = isEnabled
                }
                Toast.makeText(this@ConfigurationActivity, "Configuration saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@ConfigurationActivity, "Failed to save configuration: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

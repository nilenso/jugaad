package com.nilenso.jugaad.activity

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nilenso.jugaad.R
import com.nilenso.jugaad.api.JugaadSendRequest
import com.nilenso.jugaad.api.JugaadWebService
import com.nilenso.jugaad.datastore.dataStore
import com.nilenso.jugaad.datastore.PreferencesKeys
import com.nilenso.jugaad.utils.StatusUpdateScheduler
import com.nilenso.jugaad.worker.StatusUpdateWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class ConfigurationActivity: AppCompatActivity() {
    
    private lateinit var editTextSlackWebhookUrl: EditText
    private lateinit var editTextSmsMatchString: EditText
    private lateinit var editTextDeviceName: EditText
    private lateinit var editTextMonitoringWebhookUrl: EditText
    private lateinit var checkBoxEnabled: CheckBox
    private lateinit var buttonTestStatusUpdate: Button
    private lateinit var textViewDisabledWarning: TextView
    private lateinit var textViewSmsDescription: TextView
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure)
        
        // Initialize UI components
        editTextSlackWebhookUrl = findViewById(R.id.editTextSlackWebhookUrl)
        editTextSmsMatchString = findViewById(R.id.editTextSmsMatchString)
        editTextDeviceName = findViewById(R.id.editTextDeviceName)
        editTextMonitoringWebhookUrl = findViewById(R.id.editTextMonitoringWebhookUrl)
        checkBoxEnabled = findViewById(R.id.checkBoxEnabled)
        buttonTestStatusUpdate = findViewById(R.id.buttonTestStatusUpdate)
        textViewDisabledWarning = findViewById(R.id.textViewDisabledWarning)
        textViewSmsDescription = findViewById(R.id.textViewSmsDescription)
        
        // Load current configuration
        loadConfiguration()
        
        // Set up checkbox listener to show/hide warning and auto-save
        checkBoxEnabled.setOnCheckedChangeListener { _, isChecked ->
            Log.d("ConfigurationActivity", "Checkbox changed to: $isChecked")
            updateDisabledWarningVisibility(isChecked)
            
            // Auto-save the enabled state immediately (but not during initial load)
            if (!isLoading) {
                lifecycleScope.launch {
                    try {
                        Log.d("ConfigurationActivity", "Auto-saving JUGAAD_ENABLED as: $isChecked")
                        dataStore.edit { preferences ->
                            preferences[PreferencesKeys.JUGAAD_ENABLED] = isChecked
                        }
                        updateStatusUpdateScheduling()
                    } catch (e: Exception) {
                        Log.e("ConfigurationActivity", "Failed to auto-save JUGAAD_ENABLED", e)
                    }
                }
            }
        }
        
        // Set up auto-save text watchers for all EditText fields
        editTextSlackWebhookUrl.addTextChangedListener(createAutoSaveTextWatcher(PreferencesKeys.SLACK_WEBHOOK_URL))
        editTextDeviceName.addTextChangedListener(createAutoSaveTextWatcher(PreferencesKeys.DEVICE_NAME))
        editTextMonitoringWebhookUrl.addTextChangedListener(createAutoSaveTextWatcher(PreferencesKeys.MONITORING_WEBHOOK_URL))
        
        // Set up SMS match string text watcher for dynamic description and auto-save
        editTextSmsMatchString.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateSmsDescription()
                if (!isLoading) {
                    autoSaveSmsMatchString()
                }
            }
        })
        
        // Set up button click listeners
        buttonTestStatusUpdate.setOnClickListener {
            testStatusUpdate()
        }
    }
    
    private fun updateDisabledWarningVisibility(isEnabled: Boolean) {
        textViewDisabledWarning.visibility = android.view.View.VISIBLE
        if (isEnabled) {
            textViewDisabledWarning.text = "✅ Forwarding enabled"
            textViewDisabledWarning.setTextColor(resources.getColor(R.color.success_green, null))
        } else {
            textViewDisabledWarning.text = "⚠️ Forwarding disabled"
            textViewDisabledWarning.setTextColor(resources.getColor(R.color.secondary_text, null))
        }
    }
    
    private fun updateSmsDescription() {
        val matchString = editTextSmsMatchString.text.toString().trim()
        if (matchString.isEmpty()) {
            textViewSmsDescription.text = "Forwarding all SMSes"
        } else {
            textViewSmsDescription.text = "Forwarding SMS containing '$matchString'"
        }
    }
    
    private fun loadConfiguration() {
        isLoading = true
        lifecycleScope.launch {
            dataStore.data.first().let { preferences ->
                Log.d("ConfigurationActivity", "Loading configuration...")
                
                editTextSlackWebhookUrl.setText(preferences[PreferencesKeys.SLACK_WEBHOOK_URL] ?: "")
                editTextSmsMatchString.setText(preferences[PreferencesKeys.SMS_MATCH_STRING] ?: "OTP")
                editTextDeviceName.setText(preferences[PreferencesKeys.DEVICE_NAME] ?: "")
                editTextMonitoringWebhookUrl.setText(preferences[PreferencesKeys.MONITORING_WEBHOOK_URL] ?: "")
                
                val isEnabled = preferences[PreferencesKeys.JUGAAD_ENABLED] ?: false
                Log.d("ConfigurationActivity", "JUGAAD_ENABLED preference value: $isEnabled")
                
                checkBoxEnabled.isChecked = isEnabled
                Log.d("ConfigurationActivity", "Checkbox set to: ${checkBoxEnabled.isChecked}")
                
                // Update warning visibility based on initial state
                updateDisabledWarningVisibility(isEnabled)
                
                // Update SMS description based on initial value
                updateSmsDescription()
                
                isLoading = false
            }
        }
    }
    
    private fun createAutoSaveTextWatcher(key: Preferences.Key<String>): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isLoading) {
                    autoSaveField(key, s.toString().trim())
                }
            }
        }
    }
    
    private fun autoSaveField(key: Preferences.Key<String>, value: String) {
        lifecycleScope.launch {
            try {
                dataStore.edit { preferences ->
                    preferences[key] = value
                }
                Log.d("ConfigurationActivity", "Auto-saved ${key.name}: $value")
                
                // Update status update scheduling when monitoring webhook changes
                if (key == PreferencesKeys.MONITORING_WEBHOOK_URL) {
                    updateStatusUpdateScheduling()
                }
            } catch (e: Exception) {
                Log.e("ConfigurationActivity", "Failed to auto-save ${key.name}", e)
            }
        }
    }
    
    private fun autoSaveSmsMatchString() {
        val value = editTextSmsMatchString.text.toString().trim()
        val finalValue = if (value.isEmpty()) "OTP" else value
        autoSaveField(PreferencesKeys.SMS_MATCH_STRING, finalValue)
    }
    
    private fun updateStatusUpdateScheduling() {
        lifecycleScope.launch {
            dataStore.data.first().let { preferences ->
                val monitoringWebhookUrl = preferences[PreferencesKeys.MONITORING_WEBHOOK_URL] ?: ""
                if (monitoringWebhookUrl.isNotEmpty()) {
                    StatusUpdateScheduler.scheduleStatusUpdate(this@ConfigurationActivity)
                } else {
                    StatusUpdateScheduler.cancelStatusUpdate(this@ConfigurationActivity)
                }
            }
        }
    }
    
    private fun testStatusUpdate() {
        val mainWebhookUrl = editTextSlackWebhookUrl.text.toString().trim()
        val monitoringWebhookUrl = editTextMonitoringWebhookUrl.text.toString().trim()
        val deviceName = editTextDeviceName.text.toString().trim()
        
        // Validate that at least one webhook is configured
        if (mainWebhookUrl.isEmpty() && monitoringWebhookUrl.isEmpty()) {
            Toast.makeText(this, "Please enter at least one webhook URL to test", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validate webhook URLs if provided
        if (mainWebhookUrl.isNotEmpty() && !mainWebhookUrl.startsWith("https://hooks.slack.com/")) {
            Toast.makeText(this, "Please enter a valid main webhook URL", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (monitoringWebhookUrl.isNotEmpty() && !monitoringWebhookUrl.startsWith("https://hooks.slack.com/")) {
            Toast.makeText(this, "Please enter a valid monitoring webhook URL", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show progress message
        Toast.makeText(this, "Sending test messages...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                var successCount = 0
                var totalCount = 0
                
                // Send to main webhook if configured
                if (mainWebhookUrl.isNotEmpty()) {
                    totalCount++
                    val mainMessage = if (deviceName.isNotEmpty()) "$deviceName: Testing Jugaad Status." else "Testing Jugaad Status."
                    if (sendTestMessage(mainWebhookUrl, mainMessage)) {
                        successCount++
                    }
                }
                
                // Send to monitoring webhook if configured
                if (monitoringWebhookUrl.isNotEmpty()) {
                    totalCount++
                    val monitoringMessage = if (deviceName.isNotEmpty()) "$deviceName: Jugaad SMS forwarder is operational" else "Jugaad SMS forwarder is operational"
                    if (sendTestMessage(monitoringWebhookUrl, monitoringMessage)) {
                        successCount++
                    }
                }
                
                // Show result
                if (successCount == totalCount) {
                    Toast.makeText(this@ConfigurationActivity, "✅ Test messages sent successfully to $successCount webhook(s)!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@ConfigurationActivity, "⚠️ Sent to $successCount/$totalCount webhooks. Check logs for errors.", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@ConfigurationActivity, "❌ Failed to send test messages: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private suspend fun sendTestMessage(webhookUrl: String, message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val logging = HttpLoggingInterceptor()
                logging.setLevel(HttpLoggingInterceptor.Level.BODY)

                val httpClient = OkHttpClient.Builder()
                httpClient.addInterceptor(logging)

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://hooks.slack.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build()

                val api = retrofit.create(JugaadWebService::class.java)
                val request = JugaadSendRequest(message)
                val response = api.sendMessageAsync(webhookUrl, request)
                
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: ""
                    Log.d("ConfigurationActivity", "Webhook response: $responseBody")
                    true
                } else {
                    Log.e("ConfigurationActivity", "Webhook request failed with code: ${response.code()}")
                    false
                }
            } catch (e: Exception) {
                Log.e("ConfigurationActivity", "Failed to send test message to $webhookUrl", e)
                false
            }
        }
    }
}

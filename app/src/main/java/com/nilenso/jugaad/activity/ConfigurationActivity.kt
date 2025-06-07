package com.nilenso.jugaad.activity

import android.content.Context
import android.os.Bundle
import android.util.Log
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
    private lateinit var buttonSave: Button
    private lateinit var buttonTestStatusUpdate: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure)
        
        // Initialize UI components
        editTextSlackWebhookUrl = findViewById(R.id.editTextSlackWebhookUrl)
        editTextSmsMatchString = findViewById(R.id.editTextSmsMatchString)
        editTextDeviceName = findViewById(R.id.editTextDeviceName)
        editTextMonitoringWebhookUrl = findViewById(R.id.editTextMonitoringWebhookUrl)
        checkBoxEnabled = findViewById(R.id.checkBoxEnabled)
        buttonSave = findViewById(R.id.buttonSave)
        buttonTestStatusUpdate = findViewById(R.id.buttonTestStatusUpdate)
        
        // Load current configuration
        loadConfiguration()
        
        // Set up button click listeners
        buttonSave.setOnClickListener {
            saveConfiguration()
        }
        
        buttonTestStatusUpdate.setOnClickListener {
            testStatusUpdate()
        }
    }
    
    private fun loadConfiguration() {
        lifecycleScope.launch {
            dataStore.data.first().let { preferences ->
                editTextSlackWebhookUrl.setText(preferences[PreferencesKeys.SLACK_WEBHOOK_URL] ?: "")
                editTextSmsMatchString.setText(preferences[PreferencesKeys.SMS_MATCH_STRING] ?: "OTP")
                editTextDeviceName.setText(preferences[PreferencesKeys.DEVICE_NAME] ?: "")
                editTextMonitoringWebhookUrl.setText(preferences[PreferencesKeys.MONITORING_WEBHOOK_URL] ?: "")
                checkBoxEnabled.isChecked = preferences[PreferencesKeys.JUGAAD_ENABLED] ?: false
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
                    val monitoringMessage = if (deviceName.isNotEmpty()) "$deviceName: Jugaad bolta hai" else "Jugaad bolta hai"
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
    
    private fun saveConfiguration() {
        val webhookUrl = editTextSlackWebhookUrl.text.toString().trim()
        val smsMatchString = editTextSmsMatchString.text.toString().trim()
        val deviceName = editTextDeviceName.text.toString().trim()
        val monitoringWebhookUrl = editTextMonitoringWebhookUrl.text.toString().trim()
        val isEnabled = checkBoxEnabled.isChecked
        
        // Use default values for optional fields if they're empty
        val finalSmsMatchString = if (smsMatchString.isEmpty()) "OTP" else smsMatchString
        
        // Basic validation
        if (isEnabled && webhookUrl.isEmpty()) {
            Toast.makeText(this, "Please enter a Slack webhook URL to enable Jugaad", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isEnabled && !webhookUrl.startsWith("https://hooks.slack.com/")) {
            Toast.makeText(this, "Please enter a valid Slack webhook URL", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validate monitoring webhook URL if provided
        if (monitoringWebhookUrl.isNotEmpty() && !monitoringWebhookUrl.startsWith("https://hooks.slack.com/")) {
            Toast.makeText(this, "Please enter a valid monitoring webhook URL or leave it empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                dataStore.edit { preferences ->
                    preferences[PreferencesKeys.SLACK_WEBHOOK_URL] = webhookUrl
                    preferences[PreferencesKeys.SMS_MATCH_STRING] = finalSmsMatchString
                    preferences[PreferencesKeys.DEVICE_NAME] = deviceName
                    preferences[PreferencesKeys.MONITORING_WEBHOOK_URL] = monitoringWebhookUrl
                    preferences[PreferencesKeys.JUGAAD_ENABLED] = isEnabled
                }
                
                // Schedule or cancel status updates based on monitoring webhook configuration
                if (monitoringWebhookUrl.isNotEmpty()) {
                    StatusUpdateScheduler.scheduleStatusUpdate(this@ConfigurationActivity)
                    Toast.makeText(this@ConfigurationActivity, "Configuration saved and daily status updates scheduled", Toast.LENGTH_SHORT).show()
                } else {
                    StatusUpdateScheduler.cancelStatusUpdate(this@ConfigurationActivity)
                    if (isEnabled) {
                        Toast.makeText(this@ConfigurationActivity, "Configuration saved successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ConfigurationActivity, "Jugaad disabled - configuration saved", Toast.LENGTH_SHORT).show()
                    }
                }
                
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@ConfigurationActivity, "Failed to save configuration: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

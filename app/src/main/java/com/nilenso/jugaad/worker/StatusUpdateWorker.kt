package com.nilenso.jugaad.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nilenso.jugaad.api.JugaadSendRequest
import com.nilenso.jugaad.api.JugaadWebService
import com.nilenso.jugaad.datastore.dataStore
import com.nilenso.jugaad.datastore.PreferencesKeys
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class StatusUpdateWorker(appCtx: Context, workerParams: WorkerParameters): CoroutineWorker(appCtx, workerParams) {
    
    override suspend fun doWork(): Result {
        Log.d("STATUSUPDATEWORKER", "Starting daily status update work")

        val preferences = applicationContext.dataStore.data.first()
        val monitoringWebhookUrl = preferences[PreferencesKeys.MONITORING_WEBHOOK_URL]
        val deviceName = preferences[PreferencesKeys.DEVICE_NAME]?.takeIf { it.isNotBlank() } ?: ""

        // If no monitoring webhook is configured, just return success (no-op)
        if (monitoringWebhookUrl.isNullOrEmpty()) {
            Log.d("STATUSUPDATEWORKER", "No monitoring webhook configured, skipping status update")
            return Result.success()
        }

        Log.d("STATUSUPDATEWORKER", "Sending status update to monitoring webhook: $monitoringWebhookUrl")

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
                
                // List of varied status messages - pick one randomly each day
                val statusMessages = listOf(
                    "Jugaad SMS forwarder is operational.",
                    "Jugaad SMS forwarder is running smoothly.", 
                    "Jugaad SMS forwarder is online and ready.",
                    "Jugaad SMS forwarder is monitoring incoming messages.",
                    "Jugaad SMS forwarder is ready to forward messages.",
                    "Jugaad SMS forwarder is listening for SMS messages.",
                    "Jugaad SMS forwarder is watching for messages.",
                    "Jugaad SMS forwarder is doing its thing.",
                    "Jugaad SMS forwarder is on duty.",
                    "Jugaad SMS forwarder is on the job.",
                    "Jugaad SMS forwarder reporting in."
                )
                
                // Pick a random message each day
                val baseStatusMessage = statusMessages.random()
                val statusMessage = if (deviceName.isNotEmpty()) {
                    "$deviceName: $baseStatusMessage"
                } else {
                    baseStatusMessage
                }
                
                val request = JugaadSendRequest(statusMessage)
                val response = api.sendMessageAsync(monitoringWebhookUrl, request)
                
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: ""
                    Log.d("STATUSUPDATEWORKER", "Status update sent successfully. Response: $responseBody")
                    Result.success()
                } else {
                    Log.e("STATUSUPDATEWORKER", "Failed to send status update with code: ${response.code()}")
                    Result.failure()
                }
            } catch (e: Exception) {
                Log.e("STATUSUPDATEWORKER", "Exception while sending status update", e)
                Result.failure()
            }
        }
    }
} 

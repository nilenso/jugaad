package com.nilenso.jugaad.worker

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nilenso.jugaad.api.JugaadSendRequest
import com.nilenso.jugaad.api.JugaadWebService
import com.nilenso.jugaad.datastore.dataStore
import com.nilenso.jugaad.datastore.PreferencesKeys
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class JugaadSendWorker(appCtx: Context, workerParams: WorkerParameters): Worker(appCtx, workerParams) {
    
    override fun doWork(): Result {

        val message = inputData.getString("JUGAAD_MSG") ?: return Result.failure()
        
        val (webhookUrl, deviceName) = runBlocking {
            val preferences = applicationContext.dataStore.data.first()
            val url = preferences[PreferencesKeys.SLACK_WEBHOOK_URL]
            val name = preferences[PreferencesKeys.DEVICE_NAME]?.takeIf { it.isNotBlank() } ?: ""
            Pair(url, name)
        }
        
        if (webhookUrl == null) return Result.failure()

        // Prefix message with device name if it's configured
        val finalMessage = if (deviceName.isNotEmpty()) {
            "$deviceName: $message"
        } else {
            message
        }

        Log.d("JUGAADWORKER", "Starting work to send message: $finalMessage")
        Log.d("JUGAADWORKER", "Using webhook URL: $webhookUrl")

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

        try {
            val request = JugaadSendRequest(finalMessage)
            val response = api.sendMessage(webhookUrl, request).execute()
            
            if (response.isSuccessful) {
                Log.d("JUGAADWORKER", "Message sent successfully")
                return Result.success()
            } else {
                Log.e("JUGAADWORKER", "Failed to send message: ${response.errorBody()?.string()}")
                return Result.failure()
            }
        } catch (e: Exception) {
            Log.e("JUGAADWORKER", "Exception while sending message", e)
            return Result.failure()
        }
    }
}

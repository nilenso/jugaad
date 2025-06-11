package com.nilenso.jugaad.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nilenso.jugaad.R
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
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "jugaad_send_channel"
        private const val TAG = "JugaadSendWorker"
    }
    
    override fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannelIfNeeded()
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Jugaad SMS Forwarder")
            .setContentText("Sending SMS to Slack...")
            .setSmallIcon(R.drawable.ic_slack)
            .setOngoing(true)
            .build()
        
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
    
    override fun doWork(): Result {
        val message = inputData.getString("JUGAAD_MSG") ?: return Result.failure()
        
        val (webhookUrl, deviceName) = runBlocking {
            val preferences = applicationContext.dataStore.data.first()
            val url = preferences[PreferencesKeys.SLACK_WEBHOOK_URL]
            val name = preferences[PreferencesKeys.DEVICE_NAME]?.takeIf { it.isNotBlank() } ?: ""
            Pair(url, name)
        }
        
        if (webhookUrl == null) return Result.failure()

        val finalMessage = if (deviceName.isNotEmpty()) "$deviceName: $message" else message

        return try {
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
            val request = JugaadSendRequest(finalMessage)
            val response = api.sendMessage(webhookUrl, request).execute()
            
            if (response.isSuccessful) {
                Log.d(TAG, "Message sent successfully")
                Result.success()
            } else {
                Log.e(TAG, "Failed to send message: ${response.code()}")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while sending message", e)
            Result.failure()
        }
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "SMS Forwarding",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notifications for SMS forwarding operations"
                    setShowBadge(false)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}

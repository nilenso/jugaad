package com.nilenso.jugaad.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nilenso.jugaad.worker.JugaadSendWorker
import com.nilenso.jugaad.datastore.dataStore
import com.nilenso.jugaad.datastore.PreferencesKeys
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Check permissions
        val hasReceivePermission = context.checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val hasReadPermission = context.checkSelfPermission(android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        
        if (!hasReceivePermission || !hasReadPermission) {
            Log.e(TAG, "Missing required SMS permissions")
            return
        }

        // Check if enabled and get match string
        val (isEnabled, smsMatchString) = runBlocking {
            val preferences = context.dataStore.data.first()
            val enabled = preferences[PreferencesKeys.JUGAAD_ENABLED] ?: false
            val matchString = preferences[PreferencesKeys.SMS_MATCH_STRING] ?: "OTP"
            Pair(enabled, matchString)
        }
        
        if (!isEnabled) return

        // Process SMS
        val bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return
        val format = bundle.getString("format")

        pdus.forEach { pdu ->
            try {
                val smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    SmsMessage.createFromPdu(pdu as ByteArray, format)
                } else {
                    @Suppress("DEPRECATION")
                    SmsMessage.createFromPdu(pdu as ByteArray)
                }
                
                val messageBody = smsMessage?.messageBody
                if (messageBody?.contains(smsMatchString, ignoreCase = true) == true) {
                    Log.d(TAG, "SMS matches pattern, forwarding to webhook")
                    
                    val workRequest = OneTimeWorkRequestBuilder<JugaadSendWorker>()
                        .setInputData(Data.Builder()
                            .putString("JUGAAD_MSG", messageBody)
                            .putString("SENDER", smsMessage.originatingAddress ?: "Unknown")
                            .build())
                        .addTag("JUGAAD_SEND")
                        .build()
                    
                    WorkManager.getInstance(context).enqueue(workRequest)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            }
        }
    }
}

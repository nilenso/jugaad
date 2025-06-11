package com.nilenso.jugaad.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nilenso.jugaad.worker.JugaadSendWorker
import com.nilenso.jugaad.datastore.dataStore
import com.nilenso.jugaad.datastore.PreferencesKeys
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SmsReceiver : BroadcastReceiver() {
    private val TAG = "SmsReceiver"
    companion object {
        const val pdu_type = "pdus"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "============ SMS RECEIVER TRIGGERED ============")
        Log.d(TAG, "Intent action: ${intent.action}")
        Log.d(TAG, "Device info: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        
        // Check permissions first
        val hasReceivePermission = context.checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val hasReadPermission = context.checkSelfPermission(android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        
        Log.d(TAG, "Permissions - RECEIVE_SMS: $hasReceivePermission, READ_SMS: $hasReadPermission")
        
        if (!hasReceivePermission || !hasReadPermission) {
            Log.e(TAG, "Missing required SMS permissions. Cannot process SMS.")
            return
        }
        
        // Check if Jugaad is enabled
        val isEnabled = try {
            runBlocking {
                context.dataStore.data.first()[PreferencesKeys.JUGAAD_ENABLED] ?: false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading Jugaad enabled state", e)
            false
        }
        
        if (!isEnabled) {
            Log.d(TAG, "Jugaad is disabled, ignoring SMS")
            return
        }
        
        val smsMatchString = try {
            runBlocking {
                context.dataStore.data.first()[PreferencesKeys.SMS_MATCH_STRING] ?: "OTP"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading SMS match string, using default 'OTP'", e)
            "OTP"
        }
        
        Log.d(TAG, "Looking for SMS containing: '$smsMatchString'")
        
        try {
            intent.extras?.let { bundle ->
                Log.d(TAG, "Intent extras found: ${bundle.keySet()}")
                
                val pdus = bundle.get(pdu_type) as (Array<*>?)
                val format = bundle.getString("format")
                
                Log.d(TAG, "PDUs count: ${pdus?.size ?: 0}, Format: $format")

                pdus?.let { pduArray ->
                    pduArray.forEachIndexed { index, pdu ->
                        try {
                            val msg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                SmsMessage.createFromPdu(pdu as ByteArray, format)
                            } else {
                                @Suppress("DEPRECATION")
                                SmsMessage.createFromPdu(pdu as ByteArray)
                            }
                            
                            val msgBody = msg?.messageBody
                            val sender = msg?.originatingAddress
                            
                            Log.d(TAG, "SMS [$index] from $sender: $msgBody")

                            if (msgBody != null && msgBody.contains(smsMatchString, ignoreCase = true)) {
                                Log.d(TAG, "✅ SMS matches pattern '$smsMatchString', sending to webhook")
                                
                                val wrk = OneTimeWorkRequestBuilder<JugaadSendWorker>()
                                    .setInputData(Data.Builder()
                                        .putString("JUGAAD_MSG", msgBody)
                                        .putString("SENDER", sender ?: "Unknown")
                                        .build())
                                    .addTag("JUGAAD_SEND")
                                    .build()
                                WorkManager.getInstance(context).enqueue(wrk)
                                
                                Log.d(TAG, "Work request enqueued successfully")
                            } else {
                                Log.d(TAG, "❌ SMS does not match pattern '$smsMatchString', ignoring")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing SMS PDU at index $index", e)
                        }
                    }
                } ?: Log.w(TAG, "No PDUs found in SMS intent")
            } ?: Log.w(TAG, "No extras found in SMS intent")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS intent", e)
        }
        
        Log.d(TAG, "============ SMS RECEIVER FINISHED ============")
    }
}

package com.nilenso.jugaad.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.nilenso.jugaad.worker.JugaadSendWorker

class SmsReceiver : BroadcastReceiver() {
    private val TAG = this.javaClass.simpleName
    companion object {
        const val pdu_type = "pdus"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SMSRECVD", "RECEIVED a message")
        intent.extras?.let {bundle ->
            val pdus = bundle.get(pdu_type) as (Array<*>?)
            val format = bundle.getString("format")

            pdus?.let {
                val msgs = it.forEach { pdu ->
                    val msg = SmsMessage.createFromPdu(pdu as ByteArray?, format)
                    Log.d("SMSRECVD", "sms: ${msg.messageBody}")

                    val msgBody = msg.messageBody
                    if (msgBody.contains("OTP")) {
                        val wrk = OneTimeWorkRequestBuilder<JugaadSendWorker>()
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .setInputData(Data.Builder()
                                .putString("JUGAAD_MSG", msgBody)
                                .build())
                            .addTag("JUGAAD_SEND")
                            .build()
                        WorkManager.getInstance(context).enqueue(wrk)
                    }
                }
            }
        }
    }
}
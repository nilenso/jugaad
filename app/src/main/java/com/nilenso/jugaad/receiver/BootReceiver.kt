package com.nilenso.jugaad.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nilenso.jugaad.utils.StatusUpdateScheduler

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted, rescheduling status updates")
            StatusUpdateScheduler.scheduleStatusUpdate(context)
        }
    }
} 

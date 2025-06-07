package com.nilenso.jugaad.utils

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nilenso.jugaad.worker.StatusUpdateWorker
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object StatusUpdateScheduler {
    
    private const val STATUS_UPDATE_WORK_NAME = "status_update_work"
    private const val TAG = "StatusUpdateScheduler"
    
    fun scheduleStatusUpdate(context: Context) {
        Log.d(TAG, "Scheduling daily status update at 9:00 AM IST")
        
        // Calculate initial delay to next 9:00 AM IST using Calendar
        val istTimeZone = TimeZone.getTimeZone("Asia/Kolkata")
        val now = Calendar.getInstance(istTimeZone)
        val nextRun = Calendar.getInstance(istTimeZone)
        
        // Set target time to 9:00 AM today
        nextRun.set(Calendar.HOUR_OF_DAY, 9)
        nextRun.set(Calendar.MINUTE, 0)
        nextRun.set(Calendar.SECOND, 0)
        nextRun.set(Calendar.MILLISECOND, 0)
        
        // If 9:00 AM has already passed today, schedule for tomorrow
        if (nextRun.timeInMillis <= now.timeInMillis) {
            nextRun.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        val initialDelayMillis = nextRun.timeInMillis - now.timeInMillis
        val initialDelayMinutes = initialDelayMillis / (1000 * 60)
        
        Log.d(TAG, "Initial delay: $initialDelayMinutes minutes until next 9:00 AM IST")
        
        // Create constraints for the work
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Create the periodic work request (runs every 24 hours)
        val statusUpdateWork = PeriodicWorkRequestBuilder<StatusUpdateWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag("STATUS_UPDATE")
            .build()
        
        // Enqueue the work, replacing any existing work with the same name
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            STATUS_UPDATE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            statusUpdateWork
        )
        
        Log.d(TAG, "Status update work scheduled successfully")
    }
    
    fun cancelStatusUpdate(context: Context) {
        Log.d(TAG, "Cancelling daily status update work")
        WorkManager.getInstance(context).cancelUniqueWork(STATUS_UPDATE_WORK_NAME)
    }
} 

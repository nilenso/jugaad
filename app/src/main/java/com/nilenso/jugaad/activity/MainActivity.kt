package com.nilenso.jugaad.activity

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.nilenso.jugaad.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val smsPermissions = arrayOf(
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_SMS
        )
        
        val missingPermissions = smsPermissions.filterNot { 
            checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED 
        }
        
        if (missingPermissions.isNotEmpty()) {
            Log.d("MAINJUGAAD", "Requesting SMS permissions")
            requestPermissions(missingPermissions.toTypedArray(), 1)
        } else {
            Log.d("MAINJUGAAD", "All SMS permissions granted")
        }

        setContentView(R.layout.activity_main)
    }

    fun configureJugaad(view: View) {
        startActivity(Intent(this, ConfigurationActivity::class.java))
    }
}

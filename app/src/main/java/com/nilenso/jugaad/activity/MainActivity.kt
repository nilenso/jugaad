package com.nilenso.jugaad.activity

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.nilenso.jugaad.R

class MainActivity : AppCompatActivity() {
    companion object {
        private const val SMS_PERMISSION_REQUEST_CODE = 1
        private val REQUIRED_SMS_PERMISSIONS = arrayOf(
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_SMS
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check all required SMS permissions
        val deniedPermissions = REQUIRED_SMS_PERMISSIONS.filter { permission ->
            checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED
        }
        
        if (deniedPermissions.isNotEmpty()) {
            Log.d("MAINJUGAAD", "Requesting SMS permissions: ${deniedPermissions.joinToString()}")
            requestPermissions(deniedPermissions.toTypedArray(), SMS_PERMISSION_REQUEST_CODE)
        } else {
            Log.d("MAINJUGAAD", "All SMS permissions already granted")
        }

        setContentView(R.layout.activity_main)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            SMS_PERMISSION_REQUEST_CODE -> {
                val deniedPermissions = mutableListOf<String>()
                permissions.forEachIndexed { index, permission ->
                    if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                        deniedPermissions.add(permission)
                    }
                }
                
                if (deniedPermissions.isEmpty()) {
                    Log.d("SMSJUGAAD", "All SMS permissions granted")
                    Toast.makeText(this, "SMS permissions granted. App is ready to forward messages.", Toast.LENGTH_LONG).show()
                } else {
                    Log.w("SMSJUGAAD", "Some SMS permissions denied: ${deniedPermissions.joinToString()}")
                    Toast.makeText(this, "SMS permissions are required for the app to work. Please grant all permissions.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun configureJugaad(view: View) {
        Log.d("JUGAAD", "Configure Pressed")
        startActivity(Intent(this, ConfigurationActivity::class.java))
    }
}

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
        when (checkSelfPermission(android.Manifest.permission.RECEIVE_SMS)) {
            PackageManager.PERMISSION_DENIED ->
                requestPermissions(arrayOf(android.Manifest.permission.RECEIVE_SMS), 1)
            else ->
                Log.d("MAINJUGAAD", "Already permitted to receive sms")
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
            1 -> grantResults.firstOrNull()?.let {
                if (it == PackageManager.PERMISSION_GRANTED) {
                    Log.d("SMSJUGAAD", "Permission granted")
                    // Chill
                }
            }
        }
    }

    fun configureJugaad(view: View) {
        Log.d("JUGAAD", "Configure Pressed")
        startActivity(Intent(this, ConfigurationActivity::class.java))
    }
}
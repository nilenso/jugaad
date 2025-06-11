package com.nilenso.jugaad.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

object OemDiagnostics {
    private const val TAG = "OemDiagnostics"
    
    data class DiagnosticResult(
        val deviceInfo: String,
        val smsPermissionsGranted: Boolean,
        val isDefaultSmsApp: Boolean,
        val oemRecommendations: List<String>
    )
    
    fun runDiagnostics(context: Context): DiagnosticResult {
        val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})"
        Log.d(TAG, "Running diagnostics for: $deviceInfo")
        
        // Check SMS permissions
        val hasReceiveSms = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val hasReadSms = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val smsPermissionsGranted = hasReceiveSms && hasReadSms
        
        // Check if app is default SMS app (not required but helpful for some OEMs)
        val isDefaultSmsApp = try {
            val defaultSmsPackage = Settings.Secure.getString(context.contentResolver, "sms_default_application")
            defaultSmsPackage == context.packageName
        } catch (e: Exception) {
            false
        }
        
        // Generate OEM-specific recommendations
        val recommendations = generateOemRecommendations(deviceInfo, smsPermissionsGranted, isDefaultSmsApp)
        
        return DiagnosticResult(deviceInfo, smsPermissionsGranted, isDefaultSmsApp, recommendations)
    }
    
    private fun generateOemRecommendations(deviceInfo: String, hasPermissions: Boolean, isDefaultSmsApp: Boolean): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (!hasPermissions) {
            recommendations.add("❌ Grant all SMS permissions (RECEIVE_SMS and READ_SMS)")
        }
        
        val manufacturer = Build.MANUFACTURER.lowercase()
        when {
            manufacturer.contains("oppo") || manufacturer.contains("oneplus") -> {
                recommendations.addAll(listOf(
                    "🔧 OPPO/OnePlus specific steps:",
                    "• Go to Settings > Apps > Jugaad > App Battery Usage > Background App Refresh > Enable",
                    "• Settings > Apps > Jugaad > Permissions > Allow all SMS permissions",
                    "• Settings > Privacy > Auto-start manager > Enable Jugaad",
                    "• Disable 'Phone Manager' optimizations for Jugaad"
                ))
            }
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                recommendations.addAll(listOf(
                    "🔧 Xiaomi/Redmi/POCO specific steps:",
                    "• Settings > Apps > Manage apps > Jugaad > Battery saver > No restrictions",
                    "• Settings > Apps > Manage apps > Jugaad > Autostart > Enable",
                    "• Settings > Battery & performance > Manage apps' battery usage > Jugaad > No restrictions",
                    "• Security app > Permissions > Autostart > Enable Jugaad",
                    "• Disable MIUI Optimization: Settings > Additional settings > Developer options > Turn off MIUI optimization"
                ))
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                recommendations.addAll(listOf(
                    "🔧 Huawei/Honor specific steps:",
                    "• Phone Manager > Protected apps > Enable Jugaad",
                    "• Settings > Apps > Jugaad > Battery > Manage manually > Allow all",
                    "• Settings > Battery > App launch > Jugaad > Manage manually > Enable all"
                ))
            }
            manufacturer.contains("vivo") -> {
                recommendations.addAll(listOf(
                    "🔧 Vivo specific steps:",
                    "• Settings > Battery > Background App Refresh > Enable Jugaad",
                    "• i Manager > App manager > Autostart > Enable Jugaad",
                    "• Settings > More settings > Applications > Autostart > Enable Jugaad"
                ))
            }
            manufacturer.contains("samsung") -> {
                recommendations.addAll(listOf(
                    "🔧 Samsung specific steps:",
                    "• Settings > Apps > Jugaad > Battery > Optimize battery usage > Turn off",
                    "• Settings > Device care > Battery > App power management > Apps that won't be put to sleep > Add Jugaad",
                    "• Ensure 'Put unused apps to sleep' doesn't affect Jugaad"
                ))
            }
        }
        
        // General recommendations
        recommendations.addAll(listOf(
            "📱 General troubleshooting:",
            "• Restart the device after granting permissions",
            "• Send a test SMS to verify logs",
            "• Check logcat for 'SmsReceiver' messages",
            "• Ensure the app is not being killed by battery optimization"
        ))
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            recommendations.add("• Check Settings > Special app access > Device admin apps (some OEMs require this)")
        }
        
        return recommendations
    }
    
    fun generateDiagnosticReport(context: Context): String {
        val result = runDiagnostics(context)
        return buildString {
            appendLine("=== JUGAAD SMS DIAGNOSTICS ===")
            appendLine("Device: ${result.deviceInfo}")
            appendLine("SMS Permissions: ${if (result.smsPermissionsGranted) "✅ Granted" else "❌ Missing"}")
            appendLine("Default SMS App: ${if (result.isDefaultSmsApp) "✅ Yes" else "ℹ️ No (not required)"}")
            appendLine()
            appendLine("RECOMMENDATIONS:")
            result.oemRecommendations.forEach { appendLine(it) }
            appendLine()
            appendLine("If issues persist, please share this report with the development team.")
        }
    }
} 

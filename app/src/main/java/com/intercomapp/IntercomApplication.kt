package com.intercomapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp

class IntercomApplication : Application() {
    
    companion object {
        lateinit var instance: IntercomApplication
            private set
        
        const val CHANNEL_INTERCOM = "intercom_service"
        const val CHANNEL_CALLS = "call_notifications"
        const val CHANNEL_GENERAL = "general_notifications"
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Create notification channels
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Intercom service channel
            val intercomChannel = NotificationChannel(
                CHANNEL_INTERCOM,
                "Intercom Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Intercom communication service"
                setShowBadge(false)
            }
            
            // Call notifications channel
            val callChannel = NotificationChannel(
                CHANNEL_CALLS,
                "Call Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming call notifications"
                enableVibration(true)
                enableLights(true)
            }
            
            // General notifications channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            }
            
            notificationManager.createNotificationChannels(
                listOf(intercomChannel, callChannel, generalChannel)
            )
        }
    }
}

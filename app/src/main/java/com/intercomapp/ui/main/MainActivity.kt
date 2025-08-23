package com.intercomapp.ui.main

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.intercomapp.R
import com.intercomapp.databinding.ActivityMainBinding
import com.intercomapp.service.IntercomService
import com.intercomapp.ui.contacts.ContactsFragment
import com.intercomapp.ui.groups.GroupsFragment
import com.intercomapp.ui.home.HomeFragment
import com.intercomapp.ui.profile.ProfileFragment
// import dagger.hilt.android.AndroidEntryPoint

// @AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var intercomService: IntercomService? = null
    private var isServiceBound = false
    
    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startIntercomService()
        } else {
            Toast.makeText(this, "Uygulama için gerekli izinler verilmedi", Toast.LENGTH_LONG).show()
        }
    }
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as IntercomService.IntercomBinder
            intercomService = binder.getService()
            isServiceBound = true
            // Notify fragments that service is ready
            notifyServiceReady()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            intercomService = null
            isServiceBound = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupBottomNavigation()
        checkPermissions()
        
        // Handle notification intent
        handleNotificationIntent()
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_friends -> {
                    loadFragment(ContactsFragment())
                    true
                }
                R.id.nav_groups -> {
                    loadFragment(GroupsFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
        
        // Set default fragment
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    
    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startIntercomService()
        }
    }
    
    private fun startIntercomService() {
        val serviceIntent = Intent(this, IntercomService::class.java).apply {
            action = "START_FOREGROUND"
        }
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun handleNotificationIntent() {
        intent?.getStringExtra("notification_type")?.let { type ->
            when (type) {
                "call" -> {
                    binding.bottomNavigation.selectedItemId = R.id.nav_friends
                }
                "group" -> {
                    binding.bottomNavigation.selectedItemId = R.id.nav_groups
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }
    
    // Public method to get service
    fun getIntercomService(): IntercomService? = intercomService
    
    private fun notifyServiceReady() {
        // Notify current fragment that service is ready
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is HomeFragment) {
            currentFragment.onServiceReady()
        }
    }
}

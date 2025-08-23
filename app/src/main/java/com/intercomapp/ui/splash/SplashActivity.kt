package com.intercomapp.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.intercomapp.R
import com.intercomapp.data.repository.AuthRepository
import com.intercomapp.ui.auth.AuthActivity
import com.intercomapp.ui.main.MainActivity
// import dagger.hilt.android.AndroidEntryPoint
// import javax.inject.Inject

// @AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    
    private val authRepository = AuthRepository()
    
    companion object {
        private const val SPLASH_DELAY = 2000L // 2 seconds
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthStatus()
        }, SPLASH_DELAY)
    }
    
    private fun checkAuthStatus() {
        if (authRepository.isUserLoggedIn) {
            // User is logged in, go to main activity
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // User is not logged in, go to auth activity
            startActivity(Intent(this, AuthActivity::class.java))
        }
        finish()
    }
}

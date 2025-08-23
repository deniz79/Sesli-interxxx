package com.intercomapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.intercomapp.R
import com.intercomapp.databinding.ActivityAuthBinding
import com.intercomapp.ui.main.MainActivity
// import dagger.hilt.android.AndroidEntryPoint

// @AndroidEntryPoint
class AuthActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAuthBinding
    private lateinit var viewModel: AuthViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        
        setupViews()
        observeViewModel()
    }
    
    private fun setupViews() {
        // Login button
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            
            if (validateInput(email, password)) {
                viewModel.login(email, password)
            }
        }
        
        // Register button
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val name = binding.etName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            
            if (validateRegisterInput(email, password, confirmPassword, name, phone)) {
                viewModel.register(email, password, name, phone)
            }
        }
        
        // Toggle between login and register
        binding.tvToggleAuth.setOnClickListener {
            toggleAuthMode()
        }
        
        // Forgot password
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                viewModel.resetPassword(email)
            } else {
                Toast.makeText(this, "Lütfen e-posta adresinizi girin", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.authState.observe(this) { state ->
            android.util.Log.d("AuthActivity", "AuthState changed: $state")
            when (state) {
                is AuthState.Loading -> {
                    android.util.Log.d("AuthActivity", "Showing loading")
                    showLoading(true)
                }
                is AuthState.Success -> {
                    android.util.Log.d("AuthActivity", "Auth successful, starting MainActivity")
                    showLoading(false)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is AuthState.Error -> {
                    android.util.Log.e("AuthActivity", "Auth error: ${state.message}")
                    showLoading(false)
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is AuthState.PasswordReset -> {
                    android.util.Log.d("AuthActivity", "Password reset email sent")
                    showLoading(false)
                    Toast.makeText(this, "Şifre sıfırlama e-postası gönderildi", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "E-posta gerekli"
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Geçerli bir e-posta adresi girin"
            return false
        }
        
        if (password.isEmpty()) {
            binding.etPassword.error = "Şifre gerekli"
            return false
        }
        
        if (password.length < 6) {
            binding.etPassword.error = "Şifre en az 6 karakter olmalı"
            return false
        }
        
        return true
    }
    
    private fun validateRegisterInput(
        email: String,
        password: String,
        confirmPassword: String,
        name: String,
        phone: String
    ): Boolean {
        if (!validateInput(email, password)) {
            return false
        }
        
        if (name.isEmpty()) {
            binding.etName.error = "Ad soyad gerekli"
            return false
        }
        
        if (phone.isEmpty()) {
            binding.etPhone.error = "Telefon gerekli"
            return false
        }
        
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Şifreler eşleşmiyor"
            return false
        }
        
        return true
    }
    
    private fun toggleAuthMode() {
        val isLoginMode = binding.btnLogin.visibility == View.VISIBLE
        
        if (isLoginMode) {
            // Switch to register mode
            binding.btnLogin.visibility = View.GONE
            binding.btnRegister.visibility = View.VISIBLE
            binding.tvForgotPassword.visibility = View.GONE
            
            // Show register fields
            binding.tilName.visibility = View.VISIBLE
            binding.tilPhone.visibility = View.VISIBLE
            binding.tilConfirmPassword.visibility = View.VISIBLE
            
            binding.tvToggleAuth.text = getString(R.string.already_have_account)
        } else {
            // Switch to login mode
            binding.btnLogin.visibility = View.VISIBLE
            binding.btnRegister.visibility = View.GONE
            binding.tvForgotPassword.visibility = View.VISIBLE
            
            // Hide register fields
            binding.tilName.visibility = View.GONE
            binding.tilPhone.visibility = View.GONE
            binding.tilConfirmPassword.visibility = View.GONE
            
            binding.tvToggleAuth.text = getString(R.string.dont_have_account)
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        binding.btnRegister.isEnabled = !show
    }
}

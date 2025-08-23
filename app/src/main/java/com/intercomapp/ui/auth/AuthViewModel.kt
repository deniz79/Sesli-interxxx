package com.intercomapp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intercomapp.data.repository.AuthRepository
// import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
// import javax.inject.Inject

// @HiltViewModel
class AuthViewModel : ViewModel() {
    
    private val authRepository = AuthRepository()
    
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState
    
    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        
        viewModelScope.launch {
            try {
                val result = authRepository.signIn(email, password)
                result.fold(
                    onSuccess = {
                        _authState.value = AuthState.Success
                    },
                    onFailure = { exception ->
                        _authState.value = AuthState.Error(exception.message ?: "Giriş başarısız")
                    }
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Beklenmeyen hata")
            }
        }
    }
    
    fun register(email: String, password: String, name: String, phone: String) {
        android.util.Log.d("AuthViewModel", "Register called with email: $email")
        _authState.value = AuthState.Loading
        
        viewModelScope.launch {
            try {
                android.util.Log.d("AuthViewModel", "Calling authRepository.signUp")
                val result = authRepository.signUp(email, password, name, phone)
                android.util.Log.d("AuthViewModel", "SignUp result: $result")
                
                result.fold(
                    onSuccess = { user ->
                        android.util.Log.d("AuthViewModel", "SignUp successful, user: ${user.uid}")
                        _authState.value = AuthState.Success
                    },
                    onFailure = { exception ->
                        android.util.Log.e("AuthViewModel", "SignUp failed", exception)
                        _authState.value = AuthState.Error(exception.message ?: "Kayıt başarısız")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Unexpected error in register", e)
                _authState.value = AuthState.Error(e.message ?: "Beklenmeyen hata")
            }
        }
    }
    
    fun resetPassword(email: String) {
        _authState.value = AuthState.Loading
        
        viewModelScope.launch {
            try {
                val result = authRepository.resetPassword(email)
                result.fold(
                    onSuccess = {
                        _authState.value = AuthState.PasswordReset
                    },
                    onFailure = { exception ->
                        _authState.value = AuthState.Error(exception.message ?: "Şifre sıfırlama başarısız")
                    }
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Beklenmeyen hata")
            }
        }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Success : AuthState()
    object PasswordReset : AuthState()
    data class Error(val message: String) : AuthState()
}

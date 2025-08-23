package com.intercomapp.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intercomapp.data.repository.AuthRepository
// import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
// import javax.inject.Inject

// @HiltViewModel
class ProfileViewModel : ViewModel() {
    
    private val authRepository = AuthRepository()
    
    private val _logoutState = MutableLiveData<Boolean>()
    val logoutState: LiveData<Boolean> = _logoutState
    
    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                _logoutState.value = true
            } catch (e: Exception) {
                _logoutState.value = false
            }
        }
    }
}

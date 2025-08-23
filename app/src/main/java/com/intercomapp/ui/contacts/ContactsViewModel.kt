package com.intercomapp.ui.contacts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.intercomapp.data.repository.AuthRepository
import com.intercomapp.service.IntercomService

class ContactsViewModel : ViewModel() {
    
    private var intercomService: IntercomService? = null
    private val authRepository = AuthRepository()
    
    // LiveData for UI updates
    private val _friends = MutableLiveData<List<Friend>>()
    val friends: LiveData<List<Friend>> = _friends
    
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message
    
    init {
        loadFriends()
    }
    
    fun setIntercomService(service: IntercomService?) {
        intercomService = service
        if (service != null) {
            _message.value = "Kişiler servisi bağlandı"
        } else {
            _message.value = "Kişiler servisi bulunamadı"
        }
    }
    
    fun addFriend(friendId: String) {
        val currentUserId = authRepository.currentUser?.uid
        if (currentUserId == null) {
            _message.value = "Kullanıcı girişi yapılmamış"
            return
        }
        
        if (friendId == currentUserId) {
            _message.value = "Kendinizi arkadaş olarak ekleyemezsiniz"
            return
        }
        
        // Check if friend already exists
        val currentFriends = _friends.value ?: emptyList()
        if (currentFriends.any { it.id == friendId }) {
            _message.value = "Bu kullanıcı zaten arkadaş listenizde"
            return
        }
        
        // Add friend to local list
        val newFriend = Friend(
            id = friendId,
            name = "Kullanıcı ${friendId.take(8)}...",
            isOnline = false
        )
        
        val updatedFriends = currentFriends + newFriend
        _friends.value = updatedFriends
        
        // Save to local storage (in a real app, this would be saved to a database)
        saveFriendsToLocal(updatedFriends)
        
        _message.value = "Arkadaş eklendi: ${friendId.take(8)}..."
    }
    
    fun removeFriend(friendId: String) {
        val currentFriends = _friends.value ?: emptyList()
        val updatedFriends = currentFriends.filter { it.id != friendId }
        _friends.value = updatedFriends
        
        // Save to local storage
        saveFriendsToLocal(updatedFriends)
        
        _message.value = "Arkadaş kaldırıldı"
    }
    
    fun getCurrentUserId(): String? {
        return authRepository.currentUser?.uid
    }
    
    private fun loadFriends() {
        // Load friends from local storage (in a real app, this would load from a database)
        // For now, we'll start with an empty list
        _friends.value = emptyList()
    }
    
    private fun saveFriendsToLocal(friends: List<Friend>) {
        // Save friends to local storage (in a real app, this would save to a database)
        // For now, we'll just log the operation
        android.util.Log.d("ContactsViewModel", "Saving ${friends.size} friends to local storage")
    }
    
    // Data class for Friend
    data class Friend(
        val id: String,
        val name: String,
        val isOnline: Boolean
    )
}

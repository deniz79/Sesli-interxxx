package com.intercomapp.ui.friends

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.intercomapp.databinding.FragmentFriendsBinding
import com.intercomapp.ui.main.MainActivity
import com.intercomapp.ui.voiceroom.VoiceRoomFragment

class FriendsFragment : Fragment() {
    
    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: FriendsViewModel
    private lateinit var friendsAdapter: FriendsListAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            viewModel = ViewModelProvider(this)[FriendsViewModel::class.java]
            
            // Get service from MainActivity
            (activity as? MainActivity)?.let { mainActivity ->
                viewModel.setIntercomService(mainActivity.getIntercomService())
            }
            
            setupViews()
            setupRecyclerView()
            observeViewModel()
        } catch (e: Exception) {
            Log.e("FriendsFragment", "Error in onViewCreated", e)
            Toast.makeText(context, "Arkadaşlar sayfası yüklenirken hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupViews() {
        try {
            // Add friend button
            binding.btnAddFriend.setOnClickListener {
                val friendId = binding.etFriendId.text.toString().trim()
                if (friendId.isNotEmpty()) {
                    viewModel.addFriend(friendId)
                    binding.etFriendId.text?.clear()
                } else {
                    Toast.makeText(context, "Lütfen arkadaş ID'sini girin", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("FriendsFragment", "Error in setupViews", e)
        }
    }
    
    private fun setupRecyclerView() {
        try {
            friendsAdapter = FriendsListAdapter { friend ->
                // Call friend directly
                callFriend(friend.id)
            }
            
            binding.recyclerViewFriends.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = friendsAdapter
            }
        } catch (e: Exception) {
            Log.e("FriendsFragment", "Error in setupRecyclerView", e)
            Toast.makeText(context, "Arkadaş listesi yüklenirken hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeViewModel() {
        try {
            viewModel.friends.observe(viewLifecycleOwner) { friends ->
                try {
                    friendsAdapter.submitList(friends)
                    binding.tvNoFriends.visibility = if (friends.isEmpty()) View.VISIBLE else View.GONE
                } catch (e: Exception) {
                    Log.e("FriendsFragment", "Error updating friends list", e)
                }
            }
            
            viewModel.message.observe(viewLifecycleOwner) { message ->
                message?.let {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("FriendsFragment", "Error in observeViewModel", e)
        }
    }
    
    private fun callFriend(friendId: String) {
        // Create room ID
        val currentUserId = viewModel.getCurrentUserId() ?: "unknown"
        val roomId = if (currentUserId < friendId) {
            "${currentUserId}_${friendId}"
        } else {
            "${friendId}_${currentUserId}"
        }
        
        // Navigate to voice room
        val voiceRoomFragment = VoiceRoomFragment.newInstance(roomId, friendId)
        
        parentFragmentManager.beginTransaction()
            .replace(com.intercomapp.R.id.fragment_container, voiceRoomFragment)
            .addToBackStack(null)
            .commit()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.intercomapp.ui.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.intercomapp.databinding.FragmentContactsBinding
import com.intercomapp.ui.main.MainActivity
import com.intercomapp.ui.voiceroom.VoiceRoomFragment

class ContactsFragment : Fragment() {
    
    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ContactsViewModel
    private lateinit var friendsAdapter: FriendsAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[ContactsViewModel::class.java]
        
        // Get service from MainActivity
        (activity as? MainActivity)?.let { mainActivity ->
            viewModel.setIntercomService(mainActivity.getIntercomService())
        }
        
        setupViews()
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupViews() {
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
    }
    
    private fun setupRecyclerView() {
        friendsAdapter = FriendsAdapter { friend ->
            // Call friend directly
            callFriend(friend.id)
        }
        
        binding.recyclerViewFriends.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = friendsAdapter
        }
    }
    
    private fun observeViewModel() {
        viewModel.friends.observe(viewLifecycleOwner) { friends ->
            friendsAdapter.submitList(friends)
            binding.tvNoFriends.visibility = if (friends.isEmpty()) View.VISIBLE else View.GONE
        }
        
        viewModel.message.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
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

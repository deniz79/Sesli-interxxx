package com.intercomapp.ui.voiceroom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.intercomapp.databinding.FragmentVoiceRoomBinding
import com.intercomapp.ui.main.MainActivity

class VoiceRoomFragment : Fragment() {
    
    private var _binding: FragmentVoiceRoomBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: VoiceRoomViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoiceRoomBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[VoiceRoomViewModel::class.java]
        
        // Get service from MainActivity
        (activity as? MainActivity)?.let { mainActivity ->
            viewModel.setIntercomService(mainActivity.getIntercomService())
        }
        
        setupViews()
        observeViewModel()
        
        // Get room ID from arguments
        arguments?.getString("room_id")?.let { roomId ->
            viewModel.setRoomId(roomId)
        }
        
        // Get other user ID from arguments
        arguments?.getString("other_user_id")?.let { otherUserId ->
            viewModel.setOtherUserId(otherUserId)
        }
    }
    
    private fun setupViews() {
        // Mute button
        binding.btnMute.setOnClickListener {
            viewModel.toggleMute()
        }
        
        // Disconnect button
        binding.btnDisconnect.setOnClickListener {
            viewModel.disconnect()
            requireActivity().onBackPressed()
        }
    }
    
    private fun observeViewModel() {
        // Room ID
        viewModel.roomId.observe(viewLifecycleOwner) { roomId ->
            binding.tvRoomId.text = "Oda ID: $roomId"
        }
        
        // My user info
        viewModel.myUserInfo.observe(viewLifecycleOwner) { userInfo ->
            binding.tvMyName.text = userInfo.name
            binding.tvMyStatus.text = if (userInfo.isMuted) "🔇 Ses kapalı" else "🎤 Ses açık"
            binding.tvMyStatus.setTextColor(
                resources.getColor(
                    if (userInfo.isMuted) com.intercomapp.R.color.error 
                    else com.intercomapp.R.color.success, 
                    null
                )
            )
            binding.ivMyMicStatus.setImageResource(
                if (userInfo.isMuted) com.intercomapp.R.drawable.ic_mic_off 
                else com.intercomapp.R.drawable.ic_mic_on
            )
        }
        
        // Other user info
        viewModel.otherUserInfo.observe(viewLifecycleOwner) { userInfo ->
            binding.tvOtherName.text = userInfo.name
            binding.tvOtherStatus.text = if (userInfo.isMuted) "🔇 Ses kapalı" else "🎤 Ses açık"
            binding.tvOtherStatus.setTextColor(
                resources.getColor(
                    if (userInfo.isMuted) com.intercomapp.R.color.error 
                    else com.intercomapp.R.color.success, 
                    null
                )
            )
            binding.ivOtherMicStatus.setImageResource(
                if (userInfo.isMuted) com.intercomapp.R.drawable.ic_mic_off 
                else com.intercomapp.R.drawable.ic_mic_on
            )
        }
        
        // Connection status
        viewModel.connectionStatus.observe(viewLifecycleOwner) { status ->
            binding.tvConnectionStatus.text = status.message
            binding.tvConnectionStatus.setTextColor(resources.getColor(status.color, null))
            
            // Update button visibility based on call status
            updateButtonVisibility()
            
            // Update participants count
            updateParticipantsCount()
        }
        
        // Messages
        viewModel.message.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateButtonVisibility() {
        // Always show mute and disconnect buttons
        binding.btnMute.visibility = View.VISIBLE
    }
    
    private fun updateParticipantsCount() {
        // Count participants based on connection status
        val isConnected = viewModel.connectionStatus.value?.message?.contains("Ses bağlantısı kuruldu") == true
        val participantCount = if (isConnected) 2 else 1
        binding.tvParticipantsTitle.text = "👥 Katılımcılar ($participantCount)"
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance(roomId: String, otherUserId: String): VoiceRoomFragment {
            return VoiceRoomFragment().apply {
                arguments = Bundle().apply {
                    putString("room_id", roomId)
                    putString("other_user_id", otherUserId)
                }
            }
        }
    }
}

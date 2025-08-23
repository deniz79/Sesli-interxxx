package com.intercomapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.intercomapp.databinding.FragmentHomeBinding
import com.intercomapp.ui.main.MainActivity
// import dagger.hilt.android.AndroidEntryPoint

// @AndroidEntryPoint
class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        viewModel.setContext(requireContext())

        setupViews()
        observeViewModel()
        
        // Get service from MainActivity
        (activity as? MainActivity)?.let { mainActivity ->
            viewModel.setIntercomService(mainActivity.getIntercomService())
        }
    }
    
    private fun setupViews() {
        // Copy ID button
        binding.btnCopyId.setOnClickListener {
            viewModel.copyUserIdToClipboard()
        }
        
        // Connect by ID button
        binding.btnConnectById.setOnClickListener {
            val targetId = binding.etTargetId.text.toString().trim()
            if (targetId.isNotEmpty()) {
                viewModel.connectToUserId(targetId)
                binding.etTargetId.text?.clear()
                // Navigate to voice room
                navigateToVoiceRoom(targetId)
            } else {
                Toast.makeText(context, "Lütfen bir ID girin", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Connect button
        binding.btnConnect.setOnClickListener {
            viewModel.toggleConnection()
        }
        
        // Mute button
        binding.btnMute.setOnClickListener {
            viewModel.toggleMute()
        }
        
        // Music buttons
        binding.btnStartMusic.setOnClickListener {
            viewModel.startMusic()
        }
        
        binding.btnStopMusic.setOnClickListener {
            viewModel.stopMusic()
        }
        
        // Volume buttons
        binding.btnVolumeUp.setOnClickListener {
            viewModel.adjustVolume(true)
        }
        
        binding.btnVolumeDown.setOnClickListener {
            viewModel.adjustVolume(false)
        }
        
        // Voice command button
        binding.btnVoiceCommand.setOnClickListener {
            viewModel.toggleVoiceCommands()
        }
    }
    
    private fun observeViewModel() {
        // Observe user ID
        viewModel.getUserId()?.let { userId ->
            binding.tvMyUserId.text = userId
        } ?: run {
            binding.tvMyUserId.text = "ID YÜKLENİYOR..."
        }
        
        viewModel.connectionState.observe(viewLifecycleOwner) { isConnected ->
            updateConnectionUI(isConnected)
        }
        
        viewModel.muteState.observe(viewLifecycleOwner) { isMuted ->
            updateMuteUI(isMuted)
        }
        
        viewModel.musicState.observe(viewLifecycleOwner) { isPlaying ->
            updateMusicUI(isPlaying)
        }
        
        viewModel.voiceCommandState.observe(viewLifecycleOwner) { isListening ->
            updateVoiceCommandUI(isListening)
        }
        
        viewModel.message.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                // ID ile bağlantı kurulduğunda UI'ı güncelle
                if (it.contains("Bağlantı kuruluyor")) {
                    updateConnectionUI(true)
                }
            }
        }
    }
    
    private fun updateConnectionUI(isConnected: Boolean) {
        if (isConnected) {
            binding.btnConnect.text = getString(com.intercomapp.R.string.disconnect)
            binding.btnConnect.setBackgroundColor(resources.getColor(com.intercomapp.R.color.error, null))
            binding.tvStatus.text = getString(com.intercomapp.R.string.connected)
            binding.tvStatus.setTextColor(resources.getColor(com.intercomapp.R.color.success, null))
        } else {
            binding.btnConnect.text = getString(com.intercomapp.R.string.start_connection)
            binding.btnConnect.setBackgroundColor(resources.getColor(com.intercomapp.R.color.primary, null))
            binding.tvStatus.text = getString(com.intercomapp.R.string.ready_to_connect)
            binding.tvStatus.setTextColor(resources.getColor(com.intercomapp.R.color.primary, null))
        }
    }
    
    private fun updateMuteUI(isMuted: Boolean) {
        if (isMuted) {
            binding.btnMute.text = getString(com.intercomapp.R.string.unmute)
            binding.btnMute.setBackgroundColor(resources.getColor(com.intercomapp.R.color.warning, null))
        } else {
            binding.btnMute.text = getString(com.intercomapp.R.string.mute)
            binding.btnMute.setBackgroundColor(resources.getColor(com.intercomapp.R.color.primary, null))
        }
    }
    
    private fun updateMusicUI(isPlaying: Boolean) {
        binding.btnStartMusic.isEnabled = !isPlaying
        binding.btnStopMusic.isEnabled = isPlaying
        
        if (isPlaying) {
            binding.tvMusicStatus.text = getString(com.intercomapp.R.string.music_playing)
            binding.tvMusicStatus.setTextColor(resources.getColor(com.intercomapp.R.color.success, null))
        } else {
            binding.tvMusicStatus.text = getString(com.intercomapp.R.string.music_stopped)
            binding.tvMusicStatus.setTextColor(resources.getColor(com.intercomapp.R.color.text_secondary, null))
        }
    }
    
    private fun updateVoiceCommandUI(isListening: Boolean) {
        if (isListening) {
            binding.btnVoiceCommand.text = "Sesli Komut Dinleniyor..."
            binding.btnVoiceCommand.setBackgroundColor(resources.getColor(com.intercomapp.R.color.accent, null))
        } else {
            binding.btnVoiceCommand.text = "Sesli Komutları Aç/Kapat"
            binding.btnVoiceCommand.setBackgroundColor(resources.getColor(com.intercomapp.R.color.primary, null))
        }
    }
    
    fun onServiceReady() {
        // Get service from MainActivity when service is ready
        (activity as? MainActivity)?.let { mainActivity ->
            viewModel.setIntercomService(mainActivity.getIntercomService())
        }
    }
    
    private fun navigateToVoiceRoom(otherUserId: String) {
        // Create room ID (combination of both user IDs)
        val currentUserId = viewModel.getUserId() ?: "unknown"
        val roomId = if (currentUserId < otherUserId) {
            "${currentUserId}_${otherUserId}"
        } else {
            "${otherUserId}_${currentUserId}"
        }
        
        // Navigate to voice room
        val voiceRoomFragment = com.intercomapp.ui.voiceroom.VoiceRoomFragment.newInstance(roomId, otherUserId)
        
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

package com.intercomapp.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.intercomapp.databinding.FragmentGroupsBinding

class GroupsFragment : Fragment() {
    
    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // TODO: Implement groups functionality
        binding.tvPlaceholder.text = "Gruplar sayfası yakında gelecek"
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

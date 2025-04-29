package com.example.bushido.ui.bolos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.bushido.databinding.FragmentBolosBinding

class BolosFragment : Fragment() {

    private var _binding: FragmentBolosBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bolosViewModel = ViewModelProvider(this).get(BolosViewModel::class.java)

        _binding = FragmentBolosBinding.inflate(inflater, container, false)
        val root: View = binding.root



        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

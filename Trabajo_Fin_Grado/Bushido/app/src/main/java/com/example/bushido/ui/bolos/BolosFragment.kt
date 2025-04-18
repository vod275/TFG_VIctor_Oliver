package com.example.bushido.ui.bolos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.bushido.databinding.FragmentBolosBinding

class BolosFragment : Fragment() {

    private var _binding: FragmentBolosBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val BolosViewModel =
            ViewModelProvider(this).get(BolosViewModel::class.java)

        _binding = FragmentBolosBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textBolos
        BolosViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
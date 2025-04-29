package com.example.bushido.ui.padel_tenis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.bushido.databinding.FragmentPadelTenisBinding

class Padel_tenisFragment : Fragment() {

    private var _binding: FragmentPadelTenisBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val Padel_tenisViewModel =
            ViewModelProvider(this).get(Padel_tenisViewModel::class.java)

        _binding = FragmentPadelTenisBinding.inflate(inflater, container, false)
        val root: View = binding.root


        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
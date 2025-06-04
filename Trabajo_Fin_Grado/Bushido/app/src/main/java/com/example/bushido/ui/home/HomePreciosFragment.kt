package com.example.bushido.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.bushido.databinding.FragmentPrecioshomeBinding
import com.google.firebase.firestore.FirebaseFirestore

class HomePreciosFragment : Fragment() {

    private var _binding: FragmentPrecioshomeBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrecioshomeBinding.inflate(inflater, container, false)
        val view = binding.root

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cargarPrecios()

        return view
    }

    private fun cargarPrecios() {
        db.collection("SocioPrecio").document("actual").get()
            .addOnSuccessListener { doc ->
                doc?.let {
                    binding.tvAbonosMananasIndividual.editText?.setText(it.getString("mañanas") ?: "")
                    binding.tvAbonosIndividual.editText?.setText(it.getString("individual") ?: "")
                    binding.tvAbonoPareja.editText?.setText(it.getString("pareja") ?: "")
                    binding.tvAbonoFamiliar.editText?.setText(it.getString("familiar") ?: "")
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar precios", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

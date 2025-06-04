package com.example.bushido.ui.padel_tenis

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.bushido.databinding.FragmentPrecioPadelTenisBinding
import com.google.firebase.firestore.FirebaseFirestore

class Padel_tenisPreciosFragment : Fragment() {

    private var _binding: FragmentPrecioPadelTenisBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrecioPadelTenisBinding.inflate(inflater, container, false)
        val view = binding.root

        // Ajustar los insets del sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializamos Firestore
        db = FirebaseFirestore.getInstance()

        cargarPrecios()

        return view
    }


    private fun cargarPrecios() {
        val docRef = db.collection("TenisPadelPrecio").document("actual")

        docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore", "Error al obtener documento", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                // Aquí uso editText?.setText() por si los campos son TextInputLayout
                binding.tvPreciosPadelPista.editText?.setText(snapshot.getString("padelSocio") ?: "")
                binding.tvPreciosPadelPistaInvitado.editText?.setText(snapshot.getString("padelInvitado") ?: "")
                binding.tvPreciosTenisPistaSocio.editText?.setText(snapshot.getString("tenisSocio") ?: "")
                binding.tvPreciosTenisPistaInvitado.editText?.setText(snapshot.getString("tenisInvitado") ?: "")
                binding.tvPreciosTenisPistaSocioTierra.editText?.setText(snapshot.getString("tenisSocioTierra") ?: "")
                binding.tvPreciosTenisPistaInvitadoTierra.editText?.setText(snapshot.getString("tenisInvitadoTierra") ?: "")
            } else {
                Log.d("Firestore", "Documento no existe o es null")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

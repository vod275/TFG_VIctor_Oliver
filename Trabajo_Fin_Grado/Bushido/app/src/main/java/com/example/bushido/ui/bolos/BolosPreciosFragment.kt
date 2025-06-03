package com.example.bushido.ui.bolos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.bushido.databinding.FragmentPrecioBolosBinding
import com.google.firebase.firestore.FirebaseFirestore

class BolosPreciosFragment : Fragment() {

    // Binding para acceder a las vistas del layout
    private var _binding: FragmentPrecioBolosBinding? = null
    private val binding get() = _binding!!

    // Instancia de Firestore
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflar layout usando ViewBinding
        _binding = FragmentPrecioBolosBinding.inflate(inflater, container, false)
        val view = binding.root

        // Ajustar padding para respetar Insets del sistema (status bar, navegación)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Cargar los precios desde Firestore
        cargarPrecios()

        return view
    }

    private fun cargarPrecios() {
        val docRef = firestore.collection("BolosPrecio").document("actual")

        // Escuchar cambios en tiempo real en el documento "BolosPrecio"
        docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore", "Error al obtener documento", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val socio = snapshot.getString("socio") ?: ""
                val invitado = snapshot.getString("invitado") ?: ""

                Log.d("Firestore", "Datos cargados: socio=$socio, invitado=$invitado")

                // Poner los valores cargados en los campos de texto
                binding.tvPreciosBoloslPista.editText?.setText(socio)
                binding.tvPreciosBolosPistaInvitado.editText?.setText(invitado)
            } else {
                Log.d("Firestore", "Documento no existe o es null")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Liberar el binding para evitar fugas de memoria
        _binding = null
    }
}

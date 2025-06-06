package com.example.bushido.ui.reservasLista

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bushido.adaptadorListaReservas.ListaReservasAdapter
import com.example.bushido.databinding.FragmentListaReservasBinding
import com.example.bushido.models.ReservaBolos
import com.google.firebase.firestore.FirebaseFirestore
import objetos.UserSession

class ListaReservasFragment : Fragment() {

    private var _binding: FragmentListaReservasBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: ListaReservasAdapter
    private val listaReservas = mutableListOf<ReservaBolos>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaReservasBinding.inflate(inflater, container, false)
        val view = binding.root

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cargarReservas()
        return view
    }

    private fun cargarReservas() {
        adapter = ListaReservasAdapter(listaReservas, requireContext()) { reserva ->
            borrarReserva(reserva)
        }

        binding.rvListaReservas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvListaReservas.adapter = adapter

        db.collection("reservas")
            .whereEqualTo("usuarioId", UserSession.id)
            .get()
            .addOnSuccessListener { result ->
                listaReservas.clear()
                for (document in result) {
                    val reserva = document.toObject(ReservaBolos::class.java)
                    reserva.idReserva = document.id
                    listaReservas.add(reserva)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error cargando reservas", Toast.LENGTH_SHORT).show()
            }
    }


    private fun borrarReserva(reserva: ReservaBolos) {
        db.collection("reservas").document(reserva.idReserva!!)
            .delete()
            .addOnSuccessListener {
                val bloqueosRef = db.collection("bloqueos")
                    .document("pista${reserva.numeroPistaBolos}")
                    .collection(reserva.fecha ?: "")
                    .document(reserva.hora ?: "")

                bloqueosRef.delete()

                listaReservas.remove(reserva)
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Reserva borrada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al borrar reserva", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

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
import com.example.bushido.R
import com.example.bushido.adaptadorListaReservas.ListaReservasAdapter
import com.example.bushido.databinding.FragmentListaReservasBinding
import com.example.bushido.models.Reservas
import com.google.firebase.firestore.FirebaseFirestore
import objetos.UserSession
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class ListaReservasFragment : Fragment() {

    private var _binding: FragmentListaReservasBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: ListaReservasAdapter
    private val listaReservas = mutableListOf<Reservas>()

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

        val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }

        val ahora = Calendar.getInstance()  // Fecha y hora actual

        db.collection("reservas")
            .whereEqualTo("usuarioId", UserSession.id)
            .get()
            .addOnSuccessListener { result ->
                listaReservas.clear()

                for (document in result) {
                    val reserva = document.toObject(Reservas::class.java)
                    reserva.idReserva = document.id

                    val fechaStr = reserva.fecha?.trim()
                    val horaStr = reserva.hora?.trim()

                    if (!fechaStr.isNullOrEmpty() && !horaStr.isNullOrEmpty()) {
                        try {
                            val fechaReserva = sdfFecha.parse(fechaStr)
                            val horaReserva = sdfHora.parse(horaStr)

                            if (fechaReserva != null && horaReserva != null) {
                                // Creamos un Calendar con la fecha y hora de la reserva combinadas
                                val calReserva = Calendar.getInstance().apply {
                                    time = fechaReserva
                                    set(Calendar.HOUR_OF_DAY, Calendar.getInstance().apply { time = horaReserva }.get(Calendar.HOUR_OF_DAY))
                                    set(Calendar.MINUTE, Calendar.getInstance().apply { time = horaReserva }.get(Calendar.MINUTE))
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }

                                // Solo añadimos si la reserva es hoy o en el futuro respecto a ahora
                                if (calReserva >= ahora) {
                                    listaReservas.add(reserva)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    getString(R.string.error_cargando_reservas), Toast.LENGTH_SHORT).show()
            }
    }




    private fun borrarReserva(reserva: Reservas) {
        db.collection("reservas").document(reserva.idReserva!!)
            .delete()
            .addOnSuccessListener {
                val bloqueosRef = db.collection("bloqueos")
                    .document("pista${reserva.numeroPista}")
                    .collection(reserva.fecha ?: "")
                    .document(reserva.hora ?: "")

                bloqueosRef.delete()

                listaReservas.remove(reserva)
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(),
                    getString(R.string.reserva_borrada), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    getString(R.string.error_al_borrar_reserva), Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

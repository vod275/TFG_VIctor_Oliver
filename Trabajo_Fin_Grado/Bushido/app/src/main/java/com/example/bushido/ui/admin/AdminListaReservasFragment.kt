package com.example.bushido.ui.admin

import android.app.DatePickerDialog
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
import com.example.bushido.databinding.FragmentAdminListaReservasBinding
import com.example.bushido.models.Reservas
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminListaReservasFragment : Fragment() {

    private var _binding: FragmentAdminListaReservasBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ListaReservasAdapter
    private val listaReservas = mutableListOf<Reservas>()
    private val db = FirebaseFirestore.getInstance()
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminListaReservasBinding.inflate(inflater, container, false)
        val view = binding.root

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarRecyclerView()
        configurarDatePicker()
        cargarReservas() // Carga inicial sin filtro

        return view
    }

    private fun configurarRecyclerView() {
        adapter = ListaReservasAdapter(listaReservas, requireContext()) {
            Toast.makeText(requireContext(),
                getString(R.string.reserva_eliminada), Toast.LENGTH_SHORT).show()
        }
        binding.rvListareservasAdmin.layoutManager = LinearLayoutManager(requireContext())
        binding.rvListareservasAdmin.adapter = adapter
    }

    private fun configurarDatePicker() {
        binding.etFechaReserva.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val fechaSeleccionada = Calendar.getInstance()
                    fechaSeleccionada.set(year, month, dayOfMonth)
                    val fechaFormateada = sdf.format(fechaSeleccionada.time)
                    binding.etFechaReserva.setText(fechaFormateada)
                    cargarReservas(fechaFormateada)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun cargarReservas(fechaFiltrada: String? = null) {
        db.collection("reservas")
            .get()
            .addOnSuccessListener { documentos ->
                val listaFiltrada = documentos.mapNotNull { it.toObject(Reservas::class.java).apply { idReserva = it.id } }
                    .filter {
                        fechaFiltrada == null || it.fecha == fechaFiltrada
                    }
                adapter.actualizarLista(listaFiltrada)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), getString(R.string.error_al_cargar_reservas), Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

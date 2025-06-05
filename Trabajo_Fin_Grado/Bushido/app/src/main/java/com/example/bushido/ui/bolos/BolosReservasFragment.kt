package com.example.bushido.ui.bolos

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.bushido.R
import com.google.android.material.button.MaterialButton
import com.example.bushido.databinding.FragmentBolosReservasBinding
import com.google.firebase.firestore.FirebaseFirestore
import objetos.UserSession
import java.util.Calendar

class BolosReservasFragment : Fragment() {

    private var _binding: FragmentBolosReservasBinding? = null
    private val binding get() = _binding!!
    private var pistaSeleccionada: MaterialButton? = null
    private var horasBloqueadas: List<String> = emptyList()
    private val pistasBloqueadas = mutableMapOf<Int, MutableSet<String>>() // pista -> horas bloqueadas




    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBolosReservasBinding.inflate(inflater, container, false)
        val view = binding.root

        // Ajustar insets del sistema
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarFecha()
        configurarBotonesPistas()

        binding.btnReservarPistaBolos.setOnClickListener {
            realizarReserva()
        }

        return view
    }

    private fun configurarFecha() {
        val fechaEditText = binding.etFechaReserva

        fechaEditText.setOnClickListener {
            val hoy = Calendar.getInstance()

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val fechaFormateada = "%02d/%02d/%04d".format(dayOfMonth, month + 1, year)
                    fechaEditText.setText(fechaFormateada)
                    actualizarHorasDisponibles(year, month, dayOfMonth)
                },
                hoy.get(Calendar.YEAR),
                hoy.get(Calendar.MONTH),
                hoy.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun actualizarHorasDisponibles(year: Int, month: Int, day: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        val diaSemana = calendar.get(Calendar.DAY_OF_WEEK)

        val horaInicio = 10
        val horaFin = if (diaSemana == Calendar.FRIDAY || diaSemana == Calendar.SATURDAY) 24 else 23

        val horas = mutableListOf<String>()
        for (h in horaInicio until horaFin) {
            horas.add(String.format("%02d:00", h))
            horas.add(String.format("%02d:30", h))
        }

        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_azul, horas)
        adapter.setDropDownViewResource(R.layout.spinner_item_azul)
        binding.spinnerHoras.adapter = adapter

        // Consultar Firestore para obtener bloqueos
        val fechaClave = "%04d-%02d-%02d".format(year, month + 1, day)
        cargarBloqueosDePistas(fechaClave)
    }


    private fun cargarBloqueosDePistas(fecha: String) {
        val db = FirebaseFirestore.getInstance()
        pistasBloqueadas.clear()

        var pistasCargadas = 0
        for (i in 1..4) {
            val bloqueos = mutableSetOf<String>()
            val ref = db.collection("Bolos")
                .document("PistaBolos")
                .collection("bloqueos")
                .document("pista$i")
                .collection(fecha)

            ref.get().addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.getString("hora")?.let { bloqueos.add(it) }
                }
                pistasBloqueadas[i] = bloqueos

                pistasCargadas++
                if (pistasCargadas == 4) {
                    setupHoraSpinnerListener()
                    val horaSeleccionada = binding.spinnerHoras.selectedItem?.toString()
                    if (horaSeleccionada != null) {
                        actualizarBotonesPistas(horaSeleccionada)
                    }
                }
            }
        }
    }

    private fun setupHoraSpinnerListener() {
        binding.spinnerHoras.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val horaSeleccionada = parent.getItemAtPosition(position).toString()
                actualizarBotonesPistas(horaSeleccionada)

                // Si la pista seleccionada está bloqueada para esta hora, deseleccionar
                pistaSeleccionada?.let { boton ->
                    val pistaTexto = boton.text.toString()
                    val numero = pistaTexto.substringAfter("PISTA ").toIntOrNull()
                    if (numero != null && pistasBloqueadas[numero]?.contains(horaSeleccionada) == true) {
                        Toast.makeText(requireContext(), "La pista seleccionada está ocupada a esa hora", Toast.LENGTH_SHORT).show()
                        pistaSeleccionada = null
                        boton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.transparent))
                        boton.setTextColor(ContextCompat.getColor(requireContext(), R.color.AzulTexto))
                        boton.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.AzulTexto)
                    }
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }





    private fun configurarBotonesPistas() {
        val botones: List<MaterialButton> = listOf(
            binding.btnPista1,
            binding.btnPista2,
            binding.btnPista3,
            binding.btnPista4
        )


        for (boton in botones) {
            boton.setOnClickListener {
                // Restaurar estilo de botón anterior si existe
                pistaSeleccionada?.apply {
                    backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.transparent)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.AzulTexto))
                    strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.AzulTexto)
                }

                pistaSeleccionada = boton

                boton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.AzulTexto))
                boton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                boton.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.AzulTexto)

                // Actualizar horas disponibles si ya hay fecha seleccionada
                val fechaTexto = binding.etFechaReserva.text.toString()
                if (fechaTexto.isNotEmpty()) {
                    val partes = fechaTexto.split("/")
                    if (partes.size == 3) {
                        val dia = partes[0].toInt()
                        val mes = partes[1].toInt() - 1
                        val año = partes[2].toInt()
                        actualizarHorasDisponibles(año, mes, dia)
                    }
                }
            }

        }
    }


    private fun realizarReserva() {
        val userId = UserSession.id
        val userName = UserSession.nombre ?: "Invitado"
        val userEmail = UserSession.email ?: ""
        val esSocio = !userEmail.endsWith("@gmail.com") && !userEmail.contains("google")

        val pistaTexto = pistaSeleccionada?.text?.toString()
        val numeroPistaBolos = pistaTexto?.substringAfter("PISTA ")?.toIntOrNull()

        val fecha = binding.etFechaReserva.text.toString()
        val hora = binding.spinnerHoras.selectedItem?.toString()

        if (userId == null || numeroPistaBolos == null || fecha.isEmpty() || hora == null) {
            Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar que la pista no esté ya reservada en esa hora
        val bloqueadas = pistasBloqueadas[numeroPistaBolos]
        if (bloqueadas != null && bloqueadas.contains(hora)) {
            Toast.makeText(requireContext(), "Esa pista no está disponible a esa hora", Toast.LENGTH_LONG).show()
            binding.spinnerHoras.setSelection(0)
            return
        }

        val db = FirebaseFirestore.getInstance()

        // Obtener el precio y guardar la reserva
        val precioRef = db.collection("BolosPrecio").document("actual")
        precioRef.get().addOnSuccessListener { doc ->
            val precioString = if (esSocio) doc.getString("socio") else doc.getString("invitado")
            val precio = precioString?.toDoubleOrNull()

            if (precio == null) {
                Toast.makeText(requireContext(), "No se pudo obtener un precio válido", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val reserva = hashMapOf(
                "usuarioId" to userId,
                "nombre" to userName,
                "numeroPistaBolos" to numeroPistaBolos,
                "fecha" to fecha,
                "hora" to hora,
                "precio" to precio
            )

            val idReserva = "${userId}_${fecha.replace("/", "-")}_${hora.replace(":", "-")}"

            db.collection("reservas")
                .document(idReserva)
                .set(reserva)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Reserva realizada correctamente", Toast.LENGTH_SHORT).show()
                    bloquearHoraPista(numeroPistaBolos, fecha, hora)
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al guardar la reserva", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error al obtener el precio", Toast.LENGTH_SHORT).show()
        }
    }



    private fun bloquearHoraPista(numeroPista: Int, fecha: String, hora: String) {
        val db = FirebaseFirestore.getInstance()
        val bloqueo = hashMapOf(
            "hora" to hora
        )

        db.collection("Bolos")
            .document("PistaBolos")
            .collection("bloqueos")
            .document("pista$numeroPista")
            .collection(fecha.replace("/", "-")) // Fecha como subcolección (opcional)
            .document(hora.replace(":", "-")) // Documento por hora
            .set(bloqueo)
    }

    private fun actualizarBotonesPistas(horaSeleccionada: String) {
        val botones: List<MaterialButton> = listOf(
            binding.btnPista1,
            binding.btnPista2,
            binding.btnPista3,
            binding.btnPista4
        )

        for ((index, boton) in botones.withIndex()) {
            val pista = index + 1
            val bloqueadas = pistasBloqueadas[pista] ?: emptySet()
            if (bloqueadas.contains(horaSeleccionada)) {
                boton.isEnabled = false
                boton.alpha = 0.5f
            } else {
                boton.isEnabled = true
                boton.alpha = 1.0f
            }
        }

        // Si la pista seleccionada quedó deshabilitada, deseleccionarla
        if (pistaSeleccionada != null && !pistaSeleccionada!!.isEnabled) {
            pistaSeleccionada = null
        }
    }





    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

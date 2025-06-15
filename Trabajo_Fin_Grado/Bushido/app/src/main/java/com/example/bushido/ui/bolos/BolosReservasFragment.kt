package com.example.bushido.ui.bolos

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.bushido.R
import com.example.bushido.databinding.FragmentBolosReservasBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import objetos.UserSession
import java.util.Calendar
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class BolosReservasFragment : Fragment() {

    private var _binding: FragmentBolosReservasBinding? = null
    private val binding get() = _binding!!

    private var pistaSeleccionada: MaterialButton? = null
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

        db.collection("reservas")
            .whereEqualTo("fecha", fecha)
            .whereEqualTo("tipo", getString(R.string.bolos))
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val pista = doc.getLong("numeroPistaBolos")?.toInt()
                    val hora = doc.getString("hora")
                    if (pista != null && hora != null) {
                        val setHoras = pistasBloqueadas.getOrPut(pista) { mutableSetOf() }
                        setHoras.add(hora)
                    }
                }
                setupHoraSpinnerListener()
                val horaSeleccionada = binding.spinnerHoras.selectedItem?.toString()
                if (horaSeleccionada != null) {
                    actualizarBotonesPistas(horaSeleccionada)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), getString(R.string.error_al_cargar_reservas), Toast.LENGTH_SHORT).show()
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

                pistaSeleccionada?.let { boton ->
                    val pistaTexto = boton.text.toString()
                    val numero =  pistaTexto?.let {
                        Regex("\\d+").find(it)?.value?.toIntOrNull()
                    }
                    if (numero != null && pistasBloqueadas[numero]?.contains(horaSeleccionada) == true) {
                        Toast.makeText(requireContext(),getString(R.string.la_pista_seleccionada_est_ocupada_a_esa_hora), Toast.LENGTH_SHORT).show()
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
        val botones = listOf(
            binding.btnPista1,
            binding.btnPista2,
            binding.btnPista3,
            binding.btnPista4
        )

        for (boton in botones) {
            boton.setOnClickListener {
                pistaSeleccionada?.apply {
                    backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.transparent)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.AzulTexto))
                    strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.AzulTexto)
                }

                pistaSeleccionada = boton

                boton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.AzulTexto))
                boton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                boton.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.AzulTexto)

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
        val numeroPistaBolos = pistaTexto?.let {
            Regex("\\d+").find(it)?.value?.toIntOrNull()
        }
        val tipo = getString(R.string.bolos)

        val fecha = binding.etFechaReserva.text.toString()
        val hora = binding.spinnerHoras.selectedItem?.toString()

        if (userId == null || numeroPistaBolos == null || fecha.isBlank() || hora.isNullOrBlank()) {
            Toast.makeText(requireContext(), getString(R.string.completa_todos_los_campos), Toast.LENGTH_SHORT).show()
            return
        }

        // 🔒 Validación de fecha y hora pasada
        try {
            val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaReservaCompleta = formato.parse("$fecha $hora")
            val ahora = Calendar.getInstance().time

            if (fechaReservaCompleta.before(ahora)) {
                Toast.makeText(requireContext(), getString(R.string.no_se_puede_reservar_en_el_pasado), Toast.LENGTH_LONG).show()
                return
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.formato_de_fecha_u_hora_inv_lido), Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val fechaFirestore = fecha.replace("/", "-")
        val horaFirestore = hora.replace(":", "-")

        // ID único de la reserva
        val idReserva = "${userId}_${fechaFirestore}_${horaFirestore}"

        // Buscar si ya existe una reserva para esa pista, fecha y hora (bloqueo)
        db.collection("reservas")
            .whereEqualTo("numeroPista", numeroPistaBolos)
            .whereEqualTo("fecha", fecha)
            .whereEqualTo("hora", hora)
            .whereEqualTo("tipo", tipo)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    Toast.makeText(requireContext(),
                        getString(R.string.esa_pista_ya_est_ocupada_a_esa_hora), Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Verificar si el usuario ya tiene reserva a esa hora
                db.collection("reservas")
                    .whereEqualTo("usuarioId", userId)
                    .whereEqualTo("fecha", fecha)
                    .whereEqualTo("hora", hora)
                    .get()
                    .addOnSuccessListener { querySnapshot  ->
                        if (!querySnapshot.isEmpty) {
                            Toast.makeText(requireContext(),
                                getString(R.string.ya_tienes_una_reserva_a_esa_hora), Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        // Obtener precio y hacer reserva
                        db.collection("BolosPrecio").document("actual").get()
                            .addOnSuccessListener { docPrecio ->
                                val precioString = if (esSocio) docPrecio.getString("socio") else docPrecio.getString("invitado")
                                val precio = precioString?.toDoubleOrNull()

                                if (precio == null) {
                                    Toast.makeText(requireContext(),
                                        getString(R.string.no_se_pudo_obtener_un_precio_v_lido), Toast.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }

                                val reserva = hashMapOf(
                                    "usuarioId" to userId,
                                    "nombre" to userName,
                                    "numeroPista" to numeroPistaBolos,
                                    "fecha" to fecha,
                                    "hora" to hora,
                                    "precio" to precio,
                                    "tipo" to tipo,
                                    "bloqueada" to true,  // <-- aquí indicas que está bloqueada/reservada
                                )

                                // Guardar reserva (y bloqueo implícito)
                                db.collection("reservas").document(idReserva).set(reserva)
                                    .addOnSuccessListener {
                                        Toast.makeText(requireContext(),
                                            getString(R.string.reserva_realizada_correctamente), Toast.LENGTH_SHORT).show()
                                        mostrarNotificacionReservaExitosa()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(requireContext(),
                                            getString(R.string.error_al_guardar_la_reserva), Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(),
                                    getString(R.string.error_al_obtener_el_precio), Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(),
                            getString(R.string.error_al_verificar_reservas_del_usuario), Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    getString(R.string.error_al_verificar_bloqueos), Toast.LENGTH_SHORT).show()
            }
    }




    private fun actualizarBotonesPistas(horaSeleccionada: String) {
        val botones = listOf(
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

        if (pistaSeleccionada != null && !pistaSeleccionada!!.isEnabled) {
            pistaSeleccionada = null
        }
    }


    private fun mostrarNotificacionReservaExitosa() {
        val channelId = "reserva_exitosa_channel"
        val notificationId = 1

        // Crear canal (solo Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.reservas)
            val descriptionText = getString(R.string.notificaciones_de_reservas_realizadas)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Crear notificación
        val builder = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(R.drawable.logo) // Usa un ícono existente en drawable
            .setContentTitle(getString(R.string.reserva_realizada))
            .setContentText(getString(R.string.tu_reserva_de_bolos_se_ha_registrado_correctamente))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Mostrar notificación
        with(NotificationManagerCompat.from(requireContext())) {
            notify(notificationId, builder.build())
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
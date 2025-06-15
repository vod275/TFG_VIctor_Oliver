package com.example.bushido.ui.padel_tenis

import android.app.DatePickerDialog
import android.app.NotificationChannel
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
import com.example.bushido.databinding.FragmentPadelTenisReservasBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import objetos.UserSession
import java.util.Calendar
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Locale

class Padel_TenisReservasFragment : Fragment() {

    private var _binding: FragmentPadelTenisReservasBinding? = null
    private val binding get() = _binding!!

    private var pistaSeleccionada: MaterialButton? = null
    private val pistasBloqueadas = mutableMapOf<Int, MutableSet<String>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPadelTenisReservasBinding.inflate(inflater, container, false)
        val view = binding.root

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarFecha()
        configurarBotonesPistas()

        binding.btnReservarPistaPadelTenis.setOnClickListener {
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

        val horaInicio = 9
        val horaFin = if (diaSemana == Calendar.SATURDAY || diaSemana == Calendar.SUNDAY) 22 else 21

        val horas = mutableListOf<String>()
        for (h in horaInicio until horaFin) {
            horas.add(String.format("%02d:00", h))
        }

        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_azul, horas)
        adapter.setDropDownViewResource(R.layout.spinner_item_azul)
        binding.spinnerHoras.adapter = adapter

        val fechaClave = "%04d-%02d-%02d".format(year, month + 1, day)
        cargarBloqueosDePistas(fechaClave)
    }

    private fun cargarBloqueosDePistas(fecha: String) {
        val db = FirebaseFirestore.getInstance()
        pistasBloqueadas.clear()

        db.collection("reservas")
            .whereEqualTo("fecha", fecha)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val tipo = doc.getString("tipo") ?: continue
                    // Solo procesar reservas de padel o tenis (ignorar bolos)
                    if (tipo != getString(R.string.padel) &&
                        tipo != getString(R.string.tenis) &&
                        tipo != getString(R.string.tenis_tierra)) continue

                    val pista = doc.getLong("numeroPista")?.toInt()
                    val hora = doc.getString("hora")
                    if (pista != null && hora != null) {
                        val setHoras = pistasBloqueadas.getOrPut(pista) { mutableSetOf() }
                        setHoras.add(hora)
                    }
                }
                setupHoraSpinnerListener()
                binding.spinnerHoras.selectedItem?.toString()?.let { actualizarBotonesPistas(it) }
            }
            .addOnFailureListener {

                Toast.makeText(requireContext(), getString(R.string.error_al_cargar_reservas), Toast.LENGTH_SHORT).show()
            }
    }


    private fun setupHoraSpinnerListener() {
        binding.spinnerHoras.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val horaSeleccionada = parent.getItemAtPosition(position).toString()
                actualizarBotonesPistas(horaSeleccionada)

                pistaSeleccionada?.let { boton ->
                    val pistaTexto = boton.text.toString()
                    val numero =  pistaTexto?.let {
                        Regex("\\d+").find(it)?.value?.toIntOrNull()
                    }
                    if (numero != null && pistasBloqueadas[numero]?.contains(horaSeleccionada) == true) {
                        Toast.makeText(requireContext(),  getString(R.string.la_pista_seleccionada_est_ocupada_a_esa_hora), Toast.LENGTH_SHORT).show()
                        // Deseleccionar visualmente la pista
                        boton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.transparent))
                        boton.setTextColor(ContextCompat.getColor(requireContext(), R.color.AzulTexto))
                        boton.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.AzulTexto)
                        pistaSeleccionada = null
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
            binding.btnPista4,
            binding.btnPista5,
            binding.btnPista6,
            binding.btnPista7,
            binding.btnPista8,
            binding.btnPista9
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

                val partes = binding.etFechaReserva.text.toString().split("/")
                if (partes.size == 3) {
                    val dia = partes[0].toInt()
                    val mes = partes[1].toInt() - 1
                    val año = partes[2].toInt()
                    actualizarHorasDisponibles(año, mes, dia)
                }
            }
        }
    }

    private fun realizarReserva() {
        val userId = UserSession.id
        val userName = UserSession.nombre ?: "Invitado"
        val userEmail = UserSession.email ?: ""
        val esSocio = !userEmail.endsWith("@gmail.com") && !userEmail.contains("google")

        if (userId == null) {
            Toast.makeText(requireContext(), getString(R.string.usuario_no_identificado), Toast.LENGTH_SHORT).show()
            return
        }

        val pistaTexto = pistaSeleccionada?.text?.toString()
        val numeroPista = pistaTexto?.let {
            Regex("\\d+").find(it)?.value?.toIntOrNull()
        }

        if (numeroPista == null) {
            Toast.makeText(requireContext(), getString(R.string.n_mero_de_pista_no_v_lido), Toast.LENGTH_SHORT).show()
            return
        }

        val tipo = when (numeroPista) {
            in 1..6 -> getString(R.string.padel)
            7, 8 -> getString(R.string.tenis)
            9 -> getString(R.string.tenis_tierra)
            else -> {
                Toast.makeText(requireContext(), getString(R.string.pista_no_v_lida), Toast.LENGTH_SHORT).show()
                return
            }
        }

        val clavePrecio = when (numeroPista) {
            in 1..6 -> if (esSocio) "padelSocio" else "padelInvitado"
            7, 8 -> if (esSocio) "tenisSocio" else "tenisInvitado"
            9 -> if (esSocio) "tenisSocioTierra" else "tenisInvitadoTierra"
            else -> null
        }

        if (clavePrecio == null) {
            Toast.makeText(requireContext(), getString(R.string.no_se_pudo_determinar_la_clave_del_precio), Toast.LENGTH_SHORT).show()
            return
        }

        val fecha = binding.etFechaReserva.text.toString()
        val hora = binding.spinnerHoras.selectedItem?.toString()

        if (fecha.isBlank() || hora.isNullOrBlank()) {
            Toast.makeText(requireContext(), getString(R.string.completa_ambos_campos), Toast.LENGTH_SHORT).show()
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
        val idReserva = "${userId}_${fechaFirestore}_${horaFirestore}_${numeroPista}"

        fun guardarReserva(precio: Double) {
            val reserva = hashMapOf(
                "usuarioId" to userId,
                "nombre" to userName,
                "numeroPista" to numeroPista,
                "fecha" to fecha,
                "hora" to hora,
                "precio" to precio,
                "tipo" to tipo,
                "bloqueada" to true
            )
            db.collection("reservas").document(idReserva).set(reserva)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), getString(R.string.reserva_realizada_correctamente), Toast.LENGTH_SHORT).show()
                    mostrarNotificacionReservaExitosa()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), getString(R.string.error_al_guardar_la_reserva), Toast.LENGTH_SHORT).show()
                }
        }

        if (tipo == getString(R.string.padel) || tipo == getString(R.string.tenis) || tipo == getString(R.string.tenis_tierra)) {
            db.collection("reservas")
                .whereEqualTo("numeroPista", numeroPista)
                .whereEqualTo("fecha", fecha)
                .whereEqualTo("hora", hora)
                .whereEqualTo("tipo", tipo)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.documents.isNotEmpty()) {
                        Toast.makeText(requireContext(), getString(R.string.esa_pista_ya_est_ocupada_a_esa_hora), Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    db.collection("reservas")
                        .whereEqualTo("usuarioId", userId)
                        .whereEqualTo("fecha", fecha)
                        .whereEqualTo("hora", hora)
                        .get()
                        .addOnSuccessListener { userRes ->
                            if (userRes.documents.isNotEmpty()) {
                                Toast.makeText(requireContext(), getString(R.string.ya_tienes_una_reserva_a_esa_hora), Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            db.collection("TenisPadelPrecio").document("actual").get()
                                .addOnSuccessListener { docPrecio ->
                                    val precioString = docPrecio.getString(clavePrecio)
                                    val precio = precioString?.toDoubleOrNull()

                                    if (precio == null) {
                                        Toast.makeText(requireContext(), getString(R.string.no_se_pudo_obtener_un_precio_v_lido), Toast.LENGTH_SHORT).show()
                                        return@addOnSuccessListener
                                    }

                                    guardarReserva(precio)
                                }
                                .addOnFailureListener {
                                    Toast.makeText(requireContext(), getString(R.string.error_al_obtener_el_precio), Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), getString(R.string.error_al_verificar_reservas_del_usuario), Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), getString(R.string.error_al_verificar_bloqueos), Toast.LENGTH_SHORT).show()
                }
        } else {
            db.collection("TenisPadelPrecio").document("actual").get()
                .addOnSuccessListener { docPrecio ->
                    val precioString = docPrecio.getString(clavePrecio)
                    val precio = precioString?.toDoubleOrNull()

                    if (precio == null) {
                        Toast.makeText(requireContext(), getString(R.string.no_se_pudo_obtener_un_precio_v_lido), Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    guardarReserva(precio)
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), getString(R.string.error_al_obtener_el_precio), Toast.LENGTH_SHORT).show()
                }
        }
    }





    private fun actualizarBotonesPistas(horaSeleccionada: String) {
        val botones = listOf(
            binding.btnPista1,
            binding.btnPista2,
            binding.btnPista3,
            binding.btnPista4,
            binding.btnPista5,
            binding.btnPista6,
            binding.btnPista7,
            binding.btnPista8,
            binding.btnPista9
        )

        for ((index, boton) in botones.withIndex()) {
            val pista = index + 1
            val bloqueadas = pistasBloqueadas[pista] ?: emptySet()
            boton.isEnabled = !bloqueadas.contains(horaSeleccionada)
            boton.alpha = if (boton.isEnabled) 1.0f else 0.5f
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
            .setContentText(getString(R.string.tu_reserva_est_realizada_con_exito))
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

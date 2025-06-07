package com.example.bushido.ui.padel_tenis

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.*
import android.provider.MediaStore
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bushido.R
import com.example.bushido.databinding.FragmentPadelTenisBinding
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target

class Padel_tenisFragment : Fragment() {

    // Binding para acceder a los elementos del layout
    private var _binding: FragmentPadelTenisBinding? = null
    private val binding get() = _binding!!

    // Handler para manejar tareas con retardo
    private val handler = Handler(Looper.getMainLooper())

    // Referencia al almacenamiento de Firebase
    private val storage = FirebaseStorage.getInstance()

    // Lista para almacenar las URLs de las imágenes
    private var listaUrls = mutableListOf<String>()

    // Índice de la imagen actualmente mostrada
    private var indiceActual = 0

    // Runnables para las animaciones periódicas
    private var animacionVaivenRunnable: Runnable? = null
    private var rotarRunnable: Runnable? = null

    // Método que infla el layout del fragmento
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPadelTenisBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Método llamado cuando la vista ha sido creada
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Cargar animaciones desde recursos
        val animFlecha = AnimationUtils.loadAnimation(requireContext(), com.example.bushido.R.anim.flecha_anim)
        val animClick = AnimationUtils.loadAnimation(requireContext(), com.example.bushido.R.anim.agrandar_click)

        // Aplicar animación a la flecha
        binding.ibflecha.startAnimation(animFlecha)

        // Iniciar la rotación de imágenes desde Firebase
        cargarImagenesRotativas()

        // Botón para ir a ver los precios
        binding.ibPrecioPistas.setOnClickListener {
            binding.ibPrecioPistas.startAnimation(animClick)
            findNavController().navigate(R.id.nav_precios_padel_tenis)
        }

        // Botón para reservar pista (solo animación aquí)
        binding.btnReservarPista.setOnClickListener {
            binding.btnReservarPista.startAnimation(animClick)
            findNavController().navigate(R.id.nav_reservas_padel_tenis)
        }

        // Volver a cargar e iniciar animaciones
        cargarImagenesRotativas()
        iniciarAnimacionesVaiven()
    }

    // Al volver al fragmento, se reanudan las animaciones e imágenes
    override fun onResume() {
        super.onResume()
        cargarImagenesRotativas()
        iniciarAnimacionesVaiven()
    }

    // Método para cargar imágenes desde Firebase Storage
    private fun cargarImagenesRotativas() {
        listaUrls.clear() // Vaciar lista para evitar duplicados
        val storageRef = storage.reference.child("FotosTenisPadel")

        storageRef.listAll().addOnSuccessListener { result ->
            if (result.items.isEmpty()) {
                if (isAdded) Toast.makeText(requireContext(), "No hay imágenes en FotosTenisPadel", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            result.items.forEach { item ->
                item.downloadUrl.addOnSuccessListener { uri ->
                    listaUrls.add(uri.toString())

                    // Mostrar la primera imagen al obtenerla
                    if (listaUrls.size == 1 && isAdded) {
                        Picasso.get().load(listaUrls[0]).into(binding.ivPadelTennis)
                    }

                    // Iniciar rotación una vez se hayan cargado todas
                    if (listaUrls.size == result.items.size) {
                        iniciarRotacion()
                    }
                }.addOnFailureListener { e ->
                    if (isAdded) Toast.makeText(requireContext(), "Error al obtener URL: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener { e ->
            if (isAdded) Toast.makeText(requireContext(), "Fallo en listAll(): ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Método para descargar imágenes y guardarlas en la galería
    private fun guardarImagenEnGaleria() {
        val storageRef = FirebaseStorage.getInstance().reference.child("FotosPrecioPistas")

        storageRef.listAll().addOnSuccessListener { result ->
            val items = result.items
            if (items.isEmpty()) {
                if (isAdded) Toast.makeText(requireContext(), "No hay imágenes para descargar", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            var descargadas = 0
            val total = items.size

            items.forEach { item ->
                item.downloadUrl.addOnSuccessListener { uri ->
                    // Descargar y guardar cada imagen
                    descargarYGuardarImagen(uri.toString()) {
                        descargadas++
                        if (descargadas == total && isAdded) {
                            Toast.makeText(requireContext(), "Se han guardado $descargadas imágenes", Toast.LENGTH_LONG).show()
                        }
                    }
                }.addOnFailureListener {
                    if (isAdded) Toast.makeText(requireContext(), "Error obteniendo URL de ${item.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener { e ->
            if (isAdded) Toast.makeText(requireContext(), "Error accediendo a Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Método para iniciar la animación vaivén en botones
    private fun iniciarAnimacionesVaiven() {
        val animVaiven = AnimationUtils.loadAnimation(requireContext(), com.example.bushido.R.anim.descargar_anim)

        animacionVaivenRunnable = object : Runnable {
            override fun run() {
                if (!isAdded || _binding == null) return

                binding.btnReservarPista.startAnimation(animVaiven)
                binding.ibPrecioPistas.startAnimation(animVaiven)

                handler.postDelayed(this, 7000) // Repetir cada 7 segundos
            }
        }

        handler.post(animacionVaivenRunnable!!)
    }

    // Método para iniciar la rotación automática de imágenes con animación
    private fun iniciarRotacion() {
        val slideOut = AnimationUtils.loadAnimation(requireContext(), com.example.bushido.R.anim.moverfotoizq)
        val slideIn = AnimationUtils.loadAnimation(requireContext(), com.example.bushido.R.anim.moverfotoder)

        rotarRunnable = object : Runnable {
            override fun run() {
                if (!isAdded || _binding == null) return

                binding.ivPadelTennis.startAnimation(slideOut)

                handler.postDelayed({
                    if (!isAdded || _binding == null) return@postDelayed

                    Picasso.get().load(listaUrls[indiceActual]).into(binding.ivPadelTennis)
                    binding.ivPadelTennis.startAnimation(slideIn)

                    // Cambiar a la siguiente imagen en la lista
                    indiceActual = (indiceActual + 1) % listaUrls.size
                    handler.postDelayed(this, 7000)
                }, slideOut.duration)
            }
        }

        handler.post(rotarRunnable!!)
    }

    // Método auxiliar para descargar una imagen desde una URL y guardarla
    private fun descargarYGuardarImagen(url: String, onImagenGuardada: () -> Unit) {
        Picasso.get().load(url).into(object : Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                if (bitmap == null) {
                    onImagenGuardada()
                    return
                }

                // Crear nombre de archivo único
                val filename = "precios_pistas_${System.currentTimeMillis()}.png"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MiApp")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                // Guardar en la galería usando ContentResolver
                val resolver = requireContext().contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    resolver.openOutputStream(it).use { outputStream ->
                        outputStream?.let { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                onImagenGuardada()
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                onImagenGuardada()
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
        })
    }

    // Limpiar recursos y detener animaciones al destruir la vista
    override fun onDestroyView() {
        super.onDestroyView()
        rotarRunnable?.let { handler.removeCallbacks(it) }
        _binding = null
    }
}

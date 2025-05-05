package com.example.bushido.ui.home

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.*
import android.provider.MediaStore
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bushido.databinding.FragmentHomeBinding
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import android.graphics.drawable.Drawable
import com.squareup.picasso.Target

class HomeFragment : Fragment() {

    // ViewBinding para acceder a los elementos del layout
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Handler para ejecutar tareas repetitivas con retardo
    private val handler = Handler(Looper.getMainLooper())

    // Referencia a Firebase Storage
    private val storage = FirebaseStorage.getInstance()

    // Lista para guardar las URLs de las imágenes que se rotarán
    private var listaUrls = mutableListOf<String>()

    // Índice de la imagen actual mostrada
    private var indiceActual = 0

    // Runnables para controlar las animaciones
    private var animacionVaivenRunnable: Runnable? = null
    private var rotarRunnable: Runnable? = null

    // Inflado del layout con binding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Configura animaciones y listeners al crear la vista
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Carga animaciones desde archivos XML en res/anim
        val animFlecha = AnimationUtils.loadAnimation(requireContext(), com.example.bushido.R.anim.flecha_anim)
        val animClick = AnimationUtils.loadAnimation(requireContext(), com.example.bushido.R.anim.agrandar_click)

        // Comienza la animación de la flecha
        binding.ibflecha.startAnimation(animFlecha)

        // Carga imágenes desde Firebase Storage y las rota en el ImageView
        cargarImagenesRotativas()

        // Al pulsar el botón de precios, se animará y descargará imágenes
        binding.ibPrecioSocios.setOnClickListener {
            binding.ibPrecioSocios.startAnimation(animClick)
            guardarImagenEnGaleria()
        }

        // Animación al pulsar el botón de info (no tiene funcionalidad extra)
        binding.ibInfo.setOnClickListener {
            binding.ibInfo.startAnimation(animClick)
        }

        // Se vuelven a cargar las imágenes y se inicia la animación vaivén
        cargarImagenesRotativas()
        iniciarAnimacionesVaiven()
    }

    // Se vuelve a ejecutar cuando el fragmento está visible
    override fun onResume() {
        super.onResume()
        cargarImagenesRotativas()
        iniciarAnimacionesVaiven()
    }

    // Carga las imágenes desde la carpeta "FotosHome" de Firebase Storage
    private fun cargarImagenesRotativas() {
        val storageRef = storage.reference.child("FotosHome")
        storageRef.listAll().addOnSuccessListener { result ->
            if (result.items.isEmpty()) {
                if (isAdded) Toast.makeText(requireContext(), "No hay imágenes en FotosHome", Toast.LENGTH_SHORT).show()
            }
            result.items.forEach { item ->
                item.downloadUrl.addOnSuccessListener { uri ->
                    listaUrls.add(uri.toString())
                    // Se carga la primera imagen si es la única hasta el momento
                    if (listaUrls.size == 1 && isAdded) {
                        Picasso.get().load(listaUrls[0]).into(binding.ivHome)
                    }
                    // Si ya se han cargado todas las URLs, se inicia la rotación automática
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

    // Guarda todas las imágenes de la carpeta "FotosPrecioSocios" en la galería del dispositivo
    private fun guardarImagenEnGaleria() {
        val storageRef = FirebaseStorage.getInstance().reference.child("FotosPrecioSocios")

        storageRef.listAll()
            .addOnSuccessListener { result ->
                val items = result.items
                if (items.isEmpty()) {
                    if (isAdded) Toast.makeText(requireContext(), "No hay imágenes para descargar", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                var descargadas = 0
                val total = items.size

                // Se descargan y guardan todas las imágenes una por una
                items.forEach { item ->
                    item.downloadUrl.addOnSuccessListener { uri ->
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
            }
            .addOnFailureListener { e ->
                if (isAdded) Toast.makeText(requireContext(), "Error accediendo a Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Inicia la animación de vaivén de los botones "Info" y "PrecioSocios" cada 7 segundos
    private fun iniciarAnimacionesVaiven() {
        val animVaiven = AnimationUtils.loadAnimation(requireContext(), com.example.bushido.R.anim.descargar_anim)

        animacionVaivenRunnable = object : Runnable {
            override fun run() {
                if (!isAdded || _binding == null) return

                binding.ibInfo.startAnimation(animVaiven)
                binding.ibPrecioSocios.startAnimation(animVaiven)

                handler.postDelayed(this, 7000) // Repite cada 7 segundos
            }
        }

        handler.post(animacionVaivenRunnable!!)
    }

    // Inicia la rotación automática de imágenes en el ImageView
    private fun iniciarRotacion() {
        val slideOut = AnimationUtils.loadAnimation(requireContext(), com.example.bushido.R.anim.moverfotoizq)
        val slideIn = AnimationUtils.loadAnimation(requireContext(), com.example.bushido.R.anim.moverfotoder)

        rotarRunnable = object : Runnable {
            override fun run() {
                if (!isAdded || _binding == null) return

                binding.ivHome.startAnimation(slideOut)

                // Cambia la imagen tras finalizar la animación de salida
                handler.postDelayed({
                    if (!isAdded || _binding == null) return@postDelayed

                    Picasso.get().load(listaUrls[indiceActual]).into(binding.ivHome)
                    binding.ivHome.startAnimation(slideIn)

                    indiceActual = (indiceActual + 1) % listaUrls.size
                    handler.postDelayed(this, 7000) // Repite cada 7 segundos

                }, slideOut.duration)
            }
        }

        handler.post(rotarRunnable!!)
    }

    // Descarga una imagen desde URL y la guarda en la galería del dispositivo
    private fun descargarYGuardarImagen(url: String, onImagenGuardada: () -> Unit) {
        Picasso.get().load(url).into(object : Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                if (bitmap == null) {
                    onImagenGuardada()
                    return
                }

                // Crea los metadatos para guardar la imagen
                val filename = "precios_socios_${System.currentTimeMillis()}.png"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MiApp")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val resolver = requireContext().contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    resolver.openOutputStream(it).use { outputStream ->
                        outputStream?.let { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    }
                    // Finaliza el guardado marcando como no pendiente
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

    // Limpieza de recursos cuando se destruye la vista
    override fun onDestroyView() {
        super.onDestroyView()
        rotarRunnable?.let { handler.removeCallbacks(it) }
        _binding = null
    }
}

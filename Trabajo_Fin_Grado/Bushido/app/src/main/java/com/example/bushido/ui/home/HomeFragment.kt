package com.example.bushido.ui.home

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.example.bushido.R
import com.example.bushido.databinding.FragmentHomeBinding
import android.view.animation.AnimationUtils

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Asegúrate de que el binding no sea null
        _binding?.let { binding ->

            // Aplicar la animación cada 7 segundos
            val handler = android.os.Handler()
            val shakeRunnable = object : Runnable {
                override fun run() {
                    val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.descargar_anim)
                    binding.ibPrecioSocios.startAnimation(shake)
                    binding.ibInfo.startAnimation(shake)
                    handler.postDelayed(this, 7000) // cada 7 segundos
                }
            }

            handler.post(shakeRunnable)

            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.flecha_anim)
            val scaleAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.agrandar_click)
            binding.ibflecha.startAnimation(anim)

            binding.ibPrecioSocios.setOnClickListener {
                binding.ibPrecioSocios.startAnimation(scaleAnim)
                guardarImagenEnGaleria()
            }

            binding.ibInfo.setOnClickListener {
                binding.ibInfo.startAnimation(scaleAnim)
            }

            cargarImagenesRotativas()
        }
    }

    private fun guardarImagenEnGaleria() {
        val storageRef = FirebaseStorage.getInstance().reference.child("FotosPrecioSocios")

        storageRef.listAll()
            .addOnSuccessListener { result ->
                if (result.items.isEmpty()) {
                    Toast.makeText(requireContext(), "No hay imágenes para descargar", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                result.items.forEach { item ->
                    item.downloadUrl.addOnSuccessListener { uri ->
                        descargarYGuardarImagen(uri.toString())
                    }.addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error obteniendo URL: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error accediendo a Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun descargarYGuardarImagen(url: String) {
        Picasso.get().load(url).into(object : com.squareup.picasso.Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                if (bitmap == null) return

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
                        if (outputStream != null) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    Toast.makeText(requireContext(), "Imagen guardada: $filename", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(requireContext(), "Error al guardar imagen", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onBitmapFailed(e: java.lang.Exception?, errorDrawable: android.graphics.drawable.Drawable?) {
                Toast.makeText(requireContext(), "Fallo al descargar imagen", Toast.LENGTH_SHORT).show()
            }

            override fun onPrepareLoad(placeHolderDrawable: android.graphics.drawable.Drawable?) {}
        })
    }


    private val handler = Handler(Looper.getMainLooper())
    private val storage = FirebaseStorage.getInstance()
    private var listaUrls = mutableListOf<String>()
    private var indiceActual = 0

    private fun cargarImagenesRotativas() {
        val storageRef = storage.reference.child("FotosHome")
        storageRef.listAll().addOnSuccessListener { result ->
            if (result.items.isEmpty()) {
                Toast.makeText(requireContext(), "No hay imágenes en FotosHome", Toast.LENGTH_SHORT).show()
            }
            result.items.forEachIndexed { index, item ->
                item.downloadUrl
                    .addOnSuccessListener { uri ->
                        listaUrls.add(uri.toString())

                        // Mostrar la primera imagen apenas esté lista
                        if (listaUrls.size == 1) {
                            Picasso.get().load(listaUrls[0]).into(binding.ivHome)
                        }

                        // Iniciar rotación cuando todas las URLs estén listas
                        if (listaUrls.size == result.items.size) {
                            rotarImagenes()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error al obtener URL: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Fallo en listAll(): ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun rotarImagenes() {
        if (listaUrls.isEmpty()) return

        val slideOut = AnimationUtils.loadAnimation(requireContext(), R.anim.moverfotoizq)
        val slideIn = AnimationUtils.loadAnimation(requireContext(), R.anim.moverfotoder)

        handler.postDelayed(object : Runnable {
            override fun run() {
                // Animación de salida
                binding.ivHome.startAnimation(slideOut)

                slideOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}

                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        // Cambiar imagen y aplicar animación de entrada
                        Picasso.get().load(listaUrls[indiceActual]).into(binding.ivHome)
                        binding.ivHome.startAnimation(slideIn)
                    }

                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                })

                indiceActual = (indiceActual + 1) % listaUrls.size
                handler.postDelayed(this, 7000)
            }
        }, 7000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

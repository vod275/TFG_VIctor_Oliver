package com.example.bushido.ui.admin

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.bushido.databinding.FragmentAdminBolosBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.*

class AdminBolosFragment : Fragment() {

    private var _binding: FragmentAdminBolosBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var tempImageUri: Uri? = null

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        _binding = FragmentAdminBolosBinding.inflate(inflater, container, false)
        val view = binding.root

        // Ajuste para insets (opcional)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Aquí cargas los precios desde Firestore para que aparezcan en los campos ---
        cargarPrecios()

        // Botón para guardar cambios
        binding.btnPreciosBolosAceptar.setOnClickListener {
            val socio = binding.tvPreciosBoloslPista.editText?.text.toString()
            val invitado = binding.tvPreciosBolosPistaInvitado.editText?.text.toString()

            if (socio.isNotBlank() && invitado.isNotBlank()) {
                val datos = mapOf(
                    "socio" to socio,
                    "invitado" to invitado
                )
                firestore.collection("BolosPrecio").document("actual")
                    .set(datos)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Precios actualizados", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Error al actualizar precios", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(requireContext(), "Completa ambos campos", Toast.LENGTH_SHORT).show()
            }
        }

        // Otros botones como subir fotos
        binding.btnSubirFotosBolos.setOnClickListener {
            mostrarDialogoImagen()
        }

        return view
    }


    private fun cargarPrecios() {
        firestore.collection("BolosPrecio").document("actual")
            .get()
            .addOnSuccessListener { doc ->
                binding.tvPreciosBoloslPista.editText?.setText(doc.getString("socio") ?: "")
                binding.tvPreciosBolosPistaInvitado.editText?.setText(doc.getString("invitado") ?: "")
            }
    }

    private val seleccionarImagenGaleria = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { subirImagen(it) }
        }
    }

    private val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempImageUri != null) {
            subirImagen(tempImageUri!!)
        }
    }

    private fun mostrarDialogoImagen() {
        val opciones = arrayOf("Hacer foto", "Elegir de galería")
        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar imagen")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> {
                        val uri = crearUriTemporal()
                        tempImageUri = uri
                        tomarFoto.launch(uri)
                    }
                    1 -> {
                        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        seleccionarImagenGaleria.launch(intent)
                    }
                }
            }
            .show()
    }

    private fun crearUriTemporal(): Uri {
        val archivo = File.createTempFile("foto_bolos", ".jpg", requireContext().cacheDir)
        return FileProvider.getUriForFile(requireContext(), "com.example.bushido.fileprovider", archivo)
    }

    private fun subirImagen(uri: Uri) {
        val nombreArchivo = "IMG_${UUID.randomUUID()}.jpg"
        val referencia = storage.reference.child("FotosBolos/$nombreArchivo")
        referencia.putFile(uri)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Imagen subida", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al subir imagen", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

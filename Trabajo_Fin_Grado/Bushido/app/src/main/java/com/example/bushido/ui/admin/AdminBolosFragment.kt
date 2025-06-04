package com.example.bushido.ui.admin

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
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
    private val storageRef = storage.reference.child("FotosBolos")

    private var tempImageUri: Uri? = null

    private val launcherGallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { subirImagen(it) }
    }

    private val launcherCamera = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempImageUri?.let { subirImagen(it) }
        } else {
            Toast.makeText(requireContext(), "Foto cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAdminBolosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cargarPrecios()

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

        binding.btnSubirFotosBolos.setOnClickListener {
            mostrarDialogoImagen()
        }
    }

    private fun cargarPrecios() {
        firestore.collection("BolosPrecio").document("actual")
            .get()
            .addOnSuccessListener { doc ->
                binding.tvPreciosBoloslPista.editText?.setText(doc.getString("socio") ?: "")
                binding.tvPreciosBolosPistaInvitado.editText?.setText(doc.getString("invitado") ?: "")
            }
    }

    private fun mostrarDialogoImagen() {
        val opciones = arrayOf("Hacer foto", "Elegir de galería")
        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar imagen")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> abrirCamara()
                    1 -> launcherGallery.launch("image/*")
                }
            }
            .show()
    }

    private fun abrirCamara() {
        val photoFile = File.createTempFile("foto_bolos_temp", ".jpg", requireContext().cacheDir)
        tempImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)
        launcherCamera.launch(tempImageUri)
    }

    private fun subirImagen(uri: Uri) {
        val nombreArchivo = "IMG_${UUID.randomUUID()}.jpg"
        val referencia = storageRef.child(nombreArchivo)

        referencia.putFile(uri)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Imagen subida con éxito", Toast.LENGTH_SHORT).show()
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

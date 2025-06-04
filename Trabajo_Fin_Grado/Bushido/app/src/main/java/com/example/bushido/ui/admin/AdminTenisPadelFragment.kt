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
import com.example.bushido.databinding.FragmentAdminTenisPadelBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class AdminTenisPadelFragment : Fragment() {

    private var _binding: FragmentAdminTenisPadelBinding? = null
    private val binding get() = _binding!!

    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storageRef = storage.reference.child("FotosTenisPadel")
    private val docRef = firestore.collection("TenisPadelPrecio").document("actual")

    private val launcherGallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadImage(it) }
    }

    private val launcherCamera = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) imageUriTemp?.let { uploadImage(it) }
    }

    private var imageUriTemp: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAdminTenisPadelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cargarPrecios()

        binding.btnPreciosTenisPadelAceptar.setOnClickListener {
            val precios = mapOf(
                "padelSocio" to binding.tvPreciosPadelPista.editText?.text?.toString().orEmpty(),
                "padelInvitado" to binding.tvPreciosPadelPistaInvitado.editText?.text?.toString().orEmpty(),
                "tenisSocio" to binding.tvPreciosTenisPistaSocio.editText?.text?.toString().orEmpty(),
                "tenisInvitado" to binding.tvPreciosTenisPistaInvitado.editText?.text?.toString().orEmpty(),
                "tenisSocioTierra" to binding.tvPreciosTenisPistaSocioTierra.editText?.text?.toString().orEmpty(),
                "tenisInvitadoTierra" to binding.tvPreciosTenisPistaInvitadoTierra.editText?.text?.toString().orEmpty()
            )

            docRef.set(precios)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Precios actualizados", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show()
                }
        }

        binding.btnSubirFotosPadelTenis.setOnClickListener {
            mostrarOpcionesImagen()
        }
    }

    private fun cargarPrecios() {
        docRef.get().addOnSuccessListener { document ->
            document?.let {
                // Para cada TextInputLayout, aseguramos que su editText no sea nulo antes de setText
                binding.tvPreciosPadelPista.editText?.setText(it.getString("padelSocio") ?: "")
                binding.tvPreciosPadelPistaInvitado.editText?.setText(it.getString("padelInvitado") ?: "")
                binding.tvPreciosTenisPistaSocio.editText?.setText(it.getString("tenisSocio") ?: "")
                binding.tvPreciosTenisPistaInvitado.editText?.setText(it.getString("tenisInvitado") ?: "")
                binding.tvPreciosTenisPistaSocioTierra.editText?.setText(it.getString("tenisSocioTierra") ?: "")
                binding.tvPreciosTenisPistaInvitadoTierra.editText?.setText(it.getString("tenisInvitadoTierra") ?: "")
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error al cargar precios", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarOpcionesImagen() {
        val opciones = arrayOf("Galería", "Cámara")
        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar imagen")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> launcherGallery.launch("image/*")
                    1 -> abrirCamara()
                }
            }
            .show()
    }

    private fun abrirCamara() {
        val photoFile = File.createTempFile("foto_temp", ".jpg", requireContext().cacheDir)
        imageUriTemp = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)
        launcherCamera.launch(imageUriTemp)
    }

    private fun uploadImage(uri: Uri) {
        val nombreArchivo = "foto_${System.currentTimeMillis()}.jpg"
        val imageRef = storageRef.child(nombreArchivo)

        imageRef.putFile(uri)
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

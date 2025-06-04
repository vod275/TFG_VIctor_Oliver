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
import com.example.bushido.databinding.FragmentAdminHomeBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.util.UUID

class AdminHomeFragment : Fragment() {

    private lateinit var binding: FragmentAdminHomeBinding
    private val storage = FirebaseStorage.getInstance()
    private val db = Firebase.firestore
    private val storageRef = storage.reference.child("FotosHome")

    private var tempImageUri: Uri? = null

    private val launcherCamera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && tempImageUri != null) {
                subirImagen(tempImageUri!!)
            }
        }

    private val launcherGaleria =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { subirImagen(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminHomeBinding.inflate(inflater, container, false)

        cargarPrecios()
        binding.btnPreciosSociosAceptar.setOnClickListener { guardarPrecios() }
        binding.btnSubirFotosHome.setOnClickListener { mostrarDialogoImagen() }

        return binding.root
    }

    private fun cargarPrecios() {
        db.collection("SocioPrecio").document("actual").get()
            .addOnSuccessListener { doc ->
                doc?.let {
                    binding.tvAbonosMananasIndividual.editText?.setText(it.getString("mañanas") ?: "")
                    binding.tvAbonosIndividual.editText?.setText(it.getString("individual") ?: "")
                    binding.tvAbonoPareja.editText?.setText(it.getString("pareja") ?: "")
                    binding.tvAbonoFamiliar.editText?.setText(it.getString("familiar") ?: "")
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar precios", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarPrecios() {
        val socioMañanas = binding.tvAbonosMananasIndividual.editText?.text.toString()
        val individual = binding.tvAbonosIndividual.editText?.text.toString()
        val pareja = binding.tvAbonoPareja.editText?.text.toString()
        val familiar = binding.tvAbonoFamiliar.editText?.text.toString()

        if (socioMañanas.isNotBlank() && individual.isNotBlank() && pareja.isNotBlank() && familiar.isNotBlank()) {
            val datos = mapOf(
                "mañanas" to socioMañanas,
                "individual" to individual,
                "pareja" to pareja,
                "familiar" to familiar
            )

            db.collection("SocioPrecio").document("actual")
                .set(datos)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Precios guardados", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
        }
    }


    private fun mostrarDialogoImagen() {
        val opciones = arrayOf("Tomar foto", "Elegir de galería")
        AlertDialog.Builder(requireContext())
            .setTitle("Subir imagen")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> abrirCamara()
                    1 -> launcherGaleria.launch("image/*")
                }
            }
            .show()
    }

    private fun abrirCamara() {
        val photoFile = File.createTempFile("foto_home_temp", ".jpg", requireContext().cacheDir)
        tempImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
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

}


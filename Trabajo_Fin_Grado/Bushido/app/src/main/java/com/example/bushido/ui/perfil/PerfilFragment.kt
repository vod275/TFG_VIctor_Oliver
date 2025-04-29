package com.example.bushido.ui.perfil

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.bushido.databinding.FragmentPerfilBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import objetos.UserSession
import java.io.ByteArrayOutputStream

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val PREFS_NAME = "PerfilPrefs"
    private val storageRef = FirebaseStorage.getInstance().reference
    private val PICK_IMAGE = 1001
    private val TAKE_PHOTO = 1002
    private var currentPhotoUri: Uri? = null

    // Launcher para pedir permiso de cámara
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                tomarFotoConCamara()
            } else {
                Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)

        UserSession.id = FirebaseAuth.getInstance().currentUser?.uid

        cargarDatosGuardados()
        cargarFotoPerfil()

        binding.ibFotoPerfil.setOnClickListener { mostrarOpcionesFoto() }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        guardarDatos()
        _binding = null
    }

    private fun guardarDatos() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString("nombre", binding.tvNombreAjustes.editText?.text.toString())
            putString("apellidos", binding.tvApellidos.editText?.text.toString())
            putString("fecha", binding.etFechaNacimiento.editText?.text.toString())
        }
    }

    private fun cargarDatosGuardados() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        binding.tvNombreAjustes.editText?.setText(prefs.getString("nombre", ""))
        binding.tvApellidos.editText?.setText(prefs.getString("apellidos", ""))
        binding.etFechaNacimiento.editText?.setText(prefs.getString("fecha", ""))
    }

    private fun mostrarOpcionesFoto() {
        val opciones = arrayOf("Galería", "Cámara")
        AlertDialog.Builder(requireContext())
            .setTitle("Selecciona una opción")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> seleccionarDesdeGaleria()
                    1 -> verificarPermisoYCamara()
                }
            }.show()
    }

    private fun verificarPermisoYCamara() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            tomarFotoConCamara()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun seleccionarDesdeGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE)
    }

    private fun tomarFotoConCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, TAKE_PHOTO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE -> {
                    val uri = data?.data
                    uri?.let { subirImagenAFirebase(it) }
                }
                TAKE_PHOTO -> {
                    val bitmap = data?.extras?.get("data") as? Bitmap
                    bitmap?.let { subirImagenAFirebase(bitmapToUri(it)) }
                }
            }
        }
    }

    private fun bitmapToUri(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(requireContext().contentResolver, bitmap, "temp", null)
        return Uri.parse(path)
    }

    private fun subirImagenAFirebase(uri: Uri) {
        val uid = UserSession.id ?: return
        val ref = storageRef.child("FotosUser/$uid.jpg")

        ref.delete().addOnCompleteListener {
            ref.putFile(uri).addOnSuccessListener {
                cargarFotoPerfil()
            }
        }
    }

    private fun cargarFotoPerfil() {
        val uid = UserSession.id ?: return
        val ref = storageRef.child("FotosUser/$uid.jpg")
        ref.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this).load(uri).into(binding.ibFotoPerfil)
        }
    }
}

package com.example.bushido.ui.perfil

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.bushido.databinding.FragmentPerfilBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import objetos.UserSession
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PerfilFragment : Fragment() {

    // Binding para el fragmento
    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val PREFS_NAME = "PerfilPrefs"
    private val storageRef = FirebaseStorage.getInstance().reference

    // Códigos para identificar la acción realizada (galería o cámara)
    private val PICK_IMAGE = 1001
    private val TAKE_PHOTO = 1002

    private var currentPhotoUri: Uri? = null

    // Permiso de cámara
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

        // Obtener UID actual del usuario
        UserSession.id = FirebaseAuth.getInstance().currentUser?.uid

        // Cargar datos y foto del usuario
        cargarDatosGuardados()
        cargarFotoPerfil()

        // Acciones para seleccionar o tomar foto de perfil
        binding.ibFotoPerfil.setOnClickListener { mostrarOpcionesFoto() }

        // Acción para guardar los datos del perfil
        binding.btnGuardar.setOnClickListener {
            guardarDatosUsuario()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Carga los datos guardados del usuario desde Firestore
     */
    private fun cargarDatosGuardados() {
        val uid = UserSession.id ?: return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nombre = document.getString("nombre") ?: ""
                    val apellidos = document.getString("apellidos") ?: ""
                    val fechaNacimiento = document.getString("fechaNacimiento") ?: ""

                    binding.tvNombreAjustes.editText?.setText(nombre)
                    binding.tvApellidos.editText?.setText(apellidos)
                    binding.etFechaNacimiento.editText?.setText(fechaNacimiento)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar los datos", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Muestra un diálogo con opciones para seleccionar foto de galería o tomar una nueva
     */
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

    /**
     * Verifica si el permiso de cámara está concedido o lo solicita
     */
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

    /**
     * Abre la galería para seleccionar una imagen
     */
    private fun seleccionarDesdeGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE)
    }

    /**
     * Abre la cámara para tomar una foto
     */
    private fun tomarFotoConCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, TAKE_PHOTO)
    }

    /**
     * Recibe el resultado de la galería o cámara y procesa la imagen
     */
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

    /**
     * Convierte un Bitmap a Uri para subirlo a Firebase
     */
    private fun bitmapToUri(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(requireContext().contentResolver, bitmap, "temp", null)
        return Uri.parse(path)
    }

    /**
     * Sube una imagen a Firebase Storage y actualiza la imagen de perfil
     */
    private fun subirImagenAFirebase(uri: Uri) {
        val uid = UserSession.id ?: return
        val ref = storageRef.child("FotosUser/$uid.jpg")

        ref.putFile(uri)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Imagen subida correctamente", Toast.LENGTH_SHORT).show()
                cargarFotoPerfil()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al subir la imagen", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Carga la imagen de perfil desde Firebase Storage usando Glide
     */
    private fun cargarFotoPerfil() {
        val uid = UserSession.id ?: return
        val ref = storageRef.child("FotosUser/$uid.jpg")
        ref.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this).load(uri).into(binding.ibFotoPerfil)
        }
    }

    /**
     * Guarda los datos del usuario en Firestore tras validarlos
     */
    private fun guardarDatosUsuario() {
        val uid = UserSession.id ?: return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        val nombre = binding.tvNombreAjustes.editText?.text.toString().trim()
        val apellidos = binding.tvApellidos.editText?.text.toString().trim()
        var fechaNacimiento = binding.etFechaNacimiento.editText?.text.toString().trim()

        if (nombre.isEmpty() || apellidos.isEmpty() || fechaNacimiento.isEmpty()) {
            Toast.makeText(requireContext(), "Todos los campos deben ser llenados", Toast.LENGTH_SHORT).show()
            return
        }

        // Validación del formato de fecha
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(fechaNacimiento)
            fechaNacimiento = sdf.format(date)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Fecha inválida. Debe ser en formato dd/MM/yyyy", Toast.LENGTH_SHORT).show()
            return
        }

        val datos = mapOf(
            "nombre" to nombre,
            "apellidos" to apellidos,
            "fechaNacimiento" to fechaNacimiento
        )

        // Guardar en Firestore
        db.collection("usuarios").document(uid)
            .set(datos)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error al guardar los datos: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

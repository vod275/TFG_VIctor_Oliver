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
import com.example.bushido.R
import com.example.bushido.databinding.FragmentPerfilBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import objetos.UserSession
import java.io.ByteArrayOutputStream

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val storageRef = FirebaseStorage.getInstance().reference

    private val PICK_IMAGE = 1001
    private val TAKE_PHOTO = 1002

    // Lanzador para pedir permiso de cámara
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                tomarFotoConCamara()
            } else {
                Toast.makeText(requireContext(),
                    getString(R.string.permiso_de_camara_denegado), Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)

        // Obtener UID actual del usuario y guardar en UserSession
        UserSession.id = FirebaseAuth.getInstance().currentUser?.uid

        cargarDatosGuardados()
        cargarFotoPerfil()

        binding.ibFotoPerfil.setOnClickListener { mostrarOpcionesFoto() }
        binding.btnGuardar.setOnClickListener { guardarDatosUsuario() }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Método llamado al cargar los datos del usuario.
     * Obtiene los datos del usuario de Firestore y los muestra en los campos correspondientes.
     * Si los datos no existen, muestra un mensaje de error.
     */
    private fun cargarDatosGuardados() {
        val uid = UserSession.id ?: return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    binding.tvNombreAjustes.editText?.setText(document.getString("nombre") ?: "")
                    binding.tvApellidos.editText?.setText(document.getString("apellidos") ?: "")
                    binding.etFechaNacimiento.editText?.setText(document.getString("fechaNacimiento") ?: "")
                    binding.tvTelefono.editText?.setText(document.getString("telefono") ?: "")
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    getString(R.string.error_al_cargar_los_datos), Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Método llamado al mostrar las opciones de la foto.
     * Muestra un diálogo con las opciones de seleccionar desde la galería o tomar una foto con la cámara.
     * Cuando se selecciona una opción, se llama al método correspondiente.
     */
    private fun mostrarOpcionesFoto() {
        val opciones = arrayOf(getString(R.string.galeria), getString(R.string.camara))
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.selecciona_una_opcion))
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> seleccionarDesdeGaleria()
                    1 -> verificarPermisoYCamara()
                }
            }.show()
    }

    /**
     * Método llamado al verificar el permiso y la camara.
     * Si el permiso ya está otorgado, se llama a tomarFotoConCamara().
     * Si no está otorgado, se solicita el permiso.
     */
    private fun verificarPermisoYCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            tomarFotoConCamara()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * Método llamado al seleciionar desde la galeria.
     * Abre la galería para seleccionar una imagen.
     * Cuando se selecciona una imagen, se sube a Firebase Storage.
     */
    private fun seleccionarDesdeGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE)
    }

    /**
     * Método llamado al tomar una foto con la camara.
     * Abre la cámara para tomar una foto.
     */
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

    /**
     * Método llamado bitmapToUri.
     *  Convierte un bitmap a un Uri.
     */
    private fun bitmapToUri(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(requireContext().contentResolver, bitmap, "temp", null)
        return Uri.parse(path)
    }


    /**
     * Método llamado al subir imagen a firebase.
     * Sube la imagen seleccionada a Firebase Storage.
     */
    private fun subirImagenAFirebase(uri: Uri) {
        val uid = UserSession.id ?: return
        val ref = storageRef.child("FotosUser/$uid.jpg")

        ref.putFile(uri)
            .addOnSuccessListener {
                Toast.makeText(requireContext(),
                    getString(R.string.imagen_subida_correctamente), Toast.LENGTH_SHORT).show()
                cargarFotoPerfil()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    getString(R.string.error_al_subir_la_imagen), Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Método llamado para cargar la foto de perfil.
     * Obtiene la foto de perfil de Firebase Storage y la muestra en el ImageView.
     */
    private fun cargarFotoPerfil() {
        val uid = UserSession.id ?: return
        val ref = storageRef.child("FotosUser/$uid.jpg")
        ref.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this).load(uri).into(binding.ibFotoPerfil)
        }
    }

    /**
     * Método llamado al guardar datos del usuario.
     * Guarda los datos del usuario en Firestore.
     */
    private fun guardarDatosUsuario() {
        val uid = UserSession.id ?: return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        val nombre = binding.tvNombreAjustes.editText?.text.toString()
        val apellidos = binding.tvApellidos.editText?.text.toString()
        val fechaNacimiento = binding.etFechaNacimiento.editText?.text.toString()
        val telefono = binding.tvTelefono.editText?.text.toString()
        val email = FirebaseAuth.getInstance().currentUser?.email ?: ""

        val usuarioMap = hashMapOf(
            "nombre" to nombre,
            "apellidos" to apellidos,
            "fechaNacimiento" to fechaNacimiento,
            "telefono" to telefono,
            "email" to email
        )

        db.collection("usuarios").document(uid)
            .set(usuarioMap, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(requireContext(),
                    getString(R.string.datos_guardados_correctamente), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    getString(R.string.error_al_guardar_los_datos), Toast.LENGTH_SHORT).show()
            }
    }
}

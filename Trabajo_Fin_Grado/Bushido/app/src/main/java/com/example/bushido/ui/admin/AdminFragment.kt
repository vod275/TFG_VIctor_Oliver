package com.example.bushido.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bushido.R
import com.example.bushido.databinding.FragmentAdminBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Ajuste de insets del sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Navegación con botones
        binding.btnPreciosBolos.setOnClickListener {
            findNavController().navigate(R.id.nav_admin_Bolos)
        }

        binding.btnPreciosPadelTenis.setOnClickListener {
            findNavController().navigate(R.id.nav_admin_TenisPadel)
        }

        binding.btnPreciosSocios.setOnClickListener {
            findNavController().navigate(R.id.nav_admin_Socios)
        }

        binding.btnVerUsuarios.setOnClickListener {
            findNavController().navigate(R.id.nav_admin_lista_usuarios)
        }

        binding.btnVerReservas.setOnClickListener {
            findNavController().navigate(R.id.nav_admin_lista_reservas)
        }




        // Añadir nuevo admin
        binding.btnAnadirAdmin.setOnClickListener {
            val email = binding.tvCorreoNuevoAdmin.editText?.text.toString().trim()
            val password = binding.tvContrasenaNuevoAdmin.editText?.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = task.result?.user?.uid
                            if (uid != null) {
                                val adminData = hashMapOf("rol" to "admin")
                                firestore.collection("usuarios").document(uid)
                                    .set(adminData)  //
                                    .addOnSuccessListener {
                                        Toast.makeText(requireContext(),
                                            getString(R.string.admin_a_adido_correctamente), Toast.LENGTH_SHORT).show()
                                        binding.tvCorreoNuevoAdmin.editText?.setText("")
                                        binding.tvContrasenaNuevoAdmin.editText?.setText("")
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(requireContext(),
                                            getString(
                                                R.string.error_al_guardar_en_firestore,
                                                e.message
                                            ), Toast.LENGTH_LONG).show()
                                    }
                            }
                        } else {
                            Toast.makeText(requireContext(),
                                getString(R.string.error_al_crear_usuario, task.exception?.message), Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(requireContext(),
                    getString(R.string.introduce_un_correo_y_una_contrase_a), Toast.LENGTH_SHORT).show()
            }
        }

        // Añadir nuevo usuario normal (no admin)
        binding.btnAnadirUsuario.setOnClickListener {
            val email = binding.tvCorreoNuevoAdmin.editText?.text.toString().trim()
            val password = binding.tvContrasenaNuevoAdmin.editText?.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = task.result?.user?.uid
                            if (uid != null) {
                                // No ponemos rol, solo creamos el documento vacío o con campos básicos si quieres
                                val userData = hashMapOf<String, Any>(
                                    "email" to email  // Opcional: puedes guardar otros datos básicos si quieres
                                )

                                firestore.collection("usuarios").document(uid)
                                    .set(userData)
                                    .addOnSuccessListener {
                                        Toast.makeText(requireContext(),
                                            getString(R.string.usuario_normal_a_adido_correctamente), Toast.LENGTH_SHORT).show()
                                        binding.tvCorreoNuevoAdmin.editText?.setText("")
                                        binding.tvContrasenaNuevoAdmin.editText?.setText("")
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(requireContext(),  getString(
                                            R.string.error_al_guardar_en_firestore,
                                            e.message
                                        ), Toast.LENGTH_LONG).show()
                                    }
                            }
                        } else {
                            Toast.makeText(requireContext(),  getString(R.string.error_al_crear_usuario, task.exception?.message), Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(requireContext(),  getString(R.string.introduce_un_correo_y_una_contrase_a), Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

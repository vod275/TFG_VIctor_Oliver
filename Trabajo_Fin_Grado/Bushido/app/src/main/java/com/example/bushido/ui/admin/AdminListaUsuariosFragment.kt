package com.example.bushido.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bushido.adaptadorListaUsuarios.UsuariosAdapter
import com.example.bushido.databinding.FragmentAdminListaUsuariosBinding
import com.example.bushido.models.Usuarios
import com.google.firebase.firestore.FirebaseFirestore

class AdminListaUsuariosFragment : Fragment() {

    private var _binding: FragmentAdminListaUsuariosBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: UsuariosAdapter
    private val usuariosList = mutableListOf<Usuarios>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminListaUsuariosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = UsuariosAdapter(requireContext(), mutableListOf())
        binding.rvListaUsuariosAdmin.layoutManager = LinearLayoutManager(requireContext())
        binding.rvListaUsuariosAdmin.adapter = adapter

        cargarUsuarios()

        binding.tvBuscadorUsuarioNombre.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filtrarUsuarios(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun cargarUsuarios() {
        FirebaseFirestore.getInstance().collection("usuarios")
            .get()
            .addOnSuccessListener { documentos ->
                usuariosList.clear()
                for (doc in documentos) {
                    val usuario = doc.toObject(Usuarios::class.java)
                    usuario.uid = doc.id
                    usuario.apellidos = doc.getString("apellidos") ?: ""
                    usuariosList.add(usuario)
                }
                adapter.actualizarLista(usuariosList)
            }
    }

    private fun filtrarUsuarios(texto: String) {
        val filtrados = if (texto.isBlank()) {
            usuariosList
        } else {
            usuariosList.filter {
                it.nombre.contains(texto, ignoreCase = true) ||
                        it.email.contains(texto, ignoreCase = true)
            }
        }
        adapter.actualizarLista(filtrados)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

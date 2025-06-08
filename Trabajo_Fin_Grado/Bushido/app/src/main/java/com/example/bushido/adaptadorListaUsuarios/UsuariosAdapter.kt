package com.example.bushido.adaptadorListaUsuarios

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bushido.R
import com.example.bushido.adaptadorListaReservas.ListaReservasAdapter
import com.example.bushido.databinding.DialogReservasUsuarioBinding
import com.example.bushido.databinding.ListaUsuariosItemBinding
import com.example.bushido.models.Reservas
import com.example.bushido.models.Usuarios
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class UsuariosAdapter(
    private val context: Context,
    private var listaUsuarios: MutableList<Usuarios>
) : RecyclerView.Adapter<UsuariosAdapter.UsuariosViewHolder>() {

    inner class UsuariosViewHolder(val binding: ListaUsuariosItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(usuario: Usuarios) {
            binding.nombre.text = usuario.nombre
            binding.apellidos.text = usuario.apellidos
            binding.email.text = usuario.email
            binding.fechaNacimiento.text = usuario.fechaNacimiento
            binding.telefono.text = usuario.telefono

            binding.root.setOnLongClickListener {
                mostrarOpciones(usuario)
                true
            }
        }

        private fun mostrarOpciones(usuario: Usuarios) {
            val opciones = arrayOf("Ver reservas futuras", "Eliminar usuario")
            AlertDialog.Builder(context).apply {
                setTitle("Opciones para ${usuario.nombre}")
                setItems(opciones) { _, which ->
                    when (which) {
                        0 -> mostrarReservasFuturas(usuario)
                        1 -> mostrarDialogoEliminar(usuario)
                    }
                }
                show()
            }
        }

        private fun mostrarReservasFuturas(usuario: Usuarios) {
            val ctx = context ?: return  // Si context es null, salimos para evitar crash

            val bindingDialog = DialogReservasUsuarioBinding.inflate(LayoutInflater.from(ctx))
            val adapterReservas = ListaReservasAdapter(mutableListOf(), ctx) {}

            bindingDialog.recyclerViewReservas.adapter = adapterReservas
            bindingDialog.recyclerViewReservas.layoutManager = LinearLayoutManager(ctx)

            FirebaseFirestore.getInstance().collection("reservas")
                .whereEqualTo("usuarioId", usuario.uid)
                .get()
                .addOnSuccessListener { docs ->
                    val lista = mutableListOf<Reservas>()
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val ahora = Date()

                    for (doc in docs) {
                        val reserva = doc.toObject(Reservas::class.java)
                        reserva.idReserva = doc.id

                        try {
                            val fechaCompleta = sdf.parse("${reserva.fecha.trim()} ${reserva.hora.trim()}")
                            if (fechaCompleta != null && fechaCompleta.after(ahora)) {
                                lista.add(reserva)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (lista.isEmpty()) {
                        Toast.makeText(ctx, "No hay reservas futuras", Toast.LENGTH_SHORT).show()
                    } else {
                        adapterReservas.actualizarLista(lista)

                        AlertDialog.Builder(ctx).apply {
                            setTitle("Reservas de ${usuario.nombre}")
                            setView(bindingDialog.root)
                            setPositiveButton("Cerrar", null)
                            show()
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(ctx, "Error al cargar reservas", Toast.LENGTH_SHORT).show()
                }
        }




        private fun mostrarDialogoEliminar(usuario: Usuarios) {
            AlertDialog.Builder(context).apply {
                setTitle("Eliminar usuario")
                setMessage("¿Estás seguro de que quieres eliminar al usuario ${usuario.nombre}?")
                setPositiveButton("Eliminar") { _, _ -> eliminarUsuarioFirestore(usuario) }
                setNegativeButton("Cancelar", null)
                show()
            }
        }

        private fun eliminarUsuarioFirestore(usuario: Usuarios) {
            FirebaseFirestore.getInstance().collection("usuarios")
                .whereEqualTo("email", usuario.email)
                .get()
                .addOnSuccessListener { documentos ->
                    for (doc in documentos) {
                        doc.reference.delete()
                    }
                    Toast.makeText(context, "Usuario eliminado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuariosViewHolder {
        val binding = ListaUsuariosItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UsuariosViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UsuariosViewHolder, position: Int) {
        holder.bind(listaUsuarios[position])
    }

    override fun getItemCount() = listaUsuarios.size

    fun actualizarLista(nuevaLista: List<Usuarios>) {
        listaUsuarios.clear()
        listaUsuarios.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}

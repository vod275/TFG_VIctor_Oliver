package com.example.bushido.adaptadorListaReservas

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bushido.R
import com.example.bushido.models.ReservaBolos
import com.google.firebase.firestore.FirebaseFirestore

class ListaReservasAdapter(
    private val reservas: MutableList<ReservaBolos>,
    private val context: android.content.Context,
    private val onReservaEliminada: (reserva: ReservaBolos) -> Unit
) : RecyclerView.Adapter<ListaReservasAdapter.ReservaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.lista_reserva_item, parent, false)
        return ReservaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservaViewHolder, position: Int) {
        val reserva = reservas[position]
        holder.bind(reserva)

        holder.itemView.setOnLongClickListener {
            AlertDialog.Builder(context)
                .setTitle("Eliminar reserva")
                .setMessage("¿Deseas eliminar esta reserva?")
                .setPositiveButton("Sí") { _, _ ->
                    eliminarReserva(reserva, position)
                }
                .setNegativeButton("No", null)
                .show()
            true
        }
    }

    override fun getItemCount(): Int = reservas.size

    private fun eliminarReserva(reserva: ReservaBolos, position: Int) {
        val db = FirebaseFirestore.getInstance()
        val docId = reserva.idReserva ?: return
        val pistaDoc = "pista${reserva.numeroPistaBolos}"
        val fecha = reserva.fecha ?: return
        val horaDoc = reserva.hora?.replace(":", "-") ?: return

        // Primero borrar la reserva
        db.collection("reservas").document(docId)
            .delete()
            .addOnSuccessListener {
                // Luego actualizar el bloqueo a false
                db.collection("Bolos")
                    .document("PistaBolos")
                    .collection("bloqueos")
                    .document(pistaDoc)
                    .collection(fecha)
                    .document(horaDoc)
                    .update("bloqueada", false)
                    .addOnSuccessListener {
                        println("Bloqueo actualizado a false: $pistaDoc / $fecha / $horaDoc")
                    }
                    .addOnFailureListener { e ->
                        println("Error actualizando el bloqueo:")
                        e.printStackTrace()
                    }

                // Actualizar UI
                reservas.removeAt(position)
                notifyItemRemoved(position)
                onReservaEliminada(reserva)
            }
            .addOnFailureListener { e ->
                println("Error eliminando la reserva:")
                e.printStackTrace()
            }
    }


    fun actualizarLista(nuevaLista: List<ReservaBolos>) {
        reservas.clear()
        reservas.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    inner class ReservaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nombre: TextView = view.findViewById(R.id.nombre)
        private val pista: TextView = view.findViewById(R.id.numeroPistaBolos)
        private val precio: TextView = view.findViewById(R.id.precio)
        private val fecha: TextView = view.findViewById(R.id.fecha)
        private val hora: TextView = view.findViewById(R.id.hora)
        private val icono: ImageView = view.findViewById(R.id.iconoTipo)

        fun bind(reserva: ReservaBolos) {
            nombre.text = reserva.nombre
            pista.text = "Pista: ${reserva.numeroPistaBolos}"
            fecha.text = "Fecha: ${reserva.fecha}"
            hora.text = "Hora: ${reserva.hora}"
            precio.text = "Precio: ${reserva.precio}" // si tienes este campo

            when (reserva.tipo.lowercase()) {
                "bolos" -> icono.setImageResource(R.drawable.bolos)
                "padel" -> icono.setImageResource(R.drawable.raqueta_padel)
                "tenis" -> icono.setImageResource(R.drawable.padel_tenis)
                else -> icono.setImageResource(R.drawable.padel_tenis)
            }
        }
    }
}

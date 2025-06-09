package com.example.bushido.adaptadorListaReservas

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bushido.R
import com.example.bushido.models.Reservas
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class ListaReservasAdapter(
    private val reservas: MutableList<Reservas>,
    private val context: android.content.Context,
    private val onReservaEliminada: (reserva: Reservas) -> Unit
) : RecyclerView.Adapter<ListaReservasAdapter.ReservaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.lista_reserva_item, parent, false)
        return ReservaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservaViewHolder, position: Int) {
        val reserva = reservas[position]
        holder.bind(reserva)
        holder.setLongClickListener {
            mostrarDialogoEliminar(reserva, position)
        }
    }

    override fun getItemCount(): Int = reservas.size

    private fun mostrarDialogoEliminar(reserva: Reservas, position: Int) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.eliminar_reserva))
            .setMessage(context.getString(R.string.deseas_eliminar_esta_reserva))
            .setPositiveButton(context.getString(R.string.si)) { _, _ ->
                eliminarReserva(reserva, position)
            }
            .setNegativeButton(context.getString(R.string.no), null)
            .show()
    }

    private fun eliminarReserva(reserva: Reservas, position: Int) {
        val db = FirebaseFirestore.getInstance()
        val docId = reserva.idReserva ?: return

        db.collection("reservas").document(docId)
            .delete()
            .addOnSuccessListener {
                // No hace falta actualizar bloqueo, ya está dentro de la reserva
                reservas.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, reservas.size)
                onReservaEliminada(reserva)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }


    fun actualizarLista(nuevaLista: List<Reservas>) {
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

        private var longClickListener: (() -> Unit)? = null

        init {
            view.setOnLongClickListener {
                longClickListener?.invoke()
                true
            }
        }

        fun bind(reserva: Reservas) {
            nombre.text = reserva.nombre ?: ""
            pista.text = context.getString(R.string.pista_numero, reserva.numeroPista.toString())
            fecha.text = context.getString(R.string.fecha_reserva, reserva.fecha ?: "")
            hora.text = context.getString(R.string.hora_reserva, reserva.hora ?: "")
            precio.text = context.getString(R.string.precio_reserva, reserva.precio ?: "")



            when (reserva.tipo?.lowercase(Locale.getDefault()) ?: "") {
                "bolos" -> icono.setImageResource(R.drawable.bolos)
                "padel" -> icono.setImageResource(R.drawable.raqueta_padel)
                "tenis" -> icono.setImageResource(R.drawable.padel_tenis)
                else -> icono.setImageResource(R.drawable.padel_tenis)
            }
        }

        fun setLongClickListener(listener: () -> Unit) {
            longClickListener = listener
        }
    }
}

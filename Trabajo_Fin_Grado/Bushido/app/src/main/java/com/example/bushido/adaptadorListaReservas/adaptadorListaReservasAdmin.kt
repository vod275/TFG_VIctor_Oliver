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

class AdaptadorListaReservasAdmin(
    private val reservas: MutableList<Reservas>,
    private val context: android.content.Context,
    private val onReservaEliminada: (reserva: Reservas) -> Unit
) : RecyclerView.Adapter<AdaptadorListaReservasAdmin.ReservaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.lista_reserva_item, parent, false)
        return ReservaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservaViewHolder, position: Int) {
        val reserva = reservas[position]
        holder.bind(reserva)
        holder.setLongClickListener {
            mostrarOpciones(reserva, position)
        }
    }

    override fun getItemCount(): Int = reservas.size

    private fun mostrarOpciones(reserva: Reservas, position: Int) {
        val opciones = arrayOf(
            context.getString(R.string.mostrar_usuario),
            context.getString(R.string.eliminar_reserva)
        )
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.opciones))
            .setItems(opciones) { dialog, which ->
                when (which) {
                    0 -> mostrarUsuario(reserva)
                    1 -> mostrarDialogoEliminar(reserva, position)
                }
            }
            .show()
    }

    private fun mostrarUsuario(reserva: Reservas) {
        val db = FirebaseFirestore.getInstance()
        val usuarioId = reserva.usuarioId ?: return


        db.collection("usuarios").document(usuarioId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val nombre = doc.getString("nombre") ?: ""
                    val apellidos = doc.getString("apellidos") ?: ""
                    val email = doc.getString("email") ?: ""
                    val fechaNacimiento = doc.getString("fechaNacimiento") ?: ""
                    val telefono = doc.getString("telefono") ?: ""

                    val view = LayoutInflater.from(context).inflate(R.layout.dialog_usuario_info, null)
                    val tvNombre = view.findViewById<TextView>(R.id.nombre)
                    val tvApellidos = view.findViewById<TextView>(R.id.apellidos)
                    val tvEmail = view.findViewById<TextView>(R.id.email)
                    val tvFechaNacimiento = view.findViewById<TextView>(R.id.fechaNacimiento)
                    val tvTelefono = view.findViewById<TextView>(R.id.telefono)

                    tvNombre.text = nombre
                    tvApellidos.text = apellidos
                    tvEmail.text = email
                    tvFechaNacimiento.text = fechaNacimiento
                    tvTelefono.text = telefono

                    AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.info_usuario))
                        .setView(view)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                } else {
                    AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.error))
                        .setMessage(context.getString(R.string.usuario_no_encontrado))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
            .addOnFailureListener {
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.error))
                    .setMessage(context.getString(R.string.error_cargando_usuario))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
    }


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

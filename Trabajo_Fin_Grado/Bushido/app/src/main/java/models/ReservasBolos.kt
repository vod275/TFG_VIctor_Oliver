package com.example.bushido.models

data class ReservaBolos(
    var idReserva: String = "",
    val numeroPistaBolos: Double = 0.0,
    val precio: Double = 0.0,
    val nombre: String = "",
    val fecha: String = "",
    val hora: String = "",
    val tipo: String = ""
)


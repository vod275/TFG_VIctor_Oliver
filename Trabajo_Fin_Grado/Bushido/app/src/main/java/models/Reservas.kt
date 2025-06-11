package com.example.bushido.models

data class Reservas(
    var idReserva: String = "",
    val numeroPista: Double = 0.0,
    val precio: Double = 0.0,
    val nombre: String = "",
    val fecha: String = "",
    val hora: String = "",
    val tipo: String = "",
    val usuarioId: String = ""
)


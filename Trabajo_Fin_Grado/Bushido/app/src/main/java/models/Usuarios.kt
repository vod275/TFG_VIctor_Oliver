package com.example.bushido.models

data class Usuarios(
    var nombre: String = "",
    var apellidos: String = "", // importante que sea `var`
    var email: String = "",
    var fechaNacimiento: String = "",
    var telefono: String = "",
    var uid: String = ""
)
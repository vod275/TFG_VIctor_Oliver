package objetos

/**
 * Objeto singleton que almacena temporalmente la información del usuario
 * autenticado durante la sesión activa de la aplicación.
 *
 * Este objeto permite acceder de forma global al email, nombre e ID del usuario
 * desde cualquier parte de la aplicación sin necesidad de pasarlos como parámetros.
 *
 * @property email Correo electrónico del usuario (puede ser null si no se ha iniciado sesión).
 * @property nombre Nombre del usuario (puede ser null si no se ha definido).
 * @property id Identificador único del usuario en Firebase (UID) (puede ser null si no está disponible).
 */
object UserSession {
    var email: String? = null
    var nombre: String? = null
    var id: String? = null
    var rol: String? = null
}

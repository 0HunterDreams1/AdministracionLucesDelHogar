package com.example.administracionlucesdelhogar.modelos

class Habitacion(id: Int, nombre: String?, estado: Boolean) {
    var id: Int = 0
    var nombre: String? = null
    var estado: Boolean = false

    /**
     * Constructor
     */
    init {
        this.id = id
        this.nombre = nombre
        this.estado = estado
    }
}

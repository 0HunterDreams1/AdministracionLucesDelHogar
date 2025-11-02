package com.example.administracionlucesdelhogar.modelos

class Habitacion(id: Int, nombre: String?, estado: Boolean, tipoHabitacion: Int) {
    var id: Int = 0
    var nombre: String? = null
    var estado: Boolean = false
    var tipoHabitacion: Int = 0




    /**
     * Constructor
     */
    init {
        this.id = id
        this.nombre = nombre
        this.estado = estado
        this.tipoHabitacion = tipoHabitacion
    }
}

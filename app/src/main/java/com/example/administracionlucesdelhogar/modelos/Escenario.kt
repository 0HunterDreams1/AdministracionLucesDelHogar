package com.example.administracionlucesdelhogar.modelos

class Escenario(id: Int, nombre: String, habitaciones: ArrayList<Habitacion>, estado: Boolean) {
    var id: Int = 0
    var nombre: String? = null
    var habitaciones: ArrayList<Habitacion> = ArrayList()
    var estado: Boolean = false

    /**
     * Constructor
     */
    init {
        this.id = id
        this.nombre = nombre
        this.habitaciones = habitaciones
        this.estado = estado
    }
}
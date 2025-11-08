package com.example.administracionlucesdelhogar.modelos

class Habitacion(id: Int, nombre: String?, estado: Boolean, tipoHabitacion: Int, codigoHabitacion: Int) {
    var id: Int = 0
    var nombre: String? = null
    var estado: Boolean = false
    var tipoHabitacion: Int = 0
    var codigoHabitacion: Int = 0

    /**
     * Constructor
     */
    init {
        this.id = id
        this.nombre = nombre
        this.estado = estado
        this.tipoHabitacion = tipoHabitacion
        this.codigoHabitacion = codigoHabitacion
    }

    fun getNombreCodigoHabitacion(): String {
        val codigosHabitacion = CodigoHabitacion.values()
        for (auxCodigoHabitacion in codigosHabitacion) {
            if (this.codigoHabitacion == auxCodigoHabitacion.codigo) {
                return auxCodigoHabitacion.nombre
            }
        }
        return ""
    }
}

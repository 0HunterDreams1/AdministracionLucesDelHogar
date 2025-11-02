package com.example.administracionlucesdelhogar.modelos

import com.example.administracionlucesdelhogar.R

sealed class TipoHabitacion(val nombre: String, val iconoResId: Int) {
    object Cocina : TipoHabitacion("Cocina", R.drawable.iconhabitacion)
    /*object Comedor : TipoHabitacion("Comedor", R.drawable.iconcomedor)
    object Baño : TipoHabitacion("Baño", R.drawable.iconbanio)
    object Living : TipoHabitacion("Living", R.drawable.ic_living)
    object Patio : TipoHabitacion("Patio", R.drawable.ic_patio)*/
    object Habitacion : TipoHabitacion("Habitación", R.drawable.iconhabitacion)
}

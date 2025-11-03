package com.example.administracionlucesdelhogar.modelos

import com.example.administracionlucesdelhogar.R

sealed class TipoHabitacion(val nombre: String, val iconoResId: Int) {
    object Cocina : TipoHabitacion("Cocina", R.drawable.outline_kitchen_24)
    object Comedor : TipoHabitacion("Comedor", R.drawable.outline_award_meal_24)
    object Baño : TipoHabitacion("Baño", R.drawable.outline_bathtub_24)
    object Living : TipoHabitacion("Living", R.drawable.outline_chair_24)
    object Patio : TipoHabitacion("Patio", R.drawable.outline_deceased_24)
    object Habitacion : TipoHabitacion("Habitación", R.drawable.outline_airline_seat_individual_suite_24)
}

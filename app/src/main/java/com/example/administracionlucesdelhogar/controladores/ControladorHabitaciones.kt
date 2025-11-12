package com.example.administracionlucesdelhogar.controladores

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.example.administracionlucesdelhogar.modelos.Habitacion
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import androidx.core.content.edit

class ControladorHabitaciones private constructor(context: Context) {
    val listaHabitaciones: ArrayList<Habitacion>
    private val context: Context = context.applicationContext

    init {
        this.listaHabitaciones = cargarDesdePrefs()
    }

    fun agregarHabitacion(h: Habitacion?) {
        listaHabitaciones.add(h!!)
        guardarEnPrefs()
    }

    fun eliminarHabitacion(h: Habitacion?) {
        listaHabitaciones.remove(h)
        guardarEnPrefs()
    }

    fun actualizarEstado(h: Habitacion, estado: Boolean) {
        h.estado = estado
        guardarEnPrefs()

        // Apagar todos los escenarios porque se alteró manualmente una habitación
        val controladorEscenarios = ControladorEscenarios.getInstance(context)
        for (e in controladorEscenarios.listaEscenarios) {
            e.estado = false
        }
        controladorEscenarios.guardarCambios()
    }

    fun guardarCambios() {
        guardarEnPrefs()
    }

    private fun guardarEnPrefs() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            val jsonArray = JSONArray()

            for (h in listaHabitaciones) {
                try {
                    val obj = JSONObject()
                    obj.put("id", h.id)
                    obj.put("nombre", h.nombre)
                    obj.put("estado", h.estado)
                    obj.put("tipoHabitacion", h.tipoHabitacion)
                    jsonArray.put(obj)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            putString("habitaciones", jsonArray.toString())
        }
    }

    private fun cargarDesdePrefs(): ArrayList<Habitacion> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString("habitaciones", null)
        val lista = ArrayList<Habitacion>()
        if (json != null) {
            try {
                val jsonArray = JSONArray(json)
                for (i in 0..<jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    lista.add(
                        Habitacion(
                            obj.getInt("id"),
                            obj.getString("nombre"),
                            obj.getBoolean("estado"),
                            obj.getInt("tipoHabitacion")
                        )
                    )
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return lista
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: ControladorHabitaciones? = null
        private const val PREFS_NAME = "HabitacionesPrefs"

        fun getInstance(context: Context): ControladorHabitaciones {
            if (instance == null) {
                instance = ControladorHabitaciones(context)
            }
            return instance!!
        }
    }
}

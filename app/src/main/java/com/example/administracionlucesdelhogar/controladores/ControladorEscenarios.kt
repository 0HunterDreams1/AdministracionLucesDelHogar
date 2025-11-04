package com.example.administracionlucesdelhogar.controladores

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
import com.example.administracionlucesdelhogar.modelos.Escenario
import com.example.administracionlucesdelhogar.modelos.Habitacion
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.collections.ArrayList

class ControladorEscenarios private constructor(context: Context) {
    private val context: Context = context.applicationContext
    val listaEscenarios: ArrayList<Escenario>

    init {
        this.listaEscenarios = cargarDesdePrefs()
    }

    fun agregarEscenario(e: Escenario) {
        listaEscenarios.add(e)
        guardarEnPrefs()
    }

    fun eliminarEscenario(e: Escenario) {
        listaEscenarios.remove(e)
        guardarEnPrefs()
    }

    fun guardarCambios() {
        guardarEnPrefs()
    }

    private fun guardarEnPrefs() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            val jsonArray = JSONArray()

            for (e in listaEscenarios) {
                try {
                    val obj = JSONObject()
                    obj.put("id", e.id)
                    obj.put("nombre", e.nombre)

                    // Guardar lista de habitaciones dentro del escenario
                    val jsonHabitaciones = JSONArray()
                    for (h in e.habitaciones) {
                        val objH = JSONObject()
                        objH.put("id", h.id)
                        objH.put("nombre", h.nombre)
                        objH.put("estado", h.estado)
                        objH.put("tipoHabitacion", h.tipoHabitacion)
                        jsonHabitaciones.put(objH)
                    }
                    obj.put("habitaciones", jsonHabitaciones)
                    obj.put("estado", e.estado)

                    jsonArray.put(obj)
                } catch (ex: JSONException) {
                    ex.printStackTrace()
                }
            }

            putString("escenarios", jsonArray.toString())
        }
    }

    private fun cargarDesdePrefs(): ArrayList<Escenario> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString("escenarios", null)
        val lista = ArrayList<Escenario>()

        if (json != null) {
            try {
                val jsonArray = JSONArray(json)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)

                    val habitacionesList = ArrayList<Habitacion>()
                    val habitacionesJson = obj.optJSONArray("habitaciones")
                    if (habitacionesJson != null) {
                        for (j in 0 until habitacionesJson.length()) {
                            val objH = habitacionesJson.getJSONObject(j)
                            habitacionesList.add(
                                Habitacion(
                                    objH.getInt("id"),
                                    objH.getString("nombre"),
                                    objH.getBoolean("estado"),
                                    objH.getInt("tipoHabitacion")
                                )
                            )
                        }
                    }

                    val escenario = Escenario(
                        obj.getInt("id"),
                        obj.getString("nombre"),
                        habitacionesList,
                        obj.getBoolean("estado")
                    )
                    lista.add(escenario)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        return lista
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: ControladorEscenarios? = null
        private const val PREFS_NAME = "EscenariosPrefs"

        fun getInstance(context: Context): ControladorEscenarios {
            if (instance == null) {
                instance = ControladorEscenarios(context)
            }
            return instance!!
        }
    }
}
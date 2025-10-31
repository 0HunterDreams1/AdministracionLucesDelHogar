package com.example.administracionlucesdelhogar.controladores;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.administracionlucesdelhogar.modelos.Habitacion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ControladorHabitaciones {
    private static ControladorHabitaciones instance;
    private ArrayList<Habitacion> listaHabitaciones;
    private Context context;
    private static final String PREFS_NAME = "HabitacionesPrefs";

    private ControladorHabitaciones(Context context) {
        this.context = context.getApplicationContext();
        this.listaHabitaciones = cargarDesdePrefs();
    }

    public static ControladorHabitaciones getInstance(Context context) {
        if (instance == null) {
            instance = new ControladorHabitaciones(context);
        }
        return instance;
    }

    public ArrayList<Habitacion> getListaHabitaciones() {
        return listaHabitaciones;
    }

    public void agregarHabitacion(Habitacion h) {
        listaHabitaciones.add(h);
        guardarEnPrefs();
    }

    public void eliminarHabitacion(Habitacion h) {
        listaHabitaciones.remove(h);
        guardarEnPrefs();
    }

    public void actualizarEstado(Habitacion h, boolean estado) {
        h.setEstado(estado);
        guardarEnPrefs();
    }

    public void guardarCambios() {
        guardarEnPrefs();
    }

    private void guardarEnPrefs() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray jsonArray = new JSONArray();

        for (Habitacion h : listaHabitaciones) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", h.getId());
                obj.put("nombre", h.getNombre());
                obj.put("estado", h.getEstado());
                jsonArray.put(obj);
            } catch (JSONException e) { e.printStackTrace(); }
        }

        editor.putString("habitaciones", jsonArray.toString());
        editor.apply();
    }

    private ArrayList<Habitacion> cargarDesdePrefs() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString("habitaciones", null);
        ArrayList<Habitacion> lista = new ArrayList<>();
        if (json != null) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    lista.add(new Habitacion(obj.getInt("id"), obj.getString("nombre"), obj.getBoolean("estado")));
                }
            } catch (JSONException e) { e.printStackTrace(); }
        }
        return lista;
    }
}

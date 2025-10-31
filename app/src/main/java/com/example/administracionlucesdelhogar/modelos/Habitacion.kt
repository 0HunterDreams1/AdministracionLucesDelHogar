package com.example.administracionlucesdelhogar.modelos;

public class Habitacion {
    private int id;
    private String nombre;
    private boolean estado;

    /**
     * Constructor
     */
    public Habitacion (int id, String nombre, boolean estado) {
        this.setId(id);
        this.setNombre(nombre);
        this.setEstado(estado);
    }

    /**
     * Getters y setters
     */
    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

    public boolean getEstado() {
        return estado;
    }
}

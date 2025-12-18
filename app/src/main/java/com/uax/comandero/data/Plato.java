package com.uax.comandero.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "platos")
public class Plato {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String nombre;
    public double precio;
    public String categoria; // Ej: "Bebida", "Comida"

    public Plato(String nombre, double precio, String categoria) {
        this.nombre = nombre;
        this.precio = precio;
        this.categoria = categoria;
    }
}
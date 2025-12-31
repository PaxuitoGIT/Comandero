package com.uax.comandero.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "platos")
public class Plato {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String codigo;
    public String nombre;
    public double precio;
    public boolean esCocina;
    public String categoria;

    public Plato(String codigo, String nombre, double precio, boolean esCocina, String categoria) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
        this.esCocina = esCocina;
        this.categoria = categoria;
    }
}
package com.uax.comandero.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// Añadimos índice único para que no se repitan los códigos
@Entity(tableName = "platos", indices = {@Index(value = {"codigo"}, unique = true)})
public class Plato {
    @PrimaryKey(autoGenerate = true)
    public int idInterno; // ID interno de Room

    public String codigo;
    public String nombre;
    public double precio;
    public String categoria;

    public Plato(String codigo, String nombre, double precio, String categoria) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
        this.categoria = categoria;
    }
}
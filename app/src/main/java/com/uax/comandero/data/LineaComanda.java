package com.uax.comandero.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "linea_comanda")
public class LineaComanda {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String nombrePlato;
    public double precio;
    public boolean esPagado; // false = En carrito (Mesa activa), true = Historial
    public long fecha; // Timestamp

    public LineaComanda(String nombrePlato, double precio, boolean esPagado, long fecha) {
        this.nombrePlato = nombrePlato;
        this.precio = precio;
        this.esPagado = esPagado;
        this.fecha = fecha;
    }
}
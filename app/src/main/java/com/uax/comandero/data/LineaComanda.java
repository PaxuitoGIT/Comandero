package com.uax.comandero.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "linea_comanda")
public class LineaComanda {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int numeroMesa; // Nueva funcionalidad
    public String nombrePlato;
    public double precio;
    public String notas; // Nueva funcionalidad (ej: "Sin cebolla")
    public boolean esPagado;
    public long fecha;

    public LineaComanda(int numeroMesa, String nombrePlato, double precio, String notas, boolean esPagado, long fecha) {
        this.numeroMesa = numeroMesa;
        this.nombrePlato = nombrePlato;
        this.precio = precio;
        this.notas = notas;
        this.esPagado = esPagado;
        this.fecha = fecha;
    }
}
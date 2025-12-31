package com.uax.comandero.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "linea_comanda")
public class LineaComanda {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int numeroMesa;
    public String nombrePlato;
    public double precio;
    public String notas;
    public boolean esPagado;
    public long fecha;

    public boolean esCocina;       // true = Comida, false = Bebida
    public boolean enviadoCocina;  // true = Ya se imprimió ticket

    public LineaComanda(int numeroMesa, String nombrePlato, double precio, String notas, boolean esPagado, long fecha, boolean esCocina, boolean enviadoCocina) {
        this.numeroMesa = numeroMesa;
        this.nombrePlato = nombrePlato;
        this.precio = precio;
        this.notas = notas;
        this.esPagado = esPagado;
        this.fecha = fecha;
        this.esCocina = esCocina;
        this.enviadoCocina = enviadoCocina;
    }
}
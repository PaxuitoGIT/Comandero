package com.uax.comandero.data;

import java.util.ArrayList;
import java.util.List;

public class ItemAgrupado {
    public String nombre;
    public double precioUnitario;
    public String notas;
    public int cantidad;
    public double precioTotal;

    // Guardamos los IDs reales por si queremos borrar uno del stack
    public List<Integer> idsOriginales = new ArrayList<>();

    public ItemAgrupado(LineaComanda linea) {
        this.nombre = linea.nombrePlato;
        this.precioUnitario = linea.precio;
        this.notas = linea.notas;
        this.cantidad = 1;
        this.precioTotal = linea.precio;
        this.idsOriginales.add(linea.id);
    }

    public void agregarUno(LineaComanda linea) {
        this.cantidad++;
        this.precioTotal += linea.precio;
        this.idsOriginales.add(linea.id);
    }
}
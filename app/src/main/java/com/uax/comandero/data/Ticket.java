package com.uax.comandero.data;

public class Ticket {
    public int numeroMesa;
    public long fecha;
    public double total;

    // Room usará este constructor automáticamente
    public Ticket(int numeroMesa, long fecha, double total) {
        this.numeroMesa = numeroMesa;
        this.fecha = fecha;
        this.total = total;
    }
}
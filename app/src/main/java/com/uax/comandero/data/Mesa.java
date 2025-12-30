package com.uax.comandero.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "mesas")
public class Mesa {
    @PrimaryKey(autoGenerate = false) // No autogenerar, usamos el número real de la mesa
    public int numero;
    public boolean abierta; // true = abierta, false = cerrada/cobrada
    public long horaApertura;

    public Mesa(int numero, boolean abierta, long horaApertura) {
        this.numero = numero;
        this.abierta = abierta;
        this.horaApertura = horaApertura;
    }
}
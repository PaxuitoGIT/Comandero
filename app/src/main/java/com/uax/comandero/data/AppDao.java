package com.uax.comandero.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface AppDao {
    // ---- PLATOS ----
    @Insert
    void insertarPlato(Plato plato);

    @Query("SELECT * FROM platos")
    LiveData<List<Plato>> getMenu();

    // ---- COMANDA (CARRITO) ----
    @Insert
    void insertarLinea(LineaComanda linea);

    // Obtener items activos (Mesa actual)
    @Query("SELECT * FROM linea_comanda WHERE esPagado = 0")
    LiveData<List<LineaComanda>> getComandaActual();

    // Obtener historial
    @Query("SELECT * FROM linea_comanda WHERE esPagado = 1 ORDER BY fecha DESC")
    LiveData<List<LineaComanda>> getHistorial();

    // Cerrar mesa (Simular pago/envío)
    @Query("UPDATE linea_comanda SET esPagado = 1 WHERE esPagado = 0")
    void cerrarMesa();
}

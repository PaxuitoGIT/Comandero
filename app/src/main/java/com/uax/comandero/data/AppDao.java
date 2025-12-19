package com.uax.comandero.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface AppDao {
    // --- GESTIÓN DE MENÚ ---
    @Insert
    void insertarPlato(Plato plato);

    @Update
    void actualizarPlato(Plato plato); // Para editar

    @Query("SELECT * FROM platos ORDER BY nombre ASC")
    LiveData<List<Plato>> getMenu();

    // Buscar por ID (código) o Nombre
    @Query("SELECT * FROM platos WHERE codigo LIKE '%' || :busqueda || '%' OR nombre LIKE '%' || :busqueda || '%'")
    List<Plato> buscarPlatos(String busqueda);

    // Comprobar si existe ID (para validación)
    @Query("SELECT COUNT(*) FROM platos WHERE codigo = :codigo")
    int contarPlatosPorCodigo(String codigo);

    // --- GESTIÓN DE MESAS ---
    @Insert
    void insertarLinea(LineaComanda linea);

    // Obtener comanda de una mesa específica
    @Query("SELECT * FROM linea_comanda WHERE numeroMesa = :mesa AND esPagado = 0")
    LiveData<List<LineaComanda>> getComandaMesa(int mesa);

    // Obtener qué mesas están abiertas (tienen items sin pagar)
    @Query("SELECT DISTINCT numeroMesa FROM linea_comanda WHERE esPagado = 0 ORDER BY numeroMesa ASC")
    LiveData<List<Integer>> getMesasAbiertas();

    // Cerrar una mesa específica
    @Query("UPDATE linea_comanda SET esPagado = 1 WHERE numeroMesa = :mesa AND esPagado = 0")
    void cerrarMesa(int mesa);

    // Historial completo
    @Query("SELECT * FROM linea_comanda WHERE esPagado = 1 ORDER BY fecha DESC")
    LiveData<List<LineaComanda>> getHistorial();
}
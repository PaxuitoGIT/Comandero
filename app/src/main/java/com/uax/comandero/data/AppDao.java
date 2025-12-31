package com.uax.comandero.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface AppDao {

    // ==========================================
    // 1. GESTIÓN DE MENÚ (PLATOS)
    // ==========================================

    @Insert
    void insertarPlato(Plato plato);

    @Update
    void actualizarPlato(Plato plato);

    // NUEVO: Permite eliminar un plato de la carta permanentemente
    @Delete
    void borrarPlato(Plato plato);

    @Query("SELECT * FROM platos ORDER BY nombre ASC")
    LiveData<List<Plato>> getMenu();

    // Buscador por nombre o código
    @Query("SELECT * FROM platos WHERE codigo LIKE '%' || :busqueda || '%' OR nombre LIKE '%' || :busqueda || '%'")
    List<Plato> buscarPlatos(String busqueda);

    // Validación para no repetir códigos al crear
    @Query("SELECT COUNT(*) FROM platos WHERE codigo = :codigo")
    int contarPlatosPorCodigo(String codigo);


    // ==========================================
    // 2. GESTIÓN DE MESAS
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void abrirMesa(Mesa mesa);

    @Query("SELECT * FROM mesas WHERE abierta = 1 ORDER BY numero ASC")
    LiveData<List<Mesa>> getMesasActivas();

    @Query("UPDATE mesas SET abierta = 0 WHERE numero = :numero")
    void cerrarEstadoMesa(int numero);


    // ==========================================
    // 3. GESTIÓN DE COMANDAS (PEDIDOS ACTIVOS)
    // ==========================================

    @Insert
    void insertarLinea(LineaComanda linea);

    // Obtener items NO pagados de una mesa
    @Query("SELECT * FROM linea_comanda WHERE numeroMesa = :mesa AND esPagado = 0")
    LiveData<List<LineaComanda>> getComandaMesa(int mesa);

    @Delete
    void borrarLinea(LineaComanda linea);

    @Update
    void actualizarLinea(LineaComanda linea);

    // NUEVO: Para editar precio o notas de varios productos iguales a la vez (Stacking)
    @Query("UPDATE linea_comanda SET precio = :precio, notas = :notas WHERE id IN (:ids)")
    void actualizarLineasEnMasa(List<Integer> ids, double precio, String notas);


    // ==========================================
    // 4. COBRO E HISTORIAL
    // ==========================================

    // Al cobrar, marcamos como pagado y guardamos la fecha/hora exacta
    @Query("UPDATE linea_comanda SET esPagado = 1, fecha = :ahora WHERE numeroMesa = :mesa AND esPagado = 0")
    void cobrarLineasMesa(int mesa, long ahora);

    // Historial Agrupado (Vista de Tickets resumen)
    @Query("SELECT numeroMesa, fecha, SUM(precio) as total " +
            "FROM linea_comanda WHERE esPagado = 1 " +
            "GROUP BY numeroMesa, fecha " +
            "ORDER BY fecha DESC")
    LiveData<List<Ticket>> getHistorialAgrupado();

    // Historial Filtrado por rango de Fechas
    @Query("SELECT numeroMesa, fecha, SUM(precio) as total " +
            "FROM linea_comanda " +
            "WHERE esPagado = 1 AND fecha >= :desde AND fecha <= :hasta " +
            "GROUP BY numeroMesa, fecha " +
            "ORDER BY fecha DESC")
    LiveData<List<Ticket>> getHistorialPorFecha(long desde, long hasta);

    // Ver detalle de un ticket antiguo (Recibo Pasado)
    @Query("SELECT * FROM linea_comanda WHERE numeroMesa = :mesa AND fecha = :fecha AND esPagado = 1")
    LiveData<List<LineaComanda>> getReciboPasado(int mesa, long fecha);


    // ==========================================
    // 5. GESTIÓN DE COCINA
    // ==========================================

    // Obtener items que SON de cocina (comida) y NO se han enviado aún
    @Query("SELECT * FROM linea_comanda WHERE numeroMesa = :mesa AND esCocina = 1 AND enviadoCocina = 0")
    List<LineaComanda> getPendientesCocina(int mesa);

    // Marcar items como enviados tras imprimir (usando lista de IDs)
    @Query("UPDATE linea_comanda SET enviadoCocina = 1 WHERE id IN (:ids)")
    void marcarComoEnviados(List<Integer> ids);
}
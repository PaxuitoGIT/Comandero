package com.uax.comandero.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
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

    @Query("SELECT * FROM platos ORDER BY nombre ASC")
    LiveData<List<Plato>> getMenu();

    // Buscador
    @Query("SELECT * FROM platos WHERE codigo LIKE '%' || :busqueda || '%' OR nombre LIKE '%' || :busqueda || '%'")
    List<Plato> buscarPlatos(String busqueda);

    // Validación
    @Query("SELECT COUNT(*) FROM platos WHERE codigo = :codigo")
    int contarPlatosPorCodigo(String codigo);


    // ==========================================
    // 2. GESTIÓN DE MESAS
    // ==========================================

    // Usamos REPLACE para que si reabrimos la mesa 5, sobrescriba la vieja cerrada
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


    // ==========================================
    // 4. COBRO E HISTORIAL
    // ==========================================

    // MÉTODO ÚNICO DE COBRO:
    // Marca como pagado Y actualiza la fecha al momento exacto del cobro (:ahora)
    @Query("UPDATE linea_comanda SET esPagado = 1, fecha = :ahora WHERE numeroMesa = :mesa AND esPagado = 0")
    void cobrarLineasMesa(int mesa, long ahora);

    // Historial Agrupado (Vista de Tickets)
    @Query("SELECT numeroMesa, fecha, SUM(precio) as total " +
            "FROM linea_comanda WHERE esPagado = 1 " +
            "GROUP BY numeroMesa, fecha " +
            "ORDER BY fecha DESC")
    LiveData<List<Ticket>> getHistorialAgrupado();

    // Historial Filtrado por Fechas
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

    // Marcar items como enviados tras imprimir
    @Query("UPDATE linea_comanda SET enviadoCocina = 1 WHERE id IN (:ids)")
    void marcarComoEnviados(List<Integer> ids);
}
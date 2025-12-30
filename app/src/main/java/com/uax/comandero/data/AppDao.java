package com.uax.comandero.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
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
    void actualizarPlato(Plato plato); // Para editar nombre/precio

    // Obtener todo el menú
    @Query("SELECT * FROM platos ORDER BY nombre ASC")
    LiveData<List<Plato>> getMenu();

    // Buscar por ID (código) o Nombre (para el buscador en la mesa)
    @Query("SELECT * FROM platos WHERE codigo LIKE '%' || :busqueda || '%' OR nombre LIKE '%' || :busqueda || '%'")
    List<Plato> buscarPlatos(String busqueda);

    // Comprobar si existe ID (para validación al crear plato)
    @Query("SELECT COUNT(*) FROM platos WHERE codigo = :codigo")
    int contarPlatosPorCodigo(String codigo);


    // ==========================================
    // 2. GESTIÓN DE MESAS
    // ==========================================

    // Abre una mesa (la añade a la lista principal)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void abrirMesa(Mesa mesa);

    // Obtiene la lista de mesas que están marcadas como "abiertas" (true)
    // Esto sustituye a la antigua query que buscaba en linea_comanda
    @Query("SELECT * FROM mesas WHERE abierta = 1 ORDER BY numero ASC")
    LiveData<List<Mesa>> getMesasActivas();

    // Cierra la mesa (la quita de la lista principal)
    @Query("UPDATE mesas SET abierta = 0 WHERE numero = :numero")
    void cerrarEstadoMesa(int numero);


    // ==========================================
    // 3. GESTIÓN DE COMANDAS (ITEMS DEL PEDIDO)
    // ==========================================

    @Insert
    void insertarLinea(LineaComanda linea);

    // Obtener los productos pedidos en una mesa específica que aún no se han cobrado
    @Query("SELECT * FROM linea_comanda WHERE numeroMesa = :mesa AND esPagado = 0")
    LiveData<List<LineaComanda>> getComandaMesa(int mesa);

    // Marcar los productos de una mesa como "Pagados" (Mover al historial)
    @Query("UPDATE linea_comanda SET esPagado = 1 WHERE numeroMesa = :mesa AND esPagado = 0")
    void cobrarLineasMesa(int mesa);

    // Historial completo de ventas
    @Query("SELECT * FROM linea_comanda WHERE esPagado = 1 ORDER BY fecha DESC")
    LiveData<List<LineaComanda>> getHistorial();

    // Para eliminar un plato (si se equivocaron)
    @androidx.room.Delete
    void borrarLinea(LineaComanda linea);

    // Para editar notas o precio
    @Update
    void actualizarLinea(LineaComanda linea);

    // ==========================================
    // 4. GESTIÓN DEL HISTORIAL
    // ==========================================

    // --- CAMBIO EN EL COBRO ---
    // Ahora, al cobrar, actualizamos TAMBIÉN la fecha (:ahora) para que todos los items
    // tengan el mismo timestamp y formen un "pack".
    @Query("UPDATE linea_comanda SET esPagado = 1, fecha = :ahora WHERE numeroMesa = :mesa AND esPagado = 0")
    void cobrarLineasMesa(int mesa, long ahora);

    // --- NUEVO HISTORIAL AGRUPADO ---
    // Devuelve una lista de "Tickets" (Resúmenes) en vez de platos sueltos
    @Query("SELECT numeroMesa, fecha, SUM(precio) as total " +
            "FROM linea_comanda WHERE esPagado = 1 " +
            "GROUP BY numeroMesa, fecha " +
            "ORDER BY fecha DESC")
    LiveData<List<Ticket>> getHistorialAgrupado();

    // --- FILTRO POR FECHA ---
    @Query("SELECT numeroMesa, fecha, SUM(precio) as total " +
            "FROM linea_comanda " +
            "WHERE esPagado = 1 AND fecha >= :desde AND fecha <= :hasta " + // Filtro de rango
            "GROUP BY numeroMesa, fecha " +
            "ORDER BY fecha DESC")
    LiveData<List<Ticket>> getHistorialPorFecha(long desde, long hasta);

    // --- RECUPERAR UN RECIBO ANTIGUO ---
    // Busca items pagados de una mesa en una fecha EXACTA
    @Query("SELECT * FROM linea_comanda WHERE numeroMesa = :mesa AND fecha = :fecha AND esPagado = 1")
    LiveData<List<LineaComanda>> getReciboPasado(int mesa, long fecha);

}
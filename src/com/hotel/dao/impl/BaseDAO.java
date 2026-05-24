
package com.hotel.dao.impl;

/**
 *
 * @author rober
 */
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.util.ConexionBaseDatos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Clase base para todos los DAOs.
 *
 * CORRECCIÓN BUG #2: Centraliza el patrón de obtener y liberar conexiones
 * del pool, garantizando que cada operación use su propia conexión y la
 * devuelva siempre al pool, incluso ante excepciones.
 *
 * Todos los DAOs deben extender esta clase y usar {@link #enTransaccion}
 * para operaciones que requieran atomicidad, o el patrón obtener/liberar
 * para operaciones de solo lectura.
 *
 * GRASP: Fabricación Pura – infraestructura que no existe en el dominio.
 * SOLID: D – los DAOs dependen de esta abstracción para el pool.
 */
public abstract class BaseDAO {

    protected final ConexionBaseDatos conexionBD;

    protected BaseDAO() {
        this.conexionBD = ConexionBaseDatos.obtenerInstancia();
    }
}
 /**
     * Obtiene una conexión libre del pool.
     * SIEMPRE debe llamarse liberar(conn) en un finally.
     */
    protected Connection obtener() {
        return conexionBD.obtenerConexion();
    }

    /**
     * Devuelve la conexión al pool.
     * Llamar siempre en el bloque finally de cada método DAO.
     */
    protected void liberar(Connection conn) {
        conexionBD.liberarConexion(conn);
    }
// ── Soporte de transacciones ──────────────────────────────────────────────

    /**
     * Interfaz funcional para operaciones que se ejecutan dentro de
     * una transacción gestionada por {@link #enTransaccion}.
     *
     * @param <T> tipo del valor retornado por la operación
     */
    @FunctionalInterface
    protected interface TransaccionCallback<T> {
        /**
         * Ejecuta la lógica de negocio usando la conexión proporcionada.
         *
         * @param conn conexión activa con autocommit desactivado
         * @return resultado de la operación (puede ser null)
         * @throws Exception cualquier error que deba provocar rollback
         */
        T ejecutar(Connection conn) throws Exception;
    }
/**
     * Ejecuta {@code callback} dentro de una transacción ACID:
     * <ol>
     *   <li>Obtiene una conexión del pool.</li>
     *   <li>Desactiva autocommit.</li>
     *   <li>Invoca {@code callback.ejecutar(conn)}.</li>
     *   <li>Si tiene éxito → commit.</li>
     *   <li>Si lanza excepción → rollback y relanza como {@link ExcepcionBaseDatos}.</li>
     *   <li>En finally → restaura autocommit y libera la conexión al pool.</li>
     * </ol>
     *
     * @param callback operación a ejecutar dentro de la transacción
     * @param <T>      tipo del valor retornado
     * @return resultado devuelto por {@code callback.ejecutar()}
     * @throws ExcepcionBaseDatos si ocurre cualquier error durante la operación
     */
    protected <T> T enTransaccion(TransaccionCallback<T> callback) {
        Connection conn = obtener();
        try {
            conn.setAutoCommit(false);
            T resultado = callback.ejecutar(conn);
            conn.commit();
            return resultado;
        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new ExcepcionBaseDatos("Error en transacción: " + e.getMessage(), e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            liberar(conn);
        }
    }
/**
     * Obtiene el siguiente valor de una secuencia Oracle.
     * Usar dentro de un bloque que ya tiene una conexión abierta.
     */
    protected int siguienteSeq(Connection conn, String secuencia) throws SQLException {
        try (PreparedStatement s = conn.prepareStatement(
                "SELECT " + secuencia + ".NEXTVAL FROM DUAL");
             java.sql.ResultSet rs = s.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Formatea un ID numérico como VARCHAR2 de la BD.
     * Ej: fmt("CLI", 6) → "CLI06"
     */
    protected static String fmt(String prefijo, int n) {
        return String.format("%s%02d", prefijo, n);
    }
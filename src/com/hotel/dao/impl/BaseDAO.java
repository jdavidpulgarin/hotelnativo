
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
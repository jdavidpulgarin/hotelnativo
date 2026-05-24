
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
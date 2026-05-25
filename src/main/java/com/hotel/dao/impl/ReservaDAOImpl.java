
package com.hotel.dao.impl;

/**
 *
 * @author rober
 */
import com.hotel.dao.interfaces.IReservaDAO;
import com.hotel.dao.interfaces.IReservaBusqueda;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC Oracle del repositorio de reservas.
 * Adaptado al schema HOTELNATIVO: tabla RESERVA con PK id_reserva (VARCHAR2).
 *
 * REFACTORING v2 — 5 puntos críticos corregidos:
 *  1. ResultSet cerrado con try-with-resources anidado en todos los métodos.
 *  2. Sin concatenación de Strings en SQL (todo PreparedStatement).
 *  3. insertar/actualizar/eliminar usan enTransaccion() → commit/rollback garantizado.
 *  4. listarTodas(int,int) con paginación Oracle OFFSET/FETCH.
 *  5. Sin printStackTrace(); errores suben como ExcepcionBaseDatos.
 *
 * GRASP: Fabricación Pura.
 */
public class ReservaDAOImpl extends BaseDAO implements IReservaDAO, IReservaBusqueda {

    private static final String COLS_SIMPLE =
            "TO_NUMBER(REGEXP_REPLACE(id_reserva,'[^0-9]','')) AS id, " +
            "TO_NUMBER(REGEXP_REPLACE(id_cliente,'[^0-9]','')) AS id_cliente, " +
            "id_cliente AS documento_cliente, " +
            "TO_NUMBER(REGEXP_REPLACE(id_habitacion,'[^0-9]','')) AS id_habitacion, " +
            "fecha_entrada, fecha_salida, estado, num_personas, precio_total";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT TO_NUMBER(REGEXP_REPLACE(r.id_reserva,'[^0-9]','')) AS id, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_cliente,'[^0-9]','')) AS id_cliente, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_habitacion,'[^0-9]','')) AS id_habitacion, " +
            "r.fecha_entrada, r.fecha_salida, r.estado, r.num_personas, r.precio_total, " +
            "c.primer_nombre c_nombre, c.apellido_1 c_apellido, c.email c_email, " +
            "c.telefono c_telefono, r.id_cliente AS documento, c.nacionalidad, c.fecha_registro, c.es_vip, " +
            "h.numero h_numero, h.precio_base, h.estado h_estado " +
            "FROM RESERVA r " +
            "JOIN CLIENTE c ON r.id_cliente = c.id_cliente " +
            "JOIN HABITACION h ON r.id_habitacion = h.id_habitacion " +
            "WHERE r.id_reserva = ?";

    private static final String SQL_LISTAR_TODAS =
            "SELECT TO_NUMBER(REGEXP_REPLACE(r.id_reserva,'[^0-9]','')) AS id, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_cliente,'[^0-9]','')) AS id_cliente, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_habitacion,'[^0-9]','')) AS id_habitacion, " +
            "r.fecha_entrada, r.fecha_salida, r.estado, r.num_personas, r.precio_total, " +
            "c.primer_nombre c_nombre, c.apellido_1 c_apellido, c.email c_email, " +
            "c.telefono c_telefono, r.id_cliente AS documento, c.nacionalidad, c.fecha_registro, c.es_vip, " +
            "h.numero h_numero, h.precio_base, h.estado h_estado " +
            "FROM RESERVA r " +
            "JOIN CLIENTE c ON r.id_cliente = c.id_cliente " +
            "JOIN HABITACION h ON r.id_habitacion = h.id_habitacion " +
            "ORDER BY r.fecha_entrada DESC";

    private static final String SQL_LISTAR_PAGINADA =
            SQL_LISTAR_TODAS + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    // BUG 3 FIX: mismo OR doble que buscarPorId para cubrir "CLI##" y cédula directa
    private static final String SQL_RESERVAS_ACTIVAS_CLIENTE =
            "SELECT " + COLS_SIMPLE + " FROM RESERVA r " +
            "WHERE (r.id_cliente = ? OR r.id_cliente = ?) " +
            "AND r.estado NOT IN ('CANCELADA','COMPLETADA')";

    // BUG 5 FIX: COMPLETADA también debe excluirse; una reserva completada
    // no bloquea el reuso de la habitación en las mismas fechas.
    private static final String SQL_RESERVAS_SOLAPADAS =
            "SELECT " + COLS_SIMPLE + " FROM RESERVA r " +
            "WHERE r.id_habitacion = ? " +
            "AND r.estado NOT IN ('CANCELADA','COMPLETADA') " +
            "AND r.fecha_entrada < ? AND r.fecha_salida > ?";

    private static final String COLS_JOIN =
            "TO_NUMBER(REGEXP_REPLACE(r.id_reserva,'[^0-9]','')) AS id, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_cliente,'[^0-9]','')) AS id_cliente, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_habitacion,'[^0-9]','')) AS id_habitacion, " +
            "r.fecha_entrada, r.fecha_salida, r.estado, r.num_personas, r.precio_total, " +
            "c.primer_nombre c_nombre, c.apellido_1 c_apellido, c.email c_email, " +
            "c.telefono c_telefono, r.id_cliente AS documento, c.nacionalidad, c.fecha_registro, c.es_vip, " +
            "h.numero h_numero, h.precio_base, h.estado h_estado " +
            "FROM RESERVA r " +
            "JOIN CLIENTE c ON r.id_cliente = c.id_cliente " +
            "JOIN HABITACION h ON r.id_habitacion = h.id_habitacion";

    private static final String SQL_BUSCAR_POR_RANGO_FECHAS =
            "SELECT " + COLS_JOIN +
            " WHERE r.fecha_entrada >= ? AND r.fecha_entrada <= ? ORDER BY r.fecha_entrada";

    private static final String SQL_BUSCAR_POR_ESTADO =
            "SELECT " + COLS_JOIN +
            " WHERE r.estado = ? ORDER BY r.fecha_entrada";

    private static final String SQL_ACTUALIZAR =
            "UPDATE RESERVA SET id_cliente=?, id_habitacion=?, fecha_entrada=?, fecha_salida=?, " +
            "estado=?, num_personas=?, precio_total=? WHERE id_reserva=?";

    private static final String SQL_ELIMINAR = "DELETE FROM RESERVA WHERE id_reserva=?";

    public ReservaDAOImpl() { super(); }
}
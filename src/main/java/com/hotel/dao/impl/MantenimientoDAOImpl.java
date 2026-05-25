
package com.hotel.dao.impl;

/**
 *
 * @author rober
 */
import com.hotel.dao.interfaces.IMantenimientoDAO;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC Oracle del repositorio de mantenimientos.
 * Adaptado al schema HOTELNATIVO: tabla MANTENIMIENTO con PK id_mantenimiento (VARCHAR2).
 *
 * REFACTORING v2 — 5 puntos críticos corregidos:
 *  1. ResultSet cerrado con try-with-resources anidado en todos los buscarPor*.
 *  2. Sin concatenación de Strings en SQL (todo PreparedStatement).
 *  3. insertar/actualizar usan enTransaccion() → commit/rollback garantizado.
 *  4. listarTodos(int,int) con paginación Oracle OFFSET/FETCH.
 *  5. Sin printStackTrace(); errores suben como ExcepcionBaseDatos.
 *
 * GRASP: Fabricación Pura.
 */
public class MantenimientoDAOImpl extends BaseDAO implements IMantenimientoDAO {

    // JOIN a HABITACION y EMPLEADO para obtener numero y nombre en una sola query
    private static final String COLS =
            "TO_NUMBER(REGEXP_REPLACE(m.id_mantenimiento,'[^0-9]','')) AS id, " +
            "TO_NUMBER(REGEXP_REPLACE(m.id_habitacion,'[^0-9]',''))    AS id_habitacion, " +
            "h.numero AS hab_numero, " +
            "TO_NUMBER(REGEXP_REPLACE(m.id_empleado,'[^0-9]',''))      AS id_empleado, " +
            "e.primer_nombre AS emp_nombre, e.apellido_1 AS emp_apellido, " +
            "m.fecha_solicitud, m.fecha_realizacion, m.tipo, m.estado, m.costo, m.descripcion_trabajo";

    private static final String FROM_JOIN =
            "FROM MANTENIMIENTO m " +
            "LEFT JOIN HABITACION h ON m.id_habitacion = h.id_habitacion " +
            "LEFT JOIN EMPLEADO   e ON m.id_empleado   = e.id_empleado";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT " + COLS + " " + FROM_JOIN + " WHERE m.id_mantenimiento = ?";

    private static final String SQL_POR_HABITACION =
            "SELECT " + COLS + " " + FROM_JOIN +
            " WHERE m.id_habitacion = ? ORDER BY m.fecha_solicitud DESC";

    private static final String SQL_PENDIENTES =
            "SELECT " + COLS + " " + FROM_JOIN +
            " WHERE m.estado IN ('SOLICITADO','EN_PROCESO') ORDER BY m.fecha_solicitud";

    private static final String SQL_LISTAR_TODOS =
            "SELECT " + COLS + " " + FROM_JOIN + " ORDER BY m.fecha_solicitud DESC";

    private static final String SQL_LISTAR_PAGINADA =
            SQL_LISTAR_TODOS + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    private static final String SQL_ACTUALIZAR =
            "UPDATE MANTENIMIENTO SET id_empleado=?, fecha_realizacion=?, estado=?, " +
            "costo=?, descripcion_trabajo=? WHERE id_mantenimiento=?";

    public MantenimientoDAOImpl() { super(); }
}
@Override
    public Mantenimiento insertar(Mantenimiento m) {
        String sql = "INSERT INTO MANTENIMIENTO " +
                "(id_mantenimiento, id_habitacion, id_empleado, fecha_solicitud, tipo, estado, descripcion_trabajo) " +
                "VALUES (?, ?, ?, ?, ?, 'SOLICITADO', ?)";
        return enTransaccion(conn -> {
            int seqVal = siguienteSeq(conn, "seq_mantenimiento");
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fmt("MAN", seqVal));
                stmt.setString(2, fmt("HAB", m.getHabitacion().getId()));
                stmt.setString(3, fmt("EMP", m.getEmpleadoResponsable().getId()));
                stmt.setDate(4, Date.valueOf(m.getFechaSolicitud()));
                stmt.setString(5, m.getTipo().name());
                stmt.setString(6, m.getDescripcionTrabajo());
                stmt.executeUpdate();
            }
            m.setId(seqVal);
            return m;
        });
    }
@Override
    public boolean actualizar(Mantenimiento m) {
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR)) {
                stmt.setString(1, fmt("EMP", m.getEmpleadoResponsable().getId()));
                stmt.setDate(2, m.getFechaRealizacion() != null
                        ? Date.valueOf(m.getFechaRealizacion()) : null);
                stmt.setString(3, m.getEstado().name());
                stmt.setDouble(4, m.getCosto());
                stmt.setString(5, m.getDescripcionTrabajo());
                stmt.setString(6, fmt("MAN", m.getId()));
                return stmt.executeUpdate() > 0;
            }
        });
    }
@Override
    public Optional<Mantenimiento> buscarPorId(int id) {
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {
            stmt.setString(1, fmt("MAN", id));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar mantenimiento: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public List<Mantenimiento> listarPorHabitacion(int idHabitacion) {
        List<Mantenimiento> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_POR_HABITACION)) {
            stmt.setString(1, fmt("HAB", idHabitacion));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar mantenimientos por habitación: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }
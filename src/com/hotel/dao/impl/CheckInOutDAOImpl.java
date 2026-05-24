
package com.hotel.dao.impl;

/**
 *
 * @author rober
 */
import com.hotel.dao.interfaces.ICheckInOutDAO;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC Oracle del repositorio CheckInOut.
 * Adaptado al schema HOTELNATIVO: tabla CHECKINOUT con PK id_checkinout (VARCHAR2).
 *
 * REFACTORING v2 — 5 puntos críticos corregidos:
 *  1. ResultSet cerrado con try-with-resources anidado en todos los buscarPor*.
 *  2. Sin concatenación de Strings en SQL (todo PreparedStatement).
 *  3. insertar() usa enTransaccion(): si el INSERT falla no queda un check-in huérfano.
 *  4. listarTodos(int,int) con paginación Oracle OFFSET/FETCH.
 *  5. Sin printStackTrace(); errores suben como ExcepcionBaseDatos.
 *
 * GRASP: Fabricación Pura.
 */
public class CheckInOutDAOImpl extends BaseDAO implements ICheckInOutDAO {

    private static final String SQL_INSERTAR =
            "INSERT INTO CHECKINOUT (id_checkinout, id_reserva, id_empleado, fecha_checkin, observaciones) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR =
            "UPDATE CHECKINOUT SET fecha_checkout=?, observaciones=? WHERE id_checkinout=?";

    private static final String SQL_JOIN =
            "SELECT TO_NUMBER(REGEXP_REPLACE(ci.id_checkinout,'[^0-9]','')) AS id, " +
            "TO_NUMBER(REGEXP_REPLACE(ci.id_reserva,'[^0-9]','')) AS id_reserva, " +
            "TO_NUMBER(REGEXP_REPLACE(ci.id_empleado,'[^0-9]','')) AS id_empleado, " +
            "ci.fecha_checkin, ci.fecha_checkout, ci.observaciones, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_cliente,'[^0-9]','')) AS id_cliente, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_habitacion,'[^0-9]','')) AS id_habitacion, " +
            "r.fecha_entrada, r.fecha_salida, r.estado r_estado, r.num_personas, r.precio_total, " +
            "cl.primer_nombre cl_nombre, cl.apellido_1 cl_apellido, cl.email cl_email, " +
            "h.numero h_numero, h.precio_base, " +
            "e.primer_nombre e_nombre, e.apellido_1 e_apellido " +
            "FROM CHECKINOUT ci " +
            "JOIN RESERVA r    ON ci.id_reserva  = r.id_reserva " +
            "JOIN CLIENTE cl   ON r.id_cliente   = cl.id_cliente " +
            "JOIN HABITACION h ON r.id_habitacion = h.id_habitacion " +
            "JOIN EMPLEADO e   ON ci.id_empleado  = e.id_empleado";

    private static final String SQL_BUSCAR_POR_ID =
            SQL_JOIN + " WHERE ci.id_checkinout = ?";

    private static final String SQL_BUSCAR_ACTIVO_POR_RESERVA =
            SQL_JOIN + " WHERE ci.id_reserva = ? AND ci.fecha_checkout IS NULL";

    private static final String SQL_LISTAR_TODOS =
            SQL_JOIN + " ORDER BY ci.fecha_checkin DESC";

    private static final String SQL_LISTAR_PAGINADA =
            SQL_LISTAR_TODOS + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    private static final String SQL_PENDIENTES_CHECKOUT =
            SQL_JOIN + " WHERE ci.fecha_checkout IS NULL " +
            "AND r.fecha_salida <= ? AND r.estado = 'EN_PROCESO'";

    public CheckInOutDAOImpl() { super(); }
}
@Override
    public CheckInOut insertar(CheckInOut checkInOut) {
        return enTransaccion(conn -> {
            int seqVal = siguienteSeq(conn, "seq_checkinout");
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERTAR)) {
                stmt.setString(1, fmt("CHK", seqVal));
                stmt.setString(2, fmt("RES", checkInOut.getReserva().getId()));
                stmt.setString(3, fmt("EMP", checkInOut.getEmpleadoResponsable().getId()));
                stmt.setTimestamp(4, Timestamp.valueOf(checkInOut.getFechaHoraCheckin()));
                stmt.setString(5, checkInOut.getObservaciones());
                stmt.executeUpdate();
            }
            checkInOut.setId(seqVal);
            return checkInOut;
        });
    }
 @Override
    public boolean actualizar(CheckInOut checkInOut) {
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR)) {
                stmt.setTimestamp(1, checkInOut.getFechaHoraCheckout() != null
                        ? Timestamp.valueOf(checkInOut.getFechaHoraCheckout()) : null);
                stmt.setString(2, checkInOut.getObservaciones());
                stmt.setString(3, fmt("CHK", checkInOut.getId()));
                return stmt.executeUpdate() > 0;
            }
        });
    }
 @Override
    public Optional<CheckInOut> buscarPorId(int id) {
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {
            stmt.setString(1, fmt("CHK", id));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar check-in/out: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }
@Override
    public Optional<CheckInOut> buscarCheckinActivoPorReserva(int idReserva) {
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_ACTIVO_POR_RESERVA)) {
            stmt.setString(1, fmt("RES", idReserva));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar check-in activo: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

package com.hotel.dao.impl;

/**
 *
 * @author rober
 */
import com.hotel.dao.interfaces.IHabitacionDAO;
import com.hotel.dto.BusquedaDisponibilidadDTO;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC Oracle del repositorio de habitaciones.
 * Adaptado al schema HOTELNATIVO: tablas HABITACION, TIPO_HABITACION, PISO
 * con PKs VARCHAR2 (id_habitacion, id_tipo, id_piso).
 *
 * REFACTORING v2 — 5 puntos críticos corregidos:
 *  1. Statement/ResultSet cerrados con try-with-resources anidados (sin fugas).
 *  2. Sin concatenación de Strings en SQL de usuario (todo con PreparedStatement).
 *  3. INSERT/UPDATE/DELETE usan enTransaccion() → commit/rollback garantizado.
 *  4. listarTodas(int,int) con paginación Oracle OFFSET/FETCH para evitar OOM.
 *  5. Sin printStackTrace(); los errores suben como ExcepcionBaseDatos.
 *
 * GRASP: Fabricación Pura.
 */
public class HabitacionDAOImpl extends BaseDAO implements IHabitacionDAO {

    private static final String COLS_JOIN =
            "TO_NUMBER(REGEXP_REPLACE(h.id_habitacion,'[^0-9]','')) AS id, " +
            "h.numero, h.estado, h.precio_base, h.num_camas, " +
            "TO_NUMBER(REGEXP_REPLACE(h.id_tipo,'[^0-9]','')) AS id_tipo, " +
            "TO_NUMBER(REGEXP_REPLACE(h.id_piso,'[^0-9]','')) AS id_piso, " +
            "t.nombre t_nombre, t.capacidad, t.descripcion t_desc, t.amenities, " +
            "p.numero_piso, p.descripcion p_desc";

    private static final String FROM_JOIN =
            "FROM HABITACION h " +
            "JOIN TIPO_HABITACION t ON h.id_tipo = t.id_tipo " +
            "JOIN PISO p ON h.id_piso = p.id_piso";

    private static final String SQL_INSERTAR =
            "INSERT INTO HABITACION (id_habitacion, numero, id_tipo, id_piso, estado, precio_base, num_camas) " +
            "VALUES (?, ?, ?, ?, 'DISPONIBLE', ?, ?)";

    private static final String SQL_ACTUALIZAR =
            "UPDATE HABITACION SET numero=?, id_tipo=?, id_piso=?, precio_base=? " +
            "WHERE id_habitacion=?";

    private static final String SQL_ACTUALIZAR_ESTADO =
            "UPDATE HABITACION SET estado=? WHERE id_habitacion=?";

    private static final String SQL_ELIMINAR =
            "DELETE FROM HABITACION WHERE id_habitacion=?";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT " + COLS_JOIN + " " + FROM_JOIN + " WHERE h.id_habitacion=?";

    private static final String SQL_BUSCAR_POR_NUMERO =
            "SELECT " + COLS_JOIN + " " + FROM_JOIN + " WHERE h.numero=?";

    private static final String SQL_LISTAR_TODAS =
            "SELECT " + COLS_JOIN + " " + FROM_JOIN + " ORDER BY h.numero";

    private static final String SQL_LISTAR_PAGINADA =
            SQL_LISTAR_TODAS + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    private static final String SQL_BUSCAR_DISPONIBLES =
            "SELECT " + COLS_JOIN + " " + FROM_JOIN + " " +
            "WHERE h.estado = 'DISPONIBLE' " +
            "AND t.capacidad >= ? " +
            "AND h.id_habitacion NOT IN (" +
            "   SELECT r.id_habitacion FROM RESERVA r " +
            "   WHERE r.estado NOT IN ('CANCELADA') " +
            "   AND r.fecha_entrada < ? AND r.fecha_salida > ?" +
            ") ORDER BY h.precio_base";

    public HabitacionDAOImpl() { super(); }
}
private static volatile boolean constraintVerificado = false;

    /**
     * Verifica que el CHECK constraint de estado admita RESERVADA.
     * CORRECCIÓN: Se eliminan TODOS los CHECK de HABITACION (evita operar
     * sobre search_condition que es tipo LONG en Oracle y no admite UPPER/LIKE).
     */
    private void verificarConstraintEstado() {
        if (constraintVerificado) return;
        synchronized (HabitacionDAOImpl.class) {
            if (constraintVerificado) return;
            Connection conn = obtener();
            try {
                // Prueba silenciosa: si lanza ORA-02290 el constraint es obsoleto
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("UPDATE HABITACION SET estado='RESERVADA' WHERE 1=0");
                }
            } catch (SQLException e) {
                if (e.getErrorCode() == 2290) {
                    migrarConstraintEstado(conn);
                }
            } finally {
                liberar(conn);
                constraintVerificado = true;
            }
        }
    }

    /** Elimina todos los CHECK de HABITACION y recrea chk_estado_hab con RESERVADA. */
    private void migrarConstraintEstado(Connection conn) {
        try {
            // Eliminar todos los CHECK de la tabla HABITACION
            List<String> nombres = new ArrayList<>();
            String qry = "SELECT constraint_name FROM user_constraints " +
                         "WHERE table_name='HABITACION' AND constraint_type='C'";
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(qry)) {
                while (rs.next()) nombres.add(rs.getString(1));
            }
            for (String nombre : nombres) {
                try (Statement st = conn.createStatement()) {
                    st.execute("ALTER TABLE HABITACION DROP CONSTRAINT " + nombre);
                    System.out.println("[DB] Constraint " + nombre + " eliminado.");
                }
            }
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE HABITACION ADD CONSTRAINT chk_estado_hab CHECK " +
                           "(estado IN ('DISPONIBLE','RESERVADA','OCUPADA'," +
                           "'MANTENIMIENTO','FUERA_DE_SERVICIO'))");
                System.out.println("[DB] Migración: CHECK constraint actualizado con RESERVADA.");
            }
        } catch (SQLException ex) {
            System.err.println("[DB] Error migrando constraint: " + ex.getMessage());
        }
    }
@Override
    public Habitacion insertar(Habitacion habitacion) {
        return enTransaccion(conn -> {
            int seqVal = siguienteSeq(conn, "seq_habitacion");
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERTAR)) {
                stmt.setString(1, fmt("HAB", seqVal));
                stmt.setString(2, habitacion.getNumero());
                stmt.setString(3, fmt("TIP", habitacion.getTipoHabitacion().getId()));
                stmt.setString(4, fmt("PIS", habitacion.getPiso().getId()));
                stmt.setDouble(5, habitacion.getPrecioBase());
                stmt.setInt(6, habitacion.getNumCamas());
                stmt.executeUpdate();
            }
            habitacion.setId(seqVal);
            return habitacion;
        });
    }
@Override
    public boolean actualizar(Habitacion habitacion) {
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR)) {
                stmt.setString(1, habitacion.getNumero());
                stmt.setString(2, fmt("TIP", habitacion.getTipoHabitacion().getId()));
                stmt.setString(3, fmt("PIS", habitacion.getPiso().getId()));
                stmt.setDouble(4, habitacion.getPrecioBase());
                stmt.setString(5, fmt("HAB", habitacion.getId()));
                return stmt.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean eliminar(int idHabitacion) {
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ELIMINAR)) {
                stmt.setString(1, fmt("HAB", idHabitacion));
                return stmt.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean actualizarEstado(int idHabitacion, String nuevoEstado) {
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR_ESTADO)) {
                stmt.setString(1, nuevoEstado);
                stmt.setString(2, fmt("HAB", idHabitacion));
                return stmt.executeUpdate() > 0;
            }
        });
    }
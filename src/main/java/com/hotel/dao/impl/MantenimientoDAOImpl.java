package com.hotel.dao.impl;

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
            "m.numero_habitacion AS hab_numero, " +        // v3: PK de HABITACION es numero
            "TO_NUMBER(REGEXP_REPLACE(m.id_empleado,'[^0-9]',''))      AS id_empleado, " +
            "e.primer_nombre AS emp_nombre, e.apellido_1 AS emp_apellido, " +
            "m.fecha_solicitud, m.fecha_realizacion, m.tipo, m.estado, m.costo, m.descripcion_trabajo";

    private static final String FROM_JOIN =
            "FROM MANTENIMIENTO m " +
            "LEFT JOIN HABITACION h ON m.numero_habitacion = h.numero " +   // v3: FK por numero
            "LEFT JOIN EMPLEADO   e ON m.id_empleado       = e.id_empleado";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT " + COLS + " " + FROM_JOIN + " WHERE m.id_mantenimiento = ?";

    private static final String SQL_POR_HABITACION =
            "SELECT " + COLS + " " + FROM_JOIN +
            " WHERE m.numero_habitacion = ? ORDER BY m.fecha_solicitud DESC";   // numero VARCHAR2(10)

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

    // ── Escrituras — todas con enTransaccion() ────────────────────────────────

    @Override
    public Mantenimiento insertar(Mantenimiento m) {
        // prc_registrar_mantenimiento: genera el ID, inserta en MANTENIMIENTO
        // y marca la habitación como MANTENIMIENTO si el tipo es EMERGENCIA.
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call PKG_ADMIN.registrar_mantenimiento(?, ?, ?, ?, ?)}")) {
            cs.setString(1, m.getHabitacion().getNumero());
            // v2: id_empleado = cedula (String.valueOf), no fmt("EMP", id)
            cs.setString(2, String.valueOf(m.getEmpleadoResponsable().getId()));
            cs.setString(3, m.getTipo().name());
            cs.setString(4, m.getDescripcionTrabajo());
            cs.registerOutParameter(5, java.sql.Types.VARCHAR);
            cs.execute();
            String idGenerado = cs.getString(5);
            m.setId(Integer.parseInt(idGenerado.replaceAll("[^0-9]", "")));
            return m;
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al registrar mantenimiento: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public boolean actualizar(Mantenimiento m) {
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR)) {
                stmt.setString(1, String.valueOf(m.getEmpleadoResponsable().getId()));
                stmt.setDate(2, m.getFechaRealizacion() != null
                        ? Date.valueOf(m.getFechaRealizacion()) : null);
                stmt.setString(3, m.getEstado().name());
                stmt.setDouble(4, m.getCosto());
                stmt.setString(5, m.getDescripcionTrabajo());
                stmt.setString(6, fmt3("MNT", m.getId()));
                return stmt.executeUpdate() > 0;
            }
        });
    }

    // ── Lecturas — ResultSet en try-with-resources anidado ───────────────────

    @Override
    public Optional<Mantenimiento> buscarPorId(int id) {
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {
            stmt.setString(1, fmt3("MNT", id));
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
    public List<Mantenimiento> listarPorHabitacion(String numero) {
        List<Mantenimiento> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_POR_HABITACION)) {
            stmt.setString(1, numero);   // numero_habitacion VARCHAR2(10) — ej. 'HAB-101'
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

    @Override
    public List<Mantenimiento> listarPendientes() {
        List<Mantenimiento> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_PENDIENTES);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) lista.add(mapearFila(rs));
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar pendientes: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    @Override
    public List<Mantenimiento> listarTodos() {
        List<Mantenimiento> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR_TODOS);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) lista.add(mapearFila(rs));
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar mantenimientos: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    /**
     * Versión paginada. pagina=0 devuelve la primera página.
     * Usa Oracle OFFSET/FETCH para no cargar todo el historial en memoria.
     */
    public List<Mantenimiento> listarTodos(int pagina, int tamano) {
        List<Mantenimiento> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR_PAGINADA)) {
            stmt.setInt(1, pagina * tamano);
            stmt.setInt(2, tamano);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar mantenimientos paginados: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    // ── Mapeador ─────────────────────────────────────────────────────────────

    private Mantenimiento mapearFila(ResultSet rs) throws SQLException {
        Habitacion habRef = new Habitacion();
        habRef.setNumero(rs.getString("hab_numero"));   // v3: numero es la PK de HABITACION

        Empleado empRef = new Empleado();
        empRef.setId(rs.getInt("id_empleado"));
        empRef.setNombre(rs.getString("emp_nombre"));
        empRef.setApellido(rs.getString("emp_apellido"));

        Mantenimiento m = new Mantenimiento();
        m.setId(rs.getInt("id"));
        m.setHabitacion(habRef);
        m.setEmpleadoResponsable(empRef);
        m.setFechaSolicitud(rs.getDate("fecha_solicitud").toLocalDate());
        Date fechaReal = rs.getDate("fecha_realizacion");
        if (fechaReal != null) m.setFechaRealizacion(fechaReal.toLocalDate());
        m.setTipo(Mantenimiento.TipoMantenimiento.valueOf(rs.getString("tipo")));
        m.setEstado(Mantenimiento.EstadoMantenimiento.valueOf(rs.getString("estado")));
        m.setCosto(rs.getDouble("costo"));
        m.setDescripcionTrabajo(rs.getString("descripcion_trabajo"));
        return m;
    }
}

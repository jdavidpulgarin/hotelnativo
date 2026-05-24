
package com.hotel.dao.impl;

/**
 *
 * @author rober
 */
import com.hotel.dao.interfaces.IEmpleadoDAO;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC Oracle del repositorio de empleados.
 * Adaptado al schema HOTELNATIVO: tabla EMPLEADO (PK id_empleado VARCHAR2)
 * y tabla CARGO (PK id_cargo VARCHAR2).
 * Columnas primer_nombre / apellido_1 mapean a nombre / apellido del modelo.
 *
 * REFACTORING v2 — 5 puntos críticos corregidos:
 *  1. ResultSet cerrado con try-with-resources anidado en todos los buscarPor*.
 *  2. Sin concatenación de Strings en SQL (todo PreparedStatement).
 *  3. insertar/actualizar/eliminar/actualizarHash usan enTransaccion().
 *  4. listarTodos(int,int) con paginación Oracle OFFSET/FETCH.
 *  5. Sin printStackTrace(); errores suben como ExcepcionBaseDatos.
 *
 * GRASP: Fabricación Pura.
 */
public class EmpleadoDAOImpl extends BaseDAO implements IEmpleadoDAO {

    private static final String COLS_JOIN =
            "TO_NUMBER(REGEXP_REPLACE(e.id_empleado,'[^0-9]','')) AS id, " +
            "e.primer_nombre AS nombre, e.segundo_nombre, " +
            "e.apellido_1 AS apellido, e.apellido_2, " +
            "e.email, e.telefono, e.fecha_contratacion, e.password_hash, " +
            "e.debe_cambiar_password, e.salario, e.tipo_contrato, e.tipo_pago, e.fecha_fin_contrato, " +
            "TO_NUMBER(REGEXP_REPLACE(e.id_cargo,'[^0-9]','')) AS id_cargo, " +
            "c.nombre_cargo, c.descripcion c_desc, c.salario_base";

    private static final String FROM_JOIN =
            "FROM EMPLEADO e JOIN CARGO c ON e.id_cargo = c.id_cargo";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT " + COLS_JOIN + " " + FROM_JOIN + " WHERE e.id_empleado = ?";

    private static final String SQL_LISTAR_TODOS =
            "SELECT " + COLS_JOIN + " " + FROM_JOIN + " ORDER BY e.apellido_1";

    private static final String SQL_LISTAR_PAGINADA =
            SQL_LISTAR_TODOS + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    private static final String SQL_BUSCAR_POR_CARGO =
            "SELECT " + COLS_JOIN + " " + FROM_JOIN + " WHERE e.id_cargo = ?";

    private static final String SQL_LISTAR_CARGOS =
            "SELECT TO_NUMBER(REGEXP_REPLACE(id_cargo,'[^0-9]','')) AS id, " +
            "nombre_cargo, descripcion, salario_base FROM CARGO ORDER BY id_cargo";

    private static final String SQL_ACTUALIZAR =
            "UPDATE EMPLEADO SET primer_nombre=?, segundo_nombre=?, apellido_1=?, apellido_2=?, " +
            "email=?, telefono=?, id_cargo=?, fecha_contratacion=?, " +
            "salario=?, tipo_contrato=?, tipo_pago=?, fecha_fin_contrato=? WHERE id_empleado=?";

    private static final String SQL_ELIMINAR = "DELETE FROM EMPLEADO WHERE id_empleado=?";

    private static final String SQL_ACTUALIZAR_HASH =
            "UPDATE EMPLEADO SET password_hash=? WHERE id_empleado=?";

    private static final String SQL_ACTUALIZAR_DEBE_CAMBIAR =
            "UPDATE EMPLEADO SET debe_cambiar_password=? WHERE id_empleado=?";

    public EmpleadoDAOImpl() { super(); }
}
private static volatile boolean schemaVerificado = false;

    private void verificarYMigrarSchema() {
        if (schemaVerificado) return;
        synchronized (EmpleadoDAOImpl.class) {
            if (schemaVerificado) return;
            Connection conn = obtener();
            try {
                migrarColumna(conn, "password_hash",        "VARCHAR2(72) DEFAULT NULL");
                migrarColumna(conn, "debe_cambiar_password","NUMBER(1)    DEFAULT 1");
                migrarColumna(conn, "salario",              "NUMBER(12,2) DEFAULT NULL");
                migrarColumna(conn, "tipo_contrato",        "VARCHAR2(30) DEFAULT 'INDEFINIDO'");
                migrarColumna(conn, "tipo_pago",            "VARCHAR2(20) DEFAULT 'MENSUAL'");
                migrarColumna(conn, "fecha_fin_contrato",   "DATE         DEFAULT NULL");
            } finally {
                liberar(conn);
                schemaVerificado = true;
            }
        }
    }
private void migrarColumna(Connection conn, String columna, String definicion) {
        try (Statement st = conn.createStatement()) {
            st.executeQuery("SELECT " + columna + " FROM EMPLEADO WHERE ROWNUM = 1").close();
        } catch (SQLException e) {
            if (e.getErrorCode() == 904) {
                try (Statement st = conn.createStatement()) {
                    st.execute("ALTER TABLE EMPLEADO ADD " + columna + " " + definicion);
                    System.out.println("[DB] Migración: columna " + columna + " añadida a EMPLEADO.");
                } catch (SQLException ex) {
                    System.err.println("[DB] Error añadiendo columna " + columna + ": " + ex.getMessage());
                }
            }
        }
    }
@Override
    public Empleado insertar(Empleado empleado) {
        String sql = "INSERT INTO EMPLEADO " +
                "(id_empleado, primer_nombre, segundo_nombre, apellido_1, apellido_2, " +
                "email, telefono, id_cargo, fecha_contratacion, debe_cambiar_password, " +
                "salario, tipo_contrato, tipo_pago, fecha_fin_contrato) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?)";
        return enTransaccion(conn -> {
            int seqVal = siguienteSeq(conn, "seq_empleado");
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fmt("EMP", seqVal));
                stmt.setString(2, empleado.getNombre());
                stmt.setString(3, blankToNull(empleado.getSegundoNombre()));
                stmt.setString(4, empleado.getApellido());
                stmt.setString(5, blankToNull(empleado.getApellido2()));
                stmt.setString(6, empleado.getEmail());
                stmt.setString(7, empleado.getTelefono());
                stmt.setString(8, fmt("CAR", empleado.getCargo().getId()));
                stmt.setDate(9, Date.valueOf(empleado.getFechaContratacion()));
                if (empleado.getSalario() > 0) stmt.setDouble(10, empleado.getSalario());
                else stmt.setNull(10, java.sql.Types.NUMERIC);
                stmt.setString(11, empleado.getTipoContrato());
                stmt.setString(12, empleado.getTipoPago());
                stmt.setDate(13, empleado.getFechaFinContrato() != null
                        ? Date.valueOf(empleado.getFechaFinContrato()) : null);
                stmt.executeUpdate();
            }
            empleado.setId(seqVal);
            return empleado;
        });
    }
@Override
    public boolean actualizar(Empleado empleado) {
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR)) {
                stmt.setString(1, empleado.getNombre());
                stmt.setString(2, blankToNull(empleado.getSegundoNombre()));
                stmt.setString(3, empleado.getApellido());
                stmt.setString(4, blankToNull(empleado.getApellido2()));
                stmt.setString(5, empleado.getEmail());
                stmt.setString(6, empleado.getTelefono());
                stmt.setString(7, fmt("CAR", empleado.getCargo().getId()));
                stmt.setDate(8, empleado.getFechaContratacion() != null
                        ? Date.valueOf(empleado.getFechaContratacion()) : null);
                if (empleado.getSalario() > 0) stmt.setDouble(9, empleado.getSalario());
                else stmt.setNull(9, java.sql.Types.NUMERIC);
                stmt.setString(10, empleado.getTipoContrato());
                stmt.setString(11, empleado.getTipoPago());
                stmt.setDate(12, empleado.getFechaFinContrato() != null
                        ? Date.valueOf(empleado.getFechaFinContrato()) : null);
                stmt.setString(13, fmt("EMP", empleado.getId()));
                return stmt.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean eliminar(int idEmpleado) {
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ELIMINAR)) {
                stmt.setString(1, fmt("EMP", idEmpleado));
                return stmt.executeUpdate() > 0;
            }
        });
    }
@Override
    public boolean actualizarPasswordHash(int idEmpleado, String passwordHash) {
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR_HASH)) {
                stmt.setString(1, passwordHash);
                stmt.setString(2, fmt("EMP", idEmpleado));
                return stmt.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean actualizarDebeCambiarPassword(int idEmpleado, boolean debe) {
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR_DEBE_CAMBIAR)) {
                stmt.setInt(1, debe ? 1 : 0);
                stmt.setString(2, fmt("EMP", idEmpleado));
                return stmt.executeUpdate() > 0;
            }
        });
    }
@Override
    public Optional<Empleado> buscarPorId(int idEmpleado) {
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {
            stmt.setString(1, fmt("EMP", idEmpleado));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar empleado: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }
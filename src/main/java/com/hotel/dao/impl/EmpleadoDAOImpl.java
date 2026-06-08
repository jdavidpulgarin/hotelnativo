package com.hotel.dao.impl;

import com.hotel.dao.interfaces.IEmpleadoDAO;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class EmpleadoDAOImpl extends BaseDAO implements IEmpleadoDAO {

    // v2: password_hash y debe_cambiar_password están en EMPLEADO (CREDENCIALES eliminada)
    private static final String COLS_JOIN =
            "TO_NUMBER(REGEXP_REPLACE(e.id_empleado,'[^0-9]','')) AS id, " +
            "e.primer_nombre AS nombre, e.segundo_nombre, " +
            "e.apellido_1 AS apellido, e.apellido_2, " +
            "e.email, e.telefono, e.fecha_contratacion, " +
            "e.password_hash, e.debe_cambiar_password, " +
            "e.salario, e.id_tipo_contrato, e.id_tipo_pago, e.fecha_fin_contrato, " +
            "TO_NUMBER(REGEXP_REPLACE(e.id_cargo,'[^0-9]','')) AS id_cargo, " +
            "c.nombre_cargo, c.descripcion c_desc, c.salario_base";

    private static final String FROM_JOIN =
            "FROM EMPLEADO e " +
            "JOIN CARGO c ON e.id_cargo = c.id_cargo";

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

    // Forzar cambio de contraseña en próximo login (no tiene procedimiento equivalente)
    private static final String SQL_ACTUALIZAR_DEBE_CAMBIAR =
            "UPDATE EMPLEADO SET debe_cambiar_password=?, fecha_mod_cred=SYSDATE WHERE id_empleado=?";

    public EmpleadoDAOImpl() { super(); }

    // ── Escrituras — todas con enTransaccion() ────────────────────────────────

    @Override
    public Empleado insertar(Empleado empleado) {
        // v2: id_empleado = cedula (clave natural). password_hash en EMPLEADO directamente.
        String sqlEmp = "INSERT INTO EMPLEADO " +
                "(id_empleado, tipo_documento, primer_nombre, segundo_nombre, apellido_1, apellido_2, " +
                "email, telefono, id_cargo, fecha_contratacion, " +
                "salario, id_tipo_contrato, id_tipo_pago, fecha_fin_contrato, " +
                "password_hash, debe_cambiar_password, fecha_creacion_cred, fecha_mod_cred) " +
                "VALUES (?, 'CEDULA', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, SYSDATE, SYSDATE)";
        return enTransaccion(conn -> {
            // id_empleado = cedula numérica; EmpleadoService la fija con setId() antes de llamar aquí
            String cedula = String.valueOf(empleado.getId());
            String hashInicial = empleado.getHashContrasena() != null
                    && !empleado.getHashContrasena().isBlank()
                    ? empleado.getHashContrasena() : "";
            try (PreparedStatement stmt = conn.prepareStatement(sqlEmp)) {
                stmt.setString(1, cedula);
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
                stmt.setString(14, hashInicial);
                stmt.executeUpdate();
            }
            return empleado;
        });
    }

    @Override
    public boolean actualizar(Empleado empleado) {
        // PKG_ADMIN.actualizar_empleado (corregido): incluye fecha_contratacion y fecha_fin_contrato
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call PKG_ADMIN.actualizar_empleado(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
            cs.setString(1, String.valueOf(empleado.getId()));
            cs.setString(2, empleado.getNombre());
            cs.setString(3, blankToNull(empleado.getSegundoNombre()));
            cs.setString(4, empleado.getApellido());
            cs.setString(5, blankToNull(empleado.getApellido2()));
            cs.setString(6, empleado.getEmail());
            cs.setString(7, empleado.getTelefono());
            cs.setString(8, fmt("CAR", empleado.getCargo().getId()));
            cs.setString(9, empleado.getTipoContrato());
            cs.setString(10, empleado.getTipoPago());
            if (empleado.getSalario() > 0) cs.setDouble(11, empleado.getSalario());
            else cs.setNull(11, java.sql.Types.NUMERIC);
            cs.setDate(12, empleado.getFechaContratacion() != null
                    ? Date.valueOf(empleado.getFechaContratacion()) : null);
            cs.setDate(13, empleado.getFechaFinContrato() != null
                    ? Date.valueOf(empleado.getFechaFinContrato()) : null);
            cs.execute();
            return true;
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al actualizar empleado: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public boolean eliminar(int idEmpleado) {
        // prc_eliminar_empleado limpia MANTENIMIENTO del empleado antes de borrar
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call PKG_ADMIN.eliminar_empleado(?)}")) {
            cs.setString(1, String.valueOf(idEmpleado));
            cs.execute();
            return true;
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al eliminar empleado: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public boolean actualizarPasswordHash(int idEmpleado, String passwordHash) {
        // PKG_ADMIN.cambiar_password actualiza hash, fecha_mod_cred y pone debe_cambiar=0
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call PKG_ADMIN.cambiar_password(?, ?)}")) {
            cs.setString(1, String.valueOf(idEmpleado));
            cs.setString(2, passwordHash);
            cs.execute();
            return true;
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al cambiar contraseña: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public boolean actualizarDebeCambiarPassword(int idEmpleado, boolean debe) {
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR_DEBE_CAMBIAR)) {
                stmt.setInt(1, debe ? 1 : 0);
                stmt.setString(2, String.valueOf(idEmpleado));
                return stmt.executeUpdate() > 0;
            }
        });
    }
   @Override
    public void aumentarSalarios(int idCargo, double porcentaje) {
        // PKG_HOTEL.aumento_salarios valida el porcentaje y actualiza CARGO.salario_base
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call PKG_ADMIN.aumento_salarios(?, ?)}")) {
            cs.setString(1, fmt("CAR", idCargo));
            cs.setDouble(2, porcentaje);
            cs.execute();
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al aumentar salarios: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    // ── Lecturas — ResultSet en try-with-resources anidado ───────────────────

    @Override
    public Optional<Empleado> buscarPorId(int idEmpleado) {
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {
            stmt.setString(1, String.valueOf(idEmpleado));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar empleado: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public List<Empleado> listarTodos() {
        List<Empleado> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR_TODOS);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) lista.add(mapearFila(rs));
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar empleados: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    /**
     * Versión paginada. pagina=0 devuelve la primera página.
     * Usa Oracle OFFSET/FETCH para no cargar toda la tabla en memoria.
     */
    public List<Empleado> listarTodos(int pagina, int tamano) {
        List<Empleado> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR_PAGINADA)) {
            stmt.setInt(1, pagina * tamano);
            stmt.setInt(2, tamano);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar empleados paginados: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    @Override
    public List<Empleado> buscarPorCargo(int idCargo) {
        List<Empleado> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_POR_CARGO)) {
            stmt.setString(1, fmt("CAR", idCargo));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar por cargo: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    @Override
    public List<Cargo> listarCargos() {
        List<Cargo> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR_CARGOS);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Cargo c = new Cargo();
                c.setId(rs.getInt("id"));
                c.setNombreCargo(rs.getString("nombre_cargo"));
                c.setDescripcion(rs.getString("descripcion"));
                c.setSalarioBase(rs.getDouble("salario_base"));
                lista.add(c);
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar cargos: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    // ── Mapeador ─────────────────────────────────────────────────────────────

    private Empleado mapearFila(ResultSet rs) throws SQLException {
        Cargo cargo = new Cargo();
        cargo.setId(rs.getInt("id_cargo"));
        cargo.setNombreCargo(rs.getString("nombre_cargo"));
        try { cargo.setDescripcion(rs.getString("c_desc")); }      catch (SQLException ignored) {}
        try { cargo.setSalarioBase(rs.getDouble("salario_base")); } catch (SQLException ignored) {}

        Empleado e = new Empleado();
        e.setId(rs.getInt("id"));
        e.setNombre(rs.getString("nombre"));
        try { e.setSegundoNombre(rs.getString("segundo_nombre")); } catch (SQLException ignored) {}
        e.setApellido(rs.getString("apellido"));
        try { e.setApellido2(rs.getString("apellido_2")); }        catch (SQLException ignored) {}
        e.setEmail(rs.getString("email"));
        try { e.setTelefono(rs.getString("telefono")); }            catch (SQLException ignored) {}
        try { e.setHashContrasena(rs.getString("password_hash")); }   catch (SQLException ignored) {}
        try { e.setDebeCambiarContrasena(rs.getInt("debe_cambiar_password") == 1); }
        catch (SQLException ignored) {}
        try { e.setSalario(rs.getDouble("salario")); }                   catch (SQLException ignored) {}
        try { e.setTipoContrato(rs.getString("id_tipo_contrato")); }   catch (SQLException ignored) {}
        try { e.setTipoPago(rs.getString("id_tipo_pago")); }           catch (SQLException ignored) {}
        try {
            Date ffc = rs.getDate("fecha_fin_contrato");
            if (ffc != null) e.setFechaFinContrato(ffc.toLocalDate());
        } catch (SQLException ignored) {}
        e.setCargo(cargo);
        e.setFechaContratacion(rs.getDate("fecha_contratacion").toLocalDate());
        return e;
    }
}

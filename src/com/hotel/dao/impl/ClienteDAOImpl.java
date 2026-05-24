
package com.hotel.dao.impl;

/**
 *
 * @author rober
 */
import com.hotel.dao.interfaces.IClienteDAO;
import com.hotel.dao.interfaces.IClienteBusqueda;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.model.Cliente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC Oracle del repositorio de clientes.
 * Adaptado al schema HOTELNATIVO: tabla CLIENTE, PK id_cliente (VARCHAR2),
 * columnas primer_nombre / apellido_1 en lugar de nombre / apellido.
 *
 * REFACTORING v2 — 5 puntos críticos corregidos:
 *  1. ResultSet cerrado con try-with-resources anidado en todos los buscarPor*.
 *  2. Sin concatenación de Strings en SQL (todo PreparedStatement).
 *  3. insertar/actualizar/eliminar usan enTransaccion() → commit/rollback garantizado.
 *  4. listarTodos(int,int) con paginación Oracle OFFSET/FETCH.
 *  5. Sin printStackTrace(); errores suben como ExcepcionBaseDatos.
 *
 * GRASP: Fabricación Pura.
 */
public class ClienteDAOImpl extends BaseDAO implements IClienteDAO, IClienteBusqueda {

    private static final String COLS =
            "id_cliente AS cedula_str, " +
            "TO_NUMBER(REGEXP_REPLACE(id_cliente,'[^0-9]','')) AS id, " +
            "primer_nombre AS nombre, segundo_nombre, apellido_1 AS apellido, apellido_2, " +
            "email, telefono, " +
            "nacionalidad, ciudad_origen, fecha_registro, es_vip";

    private static final String SQL_LISTAR_TODOS =
            "SELECT " + COLS + " FROM CLIENTE ORDER BY apellido_1, primer_nombre";

    private static final String SQL_LISTAR_PAGINADA =
            SQL_LISTAR_TODOS + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    // BUG 1 FIX: Clientes con formato "CLI##" tienen id numérico (ej. 1) pero
    // su id_cliente en BD es "CLI01". Clientes con cédula directa (ej. 1066280182)
    // coinciden tal cual. El OR cubre ambos casos sin cambiar la interfaz.
    private static final String SQL_BUSCAR_POR_ID =
            "SELECT " + COLS + " FROM CLIENTE WHERE id_cliente = ? OR id_cliente = ?";

    private static final String SQL_BUSCAR_POR_DOCUMENTO =
            "SELECT " + COLS + " FROM CLIENTE WHERE id_cliente = ?";

    private static final String SQL_BUSCAR_EMAIL =
            "SELECT " + COLS + " FROM CLIENTE WHERE email = ?";

    private static final String SQL_BUSCAR_NOMBRE =
            "SELECT " + COLS + " FROM CLIENTE " +
            "WHERE UPPER(primer_nombre) LIKE UPPER(?) ESCAPE '!' OR UPPER(apellido_1) LIKE UPPER(?) ESCAPE '!'";

    private static final String SQL_LISTAR_VIP =
            "SELECT " + COLS + " FROM CLIENTE WHERE es_vip = 1 ORDER BY apellido_1";

    private static final String SQL_ACTUALIZAR =
            "UPDATE CLIENTE SET primer_nombre=?, segundo_nombre=?, apellido_1=?, apellido_2=?, " +
            "email=?, telefono=?, nacionalidad=?, es_vip=?, ciudad_origen=? WHERE id_cliente=?";

    private static final String SQL_ELIMINAR =
            "DELETE FROM CLIENTE WHERE id_cliente=? OR id_cliente=?";

    public ClienteDAOImpl() { super(); }
}
@Override
    public Cliente insertar(Cliente cliente) {
        String sql = "INSERT INTO CLIENTE " +
                "(id_cliente, primer_nombre, segundo_nombre, apellido_1, apellido_2, " +
                " email, telefono, nacionalidad, ciudad_origen, fecha_registro, es_vip) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return enTransaccion(conn -> {
            String cedula = cliente.getDocumento();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, cedula);
                stmt.setString(2, cliente.getNombre());
                stmt.setString(3, cliente.getSegundoNombre());
                stmt.setString(4, cliente.getApellido());
                stmt.setString(5, cliente.getApellido2());
                stmt.setString(6, cliente.getEmail());
                stmt.setString(7, cliente.getTelefono());
                stmt.setString(8, cliente.getNacionalidad());
                stmt.setString(9, cliente.getCiudadOrigen());
                stmt.setDate(10, java.sql.Date.valueOf(java.time.LocalDate.now()));
                stmt.setInt(11, cliente.isEsVip() ? 1 : 0);
                stmt.executeUpdate();
            }
            try { cliente.setId(Integer.parseInt(cedula)); }
            catch (NumberFormatException ignore) { cliente.setId(0); }
            cliente.setFechaRegistro(java.time.LocalDate.now());
            return cliente;
        });
    }
@Override
    public boolean actualizar(Cliente cliente) {
        String cedula = cliente.getDocumento() != null
                ? cliente.getDocumento() : String.valueOf(cliente.getId());
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR)) {
                stmt.setString(1, cliente.getNombre());
                stmt.setString(2, cliente.getSegundoNombre());
                stmt.setString(3, cliente.getApellido());
                stmt.setString(4, cliente.getApellido2());
                stmt.setString(5, cliente.getEmail());
                stmt.setString(6, cliente.getTelefono());
                stmt.setString(7, cliente.getNacionalidad());
                stmt.setInt(8, cliente.isEsVip() ? 1 : 0);
                stmt.setString(9, cliente.getCiudadOrigen());
                stmt.setString(10, cedula);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean eliminar(int idCliente) {
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ELIMINAR)) {
                stmt.setString(1, String.valueOf(idCliente));
                stmt.setString(2, fmt("CLI", idCliente));
                return stmt.executeUpdate() > 0;
            }
        });
    }
@Override
    public Optional<Cliente> buscarPorId(int idCliente) {
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {
            stmt.setString(1, String.valueOf(idCliente));  // cedula directa (ej. "1066280182")
            stmt.setString(2, fmt("CLI", idCliente));      // formato prefijo (ej. "CLI01")
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar cliente por ID: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public List<Cliente> listarTodos() {
        List<Cliente> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR_TODOS);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) lista.add(mapearFila(rs));
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar clientes: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }
/**
     * Versión paginada. pagina=0 devuelve la primera página.
     * Usa Oracle OFFSET/FETCH para no cargar toda la tabla en memoria.
     */
    public List<Cliente> listarTodos(int pagina, int tamano) {
        List<Cliente> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR_PAGINADA)) {
            stmt.setInt(1, pagina * tamano);
            stmt.setInt(2, tamano);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar clientes paginados: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }
@Override
    public Optional<Cliente> buscarPorDocumento(String documento) {
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_POR_DOCUMENTO)) {
            stmt.setString(1, documento);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar por cédula: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public Optional<Cliente> buscarPorEmail(String email) {
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_EMAIL)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar por email: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }
@Override
    public List<Cliente> buscarPorNombre(String textoBusqueda) {
        List<Cliente> lista = new ArrayList<>();
        // Escapar wildcards LIKE para evitar que _ y % actúen como comodines SQL
        String escapado = textoBusqueda.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        String patron = "%" + escapado + "%";
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_NOMBRE)) {
            stmt.setString(1, patron);
            stmt.setString(2, patron);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar clientes por nombre: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    @Override
    public List<Cliente> listarClientesVip() {
        List<Cliente> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR_VIP);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) lista.add(mapearFila(rs));
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar VIP: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }
@Override
    public void purgarHistorialCliente(int idCliente) {
        enTransaccion(conn -> {
            String numId = String.valueOf(idCliente);
            String cliId = fmt("CLI", idCliente);

            // 1. Check-ins de las reservas del cliente
            try (PreparedStatement s = conn.prepareStatement(
                    "DELETE FROM CHECKINOUT WHERE id_reserva IN " +
                    "(SELECT id_reserva FROM RESERVA WHERE id_cliente=? OR id_cliente=?)")) {
                s.setString(1, numId);
                s.setString(2, cliId);
                s.executeUpdate();
            }
            // 2. Facturas del cliente
            try (PreparedStatement s = conn.prepareStatement(
                    "DELETE FROM FACTURA WHERE id_cliente=? OR id_cliente=?")) {
                s.setString(1, numId);
                s.setString(2, cliId);
                s.executeUpdate();
            }
            // 3. Reservas del cliente
            try (PreparedStatement s = conn.prepareStatement(
                    "DELETE FROM RESERVA WHERE id_cliente=? OR id_cliente=?")) {
                s.setString(1, numId);
                s.setString(2, cliId);
                s.executeUpdate();
            }
            return null;
        });
    }
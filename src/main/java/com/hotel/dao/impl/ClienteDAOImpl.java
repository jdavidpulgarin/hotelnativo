package com.hotel.dao.impl;

import com.hotel.dao.interfaces.IClienteDAO;
import com.hotel.dao.interfaces.IClienteBusqueda;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.model.Cliente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class ClienteDAOImpl extends BaseDAO implements IClienteDAO, IClienteBusqueda {

    // COLS hace JOIN con PAIS y CIUDAD para devolver nombres legibles
    // idPais/idCiudad: claves FK para INSERT/UPDATE
    // nacionalidad/ciudad_origen: nombres para mostrar en pantalla
    private static final String COLS =
            "cl.id_cliente AS cedula_str, " +
            "TO_NUMBER(REGEXP_REPLACE(cl.id_cliente,'[^0-9]','')) AS id, " +
            "cl.primer_nombre AS nombre, cl.segundo_nombre, cl.apellido_1 AS apellido, cl.apellido_2, " +
            "cl.email, cl.telefono, " +
            "cl.id_pais, " +
            "NVL(p.nombre,  cl.id_pais)   AS nacionalidad, " +
            "cl.id_ciudad, " +
            "NVL(ci.nombre, cl.id_ciudad) AS ciudad_origen, " +
            "cl.fecha_registro, cl.es_vip";

    private static final String FROM_JOIN =
            "FROM CLIENTE cl " +
            "LEFT JOIN PAIS   p  ON p.id_pais    = cl.id_pais " +
            "LEFT JOIN CIUDAD ci ON ci.id_ciudad = cl.id_ciudad";

    private static final String SQL_LISTAR_TODOS =
            "SELECT " + COLS + " " + FROM_JOIN + " ORDER BY cl.apellido_1, cl.primer_nombre";

    private static final String SQL_LISTAR_PAGINADA =
            SQL_LISTAR_TODOS + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT " + COLS + " " + FROM_JOIN + " WHERE cl.id_cliente = ? OR cl.id_cliente = ?";

    private static final String SQL_BUSCAR_POR_DOCUMENTO =
            "SELECT " + COLS + " " + FROM_JOIN + " WHERE cl.id_cliente = ?";

    private static final String SQL_BUSCAR_EMAIL =
            "SELECT " + COLS + " " + FROM_JOIN + " WHERE cl.email = ?";

    private static final String SQL_BUSCAR_NOMBRE =
            "SELECT " + COLS + " " + FROM_JOIN +
            " WHERE UPPER(cl.primer_nombre) LIKE UPPER(?) ESCAPE '!'" +
            " OR UPPER(cl.apellido_1) LIKE UPPER(?) ESCAPE '!'";

    private static final String SQL_LISTAR_VIP =
            "SELECT " + COLS + " " + FROM_JOIN + " WHERE cl.es_vip = 1 ORDER BY cl.apellido_1";

    public ClienteDAOImpl() { super(); }

    // ── Escrituras — delegan a PKG_ADMIN vía CallableStatement ───────────────

    @Override
    public Cliente insertar(Cliente cliente) {
        String cedula = cliente.getDocumento();
        String tipoDoc = (cliente.getSubtipoDocumento() != null
                && !cliente.getSubtipoDocumento().equals("COLOMBIANA"))
                ? cliente.getSubtipoDocumento() : "CEDULA";
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call PKG_ADMIN.registrar_cliente(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
            cs.setString(1, cedula);
            cs.setString(2, tipoDoc);
            cs.setString(3, cliente.getNombre());
            cs.setString(4, cliente.getSegundoNombre());
            cs.setString(5, cliente.getApellido());
            cs.setString(6, cliente.getApellido2());
            cs.setString(7, cliente.getEmail());
            cs.setString(8, cliente.getTelefono());
            cs.setString(9,  cliente.getIdPais()   != null ? cliente.getIdPais()   : "PAI01");
            cs.setString(10, cliente.getIdCiudad() != null ? cliente.getIdCiudad() : "CIU79");
            cs.execute();
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al insertar cliente: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        try { cliente.setId(Integer.parseInt(cedula)); }
        catch (NumberFormatException ignore) { cliente.setId(0); }
        cliente.setFechaRegistro(java.time.LocalDate.now());
        return cliente;
    }

    @Override
    public boolean actualizar(Cliente cliente) {
        String cedula = cliente.getDocumento() != null
                ? cliente.getDocumento() : String.valueOf(cliente.getId());
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call PKG_ADMIN.actualizar_cliente(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
            cs.setString(1, cedula);
            cs.setString(2, cliente.getNombre());
            cs.setString(3, cliente.getSegundoNombre());
            cs.setString(4, cliente.getApellido());
            cs.setString(5, cliente.getApellido2());
            cs.setString(6, cliente.getEmail());
            cs.setString(7, cliente.getTelefono());
            cs.setString(8, cliente.getIdPais());
            cs.setString(9, cliente.getIdCiudad());
            cs.setInt(10, cliente.isEsVip() ? 1 : 0);
            cs.execute();
            return true;
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al actualizar cliente: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public boolean eliminar(int idCliente) {
        // prc_eliminar_cliente valida reservas activas, hace cascade y elimina el cliente
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call PKG_ADMIN.eliminar_cliente(?)}")) {
            cs.setString(1, String.valueOf(idCliente));
            cs.execute();
            return true;
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al eliminar cliente: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    // ── Lecturas — ResultSet en try-with-resources anidado ───────────────────

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

            // 1. Facturas del cliente
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

    // ── Mapeador ─────────────────────────────────────────────────────────────

    private Cliente mapearFila(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setDocumento(rs.getString("cedula_str"));
        c.setId(rs.getInt("id"));
        c.setNombre(rs.getString("nombre"));
        c.setSegundoNombre(rs.getString("segundo_nombre"));
        c.setApellido(rs.getString("apellido"));
        c.setApellido2(rs.getString("apellido_2"));
        c.setEmail(rs.getString("email"));
        c.setTelefono(rs.getString("telefono"));
        c.setIdPais(rs.getString("id_pais"));
        c.setNacionalidad(rs.getString("nacionalidad"));      // nombre: "Colombia"
        c.setIdCiudad(rs.getString("id_ciudad"));
        c.setCiudadOrigen(rs.getString("ciudad_origen"));     // nombre: "Bogotá"
        java.sql.Date fechaReg = rs.getDate("fecha_registro");
        c.setFechaRegistro(fechaReg != null ? fechaReg.toLocalDate() : java.time.LocalDate.now());
        c.setEsVip(rs.getInt("es_vip") == 1);
        return c;
    }
}

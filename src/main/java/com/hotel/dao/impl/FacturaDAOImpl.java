package com.hotel.dao.impl;

import com.hotel.dao.interfaces.IFacturaDAO;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class FacturaDAOImpl extends BaseDAO implements IFacturaDAO {

    private static final String SQL_JOIN =
            "SELECT TO_NUMBER(REGEXP_REPLACE(f.id_factura,'[^0-9]','')) AS id, " +
            "TO_NUMBER(REGEXP_REPLACE(f.id_reserva,'[^0-9]','')) AS id_reserva, " +
            "TO_NUMBER(REGEXP_REPLACE(f.id_cliente,'[^0-9]','')) AS id_cliente, " +
            "f.fecha_emision, f.subtotal, f.impuestos, f.total, f.estado_pago, f.id_metodo_pago AS metodo_pago, " +
            "cl.primer_nombre cl_nombre, cl.apellido_1 cl_apellido, cl.email cl_email, " +
            "cl.telefono cl_telefono, " +
            "r.fecha_entrada, r.fecha_salida " +
            "FROM FACTURA f " +
            "JOIN CLIENTE cl ON f.id_cliente = cl.id_cliente " +
            "LEFT JOIN RESERVA r  ON f.id_reserva = r.id_reserva";

    private static final String SQL_BUSCAR_POR_ID    = SQL_JOIN + " WHERE f.id_factura = ?";
    private static final String SQL_BUSCAR_RESERVA   = SQL_JOIN + " WHERE f.id_reserva = ?";

    private static final String SQL_LISTAR_CLIENTE =
            SQL_JOIN + " WHERE (f.id_cliente = ? OR f.id_cliente = ?) ORDER BY f.fecha_emision DESC";

    private static final String SQL_LISTAR_TODAS =
            SQL_JOIN + " ORDER BY f.fecha_emision DESC";

    private static final String SQL_LISTAR_PAGINADA =
            SQL_LISTAR_TODAS + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    private static final String SQL_ACTUALIZAR =
            "UPDATE FACTURA SET estado_pago=?, id_metodo_pago=? WHERE id_factura=?";

    public FacturaDAOImpl() { super(); }

    // ── Escrituras — todas con enTransaccion() ────────────────────────────────

    @Override
    public Factura insertar(Factura factura) {
        String sql = "INSERT INTO FACTURA " +
                "(id_factura, id_reserva, id_cliente, fecha_emision, subtotal, impuestos, total, " +
                " estado_pago, id_metodo_pago) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return enTransaccion(conn -> {
            int seqVal = siguienteSeq(conn, "seq_factura");
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // BUG 4 FIX: igual que Reserva, usar documento real para que el JOIN funcione
                String idClienteStr = factura.getCliente().getDocumento() != null
                        ? factura.getCliente().getDocumento()
                        : String.valueOf(factura.getCliente().getId());
                stmt.setString(1, fmt3("FAC", seqVal));
                stmt.setString(2, fmt3("RES", factura.getReserva().getId()));
                stmt.setString(3, idClienteStr);
                stmt.setDate(4, Date.valueOf(factura.getFechaEmision()));
                stmt.setDouble(5, factura.getSubtotal());
                stmt.setDouble(6, factura.getImpuestos());
                stmt.setDouble(7, factura.getTotal());
                stmt.setString(8, factura.getEstadoPago().name());
                stmt.setString(9, metodoPagoACodigo(factura.getMetodoPago()));
                stmt.executeUpdate();
            }
            factura.setId(seqVal);
            return factura;
        });
    }

    @Override
    public boolean actualizar(Factura factura) {
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR)) {
                stmt.setString(1, factura.getEstadoPago().name());
                stmt.setString(2, metodoPagoACodigo(factura.getMetodoPago()));
                stmt.setString(3, fmt3("FAC", factura.getId()));
                return stmt.executeUpdate() > 0;
            }
        });
    }

    // ── Lecturas — ResultSet en try-with-resources anidado ───────────────────

    @Override
    public Optional<Factura> buscarPorId(int idFactura) {
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {
            stmt.setString(1, fmt3("FAC", idFactura));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar factura: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public Optional<Factura> buscarPorReserva(int idReserva) {
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_RESERVA)) {
            stmt.setString(1, fmt3("RES", idReserva));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar factura por reserva: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public List<Factura> listarPorCliente(int idCliente) {
        List<Factura> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR_CLIENTE)) {
            stmt.setString(1, String.valueOf(idCliente));
            stmt.setString(2, fmt("CLI", idCliente));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar facturas por cliente: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    @Override
    public List<Factura> listarTodas() {
        List<Factura> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR_TODAS);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) lista.add(mapearFila(rs));
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar facturas: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    /**
     * Versión paginada. pagina=0 devuelve la primera página.
     * Usa Oracle OFFSET/FETCH para no cargar todo el historial en memoria.
     */
    public List<Factura> listarTodas(int pagina, int tamano) {
        List<Factura> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR_PAGINADA)) {
            stmt.setInt(1, pagina * tamano);
            stmt.setInt(2, tamano);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar facturas paginadas: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    // ── Conversión MetodoPago enum ↔ código DB ────────────────────────────────
    // La BD guarda 'MET01'..'MET04'; el enum Java usa EFECTIVO..TRANSFERENCIA

    private static String metodoPagoACodigo(Factura.MetodoPago m) {
        if (m == null) return null;
        switch (m) {
            case EFECTIVO:        return "MET01";
            case TARJETA_CREDITO: return "MET02";
            case TARJETA_DEBITO:  return "MET03";
            case TRANSFERENCIA:   return "MET04";
            default:              return null;
        }
    }

    private static Factura.MetodoPago codigoAMetodoPago(String codigo) {
        if (codigo == null) return null;
        switch (codigo) {
            case "MET01": return Factura.MetodoPago.EFECTIVO;
            case "MET02": return Factura.MetodoPago.TARJETA_CREDITO;
            case "MET03": return Factura.MetodoPago.TARJETA_DEBITO;
            case "MET04": return Factura.MetodoPago.TRANSFERENCIA;
            default:      return null;
        }
    }

    // ── Mapeador ─────────────────────────────────────────────────────────────

    private Factura mapearFila(ResultSet rs) throws SQLException {
        Reserva reservaRef = new Reserva();
        reservaRef.setId(rs.getInt("id_reserva"));
        try {
            Date fe = rs.getDate("fecha_entrada");
            if (fe != null) reservaRef.setFechaEntrada(fe.toLocalDate());
            Date fs = rs.getDate("fecha_salida");
            if (fs != null) reservaRef.setFechaSalida(fs.toLocalDate());
        } catch (SQLException ignored) {}

        Cliente clienteRef = new Cliente();
        clienteRef.setId(rs.getInt("id_cliente"));
        clienteRef.setNombre(rs.getString("cl_nombre"));
        clienteRef.setApellido(rs.getString("cl_apellido"));
        clienteRef.setEmail(rs.getString("cl_email"));
        try { clienteRef.setTelefono(rs.getString("cl_telefono")); } catch (SQLException ignored) {}

        Factura f = new Factura();
        f.setId(rs.getInt("id"));
        f.setReserva(reservaRef);
        f.setCliente(clienteRef);
        f.setFechaEmision(rs.getDate("fecha_emision").toLocalDate());
        f.setSubtotal(rs.getDouble("subtotal"));
        f.setImpuestos(rs.getDouble("impuestos"));
        f.setTotal(rs.getDouble("total"));
        f.setTasaIva(f.getSubtotal() > 0 ? f.getImpuestos() / f.getSubtotal() : 0.19);
        f.setEstadoPago(Factura.EstadoPago.valueOf(rs.getString("estado_pago")));
        f.setMetodoPago(codigoAMetodoPago(rs.getString("metodo_pago")));
        return f;
    }
}

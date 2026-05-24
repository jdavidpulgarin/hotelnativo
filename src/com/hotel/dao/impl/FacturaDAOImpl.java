
package com.hotel.dao.impl;

/**
 *
 * @author rober
 */
import com.hotel.dao.interfaces.IFacturaDAO;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC Oracle del repositorio de facturas.
 * Adaptado al schema HOTELNATIVO: tabla FACTURA con PK id_factura (VARCHAR2).
 *
 * REFACTORING v2 — 5 puntos críticos corregidos:
 *  1. ResultSet cerrado con try-with-resources anidado en todos los buscarPor*.
 *  2. Sin concatenación de Strings en SQL (todo PreparedStatement).
 *  3. insertar/actualizar usan enTransaccion() → commit/rollback garantizado.
 *  4. listarTodas(int,int) con paginación Oracle OFFSET/FETCH.
 *  5. Sin printStackTrace(); errores suben como ExcepcionBaseDatos.
 *
 * GRASP: Fabricación Pura.
 */
public class FacturaDAOImpl extends BaseDAO implements IFacturaDAO {

    private static final String SQL_JOIN =
            "SELECT TO_NUMBER(REGEXP_REPLACE(f.id_factura,'[^0-9]','')) AS id, " +
            "TO_NUMBER(REGEXP_REPLACE(f.id_reserva,'[^0-9]','')) AS id_reserva, " +
            "TO_NUMBER(REGEXP_REPLACE(f.id_cliente,'[^0-9]','')) AS id_cliente, " +
            "f.fecha_emision, f.subtotal, f.impuestos, f.total, f.estado_pago, f.metodo_pago, " +
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
            "UPDATE FACTURA SET estado_pago=?, metodo_pago=? WHERE id_factura=?";

    public FacturaDAOImpl() { super(); }
}
@Override
    public Factura insertar(Factura factura) {
        String sql = "INSERT INTO FACTURA " +
                "(id_factura, id_reserva, id_cliente, fecha_emision, subtotal, impuestos, total, " +
                " estado_pago, metodo_pago) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return enTransaccion(conn -> {
            int seqVal = siguienteSeq(conn, "seq_factura");
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Usar documento real para que el JOIN funcione
                String idClienteStr = factura.getCliente().getDocumento() != null
                        ? factura.getCliente().getDocumento()
                        : String.valueOf(factura.getCliente().getId());
                stmt.setString(1, fmt("FAC", seqVal));
                stmt.setString(2, fmt("RES", factura.getReserva().getId()));
                stmt.setString(3, idClienteStr);
                stmt.setDate(4, Date.valueOf(factura.getFechaEmision()));
                stmt.setDouble(5, factura.getSubtotal());
                stmt.setDouble(6, factura.getImpuestos());
                stmt.setDouble(7, factura.getTotal());
                stmt.setString(8, factura.getEstadoPago().name());
                stmt.setString(9, factura.getMetodoPago() != null
                        ? factura.getMetodoPago().name() : null);
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
                stmt.setString(2, factura.getMetodoPago() != null
                        ? factura.getMetodoPago().name() : null);
                stmt.setString(3, fmt("FAC", factura.getId()));
                return stmt.executeUpdate() > 0;
            }
        });
    }
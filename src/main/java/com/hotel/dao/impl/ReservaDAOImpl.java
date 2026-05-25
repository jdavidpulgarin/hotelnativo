
package com.hotel.dao.impl;

/**
 *
 * @author rober
 */
import com.hotel.dao.interfaces.IReservaDAO;
import com.hotel.dao.interfaces.IReservaBusqueda;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC Oracle del repositorio de reservas.
 * Adaptado al schema HOTELNATIVO: tabla RESERVA con PK id_reserva (VARCHAR2).
 *
 * REFACTORING v2 — 5 puntos críticos corregidos:
 *  1. ResultSet cerrado con try-with-resources anidado en todos los métodos.
 *  2. Sin concatenación de Strings en SQL (todo PreparedStatement).
 *  3. insertar/actualizar/eliminar usan enTransaccion() → commit/rollback garantizado.
 *  4. listarTodas(int,int) con paginación Oracle OFFSET/FETCH.
 *  5. Sin printStackTrace(); errores suben como ExcepcionBaseDatos.
 *
 * GRASP: Fabricación Pura.
 */
public class ReservaDAOImpl extends BaseDAO implements IReservaDAO, IReservaBusqueda {

    private static final String COLS_SIMPLE =
            "TO_NUMBER(REGEXP_REPLACE(id_reserva,'[^0-9]','')) AS id, " +
            "TO_NUMBER(REGEXP_REPLACE(id_cliente,'[^0-9]','')) AS id_cliente, " +
            "id_cliente AS documento_cliente, " +
            "TO_NUMBER(REGEXP_REPLACE(id_habitacion,'[^0-9]','')) AS id_habitacion, " +
            "fecha_entrada, fecha_salida, estado, num_personas, precio_total";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT TO_NUMBER(REGEXP_REPLACE(r.id_reserva,'[^0-9]','')) AS id, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_cliente,'[^0-9]','')) AS id_cliente, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_habitacion,'[^0-9]','')) AS id_habitacion, " +
            "r.fecha_entrada, r.fecha_salida, r.estado, r.num_personas, r.precio_total, " +
            "c.primer_nombre c_nombre, c.apellido_1 c_apellido, c.email c_email, " +
            "c.telefono c_telefono, r.id_cliente AS documento, c.nacionalidad, c.fecha_registro, c.es_vip, " +
            "h.numero h_numero, h.precio_base, h.estado h_estado " +
            "FROM RESERVA r " +
            "JOIN CLIENTE c ON r.id_cliente = c.id_cliente " +
            "JOIN HABITACION h ON r.id_habitacion = h.id_habitacion " +
            "WHERE r.id_reserva = ?";

    private static final String SQL_LISTAR_TODAS =
            "SELECT TO_NUMBER(REGEXP_REPLACE(r.id_reserva,'[^0-9]','')) AS id, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_cliente,'[^0-9]','')) AS id_cliente, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_habitacion,'[^0-9]','')) AS id_habitacion, " +
            "r.fecha_entrada, r.fecha_salida, r.estado, r.num_personas, r.precio_total, " +
            "c.primer_nombre c_nombre, c.apellido_1 c_apellido, c.email c_email, " +
            "c.telefono c_telefono, r.id_cliente AS documento, c.nacionalidad, c.fecha_registro, c.es_vip, " +
            "h.numero h_numero, h.precio_base, h.estado h_estado " +
            "FROM RESERVA r " +
            "JOIN CLIENTE c ON r.id_cliente = c.id_cliente " +
            "JOIN HABITACION h ON r.id_habitacion = h.id_habitacion " +
            "ORDER BY r.fecha_entrada DESC";

    private static final String SQL_LISTAR_PAGINADA =
            SQL_LISTAR_TODAS + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    // BUG 3 FIX: mismo OR doble que buscarPorId para cubrir "CLI##" y cédula directa
    private static final String SQL_RESERVAS_ACTIVAS_CLIENTE =
            "SELECT " + COLS_SIMPLE + " FROM RESERVA r " +
            "WHERE (r.id_cliente = ? OR r.id_cliente = ?) " +
            "AND r.estado NOT IN ('CANCELADA','COMPLETADA')";

    // BUG 5 FIX: COMPLETADA también debe excluirse; una reserva completada
    // no bloquea el reuso de la habitación en las mismas fechas.
    private static final String SQL_RESERVAS_SOLAPADAS =
            "SELECT " + COLS_SIMPLE + " FROM RESERVA r " +
            "WHERE r.id_habitacion = ? " +
            "AND r.estado NOT IN ('CANCELADA','COMPLETADA') " +
            "AND r.fecha_entrada < ? AND r.fecha_salida > ?";

    private static final String COLS_JOIN =
            "TO_NUMBER(REGEXP_REPLACE(r.id_reserva,'[^0-9]','')) AS id, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_cliente,'[^0-9]','')) AS id_cliente, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_habitacion,'[^0-9]','')) AS id_habitacion, " +
            "r.fecha_entrada, r.fecha_salida, r.estado, r.num_personas, r.precio_total, " +
            "c.primer_nombre c_nombre, c.apellido_1 c_apellido, c.email c_email, " +
            "c.telefono c_telefono, r.id_cliente AS documento, c.nacionalidad, c.fecha_registro, c.es_vip, " +
            "h.numero h_numero, h.precio_base, h.estado h_estado " +
            "FROM RESERVA r " +
            "JOIN CLIENTE c ON r.id_cliente = c.id_cliente " +
            "JOIN HABITACION h ON r.id_habitacion = h.id_habitacion";

    private static final String SQL_BUSCAR_POR_RANGO_FECHAS =
            "SELECT " + COLS_JOIN +
            " WHERE r.fecha_entrada >= ? AND r.fecha_entrada <= ? ORDER BY r.fecha_entrada";

    private static final String SQL_BUSCAR_POR_ESTADO =
            "SELECT " + COLS_JOIN +
            " WHERE r.estado = ? ORDER BY r.fecha_entrada";

    private static final String SQL_ACTUALIZAR =
            "UPDATE RESERVA SET id_cliente=?, id_habitacion=?, fecha_entrada=?, fecha_salida=?, " +
            "estado=?, num_personas=?, precio_total=? WHERE id_reserva=?";

    private static final String SQL_ELIMINAR = "DELETE FROM RESERVA WHERE id_reserva=?";

    public ReservaDAOImpl() { super(); }
}
@Override
    public Reserva insertar(Reserva reserva) {
        String sql = "INSERT INTO RESERVA " +
                "(id_reserva, id_cliente, id_habitacion, fecha_entrada, fecha_salida, " +
                " estado, num_personas, precio_total) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        return enTransaccion(conn -> {
            int seqVal = siguienteSeq(conn, "seq_reserva");
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Usar el id_cliente real (documento) para que el JOIN funcione
                String idClienteStr = reserva.getCliente().getDocumento() != null
                        ? reserva.getCliente().getDocumento()
                        : String.valueOf(reserva.getCliente().getId());
                stmt.setString(1, fmt("RES", seqVal));
                stmt.setString(2, idClienteStr);
                stmt.setString(3, fmt("HAB", reserva.getHabitacion().getId()));
                stmt.setDate(4, java.sql.Date.valueOf(reserva.getFechaEntrada()));
                stmt.setDate(5, java.sql.Date.valueOf(reserva.getFechaSalida()));
                stmt.setString(6, reserva.getEstado().name());
                stmt.setInt(7, reserva.getNumPersonas());
                stmt.setDouble(8, reserva.getPrecioTotal());
                stmt.executeUpdate();
            }
            reserva.setId(seqVal);
            return reserva;
        });
    }
@Override
    public boolean actualizar(Reserva reserva) {
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR)) {
                String idClienteStr = reserva.getCliente().getDocumento() != null
                        ? reserva.getCliente().getDocumento()
                        : String.valueOf(reserva.getCliente().getId());
                stmt.setString(1, idClienteStr);
                stmt.setString(2, fmt("HAB", reserva.getHabitacion().getId()));
                stmt.setDate(3, Date.valueOf(reserva.getFechaEntrada()));
                stmt.setDate(4, Date.valueOf(reserva.getFechaSalida()));
                stmt.setString(5, reserva.getEstado().name());
                stmt.setInt(6, reserva.getNumPersonas());
                stmt.setDouble(7, reserva.getPrecioTotal());
                stmt.setString(8, fmt("RES", reserva.getId()));
                return stmt.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean eliminar(int idReserva) {
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ELIMINAR)) {
                stmt.setString(1, fmt("RES", idReserva));
                return stmt.executeUpdate() > 0;
            }
        });
    }
 @Override
    public Optional<Reserva> buscarPorId(int idReserva) {
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {
            stmt.setString(1, fmt("RES", idReserva));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapearFilaCompleta(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar reserva: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public List<Reserva> listarTodas() {
        List<Reserva> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR_TODAS);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) lista.add(mapearFilaCompleta(rs));
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar reservas: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }
 /**
     * Versión paginada. pagina=0 devuelve la primera página.
     * Usa Oracle OFFSET/FETCH para no cargar todo el historial en memoria.
     */
    public List<Reserva> listarTodas(int pagina, int tamano) {
        List<Reserva> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR_PAGINADA)) {
            stmt.setInt(1, pagina * tamano);
            stmt.setInt(2, tamano);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapearFilaCompleta(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar reservas paginadas: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }
@Override
    public List<Reserva> buscarReservasActivasPorCliente(int idCliente) {
        List<Reserva> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_RESERVAS_ACTIVAS_CLIENTE)) {
            stmt.setString(1, String.valueOf(idCliente));
            stmt.setString(2, fmt("CLI", idCliente));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapearFilaSimple(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar reservas activas: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    @Override
    public List<Reserva> buscarReservasSolapadas(int idHabitacion,
                                                  LocalDate fechaEntrada,
                                                  LocalDate fechaSalida) {
        List<Reserva> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_RESERVAS_SOLAPADAS)) {
            stmt.setString(1, fmt("HAB", idHabitacion));
            stmt.setDate(2, Date.valueOf(fechaSalida));
            stmt.setDate(3, Date.valueOf(fechaEntrada));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapearFilaSimple(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar solapadas: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }
@Override
    public List<Reserva> buscarPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        List<Reserva> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_POR_RANGO_FECHAS)) {
            stmt.setDate(1, Date.valueOf(fechaInicio));
            stmt.setDate(2, Date.valueOf(fechaFin));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapearFilaCompleta(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar por rango: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    @Override
    public List<Reserva> buscarPorEstado(String estado) {
        List<Reserva> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_POR_ESTADO)) {
            stmt.setString(1, estado);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapearFilaCompleta(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar por estado: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }
private Reserva mapearFilaCompleta(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente();
        cliente.setId(rs.getInt("id_cliente"));
        cliente.setDocumento(rs.getString("documento"));
        cliente.setNombre(rs.getString("c_nombre"));
        cliente.setApellido(rs.getString("c_apellido"));
        cliente.setEmail(rs.getString("c_email"));
        cliente.setTelefono(rs.getString("c_telefono"));
        cliente.setNacionalidad(rs.getString("nacionalidad"));
        cliente.setEsVip(rs.getInt("es_vip") == 1);

        Habitacion habitacion = new Habitacion();
        habitacion.setId(rs.getInt("id_habitacion"));
        habitacion.setNumero(rs.getString("h_numero"));
        habitacion.setPrecioBase(rs.getDouble("precio_base"));

        return armarReserva(rs, cliente, habitacion);
    }

    private Reserva mapearFilaSimple(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id_cliente"));
        c.setDocumento(rs.getString("documento_cliente"));
        Habitacion h = new Habitacion();
        h.setId(rs.getInt("id_habitacion"));
        return armarReserva(rs, c, h);
    }
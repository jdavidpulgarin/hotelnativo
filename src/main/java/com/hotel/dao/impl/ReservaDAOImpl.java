package com.hotel.dao.impl;

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
            "numero_habitacion, " +        // v5: VARCHAR2(10), era id_habitacion
            "fecha_entrada, fecha_salida, estado, num_personas, precio_total";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT TO_NUMBER(REGEXP_REPLACE(r.id_reserva,'[^0-9]','')) AS id, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_cliente,'[^0-9]','')) AS id_cliente, " +
            "r.numero_habitacion, " +      // v5: VARCHAR2(10)
            "r.fecha_entrada, r.fecha_salida, r.estado, r.num_personas, r.precio_total, " +
            "c.primer_nombre c_nombre, c.apellido_1 c_apellido, c.email c_email, " +
            "c.telefono c_telefono, r.id_cliente AS documento, c.id_pais AS nacionalidad, c.fecha_registro, c.es_vip, " +
            "h.numero h_numero, h.precio_base, h.estado h_estado " +
            "FROM RESERVA r " +
            "JOIN CLIENTE c ON r.id_cliente = c.id_cliente " +
            "JOIN HABITACION h ON r.numero_habitacion = h.numero " +   // numero VARCHAR2(10)
            "WHERE r.id_reserva = ?";

    private static final String SQL_LISTAR_TODAS =
            "SELECT TO_NUMBER(REGEXP_REPLACE(r.id_reserva,'[^0-9]','')) AS id, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_cliente,'[^0-9]','')) AS id_cliente, " +
            "r.numero_habitacion, " +
            "r.fecha_entrada, r.fecha_salida, r.estado, r.num_personas, r.precio_total, " +
            "c.primer_nombre c_nombre, c.apellido_1 c_apellido, c.email c_email, " +
            "c.telefono c_telefono, r.id_cliente AS documento, c.id_pais AS nacionalidad, c.fecha_registro, c.es_vip, " +
            "h.numero h_numero, h.precio_base, h.estado h_estado " +
            "FROM RESERVA r " +
            "JOIN CLIENTE c ON r.id_cliente = c.id_cliente " +
            "JOIN HABITACION h ON r.numero_habitacion = h.numero " +
            "ORDER BY r.fecha_entrada DESC";

    private static final String SQL_LISTAR_PAGINADA =
            SQL_LISTAR_TODAS + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    // BUG 3 FIX: mismo OR doble que buscarPorId para cubrir "CLI##" y cédula directa
    private static final String SQL_RESERVAS_ACTIVAS_CLIENTE =
            "SELECT " + COLS_SIMPLE + " FROM RESERVA r " +
            "WHERE (r.id_cliente = ? OR r.id_cliente = ?) " +
            "AND r.estado NOT IN ('CANCELADA','COMPLETADA')";

    // v3: numero_habitacion (era id_habitacion)
    private static final String SQL_RESERVAS_SOLAPADAS =
            "SELECT " + COLS_SIMPLE + " FROM RESERVA r " +
            "WHERE r.numero_habitacion = ? " +
            "AND r.estado NOT IN ('CANCELADA','COMPLETADA') " +
            "AND r.fecha_entrada < ? AND r.fecha_salida > ?";

    private static final String COLS_JOIN =
            "TO_NUMBER(REGEXP_REPLACE(r.id_reserva,'[^0-9]','')) AS id, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_cliente,'[^0-9]','')) AS id_cliente, " +
            "r.numero_habitacion, " +
            "r.fecha_entrada, r.fecha_salida, r.estado, r.num_personas, r.precio_total, " +
            "c.primer_nombre c_nombre, c.apellido_1 c_apellido, c.email c_email, " +
            "c.telefono c_telefono, r.id_cliente AS documento, c.id_pais AS nacionalidad, c.fecha_registro, c.es_vip, " +
            "h.numero h_numero, h.precio_base, h.estado h_estado " +
            "FROM RESERVA r " +
            "JOIN CLIENTE c ON r.id_cliente = c.id_cliente " +
            "JOIN HABITACION h ON r.numero_habitacion = h.numero";

    private static final String SQL_BUSCAR_POR_RANGO_FECHAS =
            "SELECT " + COLS_JOIN +
            " WHERE r.fecha_entrada >= ? AND r.fecha_entrada <= ? ORDER BY r.fecha_entrada";

    private static final String SQL_BUSCAR_POR_ESTADO =
            "SELECT " + COLS_JOIN +
            " WHERE r.estado = ? ORDER BY r.fecha_entrada";

    private static final String SQL_ACTUALIZAR =
            "UPDATE RESERVA SET id_cliente=?, numero_habitacion=?, fecha_entrada=?, fecha_salida=?, " +
            "estado=?, num_personas=?, precio_total=? WHERE id_reserva=?";

    private static final String SQL_ELIMINAR = "DELETE FROM RESERVA WHERE id_reserva=?";

    public ReservaDAOImpl() { super(); }

    // ── Escrituras — delegan a PKG_HOTEL vía CallableStatement ──────────────

    @Override
    public Reserva insertar(Reserva reserva) {
        // PKG_HOTEL.crear_reserva verifica disponibilidad, capacidad,
        // calcula precio (con descuento VIP) e inserta en RESERVA. Todo en Oracle.
        String idCliente = reserva.getCliente().getDocumento() != null
                ? reserva.getCliente().getDocumento()
                : String.valueOf(reserva.getCliente().getId());
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call PKG_RESERVAS.crear_reserva(?, ?, ?, ?, ?, ?, ?)}")) {
            cs.setString(1, idCliente);
            cs.setString(2, reserva.getHabitacion().getNumero());
            cs.setDate(3, Date.valueOf(reserva.getFechaEntrada()));
            cs.setDate(4, Date.valueOf(reserva.getFechaSalida()));
            cs.setInt(5, reserva.getNumPersonas());
            if (reserva.getIdCanal() != null && !reserva.getIdCanal().isBlank())
                cs.setString(6, reserva.getIdCanal());
            else
                cs.setNull(6, Types.VARCHAR);
            cs.registerOutParameter(7, Types.VARCHAR); // p_id_reserva OUT → "RES001"
            cs.execute();
            String idGenerado = cs.getString(7);
            reserva.setId(Integer.parseInt(idGenerado.replaceAll("[^0-9]", "")));
            return reserva;
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al crear reserva: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public void cancelar(String idReserva, String motivo) {
        // PKG_HOTEL.cancelar actualiza RESERVA→CANCELADA y libera la habitación si aplica.
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call PKG_RESERVAS.cancelar(?, ?)}")) {
            cs.setString(1, idReserva);
            cs.setString(2, motivo);
            cs.execute();
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al cancelar reserva: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public boolean verificarDisponibilidad(String idHabitacion,
                                           LocalDate entrada, LocalDate salida) {
        // PKG_HOTEL.disponible es una FUNCIÓN Oracle → {? = call PKG_HOTEL.disponible(...)}
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{? = call PKG_RESERVAS.disponible(?, ?, ?)}")) {
            cs.registerOutParameter(1, Types.NUMERIC); // 1=disponible, 0=no disponible
            cs.setString(2, idHabitacion);
            cs.setDate(3, Date.valueOf(entrada));
            cs.setDate(4, Date.valueOf(salida));
            cs.execute();
            return cs.getInt(1) == 1;
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al verificar disponibilidad: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public double calcularPrecio(String idHabitacion, LocalDate entrada,
                                 LocalDate salida, boolean esVip) {
        // PKG_HOTEL.precio_reserva es una FUNCIÓN Oracle que aplica descuento VIP
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{? = call PKG_RESERVAS.precio_reserva(?, ?, ?, ?)}")) {
            cs.registerOutParameter(1, Types.NUMERIC);
            cs.setString(2, idHabitacion);
            cs.setDate(3, Date.valueOf(entrada));
            cs.setDate(4, Date.valueOf(salida));
            cs.setInt(5, esVip ? 1 : 0);
            cs.execute();
            return cs.getDouble(1);
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al calcular precio: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public boolean actualizar(Reserva reserva) {
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR)) {
                String idClienteStr = reserva.getCliente().getDocumento() != null
                        ? reserva.getCliente().getDocumento()
                        : String.valueOf(reserva.getCliente().getId());
                stmt.setString(1, idClienteStr);
                stmt.setString(2, reserva.getHabitacion().getNumero()); // v3: numero es PK
                stmt.setDate(3, Date.valueOf(reserva.getFechaEntrada()));
                stmt.setDate(4, Date.valueOf(reserva.getFechaSalida()));
                stmt.setString(5, reserva.getEstado().name());
                stmt.setInt(6, reserva.getNumPersonas());
                stmt.setDouble(7, reserva.getPrecioTotal());
                stmt.setString(8, fmt3("RES", reserva.getId()));
                return stmt.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean eliminar(int idReserva) {
        return enTransaccion(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ELIMINAR)) {
                stmt.setString(1, fmt3("RES", idReserva));
                return stmt.executeUpdate() > 0;
            }
        });
    }

    // ── Lecturas — ResultSet en try-with-resources anidado ───────────────────

    @Override
    public Optional<Reserva> buscarPorId(int idReserva) {
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {
            stmt.setString(1, fmt3("RES", idReserva));
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
    public List<Reserva> buscarReservasSolapadas(String numeroHabitacion,
                                                  LocalDate fechaEntrada,
                                                  LocalDate fechaSalida) {
        List<Reserva> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_RESERVAS_SOLAPADAS)) {
            stmt.setString(1, numeroHabitacion); // v3: numero VARCHAR2(4)
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

    // ── Mapeadores ───────────────────────────────────────────────────────────

    private Reserva mapearFilaCompleta(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente();
        cliente.setId(rs.getInt("id_cliente"));
        // BUG FIX: preserve raw VARCHAR2 id_cliente so actualizar() uses the real PK,
        // not String.valueOf(getId()) which would be "1" for "CLI01" → FK violation
        cliente.setDocumento(rs.getString("documento"));
        cliente.setNombre(rs.getString("c_nombre"));
        cliente.setApellido(rs.getString("c_apellido"));
        cliente.setEmail(rs.getString("c_email"));
        cliente.setTelefono(rs.getString("c_telefono"));
        cliente.setNacionalidad(rs.getString("nacionalidad"));
        cliente.setEsVip(rs.getInt("es_vip") == 1);

        Habitacion habitacion = new Habitacion();
        // v3: no hay id_habitacion; numero es la PK
        habitacion.setNumero(rs.getString("h_numero"));
        habitacion.setPrecioBase(rs.getDouble("precio_base"));

        return armarReserva(rs, cliente, habitacion);
    }

    private Reserva mapearFilaSimple(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id_cliente"));
        c.setDocumento(rs.getString("documento_cliente"));
        Habitacion h = new Habitacion();
        h.setNumero(rs.getString("numero_habitacion")); // v3: PK de HABITACION
        return armarReserva(rs, c, h);
    }

    private Reserva armarReserva(ResultSet rs, Cliente cliente, Habitacion habitacion)
            throws SQLException {
        Reserva r = new Reserva();
        r.setId(rs.getInt("id"));
        r.setCliente(cliente);
        r.setHabitacion(habitacion);
        r.setFechaEntrada(rs.getDate("fecha_entrada").toLocalDate());
        r.setFechaSalida(rs.getDate("fecha_salida").toLocalDate());
        r.setEstado(Reserva.EstadoReserva.valueOf(rs.getString("estado")));
        r.setNumPersonas(rs.getInt("num_personas"));
        r.setPrecioTotal(rs.getDouble("precio_total"));
        return r;
    }
}

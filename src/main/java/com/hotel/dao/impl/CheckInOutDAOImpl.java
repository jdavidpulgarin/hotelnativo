package com.hotel.dao.impl;

import com.hotel.dao.impl.BaseDAO;
import com.hotel.dao.interfaces.ICheckInOutDAO;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class CheckInOutDAOImpl extends BaseDAO implements ICheckInOutDAO {

    // Lee reservas que tienen check-in activo (sin checkout aún)
    private static final String SQL_ACTIVOS =
            "SELECT TO_NUMBER(REGEXP_REPLACE(r.id_reserva,'[^0-9]',''))  AS id_reserva, " +
            "TO_NUMBER(REGEXP_REPLACE(r.id_cliente,'[^0-9]',''))         AS id_cliente, " +
            "r.id_cliente AS documento, " +
            "r.numero_habitacion, r.fecha_entrada, r.fecha_salida, r.estado, " +
            "r.num_personas, r.precio_total, r.fecha_checkin, r.fecha_checkout, " +
            "r.id_empleado_checkin, " +
            "cl.primer_nombre cl_nombre, cl.apellido_1 cl_apellido, cl.email cl_email, " +
            "h.numero h_numero, h.precio_base, " +
            "e.primer_nombre e_nombre, e.apellido_1 e_apellido " +
            "FROM RESERVA r " +
            "JOIN CLIENTE    cl ON cl.id_cliente       = r.id_cliente " +
            "JOIN HABITACION h  ON h.numero            = r.numero_habitacion " +
            "LEFT JOIN EMPLEADO e ON e.id_empleado     = r.id_empleado_checkin";

    private static final String SQL_BUSCAR_ACTIVO_RESERVA =
            SQL_ACTIVOS + " WHERE r.id_reserva = ? AND r.fecha_checkin IS NOT NULL AND r.fecha_checkout IS NULL";

    private static final String SQL_LISTAR_TODOS =
            SQL_ACTIVOS + " WHERE r.fecha_checkin IS NOT NULL ORDER BY r.fecha_checkin DESC";

    private static final String SQL_LISTAR_PAGINADA =
            SQL_LISTAR_TODOS + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    private static final String SQL_PENDIENTES_CHECKOUT =
            SQL_ACTIVOS +
            " WHERE r.fecha_checkout IS NULL AND r.fecha_checkin IS NOT NULL " +
            "AND r.fecha_salida <= ? AND r.estado = 'EN_PROCESO'";

    public CheckInOutDAOImpl() { super(); }

    // ── Escrituras — delegadas a PKG_RESERVAS ────────────────────────────────

    @Override
    public CheckInOut insertar(CheckInOut checkInOut) {
        // PKG_RESERVAS.hacer_checkin: reserva→EN_PROCESO, hab→OCUPADA,
        // registra fecha_checkin e id_empleado_checkin en RESERVA
        String idReserva = fmt3("RES", checkInOut.getReserva().getId());
        String idEmpleado = checkInOut.getEmpleadoResponsable() != null
                ? String.valueOf(checkInOut.getEmpleadoResponsable().getId())
                : null;
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call PKG_RESERVAS.hacer_checkin(?, ?, ?)}")) {
            cs.setString(1, idReserva);
            cs.setString(2, idEmpleado);
            cs.setString(3, checkInOut.getObservaciones());
            cs.execute();
            // El id del CheckInOut es el mismo id numerico de la reserva
            checkInOut.setId(checkInOut.getReserva().getId());
            return checkInOut;
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error en check-in: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public boolean actualizar(CheckInOut checkInOut) {
        // PKG_RESERVAS.hacer_checkout: reserva→COMPLETADA, hab→LIMPIEZA, crea factura PENDIENTE
        String idReserva = fmt3("RES", checkInOut.getReserva().getId());
        String idEmpleado = checkInOut.getEmpleadoResponsable() != null
                ? String.valueOf(checkInOut.getEmpleadoResponsable().getId())
                : null;
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call PKG_RESERVAS.hacer_checkout(?, ?, ?)}")) {
            cs.setString(1, idReserva);
            cs.setString(2, idEmpleado);
            cs.setString(3, checkInOut.getObservaciones());
            cs.execute();
            return true;
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error en check-out: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    // ── Lecturas — sobre RESERVA con filtros de fecha_checkin ────────────────

    @Override
    public Optional<CheckInOut> buscarPorId(int idReserva) {
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_ACTIVO_RESERVA)) {
            stmt.setString(1, fmt3("RES", idReserva));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar check-in: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public Optional<CheckInOut> buscarCheckinActivoPorReserva(int idReserva) {
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_ACTIVO_RESERVA)) {
            stmt.setString(1, fmt3("RES", idReserva));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar check-in activo: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public List<CheckInOut> listarTodos() {
        List<CheckInOut> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR_TODOS);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) lista.add(mapearFila(rs));
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar check-ins: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    public List<CheckInOut> listarTodos(int pagina, int tamano) {
        List<CheckInOut> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR_PAGINADA)) {
            stmt.setInt(1, pagina * tamano);
            stmt.setInt(2, tamano);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar check-ins paginados: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    /** Retorna huespedes con check-in activo cuya fecha de salida ya pasó. */
    @Override
    public List<CheckInOut> buscarCheckinsActivosPendientesCheckout(LocalDate fecha) {
        List<CheckInOut> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_PENDIENTES_CHECKOUT)) {
            stmt.setDate(1, Date.valueOf(fecha));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar checkouts pendientes: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    // ── Mapeador — construye CheckInOut desde fila de RESERVA ────────────────

    private CheckInOut mapearFila(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente();
        cliente.setId(rs.getInt("id_cliente"));
        cliente.setDocumento(rs.getString("documento"));
        cliente.setNombre(rs.getString("cl_nombre"));
        cliente.setApellido(rs.getString("cl_apellido"));
        cliente.setEmail(rs.getString("cl_email"));

        Habitacion habitacion = new Habitacion();
        habitacion.setNumero(rs.getString("h_numero"));
        habitacion.setPrecioBase(rs.getDouble("precio_base"));

        Reserva reserva = new Reserva();
        reserva.setId(rs.getInt("id_reserva"));
        reserva.setCliente(cliente);
        reserva.setHabitacion(habitacion);
        reserva.setFechaEntrada(rs.getDate("fecha_entrada").toLocalDate());
        reserva.setFechaSalida(rs.getDate("fecha_salida").toLocalDate());
        reserva.setEstado(Reserva.EstadoReserva.valueOf(rs.getString("estado")));
        reserva.setNumPersonas(rs.getInt("num_personas"));
        reserva.setPrecioTotal(rs.getDouble("precio_total"));

        Empleado empleado = new Empleado();
        try {
            String idEmp = rs.getString("id_empleado_checkin");
            if (idEmp != null) {
                empleado.setId(Integer.parseInt(idEmp.replaceAll("[^0-9]", "")));
                empleado.setNombre(rs.getString("e_nombre"));
                empleado.setApellido(rs.getString("e_apellido"));
            }
        } catch (Exception ignored) {}

        CheckInOut c = new CheckInOut();
        c.setId(rs.getInt("id_reserva"));
        c.setReserva(reserva);
        c.setEmpleadoResponsable(empleado);

        Timestamp checkin = rs.getTimestamp("fecha_checkin");
        if (checkin != null) c.setFechaHoraCheckin(checkin.toLocalDateTime());

        Timestamp checkout = rs.getTimestamp("fecha_checkout");
        if (checkout != null) c.setFechaHoraCheckout(checkout.toLocalDateTime());

        return c;
    }
}

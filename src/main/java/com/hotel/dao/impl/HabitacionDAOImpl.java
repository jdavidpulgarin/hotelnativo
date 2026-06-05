package com.hotel.dao.impl;

import com.hotel.dao.interfaces.IHabitacionDAO;
import com.hotel.dto.BusquedaDisponibilidadDTO;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class HabitacionDAOImpl extends BaseDAO implements IHabitacionDAO {

    public HabitacionDAOImpl() { super(); }

    // ── Escrituras — delegan a PKG_ADMIN vía CallableStatement ───────────────

    @Override
    public Habitacion insertar(Habitacion habitacion) {
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call PKG_ADMIN.registrar_habitacion(?, ?, ?, ?, ?)}")) {
            cs.setString(1, habitacion.getNumero());
            cs.setString(2, fmt("TIP", habitacion.getTipoHabitacion().getId()));
            cs.setInt(3, habitacion.getPiso().getNumeroPiso());
            cs.setDouble(4, habitacion.getPrecioBase());
            cs.setInt(5, habitacion.getNumCamas());
            cs.execute();
            return habitacion;
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al insertar habitación: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public boolean actualizar(Habitacion habitacion) {
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call PKG_ADMIN.actualizar_habitacion(?, ?, ?, ?)}")) {
            cs.setString(1, habitacion.getNumero());
            cs.setString(2, fmt("TIP", habitacion.getTipoHabitacion().getId()));
            cs.setInt(3, habitacion.getPiso().getNumeroPiso());
            cs.setDouble(4, habitacion.getPrecioBase());
            cs.execute();
            return true;
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al actualizar habitación: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public boolean eliminar(String numero) {
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call PKG_ADMIN.eliminar_habitacion(?)}")) {
            cs.setString(1, numero);
            cs.execute();
            return true;
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al eliminar habitación: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public boolean actualizarEstado(String numero, String nuevoEstado) {
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call prc_actualizar_estado_habitacion(?, ?)}")) {
            cs.setString(1, numero);
            cs.setString(2, nuevoEstado);
            cs.execute();
            return true;
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al actualizar estado: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    // ── Lecturas — SYS_REFCURSOR: Oracle ejecuta el SELECT, Java solo lee ─────

    @Override
    public Optional<Habitacion> buscarPorNumero(String numero) {
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call prc_buscar_habitacion(?, ?)}")) {
            cs.setString(1, numero);
            cs.registerOutParameter(2, Types.REF_CURSOR);
            cs.execute();
            try (ResultSet rs = (ResultSet) cs.getObject(2)) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar habitación: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    @Override
    public List<Habitacion> listarTodas() {
        List<Habitacion> lista = new ArrayList<>();
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call prc_listar_habitaciones(?)}")) {
            cs.registerOutParameter(1, Types.REF_CURSOR);
            cs.execute();
            try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar habitaciones: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    public List<Habitacion> listarTodas(int pagina, int tamano) {
        List<Habitacion> lista = new ArrayList<>();
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call prc_listar_habitaciones_pag(?, ?, ?)}")) {
            cs.setInt(1, pagina * tamano);
            cs.setInt(2, tamano);
            cs.registerOutParameter(3, Types.REF_CURSOR);
            cs.execute();
            try (ResultSet rs = (ResultSet) cs.getObject(3)) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al listar habitaciones paginadas: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    @Override
    public List<Habitacion> buscarDisponibles(BusquedaDisponibilidadDTO criterios) {
        List<Habitacion> lista = new ArrayList<>();
        Connection conn = obtener();
        try (CallableStatement cs = conn.prepareCall(
                "{call prc_buscar_disponibles(?, ?, ?, ?)}")) {
            cs.setInt(1, criterios.getNumPersonas());
            cs.setDate(2, Date.valueOf(criterios.getFechaSalida()));
            cs.setDate(3, Date.valueOf(criterios.getFechaEntrada()));
            cs.registerOutParameter(4, Types.REF_CURSOR);
            cs.execute();
            try (ResultSet rs = (ResultSet) cs.getObject(4)) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error al buscar disponibles: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    // ── Mapeador — mismo para todos los métodos ───────────────────────────────

    private Habitacion mapearFila(ResultSet rs) throws SQLException {
        Piso piso = new Piso();
        piso.setNumeroPiso(rs.getInt("numero_piso"));

        TipoHabitacion tipo = resolverTipo(rs);

        Habitacion h = new Habitacion();
        h.setNumero(rs.getString("numero"));
        h.setTipoHabitacion(tipo);
        h.setPiso(piso);
        h.setEstado(Habitacion.EstadoHabitacion.valueOf(rs.getString("estado")));
        h.setPrecioBase(rs.getDouble("precio_base"));
        h.setNumCamas(rs.getInt("num_camas"));
        return h;
    }

    private TipoHabitacion resolverTipo(ResultSet rs) throws SQLException {
        int    idTipo = rs.getInt("id_tipo");
        String nombre = rs.getString("t_nombre");
        String desc;
        String am;
        try { desc = rs.getString("t_desc"); }   catch (SQLException ignored) { desc = null; }
        try { am   = rs.getString("amenities"); } catch (SQLException ignored) { am   = null; }
        String clave = (nombre != null) ? nombre.toUpperCase() : "";

        if (clave.contains("MULTIPLE") || clave.contains("MÚLTIPLE"))
            return new HabitacionMultiple(idTipo, nombre, desc, am);
        if (clave.contains("DOBLE"))
            return new HabitacionDoble(idTipo, nombre, desc, am);
        if (clave.contains("SUITE"))
            return new HabitacionSuite(idTipo, nombre, desc, am);
        return new HabitacionSimple(idTipo, nombre, desc, am);
    }
}

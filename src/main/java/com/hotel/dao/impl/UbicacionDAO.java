
package com.hotel.dao.impl;

import com.hotel.exception.ExcepcionBaseDatos;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class UbicacionDAO extends BaseDAO {

    public UbicacionDAO() { super(); }

    /** Retorna todos los países ordenados por nombre. */
    public List<String[]> listarPaises() {
        List<String[]> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id_pais, nombre FROM PAIS ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                lista.add(new String[]{ rs.getString(1), rs.getString(2) });
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error cargando países: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    /** Retorna los departamentos de un país, ordenados por nombre. */
    public List<String[]> listarDepartamentos(String idPais) {
        List<String[]> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id_departamento, nombre FROM DEPARTAMENTO " +
                "WHERE id_pais = ? ORDER BY nombre")) {
            ps.setString(1, idPais);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    lista.add(new String[]{ rs.getString(1), rs.getString(2) });
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error cargando departamentos: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    /** Retorna las ciudades de un departamento, ordenadas por nombre. */
    public List<String[]> listarCiudades(String idDepartamento) {
        List<String[]> lista = new ArrayList<>();
        Connection conn = obtener();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id_ciudad, nombre FROM CIUDAD " +
                "WHERE id_departamento = ? ORDER BY nombre")) {
            ps.setString(1, idDepartamento);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    lista.add(new String[]{ rs.getString(1), rs.getString(2) });
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error cargando ciudades: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
        return lista;
    }

    /**
     * Dado un id_ciudad, retorna el id_departamento al que pertenece.
     * Necesario para pre-seleccionar el departamento en modo edición.
     */
    public String buscarIdDepartamentoDeCiudad(String idCiudad) {
        Connection conn = obtener();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id_departamento FROM CIUDAD WHERE id_ciudad = ?")) {
            ps.setString(1, idCiudad);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error buscando departamento: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }

    /**
     * Dado un id_departamento, retorna el id_pais al que pertenece.
     * Necesario para pre-seleccionar el país en modo edición.
     */
    public String buscarIdPaisDeDepartamento(String idDepartamento) {
        Connection conn = obtener();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id_pais FROM DEPARTAMENTO WHERE id_departamento = ?")) {
            ps.setString(1, idDepartamento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("Error buscando país: " + e.getMessage(), e);
        } finally {
            liberar(conn);
        }
    }
}

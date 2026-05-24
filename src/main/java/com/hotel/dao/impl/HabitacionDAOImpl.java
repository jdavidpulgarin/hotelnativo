
package com.hotel.dao.impl;

/**
 *
 * @author rober
 */
import com.hotel.dao.interfaces.IHabitacionDAO;
import com.hotel.dto.BusquedaDisponibilidadDTO;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC Oracle del repositorio de habitaciones.
 * Adaptado al schema HOTELNATIVO: tablas HABITACION, TIPO_HABITACION, PISO
 * con PKs VARCHAR2 (id_habitacion, id_tipo, id_piso).
 *
 * REFACTORING v2 — 5 puntos críticos corregidos:
 *  1. Statement/ResultSet cerrados con try-with-resources anidados (sin fugas).
 *  2. Sin concatenación de Strings en SQL de usuario (todo con PreparedStatement).
 *  3. INSERT/UPDATE/DELETE usan enTransaccion() → commit/rollback garantizado.
 *  4. listarTodas(int,int) con paginación Oracle OFFSET/FETCH para evitar OOM.
 *  5. Sin printStackTrace(); los errores suben como ExcepcionBaseDatos.
 *
 * GRASP: Fabricación Pura.
 */
public class HabitacionDAOImpl extends BaseDAO implements IHabitacionDAO {

    private static final String COLS_JOIN =
            "TO_NUMBER(REGEXP_REPLACE(h.id_habitacion,'[^0-9]','')) AS id, " +
            "h.numero, h.estado, h.precio_base, h.num_camas, " +
            "TO_NUMBER(REGEXP_REPLACE(h.id_tipo,'[^0-9]','')) AS id_tipo, " +
            "TO_NUMBER(REGEXP_REPLACE(h.id_piso,'[^0-9]','')) AS id_piso, " +
            "t.nombre t_nombre, t.capacidad, t.descripcion t_desc, t.amenities, " +
            "p.numero_piso, p.descripcion p_desc";

    private static final String FROM_JOIN =
            "FROM HABITACION h " +
            "JOIN TIPO_HABITACION t ON h.id_tipo = t.id_tipo " +
            "JOIN PISO p ON h.id_piso = p.id_piso";

    private static final String SQL_INSERTAR =
            "INSERT INTO HABITACION (id_habitacion, numero, id_tipo, id_piso, estado, precio_base, num_camas) " +
            "VALUES (?, ?, ?, ?, 'DISPONIBLE', ?, ?)";

    private static final String SQL_ACTUALIZAR =
            "UPDATE HABITACION SET numero=?, id_tipo=?, id_piso=?, precio_base=? " +
            "WHERE id_habitacion=?";

    private static final String SQL_ACTUALIZAR_ESTADO =
            "UPDATE HABITACION SET estado=? WHERE id_habitacion=?";

    private static final String SQL_ELIMINAR =
            "DELETE FROM HABITACION WHERE id_habitacion=?";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT " + COLS_JOIN + " " + FROM_JOIN + " WHERE h.id_habitacion=?";

    private static final String SQL_BUSCAR_POR_NUMERO =
            "SELECT " + COLS_JOIN + " " + FROM_JOIN + " WHERE h.numero=?";

    private static final String SQL_LISTAR_TODAS =
            "SELECT " + COLS_JOIN + " " + FROM_JOIN + " ORDER BY h.numero";

    private static final String SQL_LISTAR_PAGINADA =
            SQL_LISTAR_TODAS + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    private static final String SQL_BUSCAR_DISPONIBLES =
            "SELECT " + COLS_JOIN + " " + FROM_JOIN + " " +
            "WHERE h.estado = 'DISPONIBLE' " +
            "AND t.capacidad >= ? " +
            "AND h.id_habitacion NOT IN (" +
            "   SELECT r.id_habitacion FROM RESERVA r " +
            "   WHERE r.estado NOT IN ('CANCELADA') " +
            "   AND r.fecha_entrada < ? AND r.fecha_salida > ?" +
            ") ORDER BY h.precio_base";

    public HabitacionDAOImpl() { super(); }
}

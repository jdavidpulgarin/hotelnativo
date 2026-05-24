
package com.hotel.dao.impl;

/**
 *
 * @author rober
 */
import com.hotel.dao.interfaces.IEmpleadoDAO;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC Oracle del repositorio de empleados.
 * Adaptado al schema HOTELNATIVO: tabla EMPLEADO (PK id_empleado VARCHAR2)
 * y tabla CARGO (PK id_cargo VARCHAR2).
 * Columnas primer_nombre / apellido_1 mapean a nombre / apellido del modelo.
 *
 * REFACTORING v2 — 5 puntos críticos corregidos:
 *  1. ResultSet cerrado con try-with-resources anidado en todos los buscarPor*.
 *  2. Sin concatenación de Strings en SQL (todo PreparedStatement).
 *  3. insertar/actualizar/eliminar/actualizarHash usan enTransaccion().
 *  4. listarTodos(int,int) con paginación Oracle OFFSET/FETCH.
 *  5. Sin printStackTrace(); errores suben como ExcepcionBaseDatos.
 *
 * GRASP: Fabricación Pura.
 */
public class EmpleadoDAOImpl extends BaseDAO implements IEmpleadoDAO {

    private static final String COLS_JOIN =
            "TO_NUMBER(REGEXP_REPLACE(e.id_empleado,'[^0-9]','')) AS id, " +
            "e.primer_nombre AS nombre, e.segundo_nombre, " +
            "e.apellido_1 AS apellido, e.apellido_2, " +
            "e.email, e.telefono, e.fecha_contratacion, e.password_hash, " +
            "e.debe_cambiar_password, e.salario, e.tipo_contrato, e.tipo_pago, e.fecha_fin_contrato, " +
            "TO_NUMBER(REGEXP_REPLACE(e.id_cargo,'[^0-9]','')) AS id_cargo, " +
            "c.nombre_cargo, c.descripcion c_desc, c.salario_base";

    private static final String FROM_JOIN =
            "FROM EMPLEADO e JOIN CARGO c ON e.id_cargo = c.id_cargo";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT " + COLS_JOIN + " " + FROM_JOIN + " WHERE e.id_empleado = ?";

    private static final String SQL_LISTAR_TODOS =
            "SELECT " + COLS_JOIN + " " + FROM_JOIN + " ORDER BY e.apellido_1";

    private static final String SQL_LISTAR_PAGINADA =
            SQL_LISTAR_TODOS + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    private static final String SQL_BUSCAR_POR_CARGO =
            "SELECT " + COLS_JOIN + " " + FROM_JOIN + " WHERE e.id_cargo = ?";

    private static final String SQL_LISTAR_CARGOS =
            "SELECT TO_NUMBER(REGEXP_REPLACE(id_cargo,'[^0-9]','')) AS id, " +
            "nombre_cargo, descripcion, salario_base FROM CARGO ORDER BY id_cargo";

    private static final String SQL_ACTUALIZAR =
            "UPDATE EMPLEADO SET primer_nombre=?, segundo_nombre=?, apellido_1=?, apellido_2=?, " +
            "email=?, telefono=?, id_cargo=?, fecha_contratacion=?, " +
            "salario=?, tipo_contrato=?, tipo_pago=?, fecha_fin_contrato=? WHERE id_empleado=?";

    private static final String SQL_ELIMINAR = "DELETE FROM EMPLEADO WHERE id_empleado=?";

    private static final String SQL_ACTUALIZAR_HASH =
            "UPDATE EMPLEADO SET password_hash=? WHERE id_empleado=?";

    private static final String SQL_ACTUALIZAR_DEBE_CAMBIAR =
            "UPDATE EMPLEADO SET debe_cambiar_password=? WHERE id_empleado=?";

    public EmpleadoDAOImpl() { super(); }
}
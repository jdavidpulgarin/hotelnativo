
package com.hotel.dao.impl;

/**
 *
 * @author rober
 */
import com.hotel.dao.interfaces.IAuditoriaDAO;
import com.hotel.model.Auditoria;

import java.sql.PreparedStatement;

/**
 * Implementación de {@link IAuditoriaDAO} sobre Oracle XE.
 * Utiliza {@link BaseDAO#enTransaccion} para garantizar atomicidad en cada INSERT.
 *
 * GRASP: Fabricación Pura – infraestructura de persistencia de auditoría.
 * SOLID: S – responsabilidad única: persistir eventos de auditoría.
 */
public class AuditoriaDAOImpl extends BaseDAO implements IAuditoriaDAO {

    private static final String SQL_INSERT =
            "INSERT INTO AUDITORIA (id_auditoria, id_empleado, accion, entidad, id_entidad, detalles) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
}

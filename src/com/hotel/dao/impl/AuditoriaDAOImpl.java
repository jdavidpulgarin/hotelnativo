
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
@Override
    public void insertar(Auditoria auditoria) {
        enTransaccion(conn -> {
            int seqVal = siguienteSeq(conn, "seq_auditoria");
            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
                ps.setString(1, fmt("AUD", seqVal));
                ps.setInt(2, auditoria.getIdEmpleado());
                ps.setString(3, auditoria.getAccion());
                ps.setString(4, auditoria.getEntidad());
                ps.setInt(5, auditoria.getIdEntidad());
                ps.setString(6, auditoria.getDetalles());
                ps.executeUpdate();
            }
            return null;
        });
    }
}

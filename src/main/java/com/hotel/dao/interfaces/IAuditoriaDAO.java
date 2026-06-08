
package com.hotel.dao.interfaces;

/**
 *
 * @author rober
 */
import com.hotel.model.Auditoria;

/**
 * Contrato de acceso a datos para registros de auditoría.
 *
 * SOLID: D – los servicios dependen de esta abstracción, no de la implementación concreta.
 */
public interface IAuditoriaDAO {

    /**
     * Inserta un nuevo registro de auditoría en la base de datos.
     *
     * @param auditoria registro a persistir
     */
    void insertar(Auditoria auditoria);
}

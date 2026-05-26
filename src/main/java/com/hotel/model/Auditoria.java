
package com.hotel.model;

/**
 *
 * @author rober
 */
import java.time.LocalDateTime;

/**
 * Entidad que representa un registro de auditoría del sistema.
 * Almacena qué empleado realizó qué acción sobre qué entidad y cuándo.
 *
 * GRASP: Experto en información – contiene y conoce todos los datos de un evento.
 * SOLID: S – responsabilidad única: transportar datos de auditoría.
 */
public class Auditoria {

    private int           id;
    private LocalDateTime fechaHora;
    private int           idEmpleado;
    private String        accion;
    private String        entidad;
    private int           idEntidad;
    private String        detalles;

    public Auditoria() {}

    public Auditoria(int idEmpleado, String accion, String entidad,
                     int idEntidad, String detalles) {
        this.idEmpleado = idEmpleado;
        this.accion     = accion;
        this.entidad    = entidad;
        this.idEntidad  = idEntidad;
        this.detalles   = detalles;
        this.fechaHora  = LocalDateTime.now();
    }
    
}
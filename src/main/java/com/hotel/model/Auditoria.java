
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
    public int           getId()          { return id; }
    public void          setId(int id)    { this.id = id; }

    public LocalDateTime getFechaHora()              { return fechaHora; }
    public void          setFechaHora(LocalDateTime v) { this.fechaHora = v; }

    public int  getIdEmpleado()            { return idEmpleado; }
    public void setIdEmpleado(int v)       { this.idEmpleado = v; }

    public String getAccion()              { return accion; }
    public void   setAccion(String v)      { this.accion = v; }

    public String getEntidad()             { return entidad; }
    public void   setEntidad(String v)     { this.entidad = v; }

    public int  getIdEntidad()             { return idEntidad; }
    public void setIdEntidad(int v)        { this.idEntidad = v; }

    public String getDetalles()            { return detalles; }
    public void   setDetalles(String v)    { this.detalles = v; }
}
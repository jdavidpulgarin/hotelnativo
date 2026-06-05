
package com.hotel.model;

/**
 *
 * @author rober
 */
import java.time.LocalDate;

/**
 * Entidad Mantenimiento de habitaciones.
 */
public class Mantenimiento {

    public enum TipoMantenimiento { PREVENTIVO, CORRECTIVO, EMERGENCIA }
    public enum EstadoMantenimiento { SOLICITADO, EN_PROCESO, COMPLETADO, CANCELADO }

    private int id;
    private Habitacion habitacion;
    private Empleado empleadoResponsable;
    private LocalDate fechaSolicitud;
    private LocalDate fechaRealizacion;
    private TipoMantenimiento tipo;
    private EstadoMantenimiento estado;
    private double costo;
    private String descripcionTrabajo;

    public Mantenimiento() {}

    public Mantenimiento(int id, Habitacion habitacion, Empleado empleadoResponsable,
                         LocalDate fechaSolicitud, TipoMantenimiento tipo) {
        this.id = id;
        this.habitacion = habitacion;
        this.empleadoResponsable = empleadoResponsable;
        this.fechaSolicitud = fechaSolicitud;
        this.tipo = tipo;
        this.estado = EstadoMantenimiento.SOLICITADO;
    }
    
}
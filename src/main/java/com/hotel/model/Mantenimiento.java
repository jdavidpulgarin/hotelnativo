
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
    public boolean estaCompletado() {
        return EstadoMantenimiento.COMPLETADO.equals(estado);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Habitacion getHabitacion() { return habitacion; }
    public void setHabitacion(Habitacion habitacion) { this.habitacion = habitacion; }

    public Empleado getEmpleadoResponsable() { return empleadoResponsable; }
    public void setEmpleadoResponsable(Empleado empleadoResponsable) {
        this.empleadoResponsable = empleadoResponsable;
    }

    public LocalDate getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDate fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public LocalDate getFechaRealizacion() { return fechaRealizacion; }
    public void setFechaRealizacion(LocalDate fechaRealizacion) { this.fechaRealizacion = fechaRealizacion; }

    public TipoMantenimiento getTipo() { return tipo; }
    public void setTipo(TipoMantenimiento tipo) { this.tipo = tipo; }

    public EstadoMantenimiento getEstado() { return estado; }
    public void setEstado(EstadoMantenimiento estado) { this.estado = estado; }

    public double getCosto() { return costo; }
    public void setCosto(double costo) { this.costo = costo; }

    public String getDescripcionTrabajo() { return descripcionTrabajo; }
    public void setDescripcionTrabajo(String descripcionTrabajo) { this.descripcionTrabajo = descripcionTrabajo; }
}
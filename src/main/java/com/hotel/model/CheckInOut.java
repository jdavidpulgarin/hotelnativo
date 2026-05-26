
package com.hotel.model;

/**
 *
 * @author rober
 */
import java.time.LocalDateTime;

/**
 * Entidad CheckInOut del hotel.
 */
public class CheckInOut {

    private int id;
    private Reserva reserva;
    private Empleado empleadoResponsable;
    private LocalDateTime fechaHoraCheckin;
    private LocalDateTime fechaHoraCheckout;
    private String observaciones;

    public CheckInOut() {}

    public CheckInOut(int id, Reserva reserva, Empleado empleadoResponsable,
                      LocalDateTime fechaHoraCheckin) {
        this.id = id;
        this.reserva = reserva;
        this.empleadoResponsable = empleadoResponsable;
        this.fechaHoraCheckin = fechaHoraCheckin;
    }
    /** Indica si el huésped ya realizó checkout. */
    public boolean haRealizadoCheckout() {
        return fechaHoraCheckout != null;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }

    public Empleado getEmpleadoResponsable() { return empleadoResponsable; }
    public void setEmpleadoResponsable(Empleado empleadoResponsable) {
        this.empleadoResponsable = empleadoResponsable;
    }

    public LocalDateTime getFechaHoraCheckin() { return fechaHoraCheckin; }
    public void setFechaHoraCheckin(LocalDateTime fechaHoraCheckin) {
        this.fechaHoraCheckin = fechaHoraCheckin;
    }

    public LocalDateTime getFechaHoraCheckout() { return fechaHoraCheckout; }
    public void setFechaHoraCheckout(LocalDateTime fechaHoraCheckout) {
        this.fechaHoraCheckout = fechaHoraCheckout;
    }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
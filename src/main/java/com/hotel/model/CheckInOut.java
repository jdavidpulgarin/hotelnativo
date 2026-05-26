
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
}
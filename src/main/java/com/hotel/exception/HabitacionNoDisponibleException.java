
package com.hotel.exception;

/**
 * Excepción específica cuando se intenta reservar una habitación no disponible.
 * SOLID: S - Excepción con responsabilidad única y semántica clara
 */
public class HabitacionNoDisponibleException extends ExcepcionNegocio {

    private final String numeroHabitacion;

    public HabitacionNoDisponibleException(String numeroHabitacion) {
        super("HABITACION_NO_DISPONIBLE",
              "La habitación " + numeroHabitacion + " no está disponible para las fechas seleccionadas.");
        this.numeroHabitacion = numeroHabitacion;
    }

    public String getNumeroHabitacion() {
        return numeroHabitacion;
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.exception;

/**
 *
 * @author Pulgarin
 */
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

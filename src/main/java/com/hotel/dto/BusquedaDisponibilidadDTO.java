/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.java.com.hotel.dto;

import java.time.LocalDate;

/**
 *
 * @author Pulgarin
 */
/**
 * DTO para búsqueda de habitaciones disponibles por criterios. Encapsula todos
 * los filtros de búsqueda en un único objeto.
 */
public class BusquedaDisponibilidadDTO {

    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private String tipoHabitacion;
    private int numPersonas;
    private double precioMaximo;

    public BusquedaDisponibilidadDTO() {
    }

    public BusquedaDisponibilidadDTO(LocalDate fechaEntrada, LocalDate fechaSalida, int numPersonas) {
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.numPersonas = numPersonas;
        this.precioMaximo = Double.MAX_VALUE;
    }

    public LocalDate getFechaEntrada() {
        return fechaEntrada;
    }

    public void setFechaEntrada(LocalDate fechaEntrada) {
        this.fechaEntrada = fechaEntrada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public String getTipoHabitacion() {
        return tipoHabitacion;
    }

    public void setTipoHabitacion(String tipoHabitacion) {
        this.tipoHabitacion = tipoHabitacion;
    }

    public int getNumPersonas() {
        return numPersonas;
    }

    public void setNumPersonas(int numPersonas) {
        this.numPersonas = numPersonas;
    }

    public double getPrecioMaximo() {
        return precioMaximo;
    }

    public void setPrecioMaximo(double precioMaximo) {
        this.precioMaximo = precioMaximo;
    }

}

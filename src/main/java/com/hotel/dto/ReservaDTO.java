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
 * DTO para crear una nueva reserva.
 * Transporta solo los datos necesarios desde la vista al servicio.
 */
public class ReservaDTO {
    private int idCliente;
    private int idHabitacion;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private int numPersonas;

    public ReservaDTO() {
    }

    public ReservaDTO(int idCliente, int idHabitacion, LocalDate fechaEntrada, LocalDate fechaSalida, int numPersonas) {
        this.idCliente = idCliente;
        this.idHabitacion = idHabitacion;
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.numPersonas = numPersonas;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public int getIdHabitacion() {
        return idHabitacion;
    }

    public LocalDate getFechaEntrada() {
        return fechaEntrada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public int getNumPersonas() {
        return numPersonas;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public void setIdHabitacion(int idHabitacion) {
        this.idHabitacion = idHabitacion;
    }

    public void setFechaEntrada(LocalDate fechaEntrada) {
        this.fechaEntrada = fechaEntrada;
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public void setNumPersonas(int numPersonas) {
        this.numPersonas = numPersonas;
    }
    
    
    
}

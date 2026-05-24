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

    
}

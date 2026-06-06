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
 * Excepción cuando no se encuentra una reserva por su identificador.
 */
public class ExcepcionReservaNoEncontrada extends ExcepcionNegocio{

    public ExcepcionReservaNoEncontrada(int idReserva) {
        super("RESERVA_NO_ENCONTRADA",
                "No se encontró la reserva con ID: " + idReserva);
    }
}

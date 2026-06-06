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
 * Excepción para errores de acceso a base de datos. Separa errores de
 * infraestructura de errores de negocio.
 */
public class ExcepcionBaseDatos extends RuntimeException {

    public ExcepcionBaseDatos(String mensaje) {
        super(mensaje);
    }

    public ExcepcionBaseDatos(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

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
 * Excepción base para errores de lógica de negocio. SOLID: S - Excepción
 * específica del dominio, no genérica
 */
public class ExcepcionNegocio {

    private final String codigoError;

    public ExcepcionNegocio(String mensaje) {
        super(mensaje);
        this.codigoError = "ERROR_NEGOCIO";
    }

    public ExcepcionNegocio(String codigoError, String mensaje) {
        super(mensaje);
        this.codigoError = codigoError;
    }

    public ExcepcionNegocio(String codigoError, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigoError = codigoError;
    }

    public String getCodigoError() {
        return codigoError;
    }
}


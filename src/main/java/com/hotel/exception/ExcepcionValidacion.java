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
 * Excepción para errores de validación de datos de entrada.
 */
public class ExcepcionValidacion extends ExcepcionNegocio {

    private final String campoInvalido;

    public ExcepcionValidacion(String campoInvalido, String mensaje) {
        super("ERROR_VALIDACION", mensaje);
        this.campoInvalido = campoInvalido;
    }

    public String getCampoInvalido() {
        return campoInvalido;
    }
}

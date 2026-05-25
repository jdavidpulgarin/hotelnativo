
package com.hotel.exception;

/**
 *
 * @author rober
 */
public class ExcepcionNegocio extends Exception {

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
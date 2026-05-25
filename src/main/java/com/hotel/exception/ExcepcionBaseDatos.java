
package com.hotel.exception;

/**
 *
 * @author rober
 */
/**
 * 

 * Excepción para errores de acceso a base de datos.
 * Separa errores de infraestructura de errores de negocio.
 */
public class ExcepcionBaseDatos extends RuntimeException {

    public ExcepcionBaseDatos(String mensaje) {
        super(mensaje);
    }

    public ExcepcionBaseDatos(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
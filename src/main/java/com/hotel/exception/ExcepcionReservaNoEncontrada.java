
package com.hotel.exception;

/**
 *
 * @author rober
 */

/**
 * Excepción cuando no se encuentra una reserva por su identificador.
 */
public class ExcepcionReservaNoEncontrada extends ExcepcionNegocio {

    public ExcepcionReservaNoEncontrada(int idReserva) {
        super("RESERVA_NO_ENCONTRADA",
              "No se encontró la reserva con ID: " + idReserva);
    }
}

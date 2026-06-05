package com.hotel.service;

import com.hotel.dao.interfaces.IReservaDAO;
import com.hotel.exception.ExcepcionNegocio;

import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de check-in / check-out.
 *
 * ANTES: este servicio actualizaba manualmente RESERVA, HABITACION y CHECKINOUT
 * con lógica de compensación frágil (rollback manual en Java).
 *
 * AHORA: delega completamente a PKG_HOTEL vía ReservaDAOImpl.
 * PKG_HOTEL ejecuta todo en una sola transacción Oracle:
 *   - hacer_checkin → actualiza RESERVA→EN_PROCESO, HABITACION→OCUPADA, inserta CHECKINOUT
 *   - hacer_checkout → actualiza CHECKINOUT, RESERVA→COMPLETADA, HABITACION→DISPONIBLE, inserta FACTURA
 */
public class CheckInOutService {

    private final IReservaDAO reservaDAO;

    public CheckInOutService(IReservaDAO reservaDAO) {
        this.reservaDAO = reservaDAO;
    }

    public void realizarCheckin(String idReserva, String idEmpleado,
                                String observaciones) throws ExcepcionNegocio {
        if (idReserva == null || idReserva.isBlank())
            throw new ExcepcionNegocio("ID_INVALIDO", "El ID de reserva es obligatorio.");
        if (idEmpleado == null || idEmpleado.isBlank())
            throw new ExcepcionNegocio("ID_INVALIDO", "El ID de empleado es obligatorio.");

        // PKG_HOTEL.hacer_checkin valida el estado, registra el checkin y
        // actualiza RESERVA y HABITACION en una sola transacción Oracle.
        reservaDAO.hacerCheckin(idReserva, idEmpleado, observaciones);
    }

    public void realizarCheckout(String idReserva, String idEmpleado,
                                 String metodoPago) throws ExcepcionNegocio {
        if (idReserva == null || idReserva.isBlank())
            throw new ExcepcionNegocio("ID_INVALIDO", "El ID de reserva es obligatorio.");

        // PKG_HOTEL.hacer_checkout registra fecha_checkout, libera la habitación
        // y genera la factura si no existe, todo en Oracle.
        reservaDAO.hacerCheckout(idReserva, idEmpleado, metodoPago, null);
    }

    /**
     * Checkout automático a las 11:00 AM.
     * Busca reservas EN_PROCESO cuya fecha_salida ya venció y las cierra.
     *
     * @return número de checkouts procesados
     */
    public int procesarCheckoutsAutomaticos(String idEmpleadoSistema) {
        List<String> vencidas = reservaDAO.buscarReservasConCheckinVencido(LocalDate.now());
        int procesados = 0;
        for (String idReserva : vencidas) {
            try {
                reservaDAO.hacerCheckout(idReserva, idEmpleadoSistema,
                        "MET01", "Checkout automático — 11:00 AM"); // MET01 = EFECTIVO en METODO_PAGO (v3)
                procesados++;
            } catch (Exception e) {
                System.err.println("[AUTO-CHECKOUT] Error en reserva " + idReserva
                        + ": " + e.getMessage());
            }
        }
        return procesados;
    }
}

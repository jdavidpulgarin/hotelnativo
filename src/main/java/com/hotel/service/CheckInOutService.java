package com.hotel.service;

import com.hotel.dao.interfaces.ICheckInOutDAO;
import com.hotel.dao.interfaces.IEmpleadoDAO;
import com.hotel.dao.interfaces.IReservaDAO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.*;
import com.hotel.util.ValidadorEntradas;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lógica de negocio para check-in y check-out.
 *
 * ANTES: actualizaba manualmente RESERVA, HABITACION y CHECKINOUT con
 * compensación frágil en Java (rollback manual en tres tablas separadas).
 *
 * AHORA: delega a PKG_RESERVAS vía CheckInOutDAOImpl (CallableStatement). -
 * PKG_RESERVAS.hacer_checkin → RESERVA→EN_PROCESO, HABITACION→OCUPADA -
 * PKG_RESERVAS.hacer_checkout → RESERVA→COMPLETADA, HABITACION→LIMPIEZA, INSERT
 * FACTURA PENDIENTE + DETALLE_FACTURA — todo en una transacción Oracle.
 *
 * GRASP: Alta Cohesión — responsabilidad única: gestión de entrada y salida.
 * GRASP: Controlador — coordina el caso de uso "Realizar Check-in".
 */
public class CheckInOutService {

    private final ICheckInOutDAO checkInOutDAO;
    private final IReservaDAO reservaDAO;
    private final IEmpleadoDAO empleadoDAO;

    public CheckInOutService(ICheckInOutDAO checkInOutDAO,
            IReservaDAO reservaDAO,
            IEmpleadoDAO empleadoDAO) {
        this.checkInOutDAO = checkInOutDAO;
        this.reservaDAO = reservaDAO;
        this.empleadoDAO = empleadoDAO;
    }

    /**
     * Registra el check-in delegando a PKG_HOTEL.hacer_checkin. Oracle valida
     * el estado, actualiza RESERVA y HABITACION en la misma transacción.
     */
    public CheckInOut realizarCheckin(int idReserva, int idEmpleado, String observaciones)
            throws ExcepcionNegocio {
        ValidadorEntradas.validarIdPositivo(idReserva, "reserva");
        ValidadorEntradas.validarIdPositivo(idEmpleado, "empleado");

        Reserva reserva = obtenerReservaOLanzarError(idReserva);
        Empleado empleado = obtenerEmpleadoOLanzarError(idEmpleado);

        CheckInOut registroCheckin = new CheckInOut(0, reserva, empleado, LocalDateTime.now());
        registroCheckin.setObservaciones(observaciones);

        // PKG_RESERVAS.hacer_checkin valida estado, actualiza RESERVA y HABITACION,
        // actualiza RESERVA→EN_PROCESO y HABITACION→OCUPADA.
        return checkInOutDAO.insertar(registroCheckin);
    }

    /**
     * Registra el check-out delegando a PKG_HOTEL.hacer_checkout. Oracle
     * registra fecha_checkout, libera habitación y genera la factura.
     */
    public CheckInOut realizarCheckout(int idReserva, String observaciones)
            throws ExcepcionNegocio {
        ValidadorEntradas.validarIdPositivo(idReserva, "reserva");

        CheckInOut checkinActivo = obtenerCheckinActivoOLanzarError(idReserva);
        checkinActivo.setObservaciones(observaciones);

        // PKG_HOTEL.hacer_checkout actualiza CHECKINOUT, RESERVA→COMPLETADA,
        // HABITACION→DISPONIBLE e inserta FACTURA si no existe.
        checkInOutDAO.actualizar(checkinActivo);
        return checkinActivo;
    }

    public List<CheckInOut> listarTodos() {
        return checkInOutDAO.listarTodos();
    }

    /**
     * Proceso automático de checkout a las 11:00 AM.
     *
     * @return número de checkouts procesados exitosamente
     */
    public int procesarCheckoutsAutomaticos() {
        List<CheckInOut> pendientes
                = checkInOutDAO.buscarCheckinsActivosPendientesCheckout(LocalDate.now());

        int procesados = 0;
        for (CheckInOut ci : pendientes) {
            try {
                realizarCheckout(ci.getReserva().getId(), "Checkout automático — 11:00 AM");
                procesados++;
            } catch (ExcepcionNegocio e) {
                System.err.println("[AUTO-CHECKOUT] Error en reserva "
                        + ci.getReserva().getId() + ": " + e.getMessage());
            }
        }
        return procesados;
    }

    // ── Métodos privados de apoyo ─────────────────────────────────────────────
    private Reserva obtenerReservaOLanzarError(int idReserva) throws ExcepcionNegocio {
        return reservaDAO.buscarPorId(idReserva)
                .orElseThrow(() -> new ExcepcionNegocio("RESERVA_NOT_FOUND",
                "No se encontró la reserva con ID: " + idReserva));
    }

    private Empleado obtenerEmpleadoOLanzarError(int idEmpleado) throws ExcepcionNegocio {
        return empleadoDAO.buscarPorId(idEmpleado)
                .orElseThrow(() -> new ExcepcionNegocio("EMPLEADO_NOT_FOUND",
                "No se encontró el empleado con ID: " + idEmpleado));
    }

    private CheckInOut obtenerCheckinActivoOLanzarError(int idReserva) throws ExcepcionNegocio {
        return checkInOutDAO.buscarCheckinActivoPorReserva(idReserva)
                .orElseThrow(() -> new ExcepcionNegocio("CHECKIN_NOT_FOUND",
                "No se encontró check-in activo para la reserva " + idReserva));
    }
}

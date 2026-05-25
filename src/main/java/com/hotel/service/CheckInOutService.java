/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.service;

import com.hotel.dao.interfaces.*;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.*;
import com.hotel.util.ValidadorEntradas;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author Pulgarin
 */
/**
 * Lógica de negocio para check-in y check-out.
 *
 * MEJORA 5: añadido procesarCheckoutsAutomaticos() — recorre todos los
 * check-ins activos cuya reserva venció y ejecuta el checkout registrando la
 * nota "Checkout automático 11:00 AM".
 *
 * GRASP: Alta Cohesión — responsabilidad única: gestión de entrada y salida.
 * GRASP: Controlador — coordina el caso de uso "Realizar Check-in".
 */
public class CheckInOutService {

    private final ICheckInOutDAO checkInOutDAO;
    private final IReservaDAO reservaDAO;
    private final IHabitacionDAO habitacionDAO;
    private final IEmpleadoDAO empleadoDAO;

    public CheckInOutService(ICheckInOutDAO checkInOutDAO,
            IReservaDAO reservaDAO,
            IHabitacionDAO habitacionDAO,
            IEmpleadoDAO empleadoDAO) {
        this.checkInOutDAO = checkInOutDAO;
        this.reservaDAO = reservaDAO;
        this.habitacionDAO = habitacionDAO;
        this.empleadoDAO = empleadoDAO;
    }

    /**
     * Registra el check-in de una reserva. Transiciona la habitación a OCUPADA
     * y la reserva a EN_PROCESO.
     */
    public CheckInOut realizarCheckin(int idReserva, int idEmpleado, String observaciones)
            throws ExcepcionNegocio {
        ValidadorEntradas.validarIdPositivo(idReserva, "reserva");
        ValidadorEntradas.validarIdPositivo(idEmpleado, "empleado");

        Reserva reserva = obtenerReservaConfirmadaOLanzarError(idReserva);
        Empleado empleado = obtenerEmpleadoOLanzarError(idEmpleado);

        verificarQueNoExisteCheckinActivo(idReserva);

        Habitacion habitacion = reserva.getHabitacion();
        if (habitacion == null) {
            throw new ExcepcionNegocio("HABITACION_NO_ENCONTRADA",
                    "La reserva #" + idReserva + " no tiene habitación asociada.");
        }

        habitacion.ocupar();
        habitacionDAO.actualizarEstado(habitacion.getId(),
                Habitacion.EstadoHabitacion.OCUPADA.name());

        try {
            reserva.iniciarEstadia();
            reservaDAO.actualizar(reserva);

            CheckInOut registroCheckin = new CheckInOut(0, reserva, empleado, LocalDateTime.now());
            registroCheckin.setObservaciones(observaciones);
            return checkInOutDAO.insertar(registroCheckin);
        } catch (Exception e) {
            // Compensar: revertir estado de habitación
            try {
                habitacionDAO.actualizarEstado(habitacion.getId(),
                        Habitacion.EstadoHabitacion.DISPONIBLE.name());
            } catch (Exception ex) {
                System.err.println("[CHECKIN] Error al revertir habitación: " + ex.getMessage());
            }
            if (e instanceof ExcepcionNegocio) {
                throw (ExcepcionNegocio) e;
            }
            throw new ExcepcionNegocio("ERROR_CHECKIN",
                    "Error al registrar check-in: " + e.getMessage());
        }
    }

    /**
     * Registra el check-out de una reserva activa.
     *
     * Carga la Reserva COMPLETA desde reservaDAO para evitar NPE al acceder a
     * reserva.getHabitacion() (el CheckInOut del DAO solo tiene el ID).
     */
    public CheckInOut realizarCheckout(int idReserva, String observaciones)
            throws ExcepcionNegocio {
        ValidadorEntradas.validarIdPositivo(idReserva, "reserva");

        CheckInOut checkinActivo = obtenerCheckinActivoOLanzarError(idReserva);
        Reserva reserva = obtenerReservaOLanzarError(idReserva);

        Habitacion habitacion = reserva.getHabitacion();
        if (habitacion == null) {
            throw new ExcepcionNegocio("HABITACION_NO_ENCONTRADA",
                    "La reserva #" + idReserva + " no tiene habitación asociada.");
        }

        checkinActivo.setFechaHoraCheckout(LocalDateTime.now());
        checkinActivo.setObservaciones(observaciones);
        checkInOutDAO.actualizar(checkinActivo);

        try {
            habitacion.liberar();
            habitacionDAO.actualizarEstado(habitacion.getId(),
                    Habitacion.EstadoHabitacion.DISPONIBLE.name());

            reserva.completar();
            reservaDAO.actualizar(reserva);
        } catch (Exception e) {
            // Compensar: revertir timestamp de checkout
            checkinActivo.setFechaHoraCheckout(null);
            try {
                checkInOutDAO.actualizar(checkinActivo);
            } catch (Exception ex) {
                System.err.println("[CHECKOUT] Error al revertir checkin: " + ex.getMessage());
            }
            if (e instanceof ExcepcionNegocio) {
                throw (ExcepcionNegocio) e;
            }
            throw new ExcepcionNegocio("ERROR_CHECKOUT",
                    "Error al registrar check-out: " + e.getMessage());
        }

        return checkinActivo;
    }

    /**
     * Proceso automático de checkout — MEJORA 5.
     *
     * Busca todos los check-ins activos cuya fecha de salida de reserva ya
     * venció (≤ hoy) y ejecuta el checkout con nota automática. Típicamente se
     * invoca a las 11:00 AM desde el menú de administración.
     *
     * @return número de checkouts procesados exitosamente
     */
    public List<CheckInOut> listarTodos() {
        return checkInOutDAO.listarTodos();
    }

    public int procesarCheckoutsAutomaticos() {
        LocalDate hoy = LocalDate.now();
        List<CheckInOut> pendientes
                = checkInOutDAO.buscarCheckinsActivosPendientesCheckout(hoy);

        int procesados = 0;
        for (CheckInOut ci : pendientes) {
            try {
                realizarCheckout(ci.getReserva().getId(),
                        "Checkout automático — 11:00 AM");
                procesados++;
            } catch (ExcepcionNegocio e) {
                System.err.println("[AUTO-CHECKOUT] Error en reserva "
                        + ci.getReserva().getId() + ": " + e.getMessage());
            }
        }
        return procesados;
    }

    // ── Métodos privados de apoyo ─────────────────────────────────────────────
    private Reserva obtenerReservaConfirmadaOLanzarError(int idReserva) throws ExcepcionNegocio {
        Reserva reserva = obtenerReservaOLanzarError(idReserva);
        if (!Reserva.EstadoReserva.CONFIRMADA.equals(reserva.getEstado())) {
            throw new ExcepcionNegocio("RESERVA_NO_CONFIRMADA",
                    "Solo se puede hacer check-in de reservas CONFIRMADAS. "
                    + "Estado actual: " + reserva.getEstado());
        }
        return reserva;
    }

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

    private void verificarQueNoExisteCheckinActivo(int idReserva) throws ExcepcionNegocio {
        if (checkInOutDAO.buscarCheckinActivoPorReserva(idReserva).isPresent()) {
            throw new ExcepcionNegocio("CHECKIN_DUPLICADO",
                    "La reserva " + idReserva + " ya tiene un check-in activo registrado.");
        }
    }

    private CheckInOut obtenerCheckinActivoOLanzarError(int idReserva) throws ExcepcionNegocio {
        return checkInOutDAO.buscarCheckinActivoPorReserva(idReserva)
                .orElseThrow(() -> new ExcepcionNegocio("CHECKIN_NOT_FOUND",
                "No se encontró check-in activo para la reserva " + idReserva));
    }

}

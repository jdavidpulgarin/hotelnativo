package com.hotel.service;

import com.hotel.dao.interfaces.*;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.exception.ExcepcionValidacion;
import com.hotel.model.*;
import com.hotel.util.ValidadorEntradas;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class MantenimientoService {

    private final IMantenimientoDAO mantenimientoDAO;
    private final IHabitacionDAO habitacionDAO;
    private final IEmpleadoDAO empleadoDAO;
    private final IReservaBusqueda reservaDAO;
    private final EmailService emailService;

    public MantenimientoService(IMantenimientoDAO mantenimientoDAO,
            IHabitacionDAO habitacionDAO,
            IEmpleadoDAO empleadoDAO,
            IReservaBusqueda reservaDAO,
            EmailService emailService) {
        this.mantenimientoDAO = mantenimientoDAO;
        this.habitacionDAO = habitacionDAO;
        this.empleadoDAO = empleadoDAO;
        this.reservaDAO = reservaDAO;
        this.emailService = emailService;
    }

    public Mantenimiento solicitarMantenimiento(String numeroHabitacion, int idEmpleado,
            Mantenimiento.TipoMantenimiento tipo,
            String descripcion) throws ExcepcionNegocio {
        ValidadorEntradas.validarCampoRequerido(numeroHabitacion, "habitacion");
        ValidadorEntradas.validarIdPositivo(idEmpleado, "empleado");

        Habitacion habitacion = obtenerHabitacionOLanzarError(numeroHabitacion);
        Empleado empleado = obtenerEmpleadoOLanzarError(idEmpleado);

        if (Habitacion.EstadoHabitacion.OCUPADA.equals(habitacion.getEstado())) {
            throw new ExcepcionNegocio("HABITACION_OCUPADA",
                    "No se puede programar mantenimiento: la habitacion "
                    + habitacion.getNumero() + " esta actualmente OCUPADA con un huesped.");
        }

        verificarSinReservasActivasProximas(habitacion);

        habitacion.enviarAMantenimiento();
        habitacionDAO.actualizarEstado(numeroHabitacion, Habitacion.EstadoHabitacion.MANTENIMIENTO.name());

        Mantenimiento solicitud = new Mantenimiento(0, habitacion, empleado, LocalDate.now(), tipo);
        solicitud.setDescripcionTrabajo(descripcion);
        Mantenimiento guardado = mantenimientoDAO.insertar(solicitud);

        emailService.notificarNuevoMantenimiento(empleado, habitacion, tipo, descripcion, guardado.getId());

        return guardado;
    }

    public void completarMantenimiento(int idMantenimiento, double costoFinal)
            throws ExcepcionNegocio {
        ValidadorEntradas.validarIdPositivo(idMantenimiento, "mantenimiento");
        ValidadorEntradas.validarPrecioPositivo(costoFinal, "costoFinal");

        Mantenimiento mantenimiento = obtenerMantenimientoOLanzarError(idMantenimiento);

        if (Mantenimiento.EstadoMantenimiento.COMPLETADO.equals(mantenimiento.getEstado())) {
            throw new ExcepcionNegocio("MANTENIMIENTO_YA_COMPLETADO",
                    "El mantenimiento #" + idMantenimiento + " ya fue completado previamente.");
        }

        Habitacion habitacion = mantenimiento.getHabitacion();
        if (habitacion == null) {
            throw new ExcepcionNegocio("HABITACION_NO_ENCONTRADA",
                    "El mantenimiento #" + idMantenimiento + " no tiene habitacion asociada.");
        }

        mantenimiento.setEstado(Mantenimiento.EstadoMantenimiento.COMPLETADO);
        mantenimiento.setFechaRealizacion(LocalDate.now());
        mantenimiento.setCosto(costoFinal);
        mantenimientoDAO.actualizar(mantenimiento);

        habitacionDAO.actualizarEstado(habitacion.getNumero(),
                Habitacion.EstadoHabitacion.DISPONIBLE.name());

        emailService.notificarMantenimientoCompletado(mantenimiento);
    }

    public void cancelarMantenimiento(int idMantenimiento, String motivo)
            throws ExcepcionNegocio {
        ValidadorEntradas.validarIdPositivo(idMantenimiento, "mantenimiento");

        Mantenimiento mantenimiento = obtenerMantenimientoOLanzarError(idMantenimiento);

        if (Mantenimiento.EstadoMantenimiento.COMPLETADO.equals(mantenimiento.getEstado())) {
            throw new ExcepcionNegocio("CANCELACION_NO_PERMITIDA",
                    "No se puede cancelar un mantenimiento ya completado.");
        }

        mantenimiento.setEstado(Mantenimiento.EstadoMantenimiento.CANCELADO);
        mantenimiento.setDescripcionTrabajo(
                mantenimiento.getDescripcionTrabajo() + " [CANCELADO: " + motivo + "]");
        mantenimientoDAO.actualizar(mantenimiento);

        habitacionDAO.actualizarEstado(mantenimiento.getHabitacion().getNumero(),
                Habitacion.EstadoHabitacion.DISPONIBLE.name());
    }

    private Habitacion obtenerHabitacionOLanzarError(String numero) throws ExcepcionNegocio {
        return habitacionDAO.buscarPorNumero(numero)
                .orElseThrow(() -> new ExcepcionNegocio("HABITACION_NOT_FOUND",
                "No se encontro la habitacion numero: " + numero));
    }

    private Empleado obtenerEmpleadoOLanzarError(int id) throws ExcepcionNegocio {
        return empleadoDAO.buscarPorId(id)
                .orElseThrow(() -> new ExcepcionNegocio("EMPLEADO_NOT_FOUND",
                "No se encontro el empleado con ID: " + id));
    }

    private void verificarSinReservasActivasProximas(Habitacion habitacion) throws ExcepcionNegocio {
    }

    private Mantenimiento obtenerMantenimientoOLanzarError(int id) throws ExcepcionNegocio {
        return mantenimientoDAO.buscarPorId(id)
                .orElseThrow(() -> new ExcepcionNegocio("MANTENIMIENTO_NOT_FOUND",
                "No se encontro el mantenimiento con ID: " + id));
    }

    public List<Mantenimiento> listarMantenimientosPendientes() {
        return mantenimientoDAO.listarPendientes();
    }

    public List<Mantenimiento> listarTodosLosMantenimientos() {
        return mantenimientoDAO.listarTodos();
    }

    public List<Mantenimiento> listarMantenimientosPorHabitacion(String numero)
            throws ExcepcionValidacion {
        ValidadorEntradas.validarCampoRequerido(numero, "numero de habitacion");
        return mantenimientoDAO.listarPorHabitacion(numero);
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.service;

import com.hotel.dao.interfaces.*;
import com.hotel.dto.BusquedaDisponibilidadDTO;
import com.hotel.dto.ReservaDTO;
import com.hotel.exception.*;
import com.hotel.model.*;
import com.hotel.service.strategy.Descuento;
import com.hotel.service.strategy.DescuentoVip;
import com.hotel.service.strategy.SinDescuento;
import com.hotel.util.Constantes;
import com.hotel.util.ValidadorEntradas;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Pulgarin
 */
/**
 * Lógica de negocio para gestión de reservas.
 *
 * CORRECCIONES APLICADAS:
 *
 * BUG #5 — Las reservas nacían CONFIRMADAS directamente: En el método
 * construirReserva() se llamaba reserva.confirmar() antes de persistir,
 * saltando el estado PENDIENTE. Ahora la reserva nace PENDIENTE y debe ser
 * confirmada explícitamente con confirmarReserva(id).
 *
 * BUG #4 — cancelarReserva() liberaba la habitación sin verificar estado: Si la
 * reserva era PENDIENTE o CONFIRMADA sin checkin, la habitación podía estar
 * DISPONIBLE o MANTENIMIENTO y se cambiaba incorrectamente a DISPONIBLE. Ahora
 * solo se libera si el estado de la habitación es OCUPADA.
 *
 * GRASP: Controlador - orquesta el caso de uso "Realizar Reserva". GRASP:
 * Creador - crea instancias de Reserva (tiene los datos necesarios). GRASP:
 * Alta Cohesión - solo coordina operaciones de reservas. SOLID: S -
 * responsabilidad única: lógica de reservas. SOLID: D - depende de interfaces
 * DAO, no de implementaciones concretas.
 */
public class ReservaService {

    private final IReservaDAO reservaDAO;
    private final IReservaBusqueda reservaBusqueda;
    private final IHabitacionDAO habitacionDAO;
    private final IClienteDAO clienteDAO;
    private final EmailService emailService;

    public ReservaService(IReservaDAO reservaDAO,
            IReservaBusqueda reservaBusqueda,
            IHabitacionDAO habitacionDAO,
            IClienteDAO clienteDAO,
            EmailService emailService) {
        this.reservaDAO = reservaDAO;
        this.reservaBusqueda = reservaBusqueda;
        this.habitacionDAO = habitacionDAO;
        this.clienteDAO = clienteDAO;
        this.emailService = emailService;
    }

    /**
     * Crea una nueva reserva en estado PENDIENTE aplicando todas las reglas de
     * negocio.
     *
     * CORRECCIÓN BUG #5: La reserva ya NO se confirma automáticamente. Nace en
     * estado PENDIENTE y debe confirmarse con confirmarReserva(id). Esto
     * permite el flujo correcto: PENDIENTE → CONFIRMADA → EN_PROCESO →
     * COMPLETADA.
     *
     * @param dto datos de la reserva solicitada
     * @return reserva creada y persistida en estado PENDIENTE
     * @throws ExcepcionNegocio si alguna regla de negocio no se cumple
     */
    public Reserva crearReserva(ReservaDTO dto) throws ExcepcionNegocio {
        validarDatosReserva(dto);

        Cliente cliente = obtenerClienteOLanzarError(dto.getIdCliente());
        Habitacion habitacion = obtenerHabitacionOLanzarError(dto.getNumeroHabitacion());

        verificarDisponibilidadHabitacion(habitacion, dto.getFechaEntrada(), dto.getFechaSalida());
        verificarCapacidadSuficiente(habitacion, dto.getNumPersonas());

        Descuento descuentoAplicable = resolverDescuento(cliente);
        double precioTotal = calcularPrecioTotal(habitacion, dto, descuentoAplicable);

        // CORRECCIÓN BUG #5: construirReserva ya NO llama confirmar()
        Reserva nuevaReserva = construirReserva(cliente, habitacion, dto, precioTotal);
        Reserva reservaGuardada = reservaDAO.insertar(nuevaReserva);

        System.out.println("[RESERVA] Reserva #" + reservaGuardada.getId() + " creada (PENDIENTE).");
        return reservaGuardada;
    }

    /**
     * Confirma una reserva en estado PENDIENTE y envía email de confirmación.
     *
     * CORRECCIÓN BUG #5: Este es el paso correcto para confirmar. Se eliminó la
     * auto-confirmación del constructor para que este método tenga razón de
     * existir.
     *
     * @throws ExcepcionNegocio si la reserva no está en estado PENDIENTE
     */
    public void confirmarReserva(int idReserva) throws ExcepcionNegocio {
        Reserva reserva = obtenerReservaOLanzarError(idReserva);

        if (!Reserva.EstadoReserva.PENDIENTE.equals(reserva.getEstado())) {
            throw new ExcepcionNegocio("ESTADO_INVALIDO",
                    "Solo se pueden confirmar reservas en estado PENDIENTE. "
                    + "Estado actual: " + reserva.getEstado());
        }

        reserva.confirmar();
        reservaDAO.actualizar(reserva);

        // Marcar la habitación como RESERVADA; si falla, revertir la reserva a PENDIENTE
        Habitacion habitacion = reserva.getHabitacion();
        if (habitacion != null) {
            try {
                habitacion.reservar();
                habitacionDAO.actualizarEstado(habitacion.getNumero(),
                        Habitacion.EstadoHabitacion.RESERVADA.name());
            } catch (Exception e) {
                reserva.setEstado(Reserva.EstadoReserva.PENDIENTE);
                try {
                    reservaDAO.actualizar(reserva);
                } catch (Exception ex) {
                    System.err.println("[CONFIRMAR] Error al revertir reserva: " + ex.getMessage());
                }
                throw new ExcepcionNegocio("ERROR_CONFIRMAR",
                        "Error al marcar habitación: " + e.getMessage());
            }
        }

        // Notificar al cliente por email al confirmar (no al crear)
        emailService.notificarConfirmacionReserva(reserva.getCliente(), reserva);
        System.out.println("[RESERVA] Reserva #" + idReserva + " confirmada.");
    }

    /**
     * Cancela una reserva activa.
     *
     * CORRECCIÓN BUG #4: Solo libera la habitación si su estado actual es
     * OCUPADA. Antes, siempre se llamaba habitacion.liberar() sin verificar, lo
     * que podía cambiar MANTENIMIENTO o DISPONIBLE a DISPONIBLE
     * incorrectamente.
     *
     * @throws ExcepcionNegocio si la reserva no existe o no puede cancelarse
     */
    public void cancelarReserva(int idReserva) throws ExcepcionNegocio {
        Reserva reserva = obtenerReservaOLanzarError(idReserva);
        verificarQueReservaEsCancelable(reserva);

        reserva.cancelar();
        reservaDAO.actualizar(reserva);

        // Liberar habitación si estaba OCUPADA o RESERVADA (confirmada pero sin checkin)
        Habitacion habitacion = reserva.getHabitacion();
        Habitacion.EstadoHabitacion estadoHab = habitacion != null ? habitacion.getEstado() : null;
        boolean debeLiberar = Habitacion.EstadoHabitacion.OCUPADA.equals(estadoHab)
                || Habitacion.EstadoHabitacion.RESERVADA.equals(estadoHab);
        if (debeLiberar) {
            habitacion.liberar();
            habitacionDAO.actualizarEstado(habitacion.getNumero(),
                    Habitacion.EstadoHabitacion.DISPONIBLE.name());
            System.out.println("[RESERVA] Habitación " + habitacion.getNumero()
                    + " liberada al cancelar la reserva.");
        }

        emailService.notificarCancelacionReserva(reserva.getCliente(), reserva);
    }

    // ── Búsquedas y consultas ─────────────────────────────────────────────────
    public List<Habitacion> buscarHabitacionesDisponibles(BusquedaDisponibilidadDTO criterios)
            throws ExcepcionValidacion {
        ValidadorEntradas.validarRangoFechas(criterios.getFechaEntrada(), criterios.getFechaSalida());
        return habitacionDAO.buscarDisponibles(criterios);
    }

    public Optional<Reserva> buscarPorId(int idReserva) throws ExcepcionValidacion {
        ValidadorEntradas.validarIdPositivo(idReserva, "reserva");
        return reservaDAO.buscarPorId(idReserva);
    }

    public List<Reserva> listarTodasLasReservas() {
        return reservaDAO.listarTodas();
    }

    public List<Reserva> listarReservasActivasPorCliente(int idCliente) throws ExcepcionValidacion {
        ValidadorEntradas.validarIdPositivo(idCliente, "cliente");
        return reservaBusqueda.buscarReservasActivasPorCliente(idCliente);
    }

}

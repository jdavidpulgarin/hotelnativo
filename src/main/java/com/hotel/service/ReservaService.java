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
}

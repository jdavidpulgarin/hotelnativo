/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.service;

import com.hotel.dao.interfaces.IHabitacionDAO;
import com.hotel.dto.HabitacionDTO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.exception.ExcepcionValidacion;
import com.hotel.model.*;
import com.hotel.util.ValidadorEntradas;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author Pulgarin
 */
/**
 * Lógica de negocio para gestión de habitaciones.
 *
 * MEJORA 6: construirHabitacionDesdeDTO() actualizado para los nuevos IDs: 1 =
 * Simple, 2 = Multiple, 3 = Doble, 4 = Suite.
 *
 * GRASP: Alta Cohesión — solo maneja operaciones de habitaciones. SOLID: D —
 * depende de IHabitacionDAO, no de la implementación JDBC.
 */
public class HabitacionService {

    private final IHabitacionDAO habitacionDAO;

    public HabitacionService(IHabitacionDAO habitacionDAO) {
        this.habitacionDAO = habitacionDAO;
    }

    public Habitacion registrarHabitacion(HabitacionDTO dto) throws ExcepcionNegocio {
        validarDatosHabitacion(dto);
        verificarNumeroNoRegistrado(dto.getNumero());

        Habitacion nuevaHabitacion = construirHabitacionDesdeDTO(dto);
        return habitacionDAO.insertar(nuevaHabitacion);
    }

    /**
     * @param numero PK de la habitación en v3, ej. "101", "202"
     */
    public Habitacion actualizarHabitacion(String numero, HabitacionDTO dto)
            throws ExcepcionNegocio {
        validarDatosHabitacion(dto);
        Habitacion existente = obtenerHabitacionOLanzarError(numero);
        existente.setNumero(dto.getNumero());
        existente.setPrecioBase(dto.getPrecioBase());
        habitacionDAO.actualizar(existente);
        return existente;
    }

    /**
     * @param numero PK de la habitación en v3, ej. "101", "202"
     */
    public void eliminarHabitacion(String numero) throws ExcepcionNegocio {
        Habitacion habitacion = obtenerHabitacionOLanzarError(numero);
        if (Habitacion.EstadoHabitacion.OCUPADA.equals(habitacion.getEstado())) {
            throw new ExcepcionNegocio("HABITACION_OCUPADA",
                    "No se puede eliminar una habitación que está ocupada.");
        }
        habitacionDAO.eliminar(numero);
    }

    /**
     * GRASP: Experto en Información — Habitacion controla su propia transición
     * de estado.
     */
    public void enviarHabitacionAMantenimiento(String numero) throws ExcepcionNegocio {
        Habitacion habitacion = obtenerHabitacionOLanzarError(numero);
        if (Habitacion.EstadoHabitacion.OCUPADA.equals(habitacion.getEstado())) {
            throw new ExcepcionNegocio("HABITACION_OCUPADA",
                    "No se puede enviar a mantenimiento una habitación ocupada.");
        }
        habitacion.enviarAMantenimiento();
        habitacionDAO.actualizarEstado(numero, habitacion.getEstado().name());
    }

    /**
     * @param numero PK de la habitación en v3, ej. "101", "202"
     */
    public void devolverHabitacionAServicio(String numero) throws ExcepcionNegocio {
        Habitacion habitacion = obtenerHabitacionOLanzarError(numero);
        habitacion.liberar();
        habitacionDAO.actualizarEstado(numero, habitacion.getEstado().name());
    }

    /**
     * @param numero PK de la habitación en v3, ej. "101", "202"
     */
    public Optional<Habitacion> buscarPorId(String numero) {
        return habitacionDAO.buscarPorNumero(numero);
    }

    public List<Habitacion> listarTodasLasHabitaciones() {
        return habitacionDAO.listarTodas();
    }

    // ── métodos privados de apoyo ──────────────────────────────────────────────
    private void validarDatosHabitacion(HabitacionDTO dto) throws ExcepcionValidacion {
        ValidadorEntradas.validarCampoRequerido(dto.getNumero(), "numero");
        ValidadorEntradas.validarPrecioPositivo(dto.getPrecioBase(), "precioBase");
        ValidadorEntradas.validarIdPositivo(dto.getIdTipoHabitacion(), "tipoHabitacion");
        ValidadorEntradas.validarIdPositivo(dto.getIdPiso(), "piso");
    }

    private void verificarNumeroNoRegistrado(String numero) throws ExcepcionNegocio {
        if (habitacionDAO.buscarPorNumero(numero).isPresent()) {
            throw new ExcepcionNegocio("NUMERO_DUPLICADO",
                    "Ya existe una habitación con el número: " + numero);
        }
    }

    private Habitacion obtenerHabitacionOLanzarError(String numero) throws ExcepcionNegocio {
        return habitacionDAO.buscarPorNumero(numero)
                .orElseThrow(() -> new ExcepcionNegocio("HABITACION_NOT_FOUND",
                "No se encontró la habitación número: " + numero));
    }

    /**
     * Construye una Habitacion de referencia desde el DTO. IDs de tipo según
     * schema Hotel Nativo: 1=Simple, 2=Multiple, 3=Doble, 4=Suite.
     */
    private Habitacion construirHabitacionDesdeDTO(HabitacionDTO dto) {
        TipoHabitacion tipoRef;
        switch (dto.getIdTipoHabitacion()) {
            case 1:
                tipoRef = new HabitacionSimple();
                break;
            case 2:
                tipoRef = new HabitacionMultiple();
                break;
            case 3:
                tipoRef = new HabitacionDoble();
                break;
            default:
                tipoRef = new HabitacionSuite();
                break;
        }

        Piso pisoRef = new Piso();
        pisoRef.setId(dto.getIdPiso());

        return new Habitacion(0, dto.getNumero(), tipoRef, pisoRef, dto.getPrecioBase());
    }
}

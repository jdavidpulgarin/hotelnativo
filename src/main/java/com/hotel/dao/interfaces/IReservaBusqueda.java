package com.hotel.dao.interfaces;

import com.hotel.model.Reserva;
import java.time.LocalDate;
import java.util.List;

/**
 * Contrato para consultas avanzadas de reservas.
 * SOLID: I - Segregación: solo los componentes que requieren búsquedas la implementan.
 */
public interface IReservaBusqueda {

    /**
     * Lista reservas activas de un cliente específico.
     *
     * @param idCliente identificador del cliente
     * @return reservas no canceladas ni completadas
     */
    List<Reserva> buscarReservasActivasPorCliente(int idCliente);

    /**
     * Busca reservas que se solapan con un rango de fechas para una habitación.
     * Usado para validar disponibilidad antes de crear una reserva.
     *
     * @param numeroHabitacion PK de HABITACION en v3, ej. "101", "202"
     * @param fechaEntrada     inicio del periodo solicitado
     * @param fechaSalida      fin del periodo solicitado
     * @return reservas que se solapan con el periodo dado
     */
    List<Reserva> buscarReservasSolapadas(String numeroHabitacion, LocalDate fechaEntrada, LocalDate fechaSalida);

    /**
     * Lista reservas cuyo check-in cae dentro del rango de fechas dado.
     *
     * @param fechaInicio inicio del rango
     * @param fechaFin    fin del rango
     * @return reservas dentro del rango
     */
    List<Reserva> buscarPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin);

    /**
     * Lista reservas por estado (PENDIENTE, CONFIRMADA, etc.).
     *
     * @param estado nombre del estado como String
     * @return lista de reservas con ese estado
     */
    List<Reserva> buscarPorEstado(String estado);
}
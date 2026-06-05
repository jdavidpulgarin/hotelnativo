
package com.hotel.model;

/**
 *
 * @author rober
 */
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Entidad Reserva del hotel.
 * GRASP: Experto en Información - Reserva conoce sus propias fechas y calcula duración
 */
public class Reserva {

    public enum EstadoReserva {
        PENDIENTE, CONFIRMADA, CANCELADA, COMPLETADA, EN_PROCESO
    }

    private int id;
    private Cliente cliente;
    private Habitacion habitacion;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private EstadoReserva estado;
    private int numPersonas;
    private double precioTotal;
    private String idCanal;

    public Reserva() {}

    public Reserva(int id, Cliente cliente, Habitacion habitacion,
                   LocalDate fechaEntrada, LocalDate fechaSalida, int numPersonas) {
        this.id = id;
        this.cliente = cliente;
        this.habitacion = habitacion;
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.numPersonas = numPersonas;
        this.estado = EstadoReserva.PENDIENTE;
    }
    
}
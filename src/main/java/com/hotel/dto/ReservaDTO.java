package com.hotel.dto;

import java.time.LocalDate;

/**
 * DTO para crear una nueva reserva.
 * Transporta solo los datos necesarios desde la vista al servicio.
 */
public class ReservaDTO {

    private int    idCliente;
    private String numeroHabitacion;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private int    numPersonas;
    private String idCanal;

    public ReservaDTO() {}

    public ReservaDTO(int idCliente, String numeroHabitacion,
                      LocalDate fechaEntrada, LocalDate fechaSalida,
                      int numPersonas, String idCanal) {
        this.idCliente        = idCliente;
        this.numeroHabitacion = numeroHabitacion;
        this.fechaEntrada     = fechaEntrada;
        this.fechaSalida      = fechaSalida;
        this.numPersonas      = numPersonas;
        this.idCanal          = idCanal;
    }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public String getNumeroHabitacion() { return numeroHabitacion; }
    public void setNumeroHabitacion(String numeroHabitacion) { this.numeroHabitacion = numeroHabitacion; }

    public LocalDate getFechaEntrada() { return fechaEntrada; }
    public void setFechaEntrada(LocalDate fechaEntrada) { this.fechaEntrada = fechaEntrada; }

    public LocalDate getFechaSalida() { return fechaSalida; }
    public void setFechaSalida(LocalDate fechaSalida) { this.fechaSalida = fechaSalida; }

    public int getNumPersonas() { return numPersonas; }
    public void setNumPersonas(int numPersonas) { this.numPersonas = numPersonas; }

    public String getIdCanal() { return idCanal; }
    public void setIdCanal(String idCanal) { this.idCanal = idCanal; }
}

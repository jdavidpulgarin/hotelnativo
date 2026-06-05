
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
    
    public long calcularTotalDiasReserva() {
        if (fechaEntrada == null || fechaSalida == null) return 0;
        return ChronoUnit.DAYS.between(fechaEntrada, fechaSalida);
    }

    /**
     * Verifica si la reserva sigue activa (no cancelada ni completada).
     */
    public boolean estaActiva() {
        return EstadoReserva.PENDIENTE.equals(estado)
                || EstadoReserva.CONFIRMADA.equals(estado)
                || EstadoReserva.EN_PROCESO.equals(estado);
    }

    public void confirmar() { this.estado = EstadoReserva.CONFIRMADA; }
    public void cancelar() { this.estado = EstadoReserva.CANCELADA; }
    public void completar() { this.estado = EstadoReserva.COMPLETADA; }
    public void iniciarEstadia() { this.estado = EstadoReserva.EN_PROCESO; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Habitacion getHabitacion() { return habitacion; }
    public void setHabitacion(Habitacion habitacion) { this.habitacion = habitacion; }

    public LocalDate getFechaEntrada() { return fechaEntrada; }
    public void setFechaEntrada(LocalDate fechaEntrada) { this.fechaEntrada = fechaEntrada; }

    public LocalDate getFechaSalida() { return fechaSalida; }
    public void setFechaSalida(LocalDate fechaSalida) { this.fechaSalida = fechaSalida; }

    public EstadoReserva getEstado() { return estado; }
    public void setEstado(EstadoReserva estado) { this.estado = estado; }

    public int getNumPersonas() { return numPersonas; }
    public void setNumPersonas(int numPersonas) { this.numPersonas = numPersonas; }

    public double getPrecioTotal() { return precioTotal; }
    public void setPrecioTotal(double precioTotal) { this.precioTotal = precioTotal; }

    public String getIdCanal() { return idCanal; }
    public void setIdCanal(String idCanal) { this.idCanal = idCanal; }

    @Override
    public String toString() {
        return String.format("Reserva[id=%d, cliente=%s, habitacion=%s, entrada=%s, salida=%s, estado=%s]",
                id,
                cliente    != null ? cliente.obtenerNombreCompleto() : "—",
                habitacion != null ? habitacion.getNumero()          : "—",
                fechaEntrada, fechaSalida, estado);
    }

}
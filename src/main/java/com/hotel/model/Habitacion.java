
package com.hotel.model;

/**
 *
 * @author rober
 */
public class Habitacion {

    public enum EstadoHabitacion {
        DISPONIBLE, RESERVADA, OCUPADA, LIMPIEZA, MANTENIMIENTO, FUERA_DE_SERVICIO
    }

    private int              id;
    private String           numero;
    private TipoHabitacion   tipoHabitacion;
    private Piso             piso;
    private EstadoHabitacion estado;
    private double           precioBase;
    private int              numCamas = 1;

    public Habitacion() {}

    public Habitacion(int id, String numero, TipoHabitacion tipoHabitacion,
                      Piso piso, double precioBase) {
        this.id             = id;
        this.numero         = numero;
        this.tipoHabitacion = tipoHabitacion;
        this.piso           = piso;
        this.precioBase     = precioBase;
        this.estado         = EstadoHabitacion.DISPONIBLE;
        this.numCamas       = 1;
    }

    public Habitacion(int id, String numero, TipoHabitacion tipoHabitacion,
                      Piso piso, double precioBase, int numCamas) {
        this(id, numero, tipoHabitacion, piso, precioBase);
        this.numCamas = numCamas;
    }
    
    public boolean estaDisponible() {
        return EstadoHabitacion.DISPONIBLE.equals(this.estado);
    }

    /** GRASP: Experto en Información — transiciona a RESERVADA al confirmar la reserva. */
    public void reservar() {
        this.estado = EstadoHabitacion.RESERVADA;
    }

    /** GRASP: Experto en Información — transiciona a estado OCUPADA. */
    public void ocupar() {
        this.estado = EstadoHabitacion.OCUPADA;
    }

    /** GRASP: Experto en Información — libera la habitación al estado DISPONIBLE. */
    public void liberar() {
        this.estado = EstadoHabitacion.DISPONIBLE;
    }

    /** GRASP: Experto en Información — envía a mantenimiento. */
    public void enviarAMantenimiento() {
        this.estado = EstadoHabitacion.MANTENIMIENTO;
    }

    /** Delega al tipo de habitación el cálculo del precio final. GRASP: Polimorfismo. */
    public double calcularPrecioFinal() {
        return tipoHabitacion.calcularPrecio(precioBase);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public TipoHabitacion getTipoHabitacion() { return tipoHabitacion; }
    public void setTipoHabitacion(TipoHabitacion tipoHabitacion) { this.tipoHabitacion = tipoHabitacion; }

    public Piso getPiso() { return piso; }
    public void setPiso(Piso piso) { this.piso = piso; }

    public EstadoHabitacion getEstado() { return estado; }
    public void setEstado(EstadoHabitacion estado) { this.estado = estado; }

    public double getPrecioBase() { return precioBase; }
    public void setPrecioBase(double precioBase) { this.precioBase = precioBase; }

    public int getNumCamas() { return numCamas; }
    public void setNumCamas(int numCamas) { this.numCamas = numCamas; }

    @Override
    public String toString() {
        return String.format("Habitacion[id=%d, numero=%s, tipo=%s, camas=%d, estado=%s, precio=%.2f]",
                id, numero, tipoHabitacion.obtenerEtiquetaTipo(),
                numCamas, estado, calcularPrecioFinal());
    }
}

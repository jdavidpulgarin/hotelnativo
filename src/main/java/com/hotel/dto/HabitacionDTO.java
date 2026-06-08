package com.hotel.dto;

/**
 * DTO para crear o actualizar una habitación.
 */
public class HabitacionDTO {

    private String numero;
    private int idTipoHabitacion;
    private int idPiso;
    private double precioBase;

    public HabitacionDTO() {}

    public HabitacionDTO(String numero, int idTipoHabitacion, int idPiso, double precioBase) {
        this.numero = numero;
        this.idTipoHabitacion = idTipoHabitacion;
        this.idPiso = idPiso;
        this.precioBase = precioBase;
    }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public int getIdTipoHabitacion() { return idTipoHabitacion; }
    public void setIdTipoHabitacion(int idTipoHabitacion) { this.idTipoHabitacion = idTipoHabitacion; }

    public int getIdPiso() { return idPiso; }
    public void setIdPiso(int idPiso) { this.idPiso = idPiso; }

    public double getPrecioBase() { return precioBase; }
    public void setPrecioBase(double precioBase) { this.precioBase = precioBase; }
}

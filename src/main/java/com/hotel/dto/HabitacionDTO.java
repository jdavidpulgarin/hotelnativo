/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.java.com.hotel.dto;

/**
 *
 * @author Pulgarin
 */
public class HabitacionDTO {

    private String numero;
    private int idTipoHabitacion;
    private int idPiso;
    private double precioBase;

    public HabitacionDTO() {
    }

    public HabitacionDTO(String numero, int idTipoHabitacion, int idPiso, double precioBase) {
        this.numero = numero;
        this.idTipoHabitacion = idTipoHabitacion;
        this.idPiso = idPiso;
        this.precioBase = precioBase;
    }

    public String getNumero() {
        return numero;
    }

    public int getIdTipoHabitacion() {
        return idTipoHabitacion;
    }

    public int getIdPiso() {
        return idPiso;
    }

    public double getPrecioBase() {
        return precioBase;
    }
    
    
}

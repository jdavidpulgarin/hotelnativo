/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.model;

/**
 *
 * @author rober
 */
public class HabitacionSimple extends TipoHabitacion {

    // Sin recargo: el precio base es el precio final
    private static final double MULTIPLICADOR_PRECIO = 1.0;

    public HabitacionSimple() {
        super(1, "Simple", 1, "Habitación individual estándar", "TV, WiFi, Baño privado");
    }

    public HabitacionSimple(int id, String nombre, String descripcion, String amenities) {
        super(id, nombre, 1, descripcion, amenities);
    }

    /** GRASP: Polimorfismo - simple no aplica recargo */
    @Override
    public double calcularPrecio(double precioBase) {
        return precioBase * MULTIPLICADOR_PRECIO;
    }

    @Override
    public String obtenerEtiquetaTipo() {
        return "SIMPLE";
    }
}

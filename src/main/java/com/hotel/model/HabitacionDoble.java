
package com.hotel.model;

/**
 * Habitación Doble — dos camas individuales, precio base definido en BD.

 */
public class HabitacionDoble extends TipoHabitacion {

    private static final double MULTIPLICADOR_PRECIO = 1.0;

    public HabitacionDoble() {
        super(3, "Doble", 2, "Habitación para dos personas", "TV, WiFi, Baño privado, Mini bar");
    }

    public HabitacionDoble(int id, String nombre, String descripcion, String amenities) {
        super(id, nombre, 2, descripcion, amenities);
    }

    @Override
    public double calcularPrecio(double precioBase) {
        return precioBase * MULTIPLICADOR_PRECIO;
    }

    @Override
    public String obtenerEtiquetaTipo() {
        return "DOBLE";
    }
}

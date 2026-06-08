
package com.hotel.model;

/**
 *
 * @author rober
 */
public class HabitacionMultiple extends TipoHabitacion {

    private static final double MULTIPLICADOR_PRECIO = 1.0;

    public HabitacionMultiple() {
        super(2, "Multiple", 4,
                "Habitación múltiple con 2 o 3 camas",
                "TV, WiFi, Baño privado, Closet amplio");
    }

    public HabitacionMultiple(int id, String nombre, String descripcion, String amenities) {
        super(id, nombre, 4, descripcion, amenities);
    }

    @Override
    public double calcularPrecio(double precioBase) {
        return precioBase * MULTIPLICADOR_PRECIO;
    }

    @Override
    public String obtenerEtiquetaTipo() {
        return "MULTIPLE";
    }
}

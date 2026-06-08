
package com.hotel.model;

/**
 *
 * @author rober
 */
public class HabitacionSuite extends TipoHabitacion {

    private static final double MULTIPLICADOR_PRECIO = 1.0;

    public HabitacionSuite() {
        super(4, "Suite", 4,
                "Suite de lujo con sala de estar",
                "TV, WiFi, Jacuzzi, Sala de estar, Mini bar, Servicio personalizado");
    }

    public HabitacionSuite(int id, String nombre, String descripcion, String amenities) {
        super(id, nombre, 4, descripcion, amenities);
    }

    @Override
    public double calcularPrecio(double precioBase) {
        return precioBase * MULTIPLICADOR_PRECIO;
    }

    @Override
    public String obtenerEtiquetaTipo() {
        return "SUITE";
    }
}
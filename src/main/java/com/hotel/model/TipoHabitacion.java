
package com.hotel.model;

/**
 *
 * @author rober
 */
public abstract class TipoHabitacion {

    private int id;
    private String nombre;
    private int capacidadMaxima;
    private String descripcion;
    private String amenities;

    protected TipoHabitacion() {}

    protected TipoHabitacion(int id, String nombre, int capacidadMaxima,
                              String descripcion, String amenities) {
        this.id = id;
        this.nombre = nombre;
        this.capacidadMaxima = capacidadMaxima;
        this.descripcion = descripcion;
        this.amenities = amenities;
    }

    /**
     
     * @param precioBase precio base de la habitación
     * @return precio ajustado según el tipo
     */
    public abstract double calcularPrecio(double precioBase);

    /**
     * @return nombre descriptivo del tipo para presentación al usuario
     */
    public abstract String obtenerEtiquetaTipo();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getCapacidadMaxima() { return capacidadMaxima; }
    public void setCapacidadMaxima(int capacidadMaxima) { this.capacidadMaxima = capacidadMaxima; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }
}
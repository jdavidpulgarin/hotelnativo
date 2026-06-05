
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
    
    
}

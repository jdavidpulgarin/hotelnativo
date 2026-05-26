
package com.hotel.model;

/**
 *
 * @author rober
 */
public class Cargo {

    private int id;
    private String nombreCargo;
    private String descripcion;
    private double salarioBase;

    public Cargo() {}

    public Cargo(int id, String nombreCargo, String descripcion, double salarioBase) {
        this.id = id;
        this.nombreCargo = nombreCargo;
        this.descripcion = descripcion;
        this.salarioBase = salarioBase;
    }
    
}
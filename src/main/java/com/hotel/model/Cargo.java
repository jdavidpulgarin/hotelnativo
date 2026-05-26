
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
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombreCargo() { return nombreCargo; }
    public void setNombreCargo(String nombreCargo) { this.nombreCargo = nombreCargo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getSalarioBase() { return salarioBase; }
    public void setSalarioBase(double salarioBase) { this.salarioBase = salarioBase; }
}
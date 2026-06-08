/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.model;

/**
 *
 * @author rober
 */
public class Piso {

    private int id;
    private int numeroPiso;
    private String descripcion;
    private int cantidadHabitaciones;

    public Piso() {}

    public Piso(int id, int numeroPiso, String descripcion, int cantidadHabitaciones) {
        this.id = id;
        this.numeroPiso = numeroPiso;
        this.descripcion = descripcion;
        this.cantidadHabitaciones = cantidadHabitaciones;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNumeroPiso() { return numeroPiso; }
    public void setNumeroPiso(int numeroPiso) { this.numeroPiso = numeroPiso; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getCantidadHabitaciones() { return cantidadHabitaciones; }
    public void setCantidadHabitaciones(int cantidadHabitaciones) {
        this.cantidadHabitaciones = cantidadHabitaciones;
    }
}
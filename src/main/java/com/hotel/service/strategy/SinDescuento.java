/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.service.strategy;

/**
 *
 * @author Pulgarin
 */
/**
 * Estrategia nula: sin descuento aplicado. Patrón Null Object: evita
 * verificaciones null en el servicio de reservas.
 */
public class SinDescuento implements Descuento {

    @Override
    public double calcularDescuento(double precioOriginal) {
        return 0.0;
    }

    @Override
    public String obtenerDescripcion() {
        return "Sin descuento";
    }
}

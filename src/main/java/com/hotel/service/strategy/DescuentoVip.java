/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.service.strategy;

import com.hotel.util.Constantes;

/**
 *
 * @author Pulgarin
 */
/**
 * Descuento para clientes VIP. SOLID - O: Extensión del contrato Descuento sin
 * modificar código existente.
 */
public class DescuentoVip implements Descuento {

    @Override
    public double calcularDescuento(double precioOriginal) {
        return precioOriginal * Constantes.DESCUENTO_CLIENTE_VIP;
    }

    @Override
    public String obtenerDescripcion() {
        return "Descuento VIP " + (int) (Constantes.DESCUENTO_CLIENTE_VIP * 100) + "%";
    }
}

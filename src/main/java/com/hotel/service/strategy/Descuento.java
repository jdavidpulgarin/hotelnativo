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
 * Contrato para estrategias de descuento.
 *
 * SOLID - O (Open/Closed): Abierto para nuevas estrategias (DescuentoGrupo,
 * DescuentoPromocion...) sin modificar código existente.
 *
 * Patrón Strategy: permite intercambiar el algoritmo de descuento en tiempo de
 * ejecución sin cambiar el servicio que lo usa.
 */
public interface Descuento {

    /**
     * Calcula el monto a descontar sobre el precio original.
     *
     * @param precioOriginal precio sin descuento
     * @return monto monetario a descontar (no el porcentaje)
     */
    double calcularDescuento(double precioOriginal);

    /**
     * Descripción legible para mostrar en la factura.
     *
     * @return etiqueta descriptiva, ej: "Descuento VIP 15%"
     */
    String obtenerDescripcion();
}

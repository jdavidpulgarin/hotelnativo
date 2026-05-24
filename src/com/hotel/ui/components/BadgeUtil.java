/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.components;

import javafx.scene.control.Label;

/**
 * Utilidad para crear badges (etiquetas de estado) visualmente diferenciados.
 *
 * El color de fondo y de texto se determina automáticamente por el estado
 * recibido. Los estados no reconocidos usan un color neutro.
 *
 * Uso típico en una celda de TableView:
 * <pre>
 *   colEstado.setCellFactory(col -> new TableCell<>() {
 *       protected void updateItem(String estado, boolean empty) {
 *           super.updateItem(estado, empty);
 *           setGraphic(empty || estado == null ? null : BadgeUtil.crearBadge(estado));
 *       }
 *   });
 * </pre>
 *
 * GRASP: Fabricación Pura – utilidad de UI sin responsabilidades de dominio.
 * SOLID: S – responsabilidad única: construir badges de estado.
 */
public final class BadgeUtil {

    private BadgeUtil() {}
}
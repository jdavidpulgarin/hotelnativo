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
    
    /**
     * Resuelve los colores [fondo, texto] para cada estado conocido.
     * Los estados no reconocidos caen en el color neutro por defecto.
     */
    private static String[] resolverColores(String estado) {
        return switch (estado) {
            case "DISPONIBLE", "COMPLETADA", "PAGADA", "COMPLETADO"
                    -> new String[]{"#dcfce7", "#15803d"};

            case "OCUPADA", "EN_PROCESO", "PENDIENTE"
                    -> new String[]{"#fef3c7", "#b45309"};

            case "CONFIRMADA"
                    -> new String[]{"#ede9fe", "#6d28d9"};

            case "CANCELADA", "ANULADA", "CANCELADO"
                    -> new String[]{"#fee2e2", "#b91c1c"};

            case "MANTENIMIENTO", "SOLICITADO", "FUERA_DE_SERVICIO"
                    -> new String[]{"#dbeafe", "#1d4ed8"};

            default -> new String[]{"#f1f5f9", "#64748b"};
        };
    }
}
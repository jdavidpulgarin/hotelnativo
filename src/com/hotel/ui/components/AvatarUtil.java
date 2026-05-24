/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.components;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Utilidad para generar avatares circulares con iniciales del usuario.
 *
 * El color se elige deterministamente a partir del hash del nombre completo,
 * garantizando que el mismo usuario siempre reciba el mismo color en todas
 * las pantallas de la aplicación.
 *
 * Uso:
 * <pre>
 *   StackPane avatar = AvatarUtil.crear("Pedro Rodriguez", 36);
 *   hboxUsuario.getChildren().add(0, avatar);
 * </pre>
 *
 * GRASP: Fabricación Pura – utilidad de UI sin responsabilidades de dominio.
 * SOLID: S – responsabilidad única: construir avatares de usuario.
 */
public final class AvatarUtil {

    // Estos colores los elegí yo mirando una paleta de Material Design
    // Mezclé azules, morados y algunos tonos más cálidos para variar
    private static final String[] COLORES = {
        "#3B82F6", "#8B5CF6", "#EC4899",
        "#F59E0B", "#10B981", "#6366F1",
        "#EF4444", "#14B8A6"
    };

    private AvatarUtil() {
        // Utility class, ni se te ocurra instanciar esto
    }
    private static String extraerIniciales(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) return "?";
        String[] partes = nombreCompleto.trim().split("\\s+");
        if (partes.length == 1) {
            // Caso: nombre simple como "Pedro" -> solo primera letra
            return String.valueOf(partes[0].charAt(0)).toUpperCase();
        }
        // Tomo primera letra del nombre y primera letra del apellido
        return (String.valueOf(partes[0].charAt(0))
              + String.valueOf(partes[1].charAt(0))).toUpperCase();
    }
    
    
}
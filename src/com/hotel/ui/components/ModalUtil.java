/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.components;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

/**
 * Utilidad para crear modales custom sobre un contenedor StackPane existente.
 *
 * El modal se compone de:
 *   - Overlay semitransparente rgba(0,0,0,0.5) que cubre toda la pantalla.
 *   - Card blanca centrada (max-width 520px, border-radius 16px, sombra).
 *   - Header con título y botón "✕".
 *   - Body con ScrollPane para el contenido.
 *
 * Animaciones: escala 0.9→1 + fade-in 200ms al abrir; fade-out 150ms al cerrar.
 *
 * GRASP: Fabricación Pura – utilidad de UI sin dominio propio.
 * SOLID: S – responsabilidad única: construir y animar modales.
 */
public final class ModalUtil {

    private ModalUtil() {}
}
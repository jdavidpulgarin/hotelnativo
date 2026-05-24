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
    
    // ── Header ────────────────────────────────────────────────────────────
        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle(
            "-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1e293b;");

        Button btnCerrar = new Button("✕");
        btnCerrar.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#64748b;" +
            "-fx-font-size:16px; -fx-cursor:hand; -fx-padding:2px 6px;");
        btnCerrar.setOnMouseEntered(e ->
            btnCerrar.setStyle(
                "-fx-background-color:#fee2e2; -fx-text-fill:#b91c1c;" +
                "-fx-font-size:16px; -fx-cursor:hand; -fx-padding:2px 6px;" +
                "-fx-background-radius:6px;"));
        btnCerrar.setOnMouseExited(e ->
            btnCerrar.setStyle(
                "-fx-background-color:transparent; -fx-text-fill:#64748b;" +
                "-fx-font-size:16px; -fx-cursor:hand; -fx-padding:2px 6px;"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(lblTitulo, spacer, btnCerrar);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 12, 0));
}
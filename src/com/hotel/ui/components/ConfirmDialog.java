/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

/**
 * Diálogo de confirmación de acción destructiva (ej: eliminar registro).
 *
 * Muestra un icono de advertencia "⚠", un título, un mensaje descriptivo
 * y dos botones: "Cancelar" y "Sí, eliminar". Usa {@link ModalUtil} para
 * la presentación y las animaciones.
 *
 * Uso típico desde un controlador:
 * <pre>
 *   ConfirmDialog.mostrar(contentArea, "Eliminar cliente",
 *       "¿Estás seguro de que deseas eliminar a Carlos Ramírez?",
 *       () -> clienteService.eliminar(id));
 * </pre>
 *
 * GRASP: Fabricación Pura – componente de UI sin dominio propio.
 * SOLID: S – responsabilidad única: confirmar acciones destructivas.
 */
public final class ConfirmDialog {

    private ConfirmDialog() {}
    
        public static void mostrar(StackPane contenedor, String titulo,
                               String mensaje, Runnable onConfirmar) {

        // Referencia para la lambda de cierre (necesaria antes de que crearModal retorne)
        StackPane[] overlayRef = new StackPane[1];

        // ── Contenido del modal ────────────────────────────────────────────
        VBox cuerpo = new VBox(14);
        cuerpo.setAlignment(Pos.TOP_CENTER);
        cuerpo.setPadding(new Insets(8, 4, 4, 4));

        Label icono = new Label("⚠");
        icono.setStyle(
            "-fx-font-size:36px;" +
            "-fx-background-color:#fef3c7;" +
            "-fx-background-radius:50%;" +
            "-fx-padding:14px 18px;" +
            "-fx-text-fill:#b45309;");

        Label lblMensaje = new Label(mensaje);
        lblMensaje.setStyle("-fx-font-size:13px; -fx-text-fill:#374151;");
        lblMensaje.setWrapText(true);
        lblMensaje.setMaxWidth(440);
        lblMensaje.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        cuerpo.getChildren().addAll(icono, lblMensaje);
    }
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.components;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Campo de formulario reutilizable con etiqueta, input y mensaje de error inline.
 *
 * Muestra un Label de título, un TextField y un Label de error (oculto por
 * defecto). El borde del input cambia visualmente según el estado de validación.
 *
 * Uso:
 * <pre>
 *   FormField campo = new FormField("NOMBRE", "Ingresa el nombre completo");
 *   campo.mostrarError("El nombre es obligatorio.");
 *   campo.limpiarError();
 *   String valor = campo.getValue();
 * </pre>
 *
 * GRASP: Fabricación Pura – componente de UI sin dominio propio.
 * SOLID: S – responsabilidad única: presentar un campo con validación visual.
 */
public class FormField extends VBox {

    private final TextField input;
    private final Label     lblError;
    
    
     /**
     * @param label       texto de la etiqueta superior (ej: "CORREO ELECTRÓNICO")
     * @param placeholder texto de ayuda dentro del campo cuando está vacío
     */
    public FormField(String label, String placeholder) {
        super(4);

        Label lblTitulo = new Label(label);
        lblTitulo.setStyle(
            "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#374151;");

        input = new TextField();
        input.setPromptText(placeholder);

        lblError = new Label();
        lblError.setStyle(
            "-fx-text-fill:#dc2626; -fx-font-size:11px; -fx-padding:2px 0 0 4px;");
        lblError.setWrapText(true);
        lblError.setVisible(false);
        lblError.setManaged(false);

        getChildren().addAll(lblTitulo, input, lblError);
    }
     /**
     * Muestra el mensaje de error y aplica el estilo de campo inválido.
     *
     * @param mensaje texto descriptivo del error de validación
     */
    public void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
        lblError.setManaged(true);
        input.getStyleClass().removeAll("field-success");
        if (!input.getStyleClass().contains("field-error")) {
            input.getStyleClass().add("field-error");
        }
    }
}
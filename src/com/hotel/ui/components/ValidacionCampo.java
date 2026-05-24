/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.components;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/**
 * Utilidad para aplicar validación en tiempo real a TextFields de JavaFX.
 * Muestra mensajes de error debajo del campo y cambia el borde a rojo
 * mientras el valor sea inválido. El usuario PUEDE seguir escribiendo,
 * no se bloquea, solo se le indica el error.
 *
 * GRASP: Fabricación Pura – utilidad de UI sin responsabilidades de dominio.
 * SOLID: S – responsabilidad única: aplicar validaciones en tiempo real.
 *
 * Uso:
 *   ValidacionCampo.aplicarSoloLetras(campoNombre, labelErrorNombre);
 *   ValidacionCampo.aplicarSoloNumeros(campoDocumento, labelErrorDoc);
 *   ValidacionCampo.aplicarEmail(campoEmail, labelErrorEmail);
 *   ValidacionCampo.aplicarMaxLength(campoNombre, 100);
 */
public final class ValidacionCampo {

    // Estos estilos los fui ajustando hasta que quedaron parejos
    private static final String ESTILO_ERROR =
        "-fx-border-color: #dc2626; -fx-border-width: 1.5px; -fx-border-radius: 8px; " +
        "-fx-background-radius: 8px; -fx-background-color: #fef2f2; -fx-padding: 8px 12px;";
    private static final String ESTILO_NORMAL =
        "-fx-border-color: #e2e8f0; -fx-border-width: 1.5px; -fx-border-radius: 8px; " +
        "-fx-background-radius: 8px; -fx-background-color: #f8fafc; -fx-padding: 8px 12px;";
    private static final String ESTILO_VALIDO =
        "-fx-border-color: #16a34a; -fx-border-width: 1.5px; -fx-border-radius: 8px; " +
        "-fx-background-radius: 8px; -fx-background-color: #f0fdf4; -fx-padding: 8px 12px;";
    private static final String ESTILO_LABEL_ERROR =
        "-fx-text-fill: #dc2626; -fx-font-size: 11px; -fx-padding: 2px 0 0 4px;";

    // Regex de letras (ASCII + latinos: áéíóúÁÉÍÓÚñÑüÜ)
    private static final String REGEX_LETRAS =
        "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]*$";

    private ValidacionCampo() {}
    
     /**
     * Solo letras y espacios. Si el usuario escribe un número o símbolo,
     * el borde se pone rojo y aparece el mensaje de error.
     */
    public static void aplicarSoloLetras(TextField campo, Label labelError) {
        labelError.setStyle(ESTILO_LABEL_ERROR);
        labelError.setVisible(false);
        labelError.setManaged(false);

        campo.textProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo == null || nuevo.isEmpty()) {
                campo.setStyle(ESTILO_NORMAL);
                ocultar(labelError); return;
            }
            if (!nuevo.matches(REGEX_LETRAS)) {
                campo.setStyle(ESTILO_ERROR);
                labelError.setText("⚠ Solo se permiten letras y espacios");
                mostrar(labelError);
            } else {
                campo.setStyle(ESTILO_VALIDO);
                ocultar(labelError);
            }
        });
    }
    
     /**
     * Solo números. Si el usuario escribe una letra, muestra error.
     */
    public static void aplicarSoloNumeros(TextField campo, Label labelError) {
        labelError.setStyle(ESTILO_LABEL_ERROR);
        labelError.setVisible(false);
        labelError.setManaged(false);

        campo.textProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo == null || nuevo.isEmpty()) {
                campo.setStyle(ESTILO_NORMAL);
                ocultar(labelError); return;
            }
            if (!nuevo.matches("^[0-9]*$")) {
                campo.setStyle(ESTILO_ERROR);
                labelError.setText("⚠ Solo se permiten números");
                mostrar(labelError);
            } else {
                campo.setStyle(ESTILO_VALIDO);
                ocultar(labelError);
            }
        });
    }

    /**
     * Solo números y hasta 2 decimales (para precios).
     */
    public static void aplicarDecimal(TextField campo, Label labelError) {
        labelError.setStyle(ESTILO_LABEL_ERROR);
        labelError.setVisible(false);
        labelError.setManaged(false);

        campo.textProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo == null || nuevo.isEmpty()) {
                campo.setStyle(ESTILO_NORMAL);
                ocultar(labelError); return;
            }
            if (!nuevo.matches("^[0-9]*(\\.[0-9]{0,2})?$")) {
                campo.setStyle(ESTILO_ERROR);
                labelError.setText("⚠ Solo números y máximo 2 decimales");
                mostrar(labelError);
            } else {
                campo.setStyle(ESTILO_VALIDO);
                ocultar(labelError);
            }
        });
    }
    
        /**
     * Valida formato de email en tiempo real.
     * Muestra error solo después de que el usuario escriba al menos un @.
     */
    public static void aplicarEmail(TextField campo, Label labelError) {
        labelError.setStyle(ESTILO_LABEL_ERROR);
        labelError.setVisible(false);
        labelError.setManaged(false);

        campo.textProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo == null || nuevo.isEmpty()) {
                campo.setStyle(ESTILO_NORMAL);
                ocultar(labelError); return;
            }
            if (nuevo.contains("@")) {
                if (!nuevo.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                    campo.setStyle(ESTILO_ERROR);
                    labelError.setText("⚠ Formato de email no válido");
                    mostrar(labelError);
                } else {
                    campo.setStyle(ESTILO_VALIDO);
                    ocultar(labelError);
                }
            } else {
                campo.setStyle(ESTILO_NORMAL);
                ocultar(labelError);
            }
        });
    }

    /**
     * Valida formato de teléfono en tiempo real.
     */
    public static void aplicarTelefono(TextField campo, Label labelError) {
        labelError.setStyle(ESTILO_LABEL_ERROR);
        labelError.setVisible(false);
        labelError.setManaged(false);

        campo.textProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo == null || nuevo.isEmpty()) {
                campo.setStyle(ESTILO_NORMAL);
                ocultar(labelError); return;
            }
            if (!nuevo.matches("^[+]?[0-9]*$")) {
                campo.setStyle(ESTILO_ERROR);
                labelError.setText("⚠ Solo números y opcionalmente + al inicio");
                mostrar(labelError);
            } else if (nuevo.replaceAll("[^0-9]", "").length() > 0
                       && nuevo.replaceAll("[^0-9]", "").length() < 7) {
                campo.setStyle(ESTILO_ERROR);
                labelError.setText("⚠ Mínimo 7 dígitos");
                mostrar(labelError);
            } else {
                campo.setStyle(ESTILO_VALIDO);
                ocultar(labelError);
            }
        });
    } 
    
      /**
     * Limita la cantidad máxima de caracteres.
     */
    public static void aplicarMaxLength(TextField campo, int max) {
        campo.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().length() > max) return null;
            return change;
        }));
    }
}
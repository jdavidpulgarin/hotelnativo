/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.components;

import com.hotel.HotelApp;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.util.Duration;

/**
 * Sistema de notificaciones tipo Toast.
 * Muestra mensajes flotantes en la esquina inferior derecha
 * con animación de entrada/salida y auto-dismiss.
 */
public class NotificationUtil {

    public enum Tipo { EXITO, ERROR, ADVERTENCIA, INFO }

    private NotificationUtil() {}
    
     /**
     * Muestra una notificación toast.
     *
     * @param mensaje texto a mostrar
     * @param tipo    EXITO, ERROR, ADVERTENCIA o INFO
     */
    public static void mostrar(String mensaje, Tipo tipo) {
        Popup popup = new Popup();
        popup.setAutoHide(false);

        // Contenedor principal
        HBox contenedor = new HBox(10);
        contenedor.setAlignment(Pos.CENTER_LEFT);
        contenedor.setPadding(new Insets(14, 18, 14, 14));
        contenedor.setMaxWidth(380);
        contenedor.setMinWidth(280);

        // Icono y colores según tipo
        String icono, estiloFondo, estiloBorde, estiloTexto;
        switch (tipo) {
            case EXITO:
                icono = "✓"; estiloFondo = "#f0fdf4"; estiloBorde = "#16a34a"; estiloTexto = "#15803d";
                break;
            case ERROR:
                icono = "✕"; estiloFondo = "#fef2f2"; estiloBorde = "#dc2626"; estiloTexto = "#b91c1c";
                break;
            case ADVERTENCIA:
                icono = "⚠"; estiloFondo = "#fffbeb"; estiloBorde = "#d97706"; estiloTexto = "#b45309";
                break;
            default:
                icono = "ℹ"; estiloFondo = "#eff6ff"; estiloBorde = "#2563eb"; estiloTexto = "#1d4ed8";
        }

        // Ícono circular
        Label iconLabel = new Label(icono);
        iconLabel.setStyle(
            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + estiloBorde + ";" +
            "-fx-background-color: " + estiloBorde + "22; -fx-background-radius: 50%;" +
            "-fx-min-width: 28px; -fx-min-height: 28px; -fx-alignment: center;"
        );

        // Texto
        Label textoLabel = new Label(mensaje);
        textoLabel.setStyle(
            "-fx-font-size: 13px; -fx-text-fill: " + estiloTexto + "; -fx-wrap-text: true;"
        );
        textoLabel.setWrapText(true);
        textoLabel.setMaxWidth(300);

        HBox.setHgrow(textoLabel, Priority.ALWAYS);
        contenedor.getChildren().addAll(iconLabel, textoLabel);

        contenedor.setStyle(
            "-fx-background-color: " + estiloFondo + ";" +
            "-fx-border-color: " + estiloBorde + ";" +
            "-fx-border-width: 0 0 0 4px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 3);"
        );

        popup.getContent().add(contenedor);
    }
}
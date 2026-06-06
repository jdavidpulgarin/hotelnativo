/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.util;

import com.hotel.exception.ExcepcionNegocio;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.exception.ExcepcionValidacion;
import com.hotel.ui.components.NotificationUtil;
import javafx.application.Platform;

/**
 *
 * @author Pulgarin
 */
/**
 * Manejador centralizado de excepciones no controladas. Intercepta errores en
 * el hilo de JavaFX y en hilos secundarios, muestra un mensaje al usuario y
 * loguea el error completo en consola.
 *
 * Registro en HotelApp.start(): Thread.setDefaultUncaughtExceptionHandler(new
 * ManejadorExcepciones());
 *
 * GRASP: Controlador – punto centralizado de manejo de errores no capturados.
 * SOLID: S – responsabilidad única: gestionar excepciones no capturadas.
 */
public class ManejadorExcepciones implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        System.err.println("[ERROR GLOBAL] Hilo: " + t.getName()
                + " | Excepción: " + e.getClass().getSimpleName());
        e.printStackTrace();

        String mensaje = clasificarError(e);

        if (Platform.isFxApplicationThread()) {
            mostrarError(mensaje, e);
        } else {
            Platform.runLater(() -> mostrarError(mensaje, e));
        }
    }

    private String clasificarError(Throwable e) {
        Throwable causa = e;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }

        if (causa instanceof ExcepcionValidacion ve) {
            return "Error de validación en '" + ve.getCampoInvalido()
                    + "': " + ve.getMessage();
        }
        if (causa instanceof ExcepcionNegocio be) {
            return "Error de negocio [" + be.getCodigoError() + "]: " + be.getMessage();
        }
        if (causa instanceof ExcepcionBaseDatos) {
            return "Error de conexión con la base de datos. Verifique que Oracle "
                    + "esté ejecutándose.\n\nDetalle: " + causa.getMessage();
        }
        if (causa instanceof java.sql.SQLException) {
            return "Error de base de datos: " + causa.getMessage()
                    + "\n\nVerifique la conexión con Oracle.";
        }
        if (causa instanceof java.net.ConnectException
                || causa instanceof java.net.SocketException) {
            return "No se pudo conectar al servidor. Verifique su conexión de red.";
        }
        if (causa instanceof NullPointerException) {
            return "Error interno: se intentó acceder a un dato que no existe."
                    + "\n\nUbicación: "
                    + (causa.getStackTrace().length > 0
                    ? causa.getStackTrace()[0].toString() : "desconocida");
        }
        if (causa instanceof NumberFormatException) {
            return "Error de formato: se ingresó texto donde se esperaba un número.";
        }
        if (causa instanceof IllegalArgumentException) {
            return "Dato inválido: " + causa.getMessage();
        }
        if (causa instanceof java.io.IOException) {
            return "Error de lectura/escritura de archivos: " + causa.getMessage();
        }
        if (causa instanceof OutOfMemoryError) {
            return "La aplicación se quedó sin memoria. Reinicie el sistema.";
        }

        return "Ha ocurrido un error inesperado.\n\n"
                + causa.getClass().getSimpleName() + ": "
                + (causa.getMessage() != null ? causa.getMessage() : "Sin detalles");
    }

    private void mostrarError(String mensaje, Throwable e) {
        try {
            NotificationUtil.error(mensaje);
        } catch (Exception fallback) {
            try {
                javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR);
                alerta.setTitle("Error");
                alerta.setHeaderText("Ha ocurrido un error");
                alerta.setContentText(mensaje);

                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                javafx.scene.control.TextArea detalles
                        = new javafx.scene.control.TextArea(sw.toString());
                detalles.setEditable(false);
                detalles.setWrapText(true);
                detalles.setMaxWidth(Double.MAX_VALUE);
                detalles.setMaxHeight(Double.MAX_VALUE);
                alerta.getDialogPane().setExpandableContent(detalles);
                alerta.showAndWait();
            } catch (Exception ignorado) {
                System.err.println("[FATAL] No se pudo mostrar el error: " + mensaje);
            }
        }
    }
}

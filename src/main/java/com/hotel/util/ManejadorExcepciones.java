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
}

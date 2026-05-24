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
}
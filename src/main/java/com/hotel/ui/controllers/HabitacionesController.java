/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.controllers;

import com.hotel.AppContext;
import com.hotel.dto.HabitacionDTO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.Habitacion;
import com.hotel.ui.components.NotificationUtil;
import com.hotel.ui.components.ValidacionCampo;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador del módulo de Habitaciones.
 * Muestra las habitaciones como tarjetas visuales con código de color por estado.
 */
public class HabitacionesController {

    @FXML private FlowPane gridHabitaciones;
    @FXML private HBox     statsRow;
    @FXML private VBox     loadingBox;

    private final AppContext ctx = AppContext.getInstance();
    private List<Habitacion> todasLasHabitaciones;
    private String filtroActual = "TODAS";
    // Contador de generación: descarta resultados de cargas anteriores (anti race-condition)
    private volatile long generacionActual = 0;

    @FXML
    public void initialize() {
        cargarDatos();
    }

    @FXML
    public void cargarDatos() {
        gridHabitaciones.setVisible(false);
        loadingBox.setVisible(true);

        final long miGeneracion = ++generacionActual;

        Thread t = new Thread(() -> {
            try {
                List<Habitacion> lista = ctx.getHabitacionService().listarTodasLasHabitaciones();
                Platform.runLater(() -> {
                    if (miGeneracion != generacionActual) return; // resultado obsoleto
                    todasLasHabitaciones = lista;
                    actualizarStats(lista);
                    renderizarGrid(lista);
                    loadingBox.setVisible(false);
                    gridHabitaciones.setVisible(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (miGeneracion != generacionActual) return;
                    NotificationUtil.error("Error: " + e.getMessage());
                    loadingBox.setVisible(false);
                });
            }
        }, "carga-habitaciones-" + miGeneracion);
        t.setDaemon(true);
        t.start();
    }
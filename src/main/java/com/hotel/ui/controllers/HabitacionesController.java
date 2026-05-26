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
     @FXML public void filtrarTodas()        { filtrarPorEstado("TODAS"); }
    @FXML public void filtrarDisponibles()  { filtrarPorEstado("DISPONIBLE"); }
    @FXML public void filtrarReservadas()   { filtrarPorEstado("RESERVADA"); }
    @FXML public void filtrarOcupadas()     { filtrarPorEstado("OCUPADA"); }
    @FXML public void filtrarMantenimiento(){ filtrarPorEstado("MANTENIMIENTO"); }

    /** Mantiene retrocompatibilidad con llamadas directas desde código. */
    @FXML
    public void filtrar() { filtrarPorEstado(filtroActual); }

    private void filtrarPorEstado(String estado) {
        if (todasLasHabitaciones == null) return;
        filtroActual = estado;
        List<Habitacion> filtradas;
        switch (filtroActual) {
            case "DISPONIBLE":
                filtradas = todasLasHabitaciones.stream()
                        .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.DISPONIBLE)
                        .collect(Collectors.toList());
                break;
            case "RESERVADA":
                filtradas = todasLasHabitaciones.stream()
                        .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.RESERVADA)
                        .collect(Collectors.toList());
                break;
            case "OCUPADA":
                filtradas = todasLasHabitaciones.stream()
                        .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.OCUPADA)
                        .collect(Collectors.toList());
                break;
            case "MANTENIMIENTO":
                filtradas = todasLasHabitaciones.stream()
                        .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.MANTENIMIENTO)
                        .collect(Collectors.toList());
                break;
            default:
                filtradas = todasLasHabitaciones;
        }
        renderizarGrid(filtradas);
    }

    @FXML
    public void nuevaHabitacion() {
        mostrarFormulario(null);
    }
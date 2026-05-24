/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.controllers;

import com.hotel.AppContext;
import com.hotel.dto.ClienteDTO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.*;
import com.hotel.ui.components.NotificationUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class CheckInOutController {

    @FXML private TableView<CheckInOut>          tabla;
    @FXML private TableColumn<CheckInOut,String> colId, colCliente, colHab;
    @FXML private TableColumn<CheckInOut,String> colEntrada, colSalida, colEmpleado;
    @FXML private TableColumn<CheckInOut,String> colHoraCheckin, colHoraCheckout, colEstado;
    @FXML private TextField                      searchField;
    @FXML private HBox                           statsRow;
    @FXML private ProgressBar                    progressBar;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppContext ctx = AppContext.getInstance();
    private ObservableList<CheckInOut> datos    = FXCollections.observableArrayList();
    private FilteredList<CheckInOut>   filtradas;
    
     @FXML
    public void initialize() {
        configurarColumnas();
        filtradas = new FilteredList<>(datos, p -> true);
        tabla.setItems(filtradas);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cargarDatos();
    }
    
       private void configurarColumnas() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(
                "#" + c.getValue().getId()));
        colCliente.setCellValueFactory(c -> {
            Reserva r = c.getValue().getReserva();
            return new SimpleStringProperty(
                    r != null && r.getCliente() != null
                            ? r.getCliente().obtenerNombreCompleto() : "—");
        });
        colHab.setCellValueFactory(c -> {
            Reserva r = c.getValue().getReserva();
            return new SimpleStringProperty(
                    r != null && r.getHabitacion() != null
                            ? r.getHabitacion().getNumero() : "—");
        });
    }
}
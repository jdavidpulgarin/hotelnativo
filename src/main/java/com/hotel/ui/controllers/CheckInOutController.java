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
       
          colEntrada.setCellValueFactory(c -> {
            Reserva r = c.getValue().getReserva();
            return new SimpleStringProperty(
                    r != null && r.getFechaEntrada() != null
                            ? r.getFechaEntrada().toString() : "—");
        });
        colSalida.setCellValueFactory(c -> {
            Reserva r = c.getValue().getReserva();
            return new SimpleStringProperty(
                    r != null && r.getFechaSalida() != null
                            ? r.getFechaSalida().toString() : "—");
        });
        colEmpleado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEmpleadoResponsable() != null
                        ? c.getValue().getEmpleadoResponsable().obtenerNombreCompleto() : "—"));
        colHoraCheckin.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaHoraCheckin() != null
                        ? c.getValue().getFechaHoraCheckin().format(FMT) : "—"));
        colHoraCheckout.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaHoraCheckout() != null
                        ? c.getValue().getFechaHoraCheckout().format(FMT) : "—"));
        
        colEstado.setCellValueFactory(c -> {
            Reserva r = c.getValue().getReserva();
            return new SimpleStringProperty(
                    r != null && r.getEstado() != null ? r.getEstado().name() : "—");
        });
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                badge.setStyle(getBadgeEstilo(item));
                setGraphic(badge); setText(null);
                setAlignment(Pos.CENTER);
            }
        });
        
         tabla.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(CheckInOut ci, boolean empty) {
                super.updateItem(ci, empty);
                if (ci == null || empty) { setStyle(""); return; }
                setStyle(ci.haRealizadoCheckout()
                        ? "-fx-background-color:#f0fdf4;"
                        : "-fx-background-color:#fffbeb;");
            }
        });
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.controllers;

import com.hotel.AppContext;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.Cargo;
import com.hotel.model.Empleado;
import com.hotel.ui.components.NotificationUtil;
import com.hotel.ui.components.ValidacionCampo;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.util.List;

public class EmpleadosController {

    @FXML private TableView<Empleado> tabla;
    @FXML private TableColumn<Empleado,String> colId, colNombre, colApellido;
    @FXML private TableColumn<Empleado,String> colEmail, colTelefono, colCargo, colFecha;
    @FXML private TextField searchField;
    @FXML private Label lblTotal;
    @FXML private ProgressBar progressBar;

    private final AppContext ctx = AppContext.getInstance();
    private ObservableList<Empleado> datos = FXCollections.observableArrayList();
    private FilteredList<Empleado> datosFiltrados;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colApellido.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getApellido()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTelefono() != null ? c.getValue().getTelefono() : ""));
        colCargo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCargo() != null ? c.getValue().getCargo().getNombreCargo() : ""));
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaContratacion() != null ?
                c.getValue().getFechaContratacion().toString() : ""));

        datosFiltrados = new FilteredList<>(datos, p -> true);
        tabla.setItems(datosFiltrados);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cargarDatos();
    }
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.controllers;

import com.hotel.AppContext;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.Empleado;
import com.hotel.model.Factura;
import com.hotel.ui.components.FacturaTermicaView;
import com.hotel.ui.components.NotificationUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class FacturacionController {

    @FXML private TableView<Factura>          tabla;
    @FXML private TableColumn<Factura,String> colId, colCliente, colReserva;
    @FXML private TableColumn<Factura,String> colFecha, colSubtotal, colImpuestos;
    @FXML private TableColumn<Factura,String> colTotal, colEstado, colMetodo;
    @FXML private TextField                   searchField;
    @FXML private Label                       lblTotal, lblPagadas;
    @FXML private ProgressBar                 progressBar;

    private final AppContext ctx = AppContext.getInstance();
    private final ObservableList<Factura> datos     = FXCollections.observableArrayList();
    private FilteredList<Factura>         filtradas;

    @FXML
    public void initialize() {
        configurarColumnas();
        filtradas = new FilteredList<>(datos, p -> true);
        tabla.setItems(filtradas);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cargarDatos();
    }
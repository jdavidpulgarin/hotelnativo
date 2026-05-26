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
     private void configurarColumnas() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(
                "#" + c.getValue().getId()));
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCliente() != null
                        ? c.getValue().getCliente().obtenerNombreCompleto() : "—"));
        colReserva.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getReserva() != null
                        ? "#" + c.getValue().getReserva().getId() : "—"));
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaEmision() != null
                        ? c.getValue().getFechaEmision().toString() : "—"));
        colSubtotal.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("$%,.0f", c.getValue().getSubtotal())));
        colImpuestos.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("$%,.0f", c.getValue().getImpuestos())));
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("$%,.0f", c.getValue().getTotal())));
        colMetodo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getMetodoPago() != null
                        ? c.getValue().getMetodoPago().name().replace("_", " ") : "—"));

        colEstado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEstadoPago() != null
                        ? c.getValue().getEstadoPago().name() : "—"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                badge.setStyle(badgeEstilo(item));
                setGraphic(badge); setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        tabla.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Factura f, boolean empty) {
                super.updateItem(f, empty);
                if (f == null || empty || f.getEstadoPago() == null) { setStyle(""); return; }
                switch (f.getEstadoPago()) {
                    case PAGADA:    setStyle("-fx-background-color:#f0fdf4;"); break;
                    case PENDIENTE: setStyle("-fx-background-color:#fefce8;"); break;
                    case ANULADA:   setStyle("-fx-background-color:#fff1f2;"); break;
                    default:        setStyle(""); break;
                }
            }
        });
    }
    

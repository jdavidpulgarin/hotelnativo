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
     @FXML
    public void cargarDatos() {
        progressBar.setVisible(true);
        new Thread(() -> {
            try {
                List<Empleado> lista = ctx.getEmpleadoService().listarTodosLosEmpleados();
                Platform.runLater(() -> {
                    datos.setAll(lista);
                    lblTotal.setText("Total: " + lista.size());
                    progressBar.setVisible(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    NotificationUtil.error("Error cargando empleados: " + e.getMessage());
                    progressBar.setVisible(false);
                });
            }
        }).start();
    }

    @FXML
    public void filtrar() {
        String texto = searchField.getText().toLowerCase().trim();
        datosFiltrados.setPredicate(e -> {
            if (texto.isEmpty()) return true;
            return e.getNombre().toLowerCase().contains(texto)
                || e.getApellido().toLowerCase().contains(texto)
                || e.getEmail().toLowerCase().contains(texto)
                || (e.getCargo() != null &&
                    e.getCargo().getNombreCargo().toLowerCase().contains(texto));
        });
    }

    @FXML public void abrirFormularioNuevo()  { mostrarFormulario(null); }
    @FXML public void abrirFormularioEditar() {
        Empleado sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { NotificationUtil.advertencia("Selecciona un empleado."); return; }
        mostrarFormulario(sel);
    }
    @FXML public void handleDobleClick(MouseEvent e) {
        if (e.getClickCount() == 2) abrirFormularioEditar();
    }
    @FXML public void eliminar() {
        Empleado sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { NotificationUtil.advertencia("Selecciona un empleado."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar");
        confirm.setHeaderText("Eliminar a " + sel.obtenerNombreCompleto() + "?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    ctx.getEmpleadoService().eliminarEmpleado(sel.getId());
                    NotificationUtil.exito("Empleado eliminado.");
                    cargarDatos();
                } catch (ExcepcionNegocio ex) {
                    NotificationUtil.error(ex.getMessage());
                }
            }
        });
    }
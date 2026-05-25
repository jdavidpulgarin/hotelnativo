/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.controllers;

import com.hotel.AppContext;
import com.hotel.dto.ClienteDTO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.Cliente;
import com.hotel.ui.components.NotificationUtil;
import com.hotel.ui.components.ValidacionCampo;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Controlador del módulo de Clientes.
 * Carga datos en hilo secundario, muestra tabla con búsqueda en tiempo real,
 * y abre formularios modales para CRUD.
 */
public class ClientesController {

    @FXML private TableView<Cliente>          tabla;
    @FXML private TableColumn<Cliente,String> colId, colNombre, colApellido;
    @FXML private TableColumn<Cliente,String> colEmail, colTelefono, colDocumento;
    @FXML private TableColumn<Cliente,String> colNacionalidad, colVip, colRegistro;
    @FXML private TextField                   searchField;
    @FXML private Label                       lblTotal, lblVip;
    @FXML private ProgressBar                 progressBar;

    private final AppContext ctx = AppContext.getInstance();
    private ObservableList<Cliente> datos = FXCollections.observableArrayList();
    private FilteredList<Cliente>   datosFiltrados;

  @FXML
    public void initialize() {
        configurarColumnas();
        datosFiltrados = new FilteredList<>(datos, p -> true);
        tabla.setItems(datosFiltrados);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cargarDatos();
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(          c -> new SimpleStringProperty(
                c.getValue().getDocumento() != null ? c.getValue().getDocumento()
                                                    : String.valueOf(c.getValue().getId())));
        colNombre.setCellValueFactory(      c -> new SimpleStringProperty(c.getValue().getNombre()));
        colApellido.setCellValueFactory(    c -> new SimpleStringProperty(c.getValue().getApellido()));
        colEmail.setCellValueFactory(       c -> new SimpleStringProperty(c.getValue().getEmail()));
        colTelefono.setCellValueFactory(    c -> new SimpleStringProperty(c.getValue().getTelefono()));
        colDocumento.setText("Ciudad Origen");
        colDocumento.setCellValueFactory(   c -> new SimpleStringProperty(
                c.getValue().getCiudadOrigen() != null ? c.getValue().getCiudadOrigen() : "—"));
        colNacionalidad.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNacionalidad()));
        colRegistro.setCellValueFactory(    c -> new SimpleStringProperty(
                c.getValue().getFechaRegistro() != null ? c.getValue().getFechaRegistro().toString() : "—"));

        // Columna VIP con badge visual — ancho mínimo para que no se trunque
        colVip.setMinWidth(82);
        colVip.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isEsVip() ? "VIP" : "—"));
        colVip.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); setStyle(""); return; }
                if ("VIP".equals(item)) {
                    Label badge = new Label("⭐  VIP");
                    badge.setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#b45309;" +
                            "-fx-background-radius:20px; -fx-padding:3px 11px;" +
                            "-fx-font-size:11px; -fx-font-weight:bold;");
                    badge.setMinWidth(64);   // evita truncación del texto dentro del badge
                    setGraphic(badge);
                    setText(null);
                    setStyle("");
                } else {
                    setGraphic(null);
                    setText("—");
                    setStyle("-fx-text-fill:#94a3b8; -fx-alignment:center;");
                }
                setAlignment(Pos.CENTER);
            }
        });

        // Alternar colores de filas
        tabla.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Cliente item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && item.isEsVip()) {
                    setStyle("-fx-background-color: #fffbeb;");
                } else {
                    setStyle("");
                }
            }
        });
    }
 @FXML
    public void cargarDatos() {
        setLoading(true);
        new Thread(() -> {
            try {
                List<Cliente> lista = ctx.getClienteService().listarTodosLosClientes();
                Platform.runLater(() -> {
                    datos.setAll(lista);
                    long vipCount = lista.stream().filter(Cliente::isEsVip).count();
                    lblTotal.setText("Total: " + lista.size());
                    lblVip.setText("VIP: " + vipCount);
                    setLoading(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    NotificationUtil.error("Error cargando clientes: " + e.getMessage());
                    setLoading(false);
                });
            }
        }).start();
    }

    @FXML
    public void filtrar() {
        String texto = searchField.getText().toLowerCase().trim();
        datosFiltrados.setPredicate(c -> {
            if (texto.isEmpty()) return true;
            return (c.getDocumento() != null && c.getDocumento().contains(texto))
                || (c.getNombre() != null && c.getNombre().toLowerCase().contains(texto))
                || (c.getApellido() != null && c.getApellido().toLowerCase().contains(texto))
                || (c.getEmail() != null && c.getEmail().toLowerCase().contains(texto))
                || (c.getCiudadOrigen() != null && c.getCiudadOrigen().toLowerCase().contains(texto))
                || (c.getTelefono() != null && c.getTelefono().contains(texto));
        });
    }

    @FXML
    public void abrirFormularioNuevo() {
        mostrarFormulario(null);
    }

    @FXML
    public void abrirFormularioEditar() {
        Cliente seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            NotificationUtil.advertencia("Selecciona un cliente para editar.");
            return;
        }
        mostrarFormulario(seleccionado);
    }

    @FXML
    public void handleDobleClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            abrirFormularioEditar();
        }
    }

    
       @FXML
    public void eliminar() {
        Cliente sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { NotificationUtil.advertencia("Selecciona un cliente."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText("¿Eliminar a " + sel.obtenerNombreCompleto() + "?");
        confirm.setContentText("Se eliminarán también sus reservas (completadas/canceladas),\n" +
                "facturas e historial de check-in.\nEsta acción no se puede deshacer.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    ctx.getClienteService().eliminarCliente(sel.getId());
                    NotificationUtil.exito("Cliente eliminado correctamente.");
                    cargarDatos();
                } catch (ExcepcionNegocio e) {
                    NotificationUtil.error(e.getMessage());
                } catch (Exception e) {
                    NotificationUtil.error("No se pudo eliminar el cliente: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    public void marcarVip() {
        Cliente sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { NotificationUtil.advertencia("Selecciona un cliente."); return; }
        try {
            ctx.getClienteService().marcarClienteComoVip(sel.getId());
            NotificationUtil.exito(sel.obtenerNombreCompleto() + " marcado como VIP ⭐");
            cargarDatos();
        } catch (ExcepcionNegocio e) {
            NotificationUtil.error(e.getMessage());
        }
    }
}
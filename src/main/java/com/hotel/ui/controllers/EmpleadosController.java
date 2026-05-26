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
     private void mostrarFormulario(Empleado editar) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editar == null ? "Nuevo Empleado" : "Editar Empleado");
        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(14);
        grid.setPadding(new Insets(20, 24, 20, 24));
        grid.setPrefWidth(460);

        TextField fNombre        = campo("Primer nombre *");
        TextField fSegundoNombre = campo("Segundo nombre");
        TextField fApellido      = campo("Primer apellido *");
        TextField fApellido2     = campo("Segundo apellido");
        TextField fEmail         = campo("Email *");
        TextField fTelefono      = campo("Telefono");
        DatePicker fFecha        = new DatePicker(LocalDate.now());
        fFecha.setPromptText("Fecha contratación");
        fFecha.setPrefWidth(170);

        // Cargos dinámicos desde la BD
        List<Cargo> cargos = ctx.getEmpleadoService().listarCargos();
        ComboBox<Cargo> fCargo = new ComboBox<>();
        fCargo.getItems().addAll(cargos);
        fCargo.setPromptText("Seleccionar cargo");
        fCargo.setCellFactory(lv -> new javafx.scene.control.ListCell<Cargo>() {
            @Override protected void updateItem(Cargo c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getNombreCargo());
            }
        });
        fCargo.setButtonCell(fCargo.getCellFactory().call(null));

        Label lblSalario = new Label("—");
        lblSalario.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#15803d;" +
                "-fx-background-color:#f0fdf4; -fx-border-color:#bbf7d0;" +
                "-fx-border-radius:6px; -fx-background-radius:6px; -fx-padding:6px 14px;");
        fCargo.valueProperty().addListener((obs, oldC, newC) -> {
            if (newC != null && newC.getSalarioBase() > 0) {
                lblSalario.setText(String.format("$%,.0f / mes", newC.getSalarioBase()));
            } else {
                lblSalario.setText("—");
            }
        });
        if (editar != null && editar.getCargo() != null) {
            fCargo.getItems().stream()
                    .filter(c -> c.getId() == editar.getCargo().getId())
                    .findFirst()
                    .ifPresent(c -> lblSalario.setText(String.format("$%,.0f / mes", c.getSalarioBase())));
        }

        TextField fPassword = campo("Contraseña inicial *");
        
 // ── Campos de contrato ────────────────────────────────────────────────
        TextField fSalario = campo("Salario (vacío = base del cargo)");
        fSalario.setPrefWidth(170);

        ComboBox<String> fTipoContrato = new ComboBox<>();
        fTipoContrato.getItems().addAll("INDEFINIDO", "TERMINO_FIJO", "PRESTACION_SERVICIOS");
        fTipoContrato.setValue("INDEFINIDO");
        fTipoContrato.setPrefWidth(170);

        ComboBox<String> fTipoPago = new ComboBox<>();
        fTipoPago.getItems().addAll("MENSUAL", "QUINCENAL", "DIARIO");
        fTipoPago.setValue("MENSUAL");
        fTipoPago.setPrefWidth(170);

        Label lblFechaFin = lab("Fecha fin contrato:");
        DatePicker fFechaFin = new DatePicker();
        fFechaFin.setPromptText("dd/mm/aaaa");
        fFechaFin.setPrefWidth(170);
        lblFechaFin.setVisible(false);  lblFechaFin.setManaged(false);
        fFechaFin.setVisible(false);    fFechaFin.setManaged(false);
        fTipoContrato.valueProperty().addListener((obs, oldV, newV) -> {
            boolean esFijo = "TERMINO_FIJO".equals(newV);
            lblFechaFin.setVisible(esFijo); lblFechaFin.setManaged(esFijo);
            fFechaFin.setVisible(esFijo);   fFechaFin.setManaged(esFijo);
        });
        
  Label errNombreE  = new Label(); Label errApellidoE = new Label();
        Label errEmailE   = new Label(); Label errTelefonoE = new Label();
        ValidacionCampo.aplicarSoloLetras(fNombre,   errNombreE);
        ValidacionCampo.aplicarSoloLetras(fApellido, errApellidoE);
        ValidacionCampo.aplicarEmail(fEmail,         errEmailE);
        ValidacionCampo.aplicarTelefono(fTelefono,   errTelefonoE);

        if (editar != null) {
            fNombre.setText(editar.getNombre());
            fSegundoNombre.setText(editar.getSegundoNombre() != null ? editar.getSegundoNombre() : "");
            fApellido.setText(editar.getApellido());
            fApellido2.setText(editar.getApellido2() != null ? editar.getApellido2() : "");
            fEmail.setText(editar.getEmail());
            fTelefono.setText(editar.getTelefono());
            fFecha.setValue(editar.getFechaContratacion() != null
                    ? editar.getFechaContratacion() : LocalDate.now());
            if (editar.getCargo() != null) {
                int idCargoEditar = editar.getCargo().getId();
                fCargo.getItems().stream()
                        .filter(c -> c.getId() == idCargoEditar)
                        .findFirst()
                        .ifPresent(fCargo::setValue);
            }
            if (editar.getSalario() > 0)
                fSalario.setText(String.format("%.0f", editar.getSalario()));
            if (editar.getTipoContrato() != null)  fTipoContrato.setValue(editar.getTipoContrato());
            if (editar.getTipoPago() != null)       fTipoPago.setValue(editar.getTipoPago());
            if (editar.getFechaFinContrato() != null) fFechaFin.setValue(editar.getFechaFinContrato());
        }

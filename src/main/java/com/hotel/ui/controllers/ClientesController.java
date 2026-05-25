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
    
       // ── Formulario modal ──────────────────────────────────────────────────────

    private void mostrarFormulario(Cliente clienteEditar) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(clienteEditar == null ? "Nuevo Cliente" : "Editar Cliente");
        dialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 24, 20, 24));
        grid.setPrefWidth(540);

        TextField fCedula        = campo("Cédula *");
        TextField fNombre        = campo("Primer nombre *");
        TextField fSegundoNombre = campo("Segundo nombre");
        TextField fApellido      = campo("Primer apellido *");
        TextField fApellido2     = campo("Segundo apellido");
        TextField fEmail         = campo("Email *");
        TextField fTelefono      = campo("Teléfono *");
        TextField fNacionalidad  = campo("Nacionalidad *");
        TextField fCiudad        = campo("Ciudad de origen");

        // Labels de error para validación en tiempo real
        Label errCedula       = new Label();
        Label errNombre       = new Label(); Label errApellido = new Label();
        Label errEmail        = new Label(); Label errTelefono = new Label();

        // Aplicar validaciones en tiempo real
        ValidacionCampo.aplicarSoloNumeros(fCedula,   errCedula);
        ValidacionCampo.aplicarMaxLength(fCedula,     10);
        ValidacionCampo.aplicarSoloLetras(fNombre,   errNombre);
        ValidacionCampo.aplicarSoloLetras(fApellido, errApellido);
        ValidacionCampo.aplicarEmail(fEmail,         errEmail);
        ValidacionCampo.aplicarTelefono(fTelefono,   errTelefono);
        ValidacionCampo.aplicarMaxLength(fNombre,     100);
        ValidacionCampo.aplicarMaxLength(fApellido,   100);

        if (clienteEditar != null) {
            // La cédula es el PK — no se permite cambiar
            String cedula = clienteEditar.getDocumento() != null
                    ? clienteEditar.getDocumento() : String.valueOf(clienteEditar.getId());
            fCedula.setText(cedula);
            fCedula.setEditable(false);
            fCedula.setStyle(fCedula.getStyle() + " -fx-opacity:0.7;");
            fNombre.setText(clienteEditar.getNombre());
            fSegundoNombre.setText(clienteEditar.getSegundoNombre() != null ? clienteEditar.getSegundoNombre() : "");
            fApellido.setText(clienteEditar.getApellido());
            fApellido2.setText(clienteEditar.getApellido2() != null ? clienteEditar.getApellido2() : "");
            fEmail.setText(clienteEditar.getEmail());
            fTelefono.setText(clienteEditar.getTelefono() != null ? clienteEditar.getTelefono() : "");
            fNacionalidad.setText(clienteEditar.getNacionalidad() != null ? clienteEditar.getNacionalidad() : "");
            fCiudad.setText(clienteEditar.getCiudadOrigen() != null ? clienteEditar.getCiudadOrigen() : "");
        }

        VBox vbCedula   = new VBox(2, fCedula,   errCedula);
        VBox vbNombre   = new VBox(2, fNombre,   errNombre);
        VBox vbApellido = new VBox(2, fApellido, errApellido);
        VBox vbEmail    = new VBox(2, fEmail,    errEmail);
        VBox vbTelefono = new VBox(2, fTelefono, errTelefono);
        // Fila 0: Cédula ocupa las dos columnas de valor
        grid.addRow(0, etiqueta("Cédula *:"), vbCedula);
        GridPane.setColumnSpan(vbCedula, 3);
        grid.addRow(1, etiqueta("Primer nombre *:"),    vbNombre,    etiqueta("Segundo nombre:"),   fSegundoNombre);
        grid.addRow(2, etiqueta("Primer apellido *:"),  vbApellido,  etiqueta("Segundo apellido:"), fApellido2);
        grid.addRow(3, etiqueta("Email *:"),             vbEmail,     etiqueta("Teléfono *:"),       vbTelefono);
        grid.addRow(4, etiqueta("Nacionalidad *:"),      fNacionalidad, etiqueta("Ciudad origen:"),  fCiudad);
    
          VBox content = new VBox(16);
        Label header = new Label(clienteEditar == null ? "Registrar nuevo cliente" : "Editar cliente");
        header.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;");
        Label errorLbl = new Label("");
        errorLbl.setStyle("-fx-text-fill:#dc2626; -fx-font-size:12px;");
        content.getChildren().addAll(header, new Separator(), grid, errorLbl);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color:white; -fx-background-radius:12px;");

        Button btnGuardar = (Button) dialog.getDialogPane().lookupButton(
                dialog.getDialogPane().getButtonTypes().get(0));
        btnGuardar.setStyle("-fx-background-color:#1a3a5c; -fx-text-fill:white; " +
                            "-fx-font-weight:bold; -fx-background-radius:8px; -fx-padding:8px 18px;");
btnGuardar.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (ValidacionCampo.tieneError(errCedula)
                    || ValidacionCampo.tieneError(errNombre) || ValidacionCampo.tieneError(errApellido)
                    || ValidacionCampo.tieneError(errEmail) || ValidacionCampo.tieneError(errTelefono)) {
                errorLbl.setText("Corrige los campos marcados en rojo antes de continuar.");
                event.consume(); return;
            }
            if (fCedula.getText().trim().isEmpty()) {
                errorLbl.setText("La cédula es obligatoria.");
                event.consume(); return;
            }
            if (fNombre.getText().trim().isEmpty()) {
                errorLbl.setText("El primer nombre es obligatorio.");
                event.consume(); return;
            }
            if (fApellido.getText().trim().isEmpty()) {
                errorLbl.setText("El primer apellido es obligatorio.");
                event.consume(); return;
            }
            if (fEmail.getText().trim().isEmpty()) {
                errorLbl.setText("El email es obligatorio.");
                event.consume(); return;
            }
            if (fTelefono.getText().trim().isEmpty()) {
                errorLbl.setText("El teléfono es obligatorio.");
                event.consume(); return;
            }
            if (fNacionalidad.getText().trim().isEmpty()) {
                errorLbl.setText("La nacionalidad es obligatoria.");
                event.consume(); return;
            }
            try {
                String sn = fSegundoNombre.getText().trim();
                String a2 = fApellido2.getText().trim();
                String ciudad = fCiudad.getText().trim();
                ClienteDTO dto = new ClienteDTO(
                        fCedula.getText().trim(),
                        fNombre.getText().trim(),
                        sn.isEmpty() ? null : sn,
                        fApellido.getText().trim(),
                        a2.isEmpty() ? null : a2,
                        fEmail.getText().trim(),
                        fTelefono.getText().trim(),
                        fNacionalidad.getText().trim(),
                        ciudad.isEmpty() ? null : ciudad);

                if (clienteEditar == null) {
                    ctx.getClienteService().registrarCliente(dto);
                    NotificationUtil.exito("Cliente registrado correctamente.");
                } else {
                    ctx.getClienteService().actualizarCliente(clienteEditar.getId(), dto);
                    NotificationUtil.exito("Cliente actualizado correctamente.");
                }
                Platform.runLater(() -> cargarDatos());
            } catch (ExcepcionNegocio e) {
                errorLbl.setText("Error: " + e.getMessage());
                event.consume();
            } catch (Exception e) {
                errorLbl.setText("Error inesperado: " + e.getMessage());
                event.consume();
            }
        });
        dialog.setResultConverter(btn -> btn);
        dialog.showAndWait();
    }
    
      // ── Helpers ───────────────────────────────────────────────────────────────

    private TextField campo(String placeholder) {
        TextField tf = new TextField();
        tf.setPromptText(placeholder);
        tf.setStyle("-fx-background-color:#f8fafc; -fx-border-color:#e2e8f0; " +
                    "-fx-border-width:1.5px; -fx-border-radius:8px; " +
                    "-fx-background-radius:8px; -fx-padding:8px 12px;");
        tf.setPrefWidth(180);
        return tf;
    }

    private Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#374151;");
        return l;
    }

    private void setLoading(boolean loading) {
        progressBar.setVisible(loading);
    }

}
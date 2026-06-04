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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class EmpleadosController {

    @FXML private TableView<Empleado> tabla;
    @FXML private TableColumn<Empleado,String> colId, colNombre, colApellido;
    @FXML private TableColumn<Empleado,String> colEmail, colTelefono, colCargo, colContratacion;
    @FXML private TextField searchField;
    @FXML private HBox statsRow;
    @FXML private Label lblFechaBadge, lblTurnoBadge;
    @FXML private ProgressBar progressBar;

    private final AppContext ctx = AppContext.getInstance();
    private ObservableList<Empleado> datos = FXCollections.observableArrayList();
    private FilteredList<Empleado> datosFiltrados;

    @FXML
    public void initialize() {
        // ── Cell value factories ──────────────────────────────────────────────
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colApellido.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getApellido()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTelefono() != null ? c.getValue().getTelefono() : ""));
        colCargo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCargo() != null ? c.getValue().getCargo().getNombreCargo() : ""));
        colContratacion.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaContratacion() != null ?
                c.getValue().getFechaContratacion().toString() : ""));

        // ── Cell factories visuales ───────────────────────────────────────────
        colId.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); }
                else { setText(v); setStyle("-fx-text-fill:#64748B; -fx-font-size:12px;"); }
            }
        });
        colNombre.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); }
                else { setText(v); setStyle("-fx-font-weight:700; -fx-text-fill:#1E293B;"); }
            }
        });
        colEmail.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); }
                else { setText(v); setStyle("-fx-text-fill:#3B82F6;"); }
            }
        });

        datosFiltrados = new FilteredList<>(datos, p -> true);
        tabla.setItems(datosFiltrados);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // ── Header: badges de fecha y turno ───────────────────────────────────
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es"));
            String fecha = LocalDate.now().format(fmt);
            fecha = Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1);
            lblFechaBadge.setText("📅  " + fecha);
            int hora = LocalTime.now().getHour();
            String turno = hora >= 6 && hora < 14 ? "Mañana" : hora >= 14 && hora < 22 ? "Tarde" : "Noche";
            lblTurnoBadge.setText("⏰  Turno " + turno);
        } catch (Exception ignored) { }

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
                    actualizarStats(lista.size());
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

    private void actualizarStats(int total) {
        Label badge = new Label("Total: " + total);
        badge.getStyleClass().add("badge-total-empleados");
        statsRow.getChildren().setAll(badge);
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

    // ── Wrappers que coinciden con onAction del FXML ─────────────────────────
    @FXML public void nuevoEmpleado()    { abrirFormularioNuevo(); }
    @FXML public void editarEmpleado()   { abrirFormularioEditar(); }
    @FXML public void eliminarEmpleado() { eliminar(); }

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

        TextField fCedula   = campo("Cédula (número de documento) *");
        TextField fPassword = campo("Contraseña inicial *");

        // ── Campos de contrato ────────────────────────────────────────────────
        TextField fSalario = campo("Salario (vacío = base del cargo)");
        fSalario.setPrefWidth(170);

        // Los valores deben coincidir con los FK de TIPO_CONTRATO en Oracle
        ComboBox<String> fTipoContrato = new ComboBox<>();
        fTipoContrato.getItems().addAll("TC01", "TC02", "TC03", "TC04");
        fTipoContrato.setValue("TC01");
        fTipoContrato.setPrefWidth(170);
        fTipoContrato.setConverter(new javafx.util.StringConverter<String>() {
            @Override public String toString(String c) {
                if (c == null) return "";
                switch (c) {
                    case "TC01": return "Indefinido";
                    case "TC02": return "Término Fijo";
                    case "TC03": return "Obra o Labor";
                    case "TC04": return "Aprendizaje";
                    default: return c;
                }
            }
            @Override public String fromString(String s) { return s; }
        });

        // Los valores deben coincidir con los FK de TIPO_PAGO en Oracle
        ComboBox<String> fTipoPago = new ComboBox<>();
        fTipoPago.getItems().addAll("TP01", "TP02", "TP03");
        fTipoPago.setValue("TP01");
        fTipoPago.setPrefWidth(170);
        fTipoPago.setConverter(new javafx.util.StringConverter<String>() {
            @Override public String toString(String c) {
                if (c == null) return "";
                switch (c) {
                    case "TP01": return "Mensual";
                    case "TP02": return "Quincenal";
                    case "TP03": return "Semanal";
                    default: return c;
                }
            }
            @Override public String fromString(String s) { return s; }
        });

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

        VBox vbNE = new VBox(2, fNombre,   errNombreE);
        VBox vbAE = new VBox(2, fApellido, errApellidoE);
        VBox vbEE = new VBox(2, fEmail,    errEmailE);
        VBox vbTE = new VBox(2, fTelefono, errTelefonoE);
        grid.addRow(0, lab("Primer nombre:"),    vbNE,  lab("Segundo nombre:"),   fSegundoNombre);
        grid.addRow(1, lab("Primer apellido:"),  vbAE,  lab("Segundo apellido:"), fApellido2);
        grid.addRow(2, lab("Email:"),            vbEE,  lab("Teléfono:"),         vbTE);
        if (editar == null) {
            grid.addRow(3, lab("Cédula *:"), fCedula);
            GridPane.setColumnSpan(fCedula, 3);
            grid.addRow(4, lab("Cargo:"), fCargo, lab("Contraseña inicial:"), fPassword);
            grid.addRow(5, lab("Salario del cargo:"),  lblSalario);
            grid.addRow(6, lab("Fecha contratación:"), fFecha);
            grid.addRow(7, lab("Salario individual:"), fSalario,  lab("Tipo contrato:"), fTipoContrato);
            grid.addRow(8, lab("Tipo de pago:"),       fTipoPago, lblFechaFin,           fFechaFin);
        } else {
            grid.addRow(3, lab("Cargo:"), fCargo);
            grid.addRow(4, lab("Salario del cargo:"),  lblSalario);
            grid.addRow(5, lab("Fecha contratación:"), fFecha);
            grid.addRow(6, lab("Salario individual:"), fSalario,  lab("Tipo contrato:"), fTipoContrato);
            grid.addRow(7, lab("Tipo de pago:"),       fTipoPago, lblFechaFin,           fFechaFin);
        }

        Label errLbl = new Label("");
        errLbl.setStyle("-fx-text-fill:#dc2626; -fx-font-size:12px;");
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(22, 22);
        spinner.setVisible(false);
        Label header = new Label(editar == null ? "Registrar empleado" : "Editar empleado");
        header.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;");
        VBox content = new VBox(12, header, new Separator(), grid, errLbl, spinner);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color:white;");

        Button btnGuardar = (Button) dialog.getDialogPane()
                .lookupButton(dialog.getDialogPane().getButtonTypes().get(0));
        btnGuardar.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            if (ValidacionCampo.tieneError(errNombreE) || ValidacionCampo.tieneError(errApellidoE)
                    || ValidacionCampo.tieneError(errEmailE) || ValidacionCampo.tieneError(errTelefonoE)) {
                errLbl.setText("Corrige los campos marcados en rojo.");
                return;
            }
            if (fCargo.getValue() == null) {
                errLbl.setText("Selecciona un cargo para el empleado.");
                return;
            }
            if (editar == null && fCedula.getText().trim().isEmpty()) {
                errLbl.setText("La cédula es obligatoria.");
                return;
            }
            if (editar == null && fPassword.getText().trim().isEmpty()) {
                errLbl.setText("La contraseña inicial es obligatoria.");
                return;
            }
            int idCargo = fCargo.getValue().getId();
            btnGuardar.setDisable(true);
            spinner.setVisible(true);
            errLbl.setText("");

            double salarioVal = 0;
            try { salarioVal = Double.parseDouble(
                    fSalario.getText().trim().replace(",","").replace("$","")); }
            catch (NumberFormatException ignored) {}
            final double salarioFinal = salarioVal;
            final LocalDate fechaFinVal = "TERMINO_FIJO".equals(fTipoContrato.getValue())
                    ? fFechaFin.getValue() : null;

            new Thread(() -> {
                try {
                    if (editar == null) {
                        ctx.getEmpleadoService().registrarEmpleado(
                                fCedula.getText().trim(),
                                fNombre.getText().trim(), fSegundoNombre.getText().trim(),
                                fApellido.getText().trim(), fApellido2.getText().trim(),
                                fEmail.getText().trim(), fTelefono.getText().trim(),
                                idCargo, fPassword.getText().trim(),
                                salarioFinal, fTipoContrato.getValue(),
                                fTipoPago.getValue(), fechaFinVal);
                        Platform.runLater(() -> {
                            NotificationUtil.exito("Empleado registrado.");
                            cargarDatos();
                            dialog.close();
                        });
                    } else {
                        ctx.getEmpleadoService().actualizarEmpleado(
                                editar.getId(), fNombre.getText().trim(),
                                fSegundoNombre.getText().trim(),
                                fApellido.getText().trim(), fApellido2.getText().trim(),
                                fEmail.getText().trim(), fTelefono.getText().trim(), idCargo,
                                fFecha.getValue(),
                                salarioFinal, fTipoContrato.getValue(),
                                fTipoPago.getValue(), fechaFinVal);
                        Platform.runLater(() -> {
                            NotificationUtil.exito("Empleado actualizado.");
                            cargarDatos();
                            dialog.close();
                        });
                    }
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        errLbl.setText("Error: " + ex.getMessage());
                        btnGuardar.setDisable(false);
                        spinner.setVisible(false);
                    });
                }
            }).start();
        });
        dialog.setResultConverter(btn -> btn);
        dialog.showAndWait();
    }

    private TextField campo(String p) {
        TextField tf = new TextField();
        tf.setPromptText(p);
        tf.setStyle("-fx-background-color:#f8fafc; -fx-border-color:#e2e8f0;" +
                    "-fx-border-width:1.5px; -fx-border-radius:8px;" +
                    "-fx-background-radius:8px; -fx-padding:8px 12px;");
        tf.setPrefWidth(170);
        return tf;
    }
    private Label lab(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#374151;");
        return l;
    }
}

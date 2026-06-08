package com.hotel.ui.controllers;

import com.hotel.AppContext;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.Empleado;
import com.hotel.model.Habitacion;
import com.hotel.model.Mantenimiento;
import com.hotel.ui.components.NotificationUtil;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Controlador del módulo de Mantenimiento.
 * Tabla con cell factories tipadas, badges outlined y cell factories para
 * tipos especiales (EMERGENCIA rojo, costo cero gris, campos vacíos en cursiva).
 * Interfaces modernizadas estilo Dashboard.
 */
public class MantenimientoController {

    // ── FXML — ids coinciden exactamente con Mantenimiento.fxml ──────────────
    @FXML private TableView<Mantenimiento>          tabla;
    @FXML private TableColumn<Mantenimiento,String> colId, colHab, colEmpleado;
    @FXML private TableColumn<Mantenimiento,String> colTipo, colEstado;
    @FXML private TableColumn<Mantenimiento,String> colSolicitud, colRealizacion;
    @FXML private TableColumn<Mantenimiento,String> colCosto, colDescripcion;
    @FXML private TextField                         searchField;
    @FXML private HBox                              statsRow;
    @FXML private ProgressBar                       progressBar;
    @FXML private Label                             lblFecha, lblTurno;

    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-CO");

    private final AppContext                   ctx      = AppContext.getInstance();
    private final ObservableList<Mantenimiento> datos   = FXCollections.observableArrayList();
    private FilteredList<Mantenimiento>         filtrados;
    private String                              filtroActual = "PENDIENTES";
    private Label                               lblConteo;

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        configurarFechaYTurno();
        configurarColumnas();
        filtrados = new FilteredList<>(datos, p -> true);
        tabla.setItems(filtrados);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        construirToggleFiltro();
        cargarDatos();
    }

    private void configurarFechaYTurno() {
        if (lblFecha != null) {
            LocalDate hoy = LocalDate.now();
            String f = hoy.getDayOfWeek().getDisplayName(TextStyle.FULL, LOCALE_ES)
                    + " " + hoy.getDayOfMonth()
                    + " de " + hoy.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES);
            lblFecha.setText("📅  " + f.substring(0,1).toUpperCase() + f.substring(1));
        }
        if (lblTurno != null) {
            int h = LocalDateTime.now().getHour();
            String t = h < 12 ? "Turno Mañana 🌤" : h < 18 ? "Turno Tarde 🌇" : "Turno Noche 🌙";
            lblTurno.setText("⏰  " + t);
        }
    }

    // ── Columnas ──────────────────────────────────────────────────────────────

    private void configurarColumnas() {
        // ID — gris con #
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colId.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); }
                else { setText(v); setStyle("-fx-text-fill:#9ca3af; -fx-font-size:12px;"); }
            }
        });

        // Habitación — bold
        colHab.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getHabitacion() != null
                        ? c.getValue().getHabitacion().getNumero() : "—"));
        colHab.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); }
                else { setText(v); setStyle("-fx-font-weight:700; -fx-text-fill:#1e293b;"); }
            }
        });

        // Empleado
        colEmpleado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEmpleadoResponsable() != null
                        ? c.getValue().getEmpleadoResponsable().obtenerNombreCompleto() : "—"));

        // Tipo — EMERGENCIA en rojo bold, resto normal
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTipo() != null ? c.getValue().getTipo().name() : "—"));
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                if ("EMERGENCIA".equals(v)) {
                    setStyle("-fx-text-fill:#dc2626; -fx-font-weight:bold;");
                } else {
                    setStyle("-fx-text-fill:#374151;");
                }
            }
        });

        // Estado — badge pill
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEstado() != null ? c.getValue().getEstado().name() : "—"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null || "—".equals(v)) { setGraphic(null); return; }
                Label badge = new Label(etiquetaEstado(v));
                badge.setStyle(badgeEstilo(v));
                setGraphic(badge); setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        // Fecha solicitud — normal
        colSolicitud.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaSolicitud() != null
                        ? c.getValue().getFechaSolicitud().toString() : "—"));

        // Fecha realización — gris cursiva si vacío
        colRealizacion.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaRealizacion() != null
                        ? c.getValue().getFechaRealizacion().toString() : "—"));
        colRealizacion.setCellFactory(col -> vacioCursivo());

        // Costo — bold oscuro si > 0, gris si $0
        colCosto.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("$%,.0f", c.getValue().getCosto())));
        colCosto.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                boolean esCero = v.equals("$0") || v.equals("$0.00");
                setStyle(esCero
                        ? "-fx-text-fill:#94a3b8;"
                        : "-fx-text-fill:#1e293b; -fx-font-weight:700;");
            }
        });

        // Descripción — gris cursiva si vacío
        colDescripcion.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDescripcionTrabajo() != null
                        && !c.getValue().getDescripcionTrabajo().isBlank()
                        ? c.getValue().getDescripcionTrabajo() : "—"));
        colDescripcion.setCellFactory(col -> vacioCursivo());

        tabla.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Mantenimiento m, boolean empty) {
                super.updateItem(m, empty);
                setStyle("");
            }
        });
    }

    // ── Carga de datos ────────────────────────────────────────────────────────

    @FXML
    public void cargarDatos() {
        progressBar.setVisible(true);
        new Thread(() -> {
            try {
                List<Mantenimiento> lista =
                        ctx.getMantenimientoService().listarTodosLosMantenimientos();
                Platform.runLater(() -> {
                    datos.setAll(lista);
                    filtrar();
                    progressBar.setVisible(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    NotificationUtil.error("Error cargando mantenimientos: " + e.getMessage());
                    progressBar.setVisible(false);
                });
            }
        }, "carga-mantenimiento").start();
    }

    private void construirToggleFiltro() {
        ToggleGroup grupo = new ToggleGroup();

        ToggleButton btnPend  = new ToggleButton("⏳  Pendientes");
        ToggleButton btnTodos = new ToggleButton("📋  Todos");
        ToggleButton btnHist  = new ToggleButton("✅  Historial");

        btnPend.setToggleGroup(grupo);  btnPend.setUserData("PENDIENTES");
        btnTodos.setToggleGroup(grupo); btnTodos.setUserData("TODOS");
        btnHist.setToggleGroup(grupo);  btnHist.setUserData("HISTORIAL");

        btnPend.setFocusTraversable(false);
        btnTodos.setFocusTraversable(false);
        btnHist.setFocusTraversable(false);

        btnPend.setSelected(true);
        aplicarEstiloToggle(btnPend,  true,  "8 0 0 8");
        aplicarEstiloToggle(btnTodos, false, "0");
        aplicarEstiloToggle(btnHist,  false, "0 8 8 0");

        grupo.selectedToggleProperty().addListener((obs, old, nuevo) -> {
            if (nuevo == null) { if (old != null) old.setSelected(true); return; }
            filtroActual = (String) nuevo.getUserData();
            aplicarEstiloToggle(btnPend,  nuevo == btnPend,  "8 0 0 8");
            aplicarEstiloToggle(btnTodos, nuevo == btnTodos, "0");
            aplicarEstiloToggle(btnHist,  nuevo == btnHist,  "0 8 8 0");
            filtrar();
        });

        lblConteo = new Label();
        lblConteo.setStyle("-fx-font-size:11.5px; -fx-text-fill:#94a3b8; -fx-padding:0 0 0 12;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        statsRow.getChildren().setAll(
                new HBox(0, btnPend, btnTodos, btnHist), lblConteo, sp);
    }

    private void aplicarEstiloToggle(ToggleButton btn, boolean seleccionado, String radius) {
        if (seleccionado) {
            btn.setStyle(
                "-fx-background-color:#3563e9; -fx-text-fill:white; -fx-font-weight:700;" +
                "-fx-font-size:12px; -fx-padding:6 16; -fx-cursor:hand;" +
                "-fx-background-radius:" + radius + ";");
        } else {
            btn.setStyle(
                "-fx-background-color:white; -fx-text-fill:#64748b; -fx-font-weight:600;" +
                "-fx-font-size:12px; -fx-padding:6 16; -fx-cursor:hand;" +
                "-fx-border-color:#e2e8f0; -fx-border-width:1;" +
                "-fx-border-radius:" + radius + "; -fx-background-radius:" + radius + ";");
        }
    }

    // ── Búsqueda ──────────────────────────────────────────────────────────────

    @FXML
    public void filtrar() {
        String q = searchField.getText().toLowerCase().trim();
        filtrados.setPredicate(m -> {
            boolean estadoOk = switch (filtroActual) {
                case "PENDIENTES" -> m.getEstado() == Mantenimiento.EstadoMantenimiento.SOLICITADO
                                  || m.getEstado() == Mantenimiento.EstadoMantenimiento.EN_PROCESO;
                case "HISTORIAL"  -> m.getEstado() == Mantenimiento.EstadoMantenimiento.COMPLETADO
                                  || m.getEstado() == Mantenimiento.EstadoMantenimiento.CANCELADO;
                default           -> true;
            };
            if (!estadoOk) return false;
            if (q.isEmpty()) return true;
            return (m.getHabitacion() != null
                        && m.getHabitacion().getNumero().toLowerCase().contains(q))
                || (m.getEmpleadoResponsable() != null
                        && m.getEmpleadoResponsable().obtenerNombreCompleto().toLowerCase().contains(q))
                || (m.getTipo() != null
                        && m.getTipo().name().toLowerCase().contains(q))
                || (m.getDescripcionTrabajo() != null
                        && m.getDescripcionTrabajo().toLowerCase().contains(q));
        });
        if (lblConteo != null) {
            int n = filtrados.size();
            lblConteo.setText(n + (n == 1 ? " registro" : " registros"));
        }
    }

    // ── Acciones FXML ─────────────────────────────────────────────────────────

    @FXML public void nuevoMantenimiento() { mostrarFormularioNuevoModerno(); }

    @FXML public void cancelarMant() { cancelarModerno(); }

    @FXML
    public void handleDobleClick(MouseEvent e) {
        if (e.getClickCount() == 2) {
            Mantenimiento sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) verDetalle(sel);
        }
    }

    @FXML
    public void completar() { completarModerno(); }

    // ─── VER DETALLE ──────────────────────────────────────────────────────────

    private void verDetalle(Mantenimiento m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Detalle Mantenimiento #" + m.getId());
        a.setHeaderText("Solicitud #" + m.getId()
                + " — " + (m.getEstado() != null ? etiquetaEstado(m.getEstado().name()) : "—"));
        a.setContentText(
            "Habitación: "   + (m.getHabitacion() != null ? m.getHabitacion().getNumero() : "—") + "\n" +
            "Empleado: "     + (m.getEmpleadoResponsable() != null ? m.getEmpleadoResponsable().obtenerNombreCompleto() : "—") + "\n" +
            "Tipo: "         + (m.getTipo() != null ? m.getTipo().name() : "—") + "\n" +
            "Solicitud: "    + (m.getFechaSolicitud() != null ? m.getFechaSolicitud().toString() : "—") + "\n" +
            "Realización: "  + (m.getFechaRealizacion() != null ? m.getFechaRealizacion().toString() : "—") + "\n" +
            "Costo: "        + String.format("$%,.0f", m.getCosto()) + "\n" +
            "Descripción: "  + (m.getDescripcionTrabajo() != null ? m.getDescripcionTrabajo() : "—"));
        a.showAndWait();
    }
        // ─── NUEVO FORMULARIO MODERNO (SOLO TÉCNICOS) ──────────────────────────────

    private void mostrarFormularioNuevoModerno() {
        List<Habitacion> habitaciones;
        try {
            habitaciones = ctx.getHabitacionService().listarTodasLasHabitaciones();
        } catch (Exception e) {
            NotificationUtil.error("Error cargando habitaciones: " + e.getMessage());
            return;
        }
        
        // ─── Cargar SOLO empleados con cargo TÉCNICO ───────────────────────────
        List<Empleado> todosLosEmpleados;
        try {
            todosLosEmpleados = ctx.getEmpleadoService().listarTodosLosEmpleados();
        } catch (Exception e) {
            NotificationUtil.error("Error cargando empleados: " + e.getMessage());
            return;
        }
        
        // Filtrar SOLO empleados con cargo TÉCNICO (no aseadores, no vigilantes)
        List<Empleado> empleadosTecnicos = todosLosEmpleados.stream()
                .filter(e -> {
                    String cargo = e.getCargo() != null ? e.getCargo().getNombreCargo().toLowerCase() : "";
                    return cargo.contains("técnico") 
                            || cargo.contains("tecnico")
                            || cargo.contains("electricista")
                            || cargo.contains("plomería")
                            || cargo.contains("plomeria")
                            || cargo.contains("aire acondicionado")
                            || cargo.contains("fontanero")
                            || cargo.contains("pintor")
                            || cargo.contains("carpintero")
                            || cargo.contains("mecánico")
                            || cargo.contains("mecanico")
                            || cargo.contains("soldador")
                            || cargo.equals("técnico")
                            || cargo.equals("tecnico");
                })
                .collect(Collectors.toList());
        
        if (empleadosTecnicos.isEmpty()) {
            NotificationUtil.advertencia("No hay empleados con cargos técnicos registrados.\n" +
                                         "Registre empleados con cargos como: Técnico, Electricista, Plomero, etc.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.25));
        shadow.setRadius(24);
        shadow.setOffsetY(8);
        dialog.getDialogPane().setEffect(shadow);
        
        VBox mainContainer = new VBox(0);
        mainContainer.setStyle("-fx-background-color: white; -fx-background-radius: 16px;");
        
        // CABECERA
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 16px 16px 0 0;");
        
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(48, 48);
        iconBox.setMaxSize(48, 48);
        iconBox.setStyle("-fx-background-color: #1e3a5f; -fx-background-radius: 12px;");
        Label iconLabel = new Label("🔧");
        iconLabel.setStyle("-fx-font-size: 22px;");
        iconBox.getChildren().add(iconLabel);
        
        VBox headerTexts = new VBox(4);
        Label titleLabel = new Label("Nueva Solicitud de Mantenimiento");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        HBox totalBadge = new HBox(6);
        totalBadge.setAlignment(Pos.CENTER);
        totalBadge.setPadding(new Insets(4, 10, 4, 10));
        totalBadge.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 20px;");
        Label dotIndicator = new Label("●");
        dotIndicator.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 8px;");
        Label totalLabel = new Label("REGISTRE LOS DATOS DEL MANTENIMIENTO");
        totalLabel.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 11px; -fx-font-weight: 600;");
        totalBadge.getChildren().addAll(dotIndicator, totalLabel);
        headerTexts.getChildren().addAll(titleLabel, totalBadge);
        
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #a0a0c0; " +
                         "-fx-font-size: 14px; -fx-background-radius: 20px; -fx-cursor: hand; " +
                         "-fx-padding: 6px 10px;");
        closeBtn.setOnAction(e -> dialog.close());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(iconBox, headerTexts, spacer, closeBtn);
        
        // CUERPO
        VBox body = new VBox(20);
        body.setPadding(new Insets(24));
        
        // SECCIÓN DATOS DEL MANTENIMIENTO
        VBox section = new VBox(12);
        Label sectionTitle = new Label("🔧 DATOS DEL MANTENIMIENTO");
        sectionTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        grid.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-padding: 20px;");
        
        // Habitación
        ComboBox<Habitacion> fHabitacion = new ComboBox<>();
        fHabitacion.getItems().addAll(habitaciones);
        fHabitacion.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Habitacion h, boolean empty) {
                super.updateItem(h, empty);
                setText(empty || h == null ? null :
                        "Hab. " + h.getNumero() +
                        " — Piso " + (h.getPiso() != null ? h.getPiso().getNumeroPiso() : "?") +
                        " [" + h.getEstado() + "]");
            }
        });
        fHabitacion.setButtonCell(fHabitacion.getCellFactory().call(null));
        fHabitacion.setPromptText("Seleccionar habitación");
        fHabitacion.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px; " +
                "-fx-padding: 8px 12px; -fx-font-size: 13px;");
        fHabitacion.setPrefWidth(300);
        
        // Empleado (SOLO TÉCNICOS)
        ComboBox<Empleado> fEmpleado = new ComboBox<>();
        fEmpleado.getItems().addAll(empleadosTecnicos);
        fEmpleado.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Empleado e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) {
                    setText(null);
                } else {
                    String cargo = e.getCargo() != null ? e.getCargo().getNombreCargo() : "Sin cargo";
                    setText(e.obtenerNombreCompleto() + " — " + cargo);
                }
            }
        });
        fEmpleado.setButtonCell(fEmpleado.getCellFactory().call(null));
        fEmpleado.setPromptText("Seleccionar empleado técnico");
        fEmpleado.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px; " +
                "-fx-padding: 8px 12px; -fx-font-size: 13px;");
        fEmpleado.setPrefWidth(300);
        
        // Tipo
        ComboBox<String> fTipo = new ComboBox<>();
        fTipo.getItems().addAll("PREVENTIVO", "CORRECTIVO", "EMERGENCIA");
        fTipo.setValue("CORRECTIVO");
        fTipo.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                       "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px; " +
                       "-fx-padding: 8px 12px; -fx-font-size: 13px;");
        fTipo.setPrefWidth(300);
        
        // Descripción
        Label descLabel = new Label("DESCRIPCIÓN DEL TRABAJO");
        descLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        
        TextArea fDesc = new TextArea();
        fDesc.setPromptText("Describa el trabajo a realizar...");
        fDesc.setPrefRowCount(3);
        fDesc.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                       "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px; " +
                       "-fx-padding: 8px 12px; -fx-font-size: 13px;");
        
        grid.addRow(0, crearLabelCampo("HABITACIÓN *:"), fHabitacion);
        grid.addRow(1, crearLabelCampo("EMPLEADO RESPONSABLE *:"), fEmpleado);
        grid.addRow(2, crearLabelCampo("TIPO DE MANTENIMIENTO *:"), fTipo);
        
        section.getChildren().addAll(sectionTitle, grid, descLabel, fDesc);
        
        body.getChildren().add(section);
        
        // LABEL DE ERROR
        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        errLabel.setWrapText(true);
        errLabel.setVisible(false);
        body.getChildren().add(errLabel);
        
        // FOOTER
        VBox footer = new VBox();
        footer.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16px 16px;");
        
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #e2e8f0;");
        
        HBox footerContent = new HBox(16);
        footerContent.setAlignment(Pos.CENTER_RIGHT);
        footerContent.setPadding(new Insets(16, 24, 20, 24));
        
        Button cancelarBtn = new Button("Cancelar");
        cancelarBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; " +
                            "-fx-background-radius: 8px; -fx-padding: 10px 24px; " +
                            "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        cancelarBtn.setOnAction(e -> dialog.close());
        
        Button solicitarBtn = new Button("Solicitar Mantenimiento");
        solicitarBtn.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: white; " +
                              "-fx-background-radius: 8px; -fx-padding: 10px 24px; " +
                              "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        
        footerContent.getChildren().addAll(cancelarBtn, solicitarBtn);
        footer.getChildren().addAll(divider, footerContent);
        
        mainContainer.getChildren().addAll(header, body, footer);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().setPrefSize(650, 600);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        
        solicitarBtn.setOnAction(e -> {
            errLabel.setVisible(false);
            if (fHabitacion.getValue() == null) {
                errLabel.setText("Selecciona una habitación."); errLabel.setVisible(true); return;
            }
            if (fEmpleado.getValue() == null) {
                errLabel.setText("Selecciona un empleado técnico."); errLabel.setVisible(true); return;
            }
            
            solicitarBtn.setDisable(true);
            
            new Thread(() -> {
                try {
                    String numHab = fHabitacion.getValue().getNumero();
                    int idEmp = fEmpleado.getValue().getId();
                    Mantenimiento.TipoMantenimiento tipo =
                            Mantenimiento.TipoMantenimiento.valueOf(fTipo.getValue());
                    ctx.getMantenimientoService().solicitarMantenimiento(
                            numHab, idEmp, tipo, fDesc.getText().trim());
                    Platform.runLater(() -> {
                        NotificationUtil.exito("Solicitud de mantenimiento registrada correctamente.");
                        cargarDatos();
                        dialog.close();
                    });
                } catch (ExcepcionNegocio ex) {
                    Platform.runLater(() -> {
                        errLabel.setText("Error: " + ex.getMessage());
                        errLabel.setVisible(true);
                        solicitarBtn.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        errLabel.setText("Error inesperado: " + ex.getMessage());
                        errLabel.setVisible(true);
                        solicitarBtn.setDisable(false);
                    });
                }
            }).start();
        });
        
        Button defaultCloseButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        defaultCloseButton.setVisible(false);
        defaultCloseButton.setManaged(false);
        
        dialog.setOnShown(ev -> {
            FadeTransition ft = new FadeTransition(Duration.millis(250), dialog.getDialogPane());
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        });
        
        dialog.showAndWait();
    }
    
    // ─── COMPLETAR MANTENIMIENTO MODERNO ───────────────────────────────────────
    
    private void completarModerno() {
        Mantenimiento sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) {
            NotificationUtil.advertencia("Selecciona un mantenimiento para completar.");
            return;
        }
        
        if (sel.getEstado() == Mantenimiento.EstadoMantenimiento.COMPLETADO) {
            NotificationUtil.advertencia("Este mantenimiento ya está completado.");
            return;
        }
        
        if (sel.getEstado() == Mantenimiento.EstadoMantenimiento.CANCELADO) {
            NotificationUtil.advertencia("Este mantenimiento está cancelado. No se puede completar.");
            return;
        }
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.25));
        shadow.setRadius(24);
        shadow.setOffsetY(8);
        dialog.getDialogPane().setEffect(shadow);
        
        VBox mainContainer = new VBox(0);
        mainContainer.setStyle("-fx-background-color: white; -fx-background-radius: 16px;");
        
        // CABECERA
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 16px 16px 0 0;");
        
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(48, 48);
        iconBox.setMaxSize(48, 48);
        iconBox.setStyle("-fx-background-color: #1e3a5f; -fx-background-radius: 12px;");
        Label iconLabel = new Label("✅");
        iconLabel.setStyle("-fx-font-size: 22px;");
        iconBox.getChildren().add(iconLabel);
        
        VBox headerTexts = new VBox(4);
        Label titleLabel = new Label("Completar Mantenimiento");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        HBox infoBadge = new HBox(6);
        infoBadge.setAlignment(Pos.CENTER);
        infoBadge.setPadding(new Insets(4, 10, 4, 10));
        infoBadge.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 20px;");
        Label infoDot = new Label("●");
        infoDot.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 8px;");
        Label infoLabel = new Label("MANTENIMIENTO #" + sel.getId() + " — HAB. " + 
                (sel.getHabitacion() != null ? sel.getHabitacion().getNumero() : "?"));
        infoLabel.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 11px; -fx-font-weight: 600;");
        infoBadge.getChildren().addAll(infoDot, infoLabel);
        headerTexts.getChildren().addAll(titleLabel, infoBadge);
        
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #a0a0c0; " +
                         "-fx-font-size: 14px; -fx-background-radius: 20px; -fx-cursor: hand; " +
                         "-fx-padding: 6px 10px;");
        closeBtn.setOnAction(e -> dialog.close());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(iconBox, headerTexts, spacer, closeBtn);
        
        // CUERPO
        VBox body = new VBox(20);
        body.setPadding(new Insets(24));
        
        // Información del mantenimiento
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(16);
        infoGrid.setVgap(12);
        infoGrid.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-padding: 16px;");
        
        String tipoMant = sel.getTipo() != null ? sel.getTipo().name() : "—";
        String tipoColor = "EMERGENCIA".equals(tipoMant) ? "#dc2626" : "#1e293b";
        
        infoGrid.addRow(0, crearLabelCampo("TIPO:"), 
                new Label(tipoMant) {{ setStyle("-fx-font-weight: bold; -fx-text-fill: " + tipoColor + ";"); }});
        infoGrid.addRow(1, crearLabelCampo("EMPLEADO RESPONSABLE:"), 
                new Label(sel.getEmpleadoResponsable() != null ? sel.getEmpleadoResponsable().obtenerNombreCompleto() : "—"));
        infoGrid.addRow(2, crearLabelCampo("FECHA SOLICITUD:"), 
                new Label(sel.getFechaSolicitud() != null ? sel.getFechaSolicitud().toString() : "—"));
        
        body.getChildren().add(infoGrid);
        
        // Campo de costo
        VBox costBox = new VBox(8);
        Label costLabel = new Label("💰 COSTO FINAL DEL MANTENIMIENTO");
        costLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        
        TextField fCosto = new TextField("0");
        fCosto.setPromptText("Ingrese el costo del mantenimiento");
        fCosto.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                       "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px; " +
                       "-fx-padding: 10px 12px; -fx-font-size: 14px;");
        
        costBox.getChildren().addAll(costLabel, fCosto);
        body.getChildren().add(costBox);
        
        // LABEL DE ERROR
        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        errLabel.setWrapText(true);
        errLabel.setVisible(false);
        body.getChildren().add(errLabel);
        
        // FOOTER
        VBox footer = new VBox();
        footer.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16px 16px;");
        
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #e2e8f0;");
        
        HBox footerContent = new HBox(16);
        footerContent.setAlignment(Pos.CENTER_RIGHT);
        footerContent.setPadding(new Insets(16, 24, 20, 24));
        
        Button cancelarBtn = new Button("Cancelar");
        cancelarBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; " +
                            "-fx-background-radius: 8px; -fx-padding: 10px 24px; " +
                            "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        cancelarBtn.setOnAction(e -> dialog.close());
        
        Button completarBtn = new Button("Completar Mantenimiento");
        completarBtn.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: white; " +
                              "-fx-background-radius: 8px; -fx-padding: 10px 24px; " +
                              "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        
        footerContent.getChildren().addAll(cancelarBtn, completarBtn);
        footer.getChildren().addAll(divider, footerContent);
        
        mainContainer.getChildren().addAll(header, body, footer);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().setPrefSize(550, 480);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        
        completarBtn.setOnAction(e -> {
            errLabel.setVisible(false);
            String costoStr = fCosto.getText().trim();
            if (costoStr.isEmpty()) {
                errLabel.setText("Ingrese el costo del mantenimiento."); errLabel.setVisible(true); return;
            }
            try {
                double costo = Double.parseDouble(costoStr.replace(",", "."));
                if (costo < 0) {
                    errLabel.setText("El costo no puede ser negativo."); errLabel.setVisible(true); return;
                }
                completarBtn.setDisable(true);
                
                new Thread(() -> {
                    try {
                        ctx.getMantenimientoService().completarMantenimiento(sel.getId(), costo);
                        Platform.runLater(() -> {
                            NotificationUtil.exito("Mantenimiento #" + sel.getId() + " completado.");
                            cargarDatos();
                            dialog.close();
                        });
                    } catch (ExcepcionNegocio ex) {
                        Platform.runLater(() -> {
                            errLabel.setText(ex.getMessage());
                            errLabel.setVisible(true);
                            completarBtn.setDisable(false);
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            errLabel.setText("Error inesperado: " + ex.getMessage());
                            errLabel.setVisible(true);
                            completarBtn.setDisable(false);
                        });
                    }
                }).start();
            } catch (NumberFormatException ex) {
                errLabel.setText("Ingrese un valor numérico válido."); errLabel.setVisible(true);
            }
        });
        
        Button defaultCloseButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        defaultCloseButton.setVisible(false);
        defaultCloseButton.setManaged(false);
        
        dialog.setOnShown(ev -> {
            FadeTransition ft = new FadeTransition(Duration.millis(250), dialog.getDialogPane());
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        });
        
        dialog.showAndWait();
    }
    
    // ─── CANCELAR MANTENIMIENTO MODERNO ───────────────────────────────────────
    
    private void cancelarModerno() {
        Mantenimiento sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) {
            NotificationUtil.advertencia("Selecciona un mantenimiento para cancelar.");
            return;
        }
        
        if (sel.getEstado() == Mantenimiento.EstadoMantenimiento.COMPLETADO) {
            NotificationUtil.advertencia("Este mantenimiento ya está completado. No se puede cancelar.");
            return;
        }
        
        if (sel.getEstado() == Mantenimiento.EstadoMantenimiento.CANCELADO) {
            NotificationUtil.advertencia("Este mantenimiento ya está cancelado.");
            return;
        }
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.25));
        shadow.setRadius(24);
        shadow.setOffsetY(8);
        dialog.getDialogPane().setEffect(shadow);
        
        VBox mainContainer = new VBox(0);
        mainContainer.setStyle("-fx-background-color: white; -fx-background-radius: 16px;");
        
        // CABECERA
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 16px 16px 0 0;");
        
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(48, 48);
        iconBox.setMaxSize(48, 48);
        iconBox.setStyle("-fx-background-color: #1e3a5f; -fx-background-radius: 12px;");
        Label iconLabel = new Label("❌");
        iconLabel.setStyle("-fx-font-size: 22px;");
        iconBox.getChildren().add(iconLabel);
        
        VBox headerTexts = new VBox(4);
        Label titleLabel = new Label("Cancelar Mantenimiento");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        HBox infoBadge = new HBox(6);
        infoBadge.setAlignment(Pos.CENTER);
        infoBadge.setPadding(new Insets(4, 10, 4, 10));
        infoBadge.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 20px;");
        Label infoDot = new Label("●");
        infoDot.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 8px;");
        Label infoLabel = new Label("MANTENIMIENTO #" + sel.getId() + " — HAB. " + 
                (sel.getHabitacion() != null ? sel.getHabitacion().getNumero() : "?"));
        infoLabel.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 11px; -fx-font-weight: 600;");
        infoBadge.getChildren().addAll(infoDot, infoLabel);
        headerTexts.getChildren().addAll(titleLabel, infoBadge);
        
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #a0a0c0; " +
                         "-fx-font-size: 14px; -fx-background-radius: 20px; -fx-cursor: hand; " +
                         "-fx-padding: 6px 10px;");
        closeBtn.setOnAction(e -> dialog.close());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(iconBox, headerTexts, spacer, closeBtn);
        
        // CUERPO
        VBox body = new VBox(20);
        body.setPadding(new Insets(24));
        
        // ADVERTENCIA
        HBox warningBox = new HBox(12);
        warningBox.setAlignment(Pos.CENTER_LEFT);
        warningBox.setStyle("-fx-background-color: #fef3c7; -fx-background-radius: 10px; -fx-padding: 12px 16px;");
        
        Label warningIcon = new Label("⚠️");
        warningIcon.setStyle("-fx-font-size: 16px;");
        
        Label warningText = new Label("Esta acción cancelará la solicitud de mantenimiento y no se podrá revertir.");
        warningText.setStyle("-fx-font-size: 12px; -fx-text-fill: #92400e; -fx-font-weight: 500;");
        warningText.setWrapText(true);
        
        warningBox.getChildren().addAll(warningIcon, warningText);
        HBox.setHgrow(warningText, Priority.ALWAYS);
        
        body.getChildren().add(warningBox);
        
        // Información del mantenimiento
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(16);
        infoGrid.setVgap(12);
        infoGrid.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-padding: 16px;");
        
        infoGrid.addRow(0, crearLabelCampo("TIPO:"), 
                new Label(sel.getTipo() != null ? sel.getTipo().name() : "—"));
        infoGrid.addRow(1, crearLabelCampo("EMPLEADO RESPONSABLE:"), 
                new Label(sel.getEmpleadoResponsable() != null ? sel.getEmpleadoResponsable().obtenerNombreCompleto() : "—"));
        
        body.getChildren().add(infoGrid);
        
        // Campo de motivo
        VBox motivoBox = new VBox(8);
        Label motivoLabel = new Label("📝 MOTIVO DE CANCELACIÓN");
        motivoLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        
        TextArea fMotivo = new TextArea();
        fMotivo.setPromptText("Ingrese el motivo de la cancelación...");
        fMotivo.setPrefRowCount(3);
        fMotivo.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                        "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px; " +
                        "-fx-padding: 8px 12px; -fx-font-size: 13px;");
        
        motivoBox.getChildren().addAll(motivoLabel, fMotivo);
        body.getChildren().add(motivoBox);
        
        // LABEL DE ERROR
        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        errLabel.setWrapText(true);
        errLabel.setVisible(false);
        body.getChildren().add(errLabel);
        
        // FOOTER
        VBox footer = new VBox();
        footer.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16px 16px;");
        
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #e2e8f0;");
        
        HBox footerContent = new HBox(16);
        footerContent.setAlignment(Pos.CENTER_RIGHT);
        footerContent.setPadding(new Insets(16, 24, 20, 24));
        
        Button cancelarBtn = new Button("Volver");
        cancelarBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; " +
                            "-fx-background-radius: 8px; -fx-padding: 10px 24px; " +
                            "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        cancelarBtn.setOnAction(e -> dialog.close());
        
        Button confirmarBtn = new Button("Confirmar Cancelación");
        confirmarBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; " +
                              "-fx-background-radius: 8px; -fx-padding: 10px 24px; " +
                              "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        
        footerContent.getChildren().addAll(cancelarBtn, confirmarBtn);
        footer.getChildren().addAll(divider, footerContent);
        
        mainContainer.getChildren().addAll(header, body, footer);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().setPrefSize(550, 520);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        
        confirmarBtn.setOnAction(e -> {
            errLabel.setVisible(false);
            String motivo = fMotivo.getText().trim();
            if (motivo.isEmpty()) {
                errLabel.setText("Ingrese el motivo de la cancelación."); errLabel.setVisible(true); return;
            }
            
            confirmarBtn.setDisable(true);
            
            new Thread(() -> {
                try {
                    ctx.getMantenimientoService().cancelarMantenimiento(sel.getId(), motivo);
                    Platform.runLater(() -> {
                        NotificationUtil.exito("Mantenimiento #" + sel.getId() + " cancelado.");
                        cargarDatos();
                        dialog.close();
                    });
                } catch (ExcepcionNegocio ex) {
                    Platform.runLater(() -> {
                        errLabel.setText(ex.getMessage());
                        errLabel.setVisible(true);
                        confirmarBtn.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        errLabel.setText("Error inesperado: " + ex.getMessage());
                        errLabel.setVisible(true);
                        confirmarBtn.setDisable(false);
                    });
                }
            }).start();
        });
        
        Button defaultCloseButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        defaultCloseButton.setVisible(false);
        defaultCloseButton.setManaged(false);
        
        dialog.setOnShown(ev -> {
            FadeTransition ft = new FadeTransition(Duration.millis(250), dialog.getDialogPane());
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        });
        
        dialog.showAndWait();
    }

    // ─── HELPERS ───────────────────────────────────────────────────────────────

    private Label crearLabelCampo(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        return l;
    }

    private TableCell<Mantenimiento, String> vacioCursivo() {
        return new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                if ("—".equals(v)) {
                    setText("—");
                    setStyle("-fx-text-fill:#94a3b8; -fx-font-style:italic;");
                } else {
                    setText(v);
                    setStyle("-fx-text-fill:#374151;");
                }
            }
        };
    }

    private String etiquetaEstado(String est) {
        switch (est) {
            case "SOLICITADO": return "Solicitado";
            case "EN_PROCESO": return "En proceso";
            case "COMPLETADO": return "Completado";
            case "CANCELADO":  return "Cancelado";
            default:           return est;
        }
    }

    private String badgeEstilo(String est) {
        String base = "-fx-background-radius:20; -fx-padding:3 12; " +
                      "-fx-font-size:11px; -fx-font-weight:bold;";
        switch (est) {
            case "SOLICITADO": return base + "-fx-background-color:#fef3c7; -fx-text-fill:#b45309;";
            case "EN_PROCESO": return base + "-fx-background-color:#dbeafe; -fx-text-fill:#1d4ed8;";
            case "COMPLETADO": return base + "-fx-background-color:#dcfce7; -fx-text-fill:#15803d;";
            case "CANCELADO":  return base + "-fx-background-color:#fee2e2; -fx-text-fill:#b91c1c;";
            default:           return base + "-fx-background-color:#f3f4f6; -fx-text-fill:#6b7280;";
        }
    }
}
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
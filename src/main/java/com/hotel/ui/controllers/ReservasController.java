package com.hotel.ui.controllers;

import com.hotel.AppContext;
import com.hotel.dto.ReservaDTO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.*;
import com.hotel.ui.components.NotificationUtil;
import javafx.animation.FadeTransition;
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
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Controlador del módulo de Reservas.
 * Tabla con filtros, badges outlined, cell factories tipadas y formulario CRUD.
 * Las reservas se muestran ordenadas por ID descendente (más recientes primero).
 */
public class ReservasController {

    // ── FXML — coinciden exactamente con fx:id en Reservas.fxml ──────────────
    @FXML private TableView<Reserva>          tabla;
    @FXML private TableColumn<Reserva,String> colId, colCliente, colHab, colEntrada;
    @FXML private TableColumn<Reserva,String> colSalida, colDias, colPersonas, colTotal, colEstado;
    @FXML private TextField                   searchField;
    @FXML private ComboBox<String>            filtroEstado;
    @FXML private HBox                        statsRow;
    @FXML private ProgressBar                 progressBar;
    @FXML private Label                       lblFecha, lblTurno;

    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-CO");

    private final AppContext             ctx      = AppContext.getInstance();
    private final ObservableList<Reserva> datos   = FXCollections.observableArrayList();
    private FilteredList<Reserva>         filtradas;

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        configurarFechaYTurno();
        configurarComboEstado();
        configurarColumnas();
        filtradas = new FilteredList<>(datos, p -> true);
        tabla.setItems(filtradas);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
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
            int hora = LocalDateTime.now().getHour();
            String t = hora < 12 ? "Turno Mañana 🌤" : hora < 18 ? "Turno Tarde 🌇" : "Turno Noche 🌙";
            lblTurno.setText("⏰  " + t);
        }
    }

    private void configurarComboEstado() {
        filtroEstado.setConverter(new StringConverter<String>() {
            @Override public String toString(String v) {
                if (v == null) return "";
                switch (v) {
                    case "Todos":      return "Todos";
                    case "PENDIENTE":  return "Pendiente";
                    case "CONFIRMADA": return "Confirmada";
                    case "EN_PROCESO": return "En proceso";
                    case "COMPLETADA": return "Completada";
                    case "CANCELADA":  return "Cancelada";
                    default:           return v;
                }
            }
            @Override public String fromString(String s) { return s; }
        });
        filtroEstado.getItems().addAll(
                "Todos", "PENDIENTE", "CONFIRMADA", "EN_PROCESO", "COMPLETADA", "CANCELADA");
        filtroEstado.setValue("Todos");
    }

    // ── Configuración de columnas ─────────────────────────────────────────────

    private void configurarColumnas() {
        // ID — gris con prefijo #
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colId.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); }
                else { setText(v); setStyle("-fx-text-fill:#64748b; -fx-font-size:12px;"); }
            }
        });

        // Cliente — bold, ★ si VIP
        colCliente.setCellValueFactory(c -> {
            if (c.getValue().getCliente() == null) return new SimpleStringProperty("—");
            String nombre = c.getValue().getCliente().obtenerNombreCompleto();
            if (c.getValue().getCliente().isEsVip()) nombre = "★ " + nombre;
            return new SimpleStringProperty(nombre);
        });
        colCliente.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); }
                else { setText(v); setStyle("-fx-font-weight:600; -fx-text-fill:#1e293b;"); }
            }
        });

        // Habitación — centrado
        colHab.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getHabitacion() != null ? c.getValue().getHabitacion().getNumero() : "—"));
        colHab.setCellFactory(col -> centeredCell());

        // Fechas
        colEntrada.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaEntrada() != null ? c.getValue().getFechaEntrada().toString() : "—"));
        colSalida.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaSalida() != null ? c.getValue().getFechaSalida().toString() : "—"));

        // Noches y Personas — centrado
        colDias.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(c.getValue().calcularTotalDiasReserva())));
        colDias.setCellFactory(col -> centeredCell());

        colPersonas.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(c.getValue().getNumPersonas())));
        colPersonas.setCellFactory(col -> centeredCell());

        // Total — bold oscuro
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("$%,.0f", c.getValue().getPrecioTotal())));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); }
                else { setText(v); setStyle("-fx-font-weight:700; -fx-text-fill:#1e293b;"); }
            }
        });

        // Estado — badge pill con colores del spec
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado().name()));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                Label badge = new Label(etiquetaEstado(v));
                badge.setStyle(badgeEstilo(v));
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        tabla.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Reserva r, boolean empty) {
                super.updateItem(r, empty);
                setStyle("");
            }
        });
    }

    // ── Carga de datos (ordenadas por ID descendente - más recientes primero) ──

    @FXML
    public void cargarDatos() {
        progressBar.setVisible(true);
        new Thread(() -> {
            try {
                List<Reserva> lista = ctx.getReservaService().listarTodasLasReservas();
                // Ordenar por ID descendente (las más recientes primero)
                lista.sort(Comparator.comparingInt(Reserva::getId).reversed());
                Platform.runLater(() -> {
                    datos.setAll(lista);
                    actualizarStats(lista);
                    progressBar.setVisible(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    NotificationUtil.error("Error cargando reservas: " + e.getMessage());
                    progressBar.setVisible(false);
                });
            }
        }, "carga-reservas").start();
    }

    // ── Búsqueda y filtro ─────────────────────────────────────────────────────

    @FXML
    public void filtrar() {
        String q     = searchField.getText().toLowerCase().trim();
        String estado = filtroEstado.getValue();
        filtradas.setPredicate(r -> {
            boolean matchTexto = q.isEmpty()
                    || String.valueOf(r.getId()).contains(q)
                    || (r.getCliente()    != null && r.getCliente().obtenerNombreCompleto().toLowerCase().contains(q))
                    || (r.getHabitacion() != null && r.getHabitacion().getNumero() != null
                            && r.getHabitacion().getNumero().toLowerCase().contains(q));
            boolean matchEstado = "Todos".equals(estado) || r.getEstado().name().equals(estado);
            return matchTexto && matchEstado;
        });
    }

    // ── Acciones FXML ─────────────────────────────────────────────────────────

    @FXML
    public void nuevaReserva() { mostrarFormularioNuevoModerno(); }

    @FXML
    public void confirmar() {
        Reserva sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { NotificationUtil.advertencia("Selecciona una reserva."); return; }
        try {
            ctx.getReservaService().confirmarReserva(sel.getId());
            NotificationUtil.exito("Reserva #" + sel.getId() + " confirmada.");
            cargarDatos();
        } catch (ExcepcionNegocio e) { NotificationUtil.error(e.getMessage()); }
    }

    @FXML
    public void cancelar() { mostrarDialogoCancelarModerno(); }

    @FXML
    public void handleDobleClick(MouseEvent e) {
        if (e.getClickCount() == 2) {
            Reserva sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) verDetalle(sel);
        }
    }

    // ─── DIÁLOGO DE CANCELAR RESERVA MODERNO ───────────────────────────────────

    private void mostrarDialogoCancelarModerno() {
        Reserva sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { 
            NotificationUtil.advertencia("Selecciona una reserva para cancelar."); 
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
        Label titleLabel = new Label("Cancelar Reserva");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        HBox infoBadge = new HBox(6);
        infoBadge.setAlignment(Pos.CENTER);
        infoBadge.setPadding(new Insets(4, 10, 4, 10));
        infoBadge.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 20px;");
        Label infoDot = new Label("●");
        infoDot.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 8px;");
        Label infoLabel = new Label("RESERVA #" + sel.getId());
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
        
        Label warningText = new Label("Esta acción cancelará la reserva y no se podrá revertir.");
        warningText.setStyle("-fx-font-size: 12px; -fx-text-fill: #92400e; -fx-font-weight: 500;");
        warningText.setWrapText(true);
        
        warningBox.getChildren().addAll(warningIcon, warningText);
        HBox.setHgrow(warningText, Priority.ALWAYS);
        body.getChildren().add(warningBox);
        
        // Información de la reserva
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(16);
        infoGrid.setVgap(12);
        infoGrid.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-padding: 16px;");
        
        String clienteNombre = sel.getCliente() != null ? sel.getCliente().obtenerNombreCompleto() : "—";
        String habNumero = sel.getHabitacion() != null ? sel.getHabitacion().getNumero() : "—";
        String entrada = sel.getFechaEntrada() != null ? sel.getFechaEntrada().toString() : "—";
        String salida = sel.getFechaSalida() != null ? sel.getFechaSalida().toString() : "—";
        
        infoGrid.addRow(0, crearLabelReserva("CLIENTE:"), new Label(clienteNombre));
        infoGrid.addRow(1, crearLabelReserva("HABITACIÓN:"), new Label(habNumero));
        infoGrid.addRow(2, crearLabelReserva("ENTRADA:"), new Label(entrada));
        infoGrid.addRow(3, crearLabelReserva("SALIDA:"), new Label(salida));
        infoGrid.addRow(4, crearLabelReserva("TOTAL:"), 
                new Label(String.format("$%,.0f", sel.getPrecioTotal())) {{ setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;"); }});
        
        body.getChildren().add(infoGrid);
        
        // Campo de motivo (opcional)
        VBox motivoBox = new VBox(8);
        Label motivoLabel = new Label("📝 MOTIVO DE CANCELACIÓN (OPCIONAL)");
        motivoLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        
        TextArea fMotivo = new TextArea();
        fMotivo.setPromptText("Ingrese el motivo de la cancelación...");
        fMotivo.setPrefRowCount(3);
        fMotivo.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                        "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px; " +
                        "-fx-padding: 8px 12px; -fx-font-size: 13px;");
        
        motivoBox.getChildren().addAll(motivoLabel, fMotivo);
        body.getChildren().add(motivoBox);
        
        // FOOTER
        VBox footer = new VBox();
        footer.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16px 16px;");
        
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #e2e8f0;");
        
        HBox footerContent = new HBox(16);
        footerContent.setAlignment(Pos.CENTER_RIGHT);
        footerContent.setPadding(new Insets(16, 24, 20, 24));
        
        Button volverBtn = new Button("Volver");
        volverBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; " +
                           "-fx-background-radius: 8px; -fx-padding: 10px 24px; " +
                           "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        volverBtn.setOnAction(e -> dialog.close());
        
        Button confirmarBtn = new Button("Confirmar Cancelación");
        confirmarBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; " +
                              "-fx-background-radius: 8px; -fx-padding: 10px 24px; " +
                              "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        
        footerContent.getChildren().addAll(volverBtn, confirmarBtn);
        footer.getChildren().addAll(divider, footerContent);
        
        mainContainer.getChildren().addAll(header, body, footer);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().setPrefSize(550, 550);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        
        confirmarBtn.setOnAction(e -> {
            confirmarBtn.setDisable(true);
            try {
                ctx.getReservaService().cancelarReserva(sel.getId());
                Platform.runLater(() -> {
                    NotificationUtil.exito("Reserva #" + sel.getId() + " cancelada.");
                    cargarDatos();
                    dialog.close();
                });
            } catch (ExcepcionNegocio ex) {
                Platform.runLater(() -> {
                    NotificationUtil.error(ex.getMessage());
                    confirmarBtn.setDisable(false);
                });
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
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
        // ─── NUEVO FORMULARIO DE RESERVA MODERNO ───────────────────────────────────

    private void mostrarFormularioNuevoModerno() {
        List<Cliente>    clientes     = ctx.getClienteService().listarTodosLosClientes();
        List<Habitacion> habitaciones = ctx.getHabitacionService().listarTodasLasHabitaciones();

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
        Label iconLabel = new Label("➕");
        iconLabel.setStyle("-fx-font-size: 22px;");
        iconBox.getChildren().add(iconLabel);
        
        VBox headerTexts = new VBox(4);
        Label titleLabel = new Label("Nueva Reserva");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        HBox totalBadge = new HBox(6);
        totalBadge.setAlignment(Pos.CENTER);
        totalBadge.setPadding(new Insets(4, 10, 4, 10));
        totalBadge.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 20px;");
        Label dotIndicator = new Label("●");
        dotIndicator.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 8px;");
        Label totalLabel = new Label("REGISTRE LOS DATOS DE LA RESERVA");
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
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        
        VBox body = new VBox(20);
        body.setPadding(new Insets(24));
        
        // SECCIÓN: DATOS DE LA RESERVA
        VBox section = new VBox(12);
        Label sectionTitle = new Label("📋 DATOS DE LA RESERVA");
        sectionTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        grid.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-padding: 20px;");
        
        // Cliente
        ComboBox<Cliente> fCliente = new ComboBox<>(FXCollections.observableArrayList(clientes));
        fCliente.setPromptText("Selecciona un cliente");
        fCliente.setPrefWidth(300);
        fCliente.setConverter(new StringConverter<Cliente>() {
            @Override public String toString(Cliente c) {
                return c == null ? "" : c.obtenerNombreCompleto() + " (ID: " + c.getId() + ")";
            }
            @Override public Cliente fromString(String s) { return null; }
        });
        fCliente.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                         "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-padding: 8px 12px;");
        
        // Habitación
        ComboBox<Habitacion> fHab = new ComboBox<>(FXCollections.observableArrayList(habitaciones));
        fHab.setPromptText("Selecciona una habitación");
        fHab.setPrefWidth(300);
        fHab.setConverter(new StringConverter<Habitacion>() {
            @Override public String toString(Habitacion h) {
                if (h == null) return "";
                String tipo = h.getTipoHabitacion() != null
                        ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "?";
                return String.format("Hab. %s — %s — $%,.0f/noche [%s]",
                        h.getNumero(), tipo, h.getPrecioBase(), h.getEstado().name());
            }
            @Override public Habitacion fromString(String s) { return null; }
        });
        fHab.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                     "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-padding: 8px 12px;");
        
        // Canal
        ComboBox<String> fCanal = new ComboBox<>();
        fCanal.getItems().addAll("CAN01", "CAN02", "CAN03", "CAN04", "CAN05");
        fCanal.setValue("CAN01");
        fCanal.setPrefWidth(300);
        fCanal.setConverter(new StringConverter<String>() {
            @Override public String toString(String c) {
                if (c == null) return "";
                switch (c) {
                    case "CAN01": return "Recepción (presencial)";
                    case "CAN02": return "Teléfono";
                    case "CAN03": return "Web propia";
                    case "CAN04": return "OTA (Booking, Airbnb…)";
                    case "CAN05": return "Corporativo";
                    default: return c;
                }
            }
            @Override public String fromString(String s) { return s; }
        });
        fCanal.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                       "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-padding: 8px 12px;");
        
        // Fechas
        DatePicker fEntrada = new DatePicker(LocalDate.now().plusDays(1));
        DatePicker fSalida  = new DatePicker(LocalDate.now().plusDays(3));
        fEntrada.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                         "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-padding: 8px 12px;");
        fSalida.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                        "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-padding: 8px 12px;");
        fEntrada.setPrefWidth(200);
        fSalida.setPrefWidth(200);
        
        // Personas
        Spinner<Integer> fPers = new Spinner<>(1, 10, 1);
        fPers.setEditable(true);
        fPers.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                      "-fx-border-width: 1.5px; -fx-border-radius: 8px;");
        fPers.setPrefWidth(200);
        
        grid.addRow(0, crearLabelReserva("CLIENTE *:"), fCliente);
        grid.addRow(1, crearLabelReserva("HABITACIÓN *:"), fHab);
        grid.addRow(2, crearLabelReserva("CANAL DE VENTA:"), fCanal);
        grid.addRow(3, crearLabelReserva("FECHA ENTRADA *:"), fEntrada);
        grid.addRow(4, crearLabelReserva("FECHA SALIDA *:"), fSalida);
        grid.addRow(5, crearLabelReserva("NÚMERO DE PERSONAS:"), fPers);
        
        section.getChildren().addAll(sectionTitle, grid);
        
        // Badge VIP
        Label vipLabel = new Label("★  Cliente VIP — descuento del 15% aplicado al total");
        vipLabel.setStyle("-fx-text-fill: #92400e; -fx-font-weight: bold; -fx-font-size: 11px; " +
                         "-fx-background-color: #fef9c3; -fx-padding: 6px 12px; -fx-background-radius: 6px;");
        vipLabel.setVisible(false);
        fCliente.valueProperty().addListener((obs, o, n) ->
                vipLabel.setVisible(n != null && n.isEsVip()));
        
        body.getChildren().addAll(section, vipLabel);
        
        // LABEL DE ERROR
        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        errLabel.setWrapText(true);
        errLabel.setVisible(false);
        body.getChildren().add(errLabel);
        
        scrollPane.setContent(body);
        
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
        
        Button crearBtn = new Button("Crear Reserva");
        crearBtn.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: white; " +
                         "-fx-background-radius: 8px; -fx-padding: 10px 24px; " +
                         "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        
        footerContent.getChildren().addAll(cancelarBtn, crearBtn);
        footer.getChildren().addAll(divider, footerContent);
        
        mainContainer.getChildren().addAll(header, scrollPane, footer);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().setPrefSize(750, 650);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        
        crearBtn.setOnAction(e -> {
            errLabel.setVisible(false);
            Cliente cs = fCliente.getValue();
            Habitacion hs = fHab.getValue();
            
            if (cs == null) { 
                errLabel.setText("Debes seleccionar un cliente."); 
                errLabel.setVisible(true); 
                return; 
            }
            if (hs == null) { 
                errLabel.setText("Debes seleccionar una habitación."); 
                errLabel.setVisible(true); 
                return; 
            }
            if (fEntrada.getValue() == null || fEntrada.getValue().isBefore(LocalDate.now())) {
                errLabel.setText("La fecha de entrada no puede ser anterior a hoy."); 
                errLabel.setVisible(true); 
                return;
            }
            if (fSalida.getValue() == null || !fSalida.getValue().isAfter(fEntrada.getValue())) {
                errLabel.setText("La fecha de salida debe ser posterior a la fecha de entrada."); 
                errLabel.setVisible(true); 
                return;
            }
            
            crearBtn.setDisable(true);
            
            new Thread(() -> {
                try {
                    ReservaDTO dto = new ReservaDTO(cs.getId(), hs.getNumero(),
                            fEntrada.getValue(), fSalida.getValue(), fPers.getValue(), fCanal.getValue());
                    Reserva r = ctx.getReservaService().crearReserva(dto);
                    Platform.runLater(() -> {
                        String vip = cs.isEsVip() ? " (descuento VIP 15%)" : "";
                        NotificationUtil.exito("Reserva #" + r.getId() + " creada. Total: $"
                                + String.format("%,.0f", r.getPrecioTotal()) + vip);
                        cargarDatos();
                        dialog.close();
                    });
                } catch (ExcepcionNegocio ex) {
                    Platform.runLater(() -> {
                        errLabel.setText("Error: " + ex.getMessage());
                        errLabel.setVisible(true);
                        crearBtn.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        errLabel.setText("Error: " + ex.getMessage());
                        errLabel.setVisible(true);
                        crearBtn.setDisable(false);
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

    // ─── Badges resumen (outlined) ─────────────────────────────────────────────

    private void actualizarStats(List<Reserva> lista) {
        statsRow.getChildren().clear();
        long pendientes  = lista.stream().filter(r -> r.getEstado() == Reserva.EstadoReserva.PENDIENTE).count();
        long confirmadas = lista.stream().filter(r -> r.getEstado() == Reserva.EstadoReserva.CONFIRMADA).count();
        long enProceso   = lista.stream().filter(r -> r.getEstado() == Reserva.EstadoReserva.EN_PROCESO).count();
        long completadas = lista.stream().filter(r -> r.getEstado() == Reserva.EstadoReserva.COMPLETADA).count();

        statsRow.getChildren().addAll(
            chip("📄  Total: "        + lista.size(),    "#93c5fd", "#1e40af"),
            chip("⏳  Pendientes: "   + pendientes,       "#fcd34d", "#92400e"),
            chip("✓  Confirmadas: "  + confirmadas,       "#86efac", "#166534"),
            chip("⚡  En proceso: "   + enProceso,        "#fdba74", "#c2410c"),
            chip("✅  Completadas: "  + completadas,       "#6366f1", "#312e81")
        );
    }

    private Label chip(String texto, String borderColor, String textColor) {
        Label l = new Label(texto);
        l.setStyle(
            "-fx-background-color:white;" +
            "-fx-border-color:"     + borderColor + ";" +
            "-fx-border-width:1.5;" +
            "-fx-border-radius:999;" +
            "-fx-background-radius:999;" +
            "-fx-padding:5 14;" +
            "-fx-font-size:12px;" +
            "-fx-font-weight:600;" +
            "-fx-text-fill:"  + textColor + ";");
        return l;
    }

    // ─── Detalle de reserva ────────────────────────────────────────────────────

    private void verDetalle(Reserva r) {
        boolean esVip = r.getCliente() != null && r.getCliente().isEsVip();
        String clienteStr = r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—";
        if (esVip) clienteStr += " ★ VIP";
        String descStr = "";
        if (esVip) {
            double orig = r.getPrecioTotal() / 0.85;
            descStr = "\nDescuento VIP 15%: -$" + String.format("%,.2f", orig - r.getPrecioTotal())
                    + "\n(Sin descuento: $" + String.format("%,.2f", orig) + ")";
        }
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Detalle Reserva #" + r.getId());
        a.setHeaderText("Reserva #" + r.getId() + " — " + etiquetaEstado(r.getEstado().name()));
        a.setContentText(
            "Cliente: "    + clienteStr + "\n" +
            "Habitación: " + (r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—") + "\n" +
            "Entrada: "    + r.getFechaEntrada() + "\n" +
            "Salida: "     + r.getFechaSalida() + "\n" +
            "Noches: "     + r.calcularTotalDiasReserva() + "\n" +
            "Personas: "   + r.getNumPersonas() + "\n" +
            "Total: "      + String.format("$%,.2f", r.getPrecioTotal()) + descStr);
        a.showAndWait();
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Label crearLabelReserva(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        return l;
    }

    private String etiquetaEstado(String est) {
        switch (est) {
            case "PENDIENTE":  return "Pendiente";
            case "CONFIRMADA": return "Confirmada";
            case "EN_PROCESO": return "En proceso";
            case "COMPLETADA": return "Completada";
            case "CANCELADA":  return "Cancelada";
            default:           return est;
        }
    }

    private String badgeEstilo(String est) {
        String base = "-fx-background-radius:20; -fx-padding:4 14; " +
                      "-fx-font-size:11px; -fx-font-weight:bold;";
        switch (est) {
            case "COMPLETADA": return base + "-fx-background-color:#dcfce7; -fx-text-fill:#15803d;";
            case "PENDIENTE":  return base + "-fx-background-color:#fef9c3; -fx-text-fill:#a16207;";
            case "CONFIRMADA": return base + "-fx-background-color:#dbeafe; -fx-text-fill:#1d4ed8;";
            case "CANCELADA":  return base + "-fx-background-color:#fee2e2; -fx-text-fill:#dc2626;";
            case "EN_PROCESO": return base + "-fx-background-color:#fed7aa; -fx-text-fill:#c2410c;";
            default:           return base + "-fx-background-color:#f3f4f6; -fx-text-fill:#6b7280;";
        }
    }

    private TableCell<Reserva, String> centeredCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); }
                else { setText(v); setAlignment(Pos.CENTER); }
            }
        };
    }
}
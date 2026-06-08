package com.hotel.ui.controllers;

import com.hotel.AppContext;

import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.*;
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
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import java.util.stream.Collectors;
import javafx.util.StringConverter;
import com.hotel.model.Reserva;

/**
 * Controlador de Check-in / Check-out.
 * Tabla con todos los registros, badges de resumen y modales de acción.
 * Los registros se muestran ordenados por ID descendente (más recientes primero).
 */
public class CheckInOutController {

    // ── FXML — coinciden exactamente con fx:id en CheckInOut.fxml ─────────────
    @FXML private TableView<CheckInOut>          tabla;
    @FXML private TableColumn<CheckInOut,String> colId, colCliente, colHab;
    @FXML private TableColumn<CheckInOut,String> colEntrada, colSalida, colEmpleado;
    @FXML private TableColumn<CheckInOut,String> colHoraCheckin, colHoraCheckout, colEstado;
    @FXML private TextField                      searchField;
    @FXML private HBox                           statsRow;
    @FXML private ProgressBar                    progressBar;
    @FXML private Label                          lblFecha, lblTurno;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-CO");

    private final AppContext              ctx      = AppContext.getInstance();
    private final ObservableList<CheckInOut> datos = FXCollections.observableArrayList();
    private FilteredList<CheckInOut>         filtradas;

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        configurarFechaYTurno();
        configurarColumnas();
        filtradas = new FilteredList<>(datos, p -> true);
        tabla.setItems(filtradas);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cargarDatos();
    }

    private void configurarFechaYTurno() {
        if (lblFecha != null) {
            LocalDate hoy = LocalDate.now();
            String fechaStr = hoy.getDayOfWeek().getDisplayName(TextStyle.FULL, LOCALE_ES)
                    + " " + hoy.getDayOfMonth()
                    + " de " + hoy.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES);
            String fechaCap = fechaStr.substring(0,1).toUpperCase() + fechaStr.substring(1);
            lblFecha.setText("📅  " + fechaCap);
        }
        if (lblTurno != null) {
            int hora = LocalDateTime.now().getHour();
            String turno = hora < 12 ? "Turno Mañana 🌤" : hora < 18 ? "Turno Tarde 🌇" : "Turno Noche 🌙";
            lblTurno.setText("⏰  " + turno);
        }
    }

    // ── Configuración de columnas ─────────────────────────────────────────────

    private void configurarColumnas() {
        // ID — gris prefijo #
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colId.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); }
                else { setText(v); setStyle("-fx-text-fill:#9ca3af; -fx-font-size:12px;"); }
            }
        });

        // Cliente — bold
        colCliente.setCellValueFactory(c -> {
            Reserva r = c.getValue().getReserva();
            String nombre = r != null && r.getCliente() != null
                    ? r.getCliente().obtenerNombreCompleto() : "—";
            return new SimpleStringProperty(nombre);
        });
        colCliente.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); }
                else { setText(v); setStyle("-fx-font-weight:600; -fx-text-fill:#111827;"); }
            }
        });

        // Habitación — centrado
        colHab.setCellValueFactory(c -> {
            Reserva r = c.getValue().getReserva();
            return new SimpleStringProperty(r != null && r.getHabitacion() != null
                    ? r.getHabitacion().getNumero() : "—");
        });
        colHab.setCellFactory(col -> centeredCell());

        // Fechas reserva
        colEntrada.setCellValueFactory(c -> {
            Reserva r = c.getValue().getReserva();
            return new SimpleStringProperty(r != null && r.getFechaEntrada() != null
                    ? r.getFechaEntrada().toString() : "—");
        });
        colSalida.setCellValueFactory(c -> {
            Reserva r = c.getValue().getReserva();
            return new SimpleStringProperty(r != null && r.getFechaSalida() != null
                    ? r.getFechaSalida().toString() : "—");
        });

        // Empleado
        colEmpleado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEmpleadoResponsable() != null
                        ? c.getValue().getEmpleadoResponsable().obtenerNombreCompleto() : "—"));

        // Hora Check-in — gris/cursiva si "—"
        colHoraCheckin.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaHoraCheckin() != null
                        ? c.getValue().getFechaHoraCheckin().format(FMT) : "—"));
        colHoraCheckin.setCellFactory(col -> timestampCell());

        // Hora Check-out — gris/cursiva si "—"
        colHoraCheckout.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaHoraCheckout() != null
                        ? c.getValue().getFechaHoraCheckout().format(FMT) : "—"));
        colHoraCheckout.setCellFactory(col -> timestampCell());

        // Estado — badge pill con colores
        colEstado.setCellValueFactory(c -> {
            Reserva r = c.getValue().getReserva();
            return new SimpleStringProperty(r != null && r.getEstado() != null
                    ? r.getEstado().name() : "—");
        });
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null || "—".equals(v)) { setGraphic(null); setText(null); return; }
                Label badge = new Label(etiquetaEstado(v));
                badge.setStyle(badgeEstilo(v));
                setGraphic(badge); setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        tabla.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(CheckInOut ci, boolean empty) {
                super.updateItem(ci, empty);
                setStyle("");
            }
        });
    }

    // ── Carga de datos (ordenados por ID descendente - más recientes primero) ──

    @FXML
    public void cargarDatos() {
        progressBar.setVisible(true);
        new Thread(() -> {
            try {
                List<CheckInOut> lista = ctx.getCheckInOutService().listarTodos();
                // Ordenar por ID descendente (los más recientes primero)
                lista.sort(Comparator.comparingInt(CheckInOut::getId).reversed());
                Platform.runLater(() -> {
                    datos.setAll(lista);
                    actualizarStats(lista);
                    progressBar.setVisible(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    NotificationUtil.error("Error cargando registros: " + e.getMessage());
                    progressBar.setVisible(false);
                });
            }
        }, "carga-checkinout").start();
    }

    // ── Búsqueda ──────────────────────────────────────────────────────────────

    @FXML
    public void filtrar() {
        String q = searchField.getText().toLowerCase().trim();
        filtradas.setPredicate(ci -> {
            if (q.isEmpty()) return true;
            Reserva r = ci.getReserva();
            return String.valueOf(ci.getId()).contains(q)
                || (r != null && r.getCliente() != null
                        && r.getCliente().obtenerNombreCompleto().toLowerCase().contains(q))
                || (r != null && r.getHabitacion() != null
                        && r.getHabitacion().getNumero().toLowerCase().contains(q))
                || (ci.getEmpleadoResponsable() != null
                        && ci.getEmpleadoResponsable().obtenerNombreCompleto().toLowerCase().contains(q));
        });
    }

    // ── Acciones FXML (wrappers que coinciden con onAction en FXML) ───────────

    @FXML public void hacerCheckin()  { mostrarFormularioCheckinModerno(); }
    @FXML public void hacerCheckout() { realizarCheckout(); }
    @FXML public void autoCheckout()  { checkoutsAutomaticos(); }

    @FXML
    public void handleDobleClick(MouseEvent e) {
        if (e.getClickCount() == 2) {
            CheckInOut sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) verDetalle(sel);
        }
    }

    // ── Lógica de negocio ─────────────────────────────────────────────────────

    private void realizarCheckout() {
        CheckInOut sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) {
            NotificationUtil.advertencia("Selecciona un registro activo para hacer check-out.");
            return;
        }
        if (sel.haRealizadoCheckout()) {
            NotificationUtil.advertencia("Este registro ya tiene check-out registrado.");
            return;
        }
        mostrarDialogCheckoutModerno(sel);
    }

    private void checkoutsAutomaticos() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Checkouts Automáticos");
        a.setHeaderText("¿Procesar checkouts automáticos?");
        a.setContentText("Se procesarán todos los check-ins activos cuya fecha de salida ya venció.");
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    int n = ctx.getCheckInOutService().procesarCheckoutsAutomaticos();
                    Platform.runLater(() -> {
                        NotificationUtil.exito("Checkouts automáticos: " + n + " procesado(s).");
                        cargarDatos();
                    });
                }).start();
            }
        });
    }

    private void verDetalle(CheckInOut ci) {
        Reserva r = ci.getReserva();
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Detalle #" + ci.getId());
        a.setHeaderText("Registro de Check-in/out #" + ci.getId());
        String txt = "Cliente: "   + (r != null && r.getCliente()     != null ? r.getCliente().obtenerNombreCompleto() : "—") + "\n"
                   + "Habitación: "+ (r != null && r.getHabitacion()  != null ? r.getHabitacion().getNumero()          : "—") + "\n"
                   + "Entrada: "   + (r != null && r.getFechaEntrada()!= null ? r.getFechaEntrada().toString()         : "—") + "\n"
                   + "Salida: "    + (r != null && r.getFechaSalida() != null ? r.getFechaSalida().toString()          : "—") + "\n"
                   + "Check-in: "  + (ci.getFechaHoraCheckin()  != null ? ci.getFechaHoraCheckin().format(FMT)         : "—") + "\n"
                   + "Check-out: " + (ci.getFechaHoraCheckout() != null ? ci.getFechaHoraCheckout().format(FMT)        : "—") + "\n"
                   + "Empleado: "  + (ci.getEmpleadoResponsable() != null ? ci.getEmpleadoResponsable().obtenerNombreCompleto() : "—");
        a.setContentText(txt);
        a.showAndWait();
    }

    // ─── NUEVO DIÁLOGO DE CHECK-IN MODERNO (ESTILO DASHBOARD) ──────────────────

    private void mostrarFormularioCheckinModerno() {
        // ── Cargar reservas CONFIRMADAS y PENDIENTES disponibles ─────────────
        List<Reserva> reservasDisp;
        try {
            reservasDisp = ctx.getReservaService().listarTodasLasReservas().stream()
                    .filter(r -> r.getEstado() == Reserva.EstadoReserva.CONFIRMADA
                              || r.getEstado() == Reserva.EstadoReserva.PENDIENTE)
                    .sorted(Comparator.comparing(r -> r.getFechaEntrada() != null
                            ? r.getFechaEntrada() : LocalDate.MAX))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            NotificationUtil.error("Error cargando reservas: " + e.getMessage());
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Sombra paralela sutil
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.25));
        shadow.setRadius(24);
        shadow.setOffsetY(8);
        dialog.getDialogPane().setEffect(shadow);
        
        VBox mainContainer = new VBox(0);
        mainContainer.setStyle("-fx-background-color: white; -fx-background-radius: 16px;");
        
        // ─── CABECERA (Fondo gris oscuro #1a1a2e) ─────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 16px 16px 0 0;");
        
        // Icono cuadrado con fondo azul real
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(48, 48);
        iconBox.setMaxSize(48, 48);
        iconBox.setStyle("-fx-background-color: #1e3a5f; -fx-background-radius: 12px;");
        Label iconLabel = new Label("🔑");
        iconLabel.setStyle("-fx-font-size: 22px;");
        iconBox.getChildren().add(iconLabel);
        
        // Textos del header
        VBox headerTexts = new VBox(4);
        Label titleLabel = new Label("Realizar Check-in");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // Badge con total disponible
        HBox totalBadge = new HBox(6);
        totalBadge.setAlignment(Pos.CENTER);
        totalBadge.setPadding(new Insets(4, 10, 4, 10));
        totalBadge.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 20px;");
        Label dotIndicator = new Label("●");
        dotIndicator.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 8px;");
        Label totalLabel = new Label(reservasDisp.size() + " RESERVA(S) DISPONIBLE(S)");
        totalLabel.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 11px; -fx-font-weight: 600;");
        totalBadge.getChildren().addAll(dotIndicator, totalLabel);
        headerTexts.getChildren().addAll(titleLabel, totalBadge);
        
        // Botón de cierre (X)
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #a0a0c0; " +
                         "-fx-font-size: 14px; -fx-background-radius: 20px; -fx-cursor: hand; " +
                         "-fx-padding: 6px 10px;");
        closeBtn.setOnAction(e -> dialog.close());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(iconBox, headerTexts, spacer, closeBtn);
        
        // ─── CUERPO DEL MODAL ─────────────────────────────────────────────────
        VBox body = new VBox(20);
        body.setPadding(new Insets(24));
        
        // ─── SECCIÓN: SELECCIÓN DE RESERVA ───────────────────────────────────
        VBox reservaSection = new VBox(8);
        Label reservaTitle = new Label("📋 RESERVA PARA CHECK-IN");
        reservaTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        
        ComboBox<Reserva> cmbReserva = new ComboBox<>(
                FXCollections.observableArrayList(reservasDisp));
        cmbReserva.setPromptText("Selecciona la reserva del cliente que llegó...");
        cmbReserva.setMaxWidth(Double.MAX_VALUE);
        cmbReserva.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; " +
                            "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px; " +
                            "-fx-padding: 10px; -fx-font-size: 13px;");
        cmbReserva.setConverter(new StringConverter<>() {
            @Override public String toString(Reserva r) {
                if (r == null) return "";
                String cli = r.getCliente() != null
                        ? r.getCliente().obtenerNombreCompleto() : "?";
                String hab = r.getHabitacion() != null
                        ? r.getHabitacion().getNumero() : "?";
                String ent = r.getFechaEntrada() != null
                        ? r.getFechaEntrada().toString() : "?";
                return "RES" + String.format("%03d", r.getId())
                        + "  │  " + cli
                        + "  │  Hab " + hab
                        + "  │  Entrada: " + ent
                        + "  [" + r.getEstado().name() + "]";
            }
            @Override public Reserva fromString(String s) { return null; }
        });
        
        Label lblAviso = new Label("⚠  No hay reservas CONFIRMADAS ni PENDIENTES en este momento.");
        lblAviso.setStyle("-fx-font-size:12px; -fx-text-fill:#b45309; -fx-padding:4 0;");
        lblAviso.setVisible(reservasDisp.isEmpty());
        lblAviso.setManaged(reservasDisp.isEmpty());
        
        reservaSection.getChildren().addAll(reservaTitle, cmbReserva, lblAviso);
        
        // ─── SECCIÓN: DATOS DEL CLIENTE ───────────────────────────────────────
        VBox clienteSection = new VBox(12);
        Label clienteTitle = new Label("👤 DATOS DEL CLIENTE");
        clienteTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        
        GridPane gridCliente = new GridPane();
        gridCliente.setHgap(16);
        gridCliente.setVgap(12);
        gridCliente.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-padding: 16px;");
        
        TextField fCedula = new TextField("—");
        TextField fNombre = new TextField("—");
        TextField fApellido = new TextField("—");
        TextField fEmail = new TextField("—");
        TextField fTelefono = new TextField("—");
        
        String campoStyle = "-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                            "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px; " +
                            "-fx-padding: 8px 12px; -fx-font-size: 13px; -fx-text-fill: #1e293b; " +
                            "-fx-disabled-opacity: 0.8;";
        
        fCedula.setStyle(campoStyle);
        fNombre.setStyle(campoStyle);
        fApellido.setStyle(campoStyle);
        fEmail.setStyle(campoStyle);
        fTelefono.setStyle(campoStyle);
        
        fCedula.setDisable(true);
        fNombre.setDisable(true);
        fApellido.setDisable(true);
        fEmail.setDisable(true);
        fTelefono.setDisable(true);
        
        gridCliente.addRow(0, crearLabelCampo("CÉDULA:"), fCedula, crearLabelCampo("NOMBRE:"), fNombre);
        gridCliente.addRow(1, crearLabelCampo("APELLIDO:"), fApellido, crearLabelCampo("EMAIL:"), fEmail);
        gridCliente.addRow(2, crearLabelCampo("TELÉFONO:"), fTelefono);
        
        clienteSection.getChildren().addAll(clienteTitle, gridCliente);
        
        // ─── SECCIÓN: DATOS DEL INGRESO ───────────────────────────────────────
        VBox ingresoSection = new VBox(12);
        Label ingresoTitle = new Label("📅 DATOS DEL INGRESO");
        ingresoTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        
        GridPane gridIngreso = new GridPane();
        gridIngreso.setHgap(16);
        gridIngreso.setVgap(12);
        gridIngreso.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-padding: 16px;");
        
        TextField fIdEmpleado = new TextField();
        Empleado emp = ctx.getEmpleadoActual();
        if (emp != null) {
            fIdEmpleado.setText(String.valueOf(emp.getId()));
            fIdEmpleado.setEditable(false);
        }
        fIdEmpleado.setStyle(campoStyle);
        
        Label lblHora = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        lblHora.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #1e293b; " +
                         "-fx-background-color: #eef2ff; -fx-background-radius: 8px; -fx-padding: 8px 12px;");
        
        TextArea fObs = new TextArea();
        fObs.setPromptText("Ingresa observaciones adicionales (opcional)...");
        fObs.setPrefRowCount(2);
        fObs.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                      "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px; " +
                      "-fx-padding: 8px 12px; -fx-font-size: 13px;");
        
        gridIngreso.addRow(0, crearLabelCampo("ID EMPLEADO:"), fIdEmpleado, crearLabelCampo("HORA INGRESO:"), lblHora);
        gridIngreso.addRow(1, crearLabelCampo("OBSERVACIONES:"), fObs);
        GridPane.setColumnSpan(fObs, 3);
        
        ingresoSection.getChildren().addAll(ingresoTitle, gridIngreso);
        
        // Cuando se selecciona una reserva, cargar los datos del cliente
        cmbReserva.setOnAction(ev -> {
            Reserva sel = cmbReserva.getValue();
            if (sel == null) return;
            Cliente c = sel.getCliente();
            if (c != null) {
                fCedula.setText(c.getDocumento() != null ? c.getDocumento() : String.valueOf(c.getId()));
                fNombre.setText(c.getNombre() != null ? c.getNombre() : "");
                fApellido.setText(c.getApellido() != null ? c.getApellido() : "");
                fEmail.setText(c.getEmail() != null ? c.getEmail() : "");
                fTelefono.setText(c.getTelefono() != null ? c.getTelefono() : "");
            }
        });
        
        body.getChildren().addAll(reservaSection, clienteSection, ingresoSection);
        
        // ─── INDICADOR DE CARGA Y ERROR ───────────────────────────────────────
        HBox loadingBox = new HBox();
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(8, 0, 0, 0));
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(28, 28);
        spinner.setVisible(false);
        loadingBox.getChildren().add(spinner);
        
        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        errLabel.setWrapText(true);
        errLabel.setVisible(false);
        
        body.getChildren().addAll(loadingBox, errLabel);
        
        // ─── FOOTER CON BOTONES ───────────────────────────────────────────────
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
        
        Button confirmarBtn = new Button("Registrar Check-in");
        confirmarBtn.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: white; " +
                              "-fx-background-radius: 8px; -fx-padding: 10px 24px; " +
                              "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        
        footerContent.getChildren().addAll(cancelarBtn, confirmarBtn);
        footer.getChildren().addAll(divider, footerContent);
        
        mainContainer.getChildren().addAll(header, body, footer);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().setPrefSize(750, 650);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        
        // Configurar acción del botón confirmar
        confirmarBtn.setOnAction(e -> {
            errLabel.setVisible(false);
            Reserva selReserva = cmbReserva.getValue();
            if (selReserva == null) {
                errLabel.setText("Selecciona una reserva de la lista.");
                errLabel.setVisible(true);
                return;
            }
            try {
                int idEmpleado = Integer.parseInt(fIdEmpleado.getText().trim());
                confirmarBtn.setDisable(true);
                spinner.setVisible(true);
                
                new Thread(() -> {
                    try {
                        ctx.getCheckInOutService().realizarCheckin(
                                selReserva.getId(), idEmpleado, fObs.getText().trim());
                        Platform.runLater(() -> {
                            NotificationUtil.exito("Check-in registrado — Reserva #" + selReserva.getId() + ".");
                            cargarDatos();
                            dialog.close();
                        });
                    } catch (ExcepcionNegocio ex) {
                        Platform.runLater(() -> {
                            errLabel.setText("Error: " + ex.getMessage());
                            errLabel.setVisible(true);
                            confirmarBtn.setDisable(false);
                            spinner.setVisible(false);
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            errLabel.setText("Error inesperado: " + ex.getMessage());
                            errLabel.setVisible(true);
                            confirmarBtn.setDisable(false);
                            spinner.setVisible(false);
                        });
                    }
                }).start();
            } catch (NumberFormatException ex) {
                errLabel.setText("ID de empleado debe ser un número entero.");
                errLabel.setVisible(true);
            }
        });
        
        // Ocultar botón de cierre por defecto
        Button defaultCloseButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        defaultCloseButton.setVisible(false);
        defaultCloseButton.setManaged(false);
        
        // Animación de entrada
        dialog.setOnShown(ev -> {
            FadeTransition ft = new FadeTransition(Duration.millis(250), dialog.getDialogPane());
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        });
        
        dialog.showAndWait();
    }

    // ─── NUEVO DIÁLOGO DE CHECK-OUT MODERNO (ESTILO DASHBOARD) ─────────────────

    private void mostrarDialogCheckoutModerno(CheckInOut sel) {
        Reserva r = sel.getReserva();
        if (r == null) {
            NotificationUtil.error("No se encontró la reserva asociada.");
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
        Label iconLabel = new Label("🔓");
        iconLabel.setStyle("-fx-font-size: 22px;");
        iconBox.getChildren().add(iconLabel);
        
        VBox headerTexts = new VBox(4);
        Label titleLabel = new Label("Check-out — Registro #" + sel.getId());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        HBox estadoBadge = new HBox(6);
        estadoBadge.setAlignment(Pos.CENTER);
        estadoBadge.setPadding(new Insets(4, 10, 4, 10));
        estadoBadge.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 20px;");
        Label dotIndicator = new Label("●");
        dotIndicator.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 8px;");
        Label estadoLabel = new Label("CHECK-IN ACTIVO");
        estadoLabel.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 11px; -fx-font-weight: 600;");
        estadoBadge.getChildren().addAll(dotIndicator, estadoLabel);
        headerTexts.getChildren().addAll(titleLabel, estadoBadge);
        
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
        
        // Información de la reserva
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(20);
        infoGrid.setVgap(16);
        infoGrid.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-padding: 20px;");
        
        VBox clienteBox = new VBox(4);
        Label clienteLabel = new Label("👤 CLIENTE");
        clienteLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #64748b;");
        String nombreCliente = r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—";
        Label clienteValor = new Label(nombreCliente);
        clienteValor.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");
        clienteBox.getChildren().addAll(clienteLabel, clienteValor);
        
        VBox habBox = new VBox(4);
        Label habLabel = new Label("🏨 HABITACIÓN");
        habLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #64748b;");
        String numHab = r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—";
        Label habValor = new Label(numHab);
        habValor.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");
        habBox.getChildren().addAll(habLabel, habValor);
        
        VBox checkinBox = new VBox(4);
        Label checkinLabel = new Label("📅 CHECK-IN");
        checkinLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #64748b;");
        String fechaCheckin = sel.getFechaHoraCheckin() != null 
                ? sel.getFechaHoraCheckin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";
        Label checkinValor = new Label(fechaCheckin);
        checkinValor.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #475569;");
        checkinBox.getChildren().addAll(checkinLabel, checkinValor);
        
        infoGrid.add(clienteBox, 0, 0);
        infoGrid.add(habBox, 1, 0);
        infoGrid.add(checkinBox, 2, 0);
        
        body.getChildren().add(infoGrid);
        
        // Advertencia
        HBox warningBox = new HBox(12);
        warningBox.setAlignment(Pos.CENTER_LEFT);
        warningBox.setStyle("-fx-background-color: #fef3c7; -fx-background-radius: 10px; -fx-padding: 12px 16px;");
        Label warningIcon = new Label("⚠️");
        warningIcon.setStyle("-fx-font-size: 16px;");
        Label warningText = new Label("Asegúrate de verificar el estado de la habitación antes de confirmar el check-out.");
        warningText.setStyle("-fx-font-size: 12px; -fx-text-fill: #92400e; -fx-font-weight: 500;");
        warningText.setWrapText(true);
        warningBox.getChildren().addAll(warningIcon, warningText);
        HBox.setHgrow(warningText, Priority.ALWAYS);
        body.getChildren().add(warningBox);
        
        // Observaciones
        VBox obsBox = new VBox(8);
        Label obsLabel = new Label("📝 OBSERVACIONES");
        obsLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        TextArea fObs = new TextArea();
        fObs.setPromptText("Ingresa observaciones adicionales (opcional)...");
        fObs.setPrefRowCount(3);
        fObs.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                      "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px; " +
                      "-fx-font-size: 13px; -fx-padding: 10px;");
        obsBox.getChildren().addAll(obsLabel, fObs);
        body.getChildren().add(obsBox);
        
        // Loading y error
        HBox loadingBox = new HBox();
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(8, 0, 0, 0));
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(28, 28);
        spinner.setVisible(false);
        loadingBox.getChildren().add(spinner);
        
        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        errLabel.setWrapText(true);
        errLabel.setVisible(false);
        
        body.getChildren().addAll(loadingBox, errLabel);
        
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
        
        Button confirmarBtn = new Button("Confirmar Check-out");
        confirmarBtn.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: white; " +
                              "-fx-background-radius: 8px; -fx-padding: 10px 24px; " +
                              "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        
        footerContent.getChildren().addAll(cancelarBtn, confirmarBtn);
        footer.getChildren().addAll(divider, footerContent);
        
        mainContainer.getChildren().addAll(header, body, footer);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().setPrefSize(600, 550);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        
        confirmarBtn.setOnAction(e -> {
            errLabel.setVisible(false);
            confirmarBtn.setDisable(true);
            spinner.setVisible(true);
            
            new Thread(() -> {
                try {
                    ctx.getCheckInOutService().realizarCheckout(r.getId(), fObs.getText().trim());
                    Platform.runLater(() -> {
                        NotificationUtil.exito("Check-out registrado correctamente (Reserva #" + r.getId() + ").");
                        cargarDatos();
                        dialog.close();
                    });
                } catch (ExcepcionNegocio ex) {
                    Platform.runLater(() -> {
                        errLabel.setText("Error: " + ex.getMessage());
                        errLabel.setVisible(true);
                        confirmarBtn.setDisable(false);
                        spinner.setVisible(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        errLabel.setText("Error inesperado: " + ex.getMessage());
                        errLabel.setVisible(true);
                        confirmarBtn.setDisable(false);
                        spinner.setVisible(false);
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

    // ─── STATS BADGES ─────────────────────────────────────────────────────────

    private void actualizarStats(List<CheckInOut> lista) {
        LocalDate hoy = LocalDate.now();
        long activos      = lista.stream().filter(ci -> !ci.haRealizadoCheckout()).count();
        long checkoutsHoy = lista.stream()
                .filter(ci -> ci.getFechaHoraCheckout() != null
                        && ci.getFechaHoraCheckout().toLocalDate().equals(hoy))
                .count();
        statsRow.getChildren().clear();
        statsRow.getChildren().addAll(
            badgeChip("🔑  Check-ins activos: " + activos,   "#fca5a5", "#991b1b"),
            badgeChip("🔓  Check-outs hoy: "  + checkoutsHoy,"#fcd34d", "#92400e")
        );
    }

    private Label badgeChip(String texto, String borderColor, String textColor) {
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

    // ── Cell factories ────────────────────────────────────────────────────────

    private TableCell<CheckInOut, String> centeredCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); }
                else { setText(v); setAlignment(Pos.CENTER); }
            }
        };
    }

    private TableCell<CheckInOut, String> timestampCell() {
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
            case "COMPLETADA":  return "✓ Completada";
            case "CANCELADA":   return "✕ Cancelada";
            case "CONFIRMADA":  return "● Confirmada";
            case "EN_PROCESO":  return "▶ En proceso";
            case "PENDIENTE":   return "⏳ Pendiente";
            default:            return est;
        }
    }

    private String badgeEstilo(String est) {
        String base = "-fx-background-radius:999; -fx-padding:3 10; " +
                      "-fx-font-size:10.5px; -fx-font-weight:700;";
        switch (est) {
            case "COMPLETADA":  return base + "-fx-background-color:#dcfce7; -fx-text-fill:#166534;";
            case "CANCELADA":   return base + "-fx-background-color:#fee2e2; -fx-text-fill:#991b1b;";
            case "CONFIRMADA":  return base + "-fx-background-color:#dbeafe; -fx-text-fill:#1e40af;";
            case "EN_PROCESO":  return base + "-fx-background-color:#ffedd5; -fx-text-fill:#c2410c;";
            case "PENDIENTE":   return base + "-fx-background-color:#fef9c3; -fx-text-fill:#92400e;";
            default:            return base + "-fx-background-color:#f3f4f6; -fx-text-fill:#6b7280;";
        }
    }

    // ─── Helpers de UI ─────────────────────────────────────────────────────────

    private Label crearLabelCampo(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        return l;
    }
}
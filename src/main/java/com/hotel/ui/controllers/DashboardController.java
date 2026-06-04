package com.hotel.ui.controllers;

import com.hotel.AppContext;
import com.hotel.model.*;
import com.hotel.service.GeminiApiService;
import com.hotel.ui.components.AvatarUtil;
import com.hotel.ui.components.NotificationUtil;
import com.hotel.util.ManejadorExcepciones;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.stage.Stage;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class DashboardController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private StackPane                    centerRoot;
    @FXML private StackPane                    contentArea;
    @FXML private Label                        labelUsuario, labelRol, labelFecha;
    @FXML private Label                        topbarTitle, topbarBreadcrumb;
    @FXML private Label                        userAvatarLabel;
    @FXML private HBox                         sidebarUserHBox;
    @FXML private javafx.scene.image.ImageView logoView;
    @FXML private Label  lblReloj;
    @FXML private Button btnMaximize, btnCerrar;
    private       Timeline relojTimeline;

    // ── Chatbot ───────────────────────────────────────────────────────────────
    private VBox       chatPanel;
    private VBox       mensajesBox;
    private ScrollPane mensajesScroll;
    private TextField  chatInput;
    private boolean    chatVisible = false;

    // ── Navegación ────────────────────────────────────────────────────────────
    @FXML private Button navDashboard, navReservas, navCheckin;
    @FXML private Button navClientes, navHabitaciones, navMantenimiento;
    @FXML private Button navFacturacion, navReportes, navEmpleados;
    @FXML private Label  sectionOperaciones, sectionRecursos, sectionAdministracion;

    private final AppContext ctx = AppContext.getInstance();
    private Button navActivo;

    private static final java.util.Locale LOCALE_ES = java.util.Locale.forLanguageTag("es-CO");

    @FXML
    public void initialize() {
        Empleado emp = ctx.getEmpleadoActual();
        if (emp != null) {
            labelUsuario.setText(emp.obtenerNombreCompleto());
            labelRol.setText(emp.getCargo() != null ? emp.getCargo().getNombreCargo() : "");
            StackPane avatar = AvatarUtil.crear(emp.obtenerNombreCompleto(), 36);
            int idx = sidebarUserHBox.getChildren().indexOf(userAvatarLabel);
            sidebarUserHBox.getChildren().set(idx, avatar);
        }
        labelFecha.setText(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM yyyy", LOCALE_ES)));

        DateTimeFormatter fmtReloj = DateTimeFormatter.ofPattern("hh:mm:ss a");
        lblReloj.setText(LocalTime.now().format(fmtReloj));
        relojTimeline = new Timeline(new KeyFrame(Duration.seconds(1),
                ev -> lblReloj.setText(LocalTime.now().format(fmtReloj))));
        relojTimeline.setCycleCount(Timeline.INDEFINITE);
        relojTimeline.play();

        Platform.runLater(() -> {
            Stage s = (Stage) contentArea.getScene().getWindow();
            if (s != null) {
                s.maximizedProperty().addListener((obs, old, max) ->
                        btnMaximize.setText(max ? "❐" : "⬜"));
                btnMaximize.setText(s.isMaximized() ? "❐" : "⬜");
            }
        });

        navActivo = navDashboard;
        cargarLogo();
        cargarPanelDashboard();
        aplicarRestriccionesRol();
        List.of(navDashboard, navReservas, navCheckin, navClientes,
                navHabitaciones, navMantenimiento, navFacturacion, navReportes, navEmpleados)
                .forEach(this::configurarHoverNav);
        Platform.runLater(this::initChatbot);
    }

    private void cargarLogo() {
        if (logoView == null) return;
        try {
            java.net.URL url = getClass().getResource("/com/hotel/ui/images/logo.png");
            if (url != null) {
                logoView.setImage(new javafx.scene.image.Image(
                        url.toExternalForm(), 130, 130, true, true));
            }
        } catch (Exception e) {
            System.out.println("[Dashboard] Logo no encontrado: " + e.getMessage());
        }
    }

    @FXML public void irADashboard()    { setActivo(navDashboard,    "Dashboard",    "Inicio > Dashboard");       cargarPanelDashboard(); }
    @FXML public void irAReservas()     { setActivo(navReservas,     "Reservas",     "Operaciones > Reservas");    cargarPanel("Reservas.fxml"); }
    @FXML public void irACheckin()      { setActivo(navCheckin,      "Check-in/out", "Operaciones > Check-in");    cargarPanel("CheckInOut.fxml"); }
    @FXML public void irAClientes()     { setActivo(navClientes,     "Clientes",     "Operaciones > Clientes");    cargarPanel("Clientes.fxml"); }
    @FXML public void irAHabitaciones() { setActivo(navHabitaciones, "Habitaciones", "Recursos > Habitaciones");   cargarPanel("Habitaciones.fxml"); }
    @FXML public void irAMantenimiento(){ setActivo(navMantenimiento,"Mantenimiento","Recursos > Mantenimiento");  cargarPanel("Mantenimiento.fxml"); }
    @FXML public void irAFacturacion()  { setActivo(navFacturacion,  "Facturación",  "Admin > Facturación");        cargarPanel("Facturacion.fxml"); }
    @FXML public void irAReportes()     { setActivo(navReportes,     "Reportes",     "Admin > Reportes");           cargarPanel("Reportes.fxml"); }
    @FXML public void irAEmpleados()    { setActivo(navEmpleados,    "Empleados",    "Admin > Empleados");          cargarPanel("Empleados.fxml"); }

    @FXML public void handleMaximize() {
        Stage stage = (Stage) contentArea.getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML public void handleCerrar() {
        Platform.exit();
        System.exit(0);
    }

    @FXML public void handleLogout() {
        ctx.getAuthService().logout(ctx.getTokenSesion());
        ctx.setTokenSesion(null);
        ctx.setEmpleadoActual(null);
        NavigatorUtil.irAlLogin();
    }

    private void initChatbot() {
        mensajesBox = new VBox(10);
        mensajesBox.setPadding(new Insets(12));
        mensajesBox.setFillWidth(true);

        mensajesScroll = new ScrollPane(mensajesBox);
        mensajesScroll.setFitToWidth(true);
        mensajesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        mensajesScroll.setStyle("-fx-background-color:transparent; -fx-border-color:transparent;");
        VBox.setVgrow(mensajesScroll, Priority.ALWAYS);

        Label titulo = new Label("🤖 Asistente Hotel");
        titulo.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:white;");
        Region spacerH = new Region();
        HBox.setHgrow(spacerH, Priority.ALWAYS);
        Button btnCerrarChat = new Button("✕");
        btnCerrarChat.setStyle("-fx-background-color:transparent; -fx-text-fill:white;" +
                           "-fx-font-size:14px; -fx-cursor:hand; -fx-padding:2px 8px;");
        btnCerrarChat.setOnAction(e -> toggleChat());
        HBox header = new HBox(8, titulo, spacerH, btnCerrarChat);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 14, 14, 16));
        header.setStyle("-fx-background-color:#1a3a5c; -fx-background-radius:16px 16px 0 0;");

        chatInput = new TextField();
        chatInput.setPromptText("Escribe tu mensaje...");
        chatInput.getStyleClass().add("chat-input-field");
        HBox.setHgrow(chatInput, Priority.ALWAYS);
        chatInput.setOnAction(e -> enviarMensaje());

        Button btnEnviar = new Button("➤");
        btnEnviar.getStyleClass().add("btn-enviar-chat");
        btnEnviar.setOnAction(e -> enviarMensaje());

        HBox inputArea = new HBox(8, chatInput, btnEnviar);
        inputArea.setAlignment(Pos.CENTER);
        inputArea.setPadding(new Insets(10, 14, 14, 14));
        inputArea.setStyle("-fx-background-color:white; -fx-background-radius:0 0 16px 16px;" +
                           "-fx-border-color:#e2e8f0; -fx-border-width:1px 0 0 0;");

        chatPanel = new VBox(header, mensajesScroll, inputArea);
        chatPanel.setPrefWidth(380);
        chatPanel.setPrefHeight(520);
        chatPanel.setMaxWidth(380);
        chatPanel.setMaxHeight(520);
        chatPanel.setStyle("-fx-background-color:white; -fx-background-radius:16px;" +
                           "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.25),24,0,0,6);");
        chatPanel.setVisible(false);
        chatPanel.setManaged(false);
        StackPane.setAlignment(chatPanel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatPanel, new Insets(0, 20, 80, 0));

        Button fab = new Button("💬");
        fab.getStyleClass().add("btn-chat-flotante");
        fab.setOnAction(e -> toggleChat());
        StackPane.setAlignment(fab, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(fab, new Insets(0, 20, 20, 0));

        centerRoot.getChildren().addAll(chatPanel, fab);
        agregarMensajeBot(ctx.getChatbotService().obtenerMensajeBienvenida());
    }

    private void toggleChat() {
        if (!chatVisible) {
            chatPanel.setVisible(true);
            chatPanel.setManaged(true);
            chatPanel.setTranslateY(30);
            chatPanel.setOpacity(0);
            TranslateTransition tt = new TranslateTransition(Duration.millis(220), chatPanel);
            tt.setToY(0);
            FadeTransition ft = new FadeTransition(Duration.millis(220), chatPanel);
            ft.setToValue(1);
            new ParallelTransition(tt, ft).play();
            chatVisible = true;
            Platform.runLater(() -> chatInput.requestFocus());
        } else {
            TranslateTransition tt = new TranslateTransition(Duration.millis(180), chatPanel);
            tt.setToY(30);
            FadeTransition ft = new FadeTransition(Duration.millis(180), chatPanel);
            ft.setToValue(0);
            ParallelTransition pt = new ParallelTransition(tt, ft);
            pt.setOnFinished(e -> { chatPanel.setVisible(false); chatPanel.setManaged(false); });
            pt.play();
            chatVisible = false;
        }
    }

    private void enviarMensaje() {
        String texto = chatInput.getText().trim();
        if (texto.isEmpty()) return;
        chatInput.clear();
        agregarMensajeUsuario(texto);

        Label typing = new Label("✦ Escribiendo...");
        typing.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:11px; -fx-padding:4px 14px;");
        mensajesBox.getChildren().add(typing);
        scrollAlFinal();

        new Thread(() -> {
            try {
                String contextoLocal = ctx.getChatbotService().procesarMensaje(texto);
                String respuesta;
                GeminiApiService gemini = ctx.getGeminiApiService();
                if (gemini != null && gemini.estaConfigurado()) {
                    try {
                        respuesta = gemini.consultar(contextoLocal, texto);
                    } catch (Exception ex) {
                        respuesta = contextoLocal;
                        System.err.println("[Chatbot] Gemini falló: " + ex.getMessage());
                    }
                } else {
                    respuesta = contextoLocal;
                }
                final String r = respuesta;
                Platform.runLater(() -> { mensajesBox.getChildren().remove(typing); agregarMensajeBot(r); });
            } catch (Exception e) {
                Platform.runLater(() -> { mensajesBox.getChildren().remove(typing);
                    agregarMensajeBot("Ocurrió un error. Por favor intenta de nuevo."); });
            }
        }).start();
    }

    private void agregarMensajeUsuario(String texto) {
        Label burbuja = new Label(texto);
        burbuja.setWrapText(true);
        burbuja.setMaxWidth(270);
        burbuja.setStyle("-fx-background-color:#1a3a5c; -fx-text-fill:white;" +
                         "-fx-background-radius:18px 18px 4px 18px;" +
                         "-fx-padding:10px 14px; -fx-font-size:13px;");
        HBox fila = new HBox(burbuja);
        fila.setAlignment(Pos.CENTER_RIGHT);
        mensajesBox.getChildren().add(fila);
        scrollAlFinal();
    }

    private void agregarMensajeBot(String texto) {
        Label burbuja = new Label(texto);
        burbuja.setWrapText(true);
        burbuja.setMaxWidth(280);
        burbuja.setStyle("-fx-background-color:#f1f5f9; -fx-text-fill:#1e293b;" +
                         "-fx-background-radius:18px 18px 18px 4px;" +
                         "-fx-padding:10px 14px; -fx-font-size:13px;");
        HBox fila = new HBox(burbuja);
        fila.setAlignment(Pos.CENTER_LEFT);
        mensajesBox.getChildren().add(fila);
        scrollAlFinal();
    }

    private void scrollAlFinal() {
        Platform.runLater(() -> mensajesScroll.setVvalue(1.0));
    }

    private void cargarPanelDashboard() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        VBox panel = new VBox(26);
        panel.setPadding(new Insets(28, 28, 40, 28));
        panel.setStyle("-fx-background-color: #f0f3fa;");

        HBox kpiRow = crearFilaKPI();
        VBox chartsContainer = new VBox(20);
        HBox bottomRow = crearFilaInferior();

        panel.getChildren().addAll(
                crearSaludoHeader(), kpiRow,
                chartsContainer, bottomRow);
        scroll.setContent(panel);
        setContenido(scroll);

        Thread hiloDashboard = new Thread(() -> {
            try {
                List<Reserva>    reservas     = ctx.getReservaService().listarTodasLasReservas();
                List<Habitacion> habitaciones = ctx.getHabitacionService().listarTodasLasHabitaciones();
                List<Cliente>    clientes     = ctx.getClienteService().listarTodosLosClientes();
                List<Factura>    facturas     = ctx.getFacturaService().listarTodasLasFacturas();
                List<CheckInOut> registros    = ctx.getCheckInOutService().listarTodos();

                long disponibles = habitaciones.stream()
                        .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.DISPONIBLE).count();
                long ocupadas    = habitaciones.stream()
                        .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.OCUPADA).count();
                double ingresos  = facturas.stream()
                        .filter(f -> f.getEstadoPago() == Factura.EstadoPago.PAGADA)
                        .mapToDouble(Factura::getTotal).sum();

                LocalDate hoy = LocalDate.now();
                long checkoutsRealizadosHoy = registros.stream()
                        .filter(c -> c.getFechaHoraCheckout() != null
                                  && c.getFechaHoraCheckout().toLocalDate().equals(hoy))
                        .count();
                long salidasPendientesHoy = reservas.stream()
                        .filter(r -> hoy.equals(r.getFechaSalida())
                                  && r.getEstado() == Reserva.EstadoReserva.EN_PROCESO)
                        .count();
                long checkoutsHoy = checkoutsRealizadosHoy + salidasPendientesHoy;

                Platform.runLater(() -> {
                    actualizarKPIs(kpiRow,
                            habitaciones.size(), disponibles, ocupadas,
                            clientes.size(), ingresos, checkoutsHoy,
                            habitaciones, clientes, facturas, reservas, registros);

                    actualizarChartsRow(chartsContainer, reservas, habitaciones, facturas);
                    actualizarFilaInferior(bottomRow, reservas, facturas);
                });

            } catch (Exception e) {
                e.printStackTrace();
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                Platform.runLater(() -> {
                    NotificationUtil.advertencia("Error cargando datos: " + msg);
                    kpiRow.getChildren().clear();
                    Label err = new Label("⚠ Error al cargar datos:\n" + msg);
                    err.setStyle("-fx-text-fill:#dc2626; -fx-font-size:13px;");
                    err.setWrapText(true);
                    kpiRow.getChildren().add(err);
                });
            }
        });
        hiloDashboard.setName("hilo-dashboard");
        hiloDashboard.setDaemon(true);
        hiloDashboard.setUncaughtExceptionHandler(new ManejadorExcepciones());
        hiloDashboard.start();
    }

    private VBox crearSaludoHeader() {
        VBox box = new VBox(5);
        int hora = java.time.LocalTime.now().getHour();
        String salTxt = hora < 12 ? "Buenos días" : hora < 18 ? "Buenas tardes" : "Buenas noches";
        String nombre = ctx.getEmpleadoActual() != null ? ctx.getEmpleadoActual().getNombre() : "";
        Label saludo = new Label(salTxt + ", " + nombre + " 👋");
        saludo.setStyle("-fx-font-size:22px; -fx-font-weight:800; -fx-text-fill:#1e293b;");
        Label sub = new Label("Aquí tienes el resumen del hotel en tiempo real");
        sub.setStyle("-fx-font-size:14px; -fx-text-fill:#64748b;");

        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        badgeRow.setPadding(new Insets(5, 0, 0, 0));

        String fechaStr = LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM", LOCALE_ES));
        String fechaCap = fechaStr.substring(0,1).toUpperCase() + fechaStr.substring(1);
        Label fechaBadge = new Label("📅 " + fechaCap);
        fechaBadge.setStyle("-fx-font-size:11.5px; -fx-font-weight:600; -fx-text-fill:#3b82f6;" +
                "-fx-background-color:#eff6ff; -fx-background-radius:20px; -fx-padding:4px 13px;");

        String turno = hora < 12 ? "Turno Mañana 🌤" : hora < 18 ? "Turno Tarde 🌇" : "Turno Noche 🌙";
        Label turnoBadge = new Label(turno);
        turnoBadge.setStyle("-fx-font-size:11.5px; -fx-font-weight:600; -fx-text-fill:#7c3aed;" +
                "-fx-background-color:#f5f3ff; -fx-background-radius:20px; -fx-padding:4px 13px;");

        badgeRow.getChildren().addAll(fechaBadge, turnoBadge);
        box.getChildren().addAll(saludo, sub, badgeRow);
        return box;
    }

    private HBox crearFilaKPI() {
        HBox row = new HBox(16);
        for (int i = 0; i < 6; i++) {
            VBox card = new VBox(10);
            card.setPadding(new Insets(20));
            card.setStyle("-fx-background-color:white; -fx-background-radius:14px;" +
                          "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");
            Label val = new Label("—");
            val.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:#e2e8f0;");
            Label lbl = new Label("Cargando...");
            lbl.setStyle("-fx-font-size:12px; -fx-text-fill:#e2e8f0;");
            card.getChildren().addAll(val, lbl);
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
        return row;
    }

    private void actualizarKPIs(HBox row,
            long totalHabs, long disponibles, long ocupadas, long clientes,
            double ingresos, long checkoutsHoy,
            List<Habitacion> habitaciones, List<Cliente> clientesList,
            List<Factura> facturasList, List<Reserva> reservasList,
            List<CheckInOut> registrosList) {

        row.getChildren().clear();

        Object[][] specs = {
            { String.valueOf(totalHabs),
              "Total Habitaciones", "🏠", "#eef2ff", "#3563e9",
              (Runnable)() -> mostrarModalTodas(habitaciones) },

            { String.valueOf(disponibles),
              "Disponibles", "✓", "#dcfce7", "#22c55e",
              (Runnable)() -> mostrarModalDisponibles(habitaciones.stream()
                      .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.DISPONIBLE)
                      .collect(Collectors.toList())) },

            { String.valueOf(ocupadas),
              "Ocupadas", "🔑", "#fff7ed", "#f59e0b",
              (Runnable)() -> mostrarModalOcupadas(habitaciones.stream()
                      .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.OCUPADA)
                      .collect(Collectors.toList())) },

            { String.valueOf(clientes),
              "Clientes", "👥", "#ede9fe", "#8b5cf6",
              (Runnable)() -> mostrarModalClientes(clientesList) },

            { formatCOP(ingresos),
              "Ingresos Totales", "💲", "#d1fae5", "#0d9488",
              (Runnable)() -> mostrarModalIngresos(facturasList) },

            { String.valueOf(checkoutsHoy),
              "Salidas Hoy", "➡", "#f3f4f6", "#6b7280",
              (Runnable)() -> mostrarModalSalidasHoy(reservasList, registrosList) }
        };

        int[] idx = {0};
        for (Object[] s : specs) {
            final String   value    = (String)   s[0];
            final String   label    = (String)   s[1];
            final String   icon     = (String)   s[2];
            final String   iconBg   = (String)   s[3];
            final String   iconColor= (String)   s[4];
            final Runnable accion   = (Runnable) s[5];
            final int      delay    = idx[0]++ * 70;

            VBox card = construirKpiCard(value, label, icon, iconBg, iconColor, accion, delay);
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
    }

    private VBox construirKpiCard(String value, String label, String icon,
            String iconBg, String iconColor,
            Runnable onDoubleClick, int delayMs) {

        VBox card = new VBox(8);
        card.setPadding(new Insets(20, 18, 20, 18));
        card.getStyleClass().add("kpi-card");
        card.setCursor(javafx.scene.Cursor.HAND);

        StackPane iconBox = new StackPane();
        iconBox.setMinSize(40, 40);
        iconBox.setMaxSize(40, 40);
        iconBox.setStyle("-fx-background-color:" + iconBg + "; -fx-background-radius:10px;");
        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size:17px; -fx-text-fill:" + iconColor + ";");
        iconBox.getChildren().add(ico);

        Label val = new Label(value);
        val.getStyleClass().add("kpi-number");
        val.setWrapText(false);

        Label lbl = new Label(label.toUpperCase());
        lbl.getStyleClass().add("kpi-label");

        Label hint = new Label("Doble clic para ver detalles");
        hint.getStyleClass().add("kpi-hint");

        card.getChildren().addAll(iconBox, val, lbl, hint);

        String baseStyle = "-fx-background-color:white; -fx-background-radius:14; " +
                "-fx-border-color:#e5e7eb; -fx-border-radius:14; -fx-border-width:1; " +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),10,0,0,2); -fx-cursor:hand;";
        String hoverStyle = "-fx-background-color:white; -fx-background-radius:14; " +
                "-fx-border-color:#d1d5db; -fx-border-radius:14; -fx-border-width:1; " +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),18,0,0,5); -fx-cursor:hand; " +
                "-fx-translate-y:-2;";
        card.setStyle(baseStyle);

        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e  -> card.setStyle(baseStyle));

        card.setOnMouseClicked((MouseEvent e) -> {
            if (e.getClickCount() == 2 && onDoubleClick != null) onDoubleClick.run();
        });

        card.setOpacity(0);
        PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
        pause.setOnFinished(ev -> {
            FadeTransition ft = new FadeTransition(Duration.millis(300), card);
            ft.setFromValue(0); ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(300), card);
            tt.setFromY(14); tt.setToY(0);
            new ParallelTransition(ft, tt).play();
        });
        pause.play();

        return card;
    }

    private String formatCOP(double valor) {
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(new java.util.Locale("es", "CO"));
        nf.setMaximumFractionDigits(0);
        nf.setGroupingUsed(true);
        return "$" + nf.format(valor);
    }
        // ── GRÁFICOS ──────────────────────────────────────────────────────────────

    private void actualizarChartsRow(VBox chartsContainer, List<Reserva> reservas,
                                      List<Habitacion> habs, List<Factura> facturas) {
        chartsContainer.getChildren().clear();

        int anioActual   = LocalDate.now().getYear();
        java.time.Month mesActual = LocalDate.now().getMonth();
        int diasEnMes    = LocalDate.now().lengthOfMonth();
        int mesActualNum = LocalDate.now().getMonthValue();
        String mesNombre = mesActual.getDisplayName(java.time.format.TextStyle.FULL, LOCALE_ES);

        List<Factura> facturasPagadas = facturas.stream()
                .filter(f -> f.getEstadoPago() == Factura.EstadoPago.PAGADA
                          && f.getFechaEmision() != null)
                .collect(Collectors.toList());

        long totalHabsCount = habs.size();
        long ocupadasCount  = habs.stream()
                .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.OCUPADA
                          || h.getEstado() == Habitacion.EstadoHabitacion.RESERVADA).count();
        double pctOcupacion = totalHabsCount > 0 ? (ocupadasCount * 100.0 / totalHabsCount) : 0;

        PieChart pie = new PieChart();
        pie.setTitle("");
        pie.setLegendVisible(false);
        pie.setPrefHeight(240);
        pie.setAnimated(true);
        pie.getStyleClass().add("pie-chart-hotel");

        Map<String, Long> estados = habs.stream()
                .collect(Collectors.groupingBy(h -> h.getEstado().name(), Collectors.counting()));

        String[] ORDEN_PIE   = { "DISPONIBLE", "OCUPADA",   "RESERVADA", "MANTENIMIENTO" };
        String[] COLORES_PIE = { "#3B82F6",    "#1E293B",   "#f97316",   "#8b5cf6"       };
        String[] LABELS_PIE  = { "Disponible", "Ocupada",   "Reservada", "Mantenim."     };

        for (int i = 0; i < ORDEN_PIE.length; i++) {
            String est = ORDEN_PIE[i];
            if (!estados.containsKey(est)) continue;
            long count = estados.get(est);
            pie.getData().add(new PieChart.Data(LABELS_PIE[i] + "  " + count, count));
        }
        estados.forEach((est, count) -> {
            if (!Arrays.asList(ORDEN_PIE).contains(est))
                pie.getData().add(new PieChart.Data(est + "  " + count, count));
        });

        Map<String, String> colorPorLabel = new HashMap<>();
        for (int i = 0; i < ORDEN_PIE.length; i++) {
            colorPorLabel.put(LABELS_PIE[i], COLORES_PIE[i]);
        }
        long pieTotal = (long) pie.getData().stream().mapToDouble(PieChart.Data::getPieValue).sum();
        pie.getData().forEach(d -> {
            String label = d.getName().split("  ")[0];
            String color = colorPorLabel.getOrDefault(label, "#64748b");

            d.nodeProperty().addListener((obs, o, node) -> {
                if (node == null) return;
                Platform.runLater(() -> node.setStyle("-fx-pie-color: " + color + ";"));

                DropShadow glow = new DropShadow();
                glow.setColor(Color.web(color));
                glow.setRadius(14);
                glow.setSpread(0.25);
                Tooltip tip = new Tooltip(
                        label + "\n" +
                        (long) d.getPieValue() + " habitaciones\n" +
                        String.format("%.1f%%", d.getPieValue() * 100.0 / Math.max(1, pieTotal)));
                tip.setStyle("-fx-background-color:#111827; -fx-text-fill:white;" +
                        "-fx-background-radius:8px; -fx-padding:7px 12px; -fx-font-size:12px;");
                Tooltip.install(node, tip);
                node.setOnMouseEntered(e -> node.setEffect(glow));
                node.setOnMouseExited(e  -> node.setEffect(null));
            });
        });

        pie.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    pie.getData().forEach(d -> {
                        String reLabel = d.getName().split("  ")[0];
                        String reColor = colorPorLabel.getOrDefault(reLabel, "#64748b");
                        if (d.getNode() != null) {
                            d.getNode().setStyle("-fx-pie-color: " + reColor + ";");
                        }
                    });
                });
            }
        });

        StackPane pieStack = new StackPane(pie);
        Circle hole = new Circle(52);
        hole.setFill(Color.WHITE);
        hole.setMouseTransparent(true);
        VBox centerBox = new VBox(1);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setMouseTransparent(true);
        if (ocupadasCount == 0) {
            Label sinLbl = new Label("Sin");
            sinLbl.setStyle("-fx-font-size:13px; -fx-font-weight:900; -fx-text-fill:#94a3b8;");
            Label sinSub = new Label("ocupación");
            sinSub.setStyle("-fx-font-size:9.5px; -fx-text-fill:#94a3b8; -fx-font-weight:700;");
            centerBox.getChildren().addAll(sinLbl, sinSub);
        } else {
            Label pctLbl = new Label(String.format("%.0f%%", pctOcupacion));
            pctLbl.setStyle("-fx-font-size:20px; -fx-font-weight:900; -fx-text-fill:#0f172a;");
            Label pctSub = new Label("Ocupación");
            pctSub.setStyle("-fx-font-size:9.5px; -fx-text-fill:#94a3b8; -fx-font-weight:700;");
            centerBox.getChildren().addAll(pctLbl, pctSub);
        }
        pieStack.getChildren().addAll(hole, centerBox);

        HBox legendRow = new HBox(14);
        legendRow.setAlignment(Pos.CENTER);
        legendRow.setPadding(new Insets(6, 0, 0, 0));
        for (int i = 0; i < ORDEN_PIE.length; i++) {
            if (!estados.containsKey(ORDEN_PIE[i])) continue;
            Circle dot = new Circle(5);
            dot.setFill(Color.web(COLORES_PIE[i]));
            long cnt = estados.get(ORDEN_PIE[i]);
            Label lbl = new Label(LABELS_PIE[i] + " " + cnt);
            lbl.setStyle("-fx-font-size:10.5px; -fx-text-fill:#64748b; -fx-font-weight:600;");
            HBox item = new HBox(5, dot, lbl);
            item.setAlignment(Pos.CENTER_LEFT);
            legendRow.getChildren().add(item);
        }

        VBox pieWithLegend = new VBox(4, pieStack, legendRow);
        VBox cardPie = enCardPremium("Ocupación de habitaciones", pieWithLegend);
        HBox.setHgrow(cardPie, Priority.ALWAYS);

        Map<Integer, Double> ingresosDiarios = facturasPagadas.stream()
                .filter(f -> f.getFechaEmision().getYear() == anioActual
                          && f.getFechaEmision().getMonth() == mesActual)
                .collect(Collectors.groupingBy(f -> f.getFechaEmision().getDayOfMonth(),
                        Collectors.summingDouble(Factura::getTotal)));

        NumberAxis xAxisD = new NumberAxis(1, diasEnMes, 5);
        xAxisD.setLabel("Día");
        xAxisD.setAutoRanging(false);
        xAxisD.setTickUnit(5);
        xAxisD.setMinorTickVisible(false);

        NumberAxis yAxisD = new NumberAxis();
        yAxisD.setLabel("Ingresos $");
        yAxisD.setAutoRanging(true);
        yAxisD.setForceZeroInRange(true);
        yAxisD.setMinorTickVisible(false);
        yAxisD.setTickLabelFormatter(new StringConverter<Number>() {
            @Override public String toString(Number n) {
                return String.format("%,.0f", n.doubleValue()).replace(',', '.');
            }
            @Override public Number fromString(String s) { return 0; }
        });

        AreaChart<Number, Number> areaChart = new AreaChart<>(xAxisD, yAxisD);
        areaChart.setPrefHeight(260);
        areaChart.setAnimated(true);
        areaChart.setCreateSymbols(true);
        areaChart.getStyleClass().add("ingresos-diarios");
        areaChart.setLegendVisible(false);

        XYChart.Series<Number, Number> serieDiaria = new XYChart.Series<>();
        serieDiaria.setName("Ingresos diarios");
        for (int dia = 1; dia <= diasEnMes; dia++) {
            final int dFinal = dia;
            final double val = ingresosDiarios.getOrDefault(dia, 0.0);
            XYChart.Data<Number, Number> dato = new XYChart.Data<>(dia, val);
            serieDiaria.getData().add(dato);
            dato.nodeProperty().addListener((obs, o, node) -> {
                if (node == null) return;
                Tooltip tip = new Tooltip("Día " + dFinal + "  ·  " + formatCOP(val));
                tip.setStyle("-fx-background-color:#1e293b; -fx-text-fill:white;" +
                        "-fx-background-radius:10px; -fx-padding:8px 13px;" +
                        "-fx-font-size:12px; -fx-font-weight:600;");
                Tooltip.install(node, tip);
            });
        }
        areaChart.getData().add(serieDiaria);

        double totalMes = ingresosDiarios.values().stream().mapToDouble(Double::doubleValue).sum();
        Label trendBadge = new Label("↗  " + formatCOP(totalMes));
        trendBadge.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill:#3563e9;" +
                "-fx-background-color:#eef2ff; -fx-background-radius:20px; -fx-padding:4px 12px;");

        VBox cardDiario = enCardConExtraPremium("Ingresos — " + mesNombre, trendBadge, areaChart);
        HBox.setHgrow(cardDiario, Priority.ALWAYS);

        HBox fila1 = new HBox(20, cardPie, cardDiario);

        String[] MESES_ABREV = {"Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"};

        Map<Integer, Double> ingresosMensuales = facturasPagadas.stream()
                .filter(f -> f.getFechaEmision().getYear() == anioActual)
                .collect(Collectors.groupingBy(f -> f.getFechaEmision().getMonthValue(),
                        Collectors.summingDouble(Factura::getTotal)));

        CategoryAxis xM = new CategoryAxis();
        NumberAxis   yM = new NumberAxis();
        xM.setLabel(""); yM.setLabel("$ Ingresos");
        yM.setMinorTickVisible(false);
        yM.setTickLabelFormatter(new StringConverter<Number>() {
            @Override public String toString(Number n) {
                return String.format("%,.0f", n.doubleValue()).replace(',', '.');
            }
            @Override public Number fromString(String s) { return 0; }
        });
        BarChart<String, Number> barMensual = new BarChart<>(xM, yM);
        barMensual.setLegendVisible(false);
        barMensual.setPrefHeight(250);
        barMensual.setAnimated(true);
        barMensual.getStyleClass().add("bar-chart-hotel");
        barMensual.setCategoryGap(6);
        barMensual.setBarGap(2);

        XYChart.Series<String, Number> serieMensual = new XYChart.Series<>();
        for (int m = 1; m <= 12; m++) {
            final boolean esMesActual = (m == mesActualNum);
            final String mesLabel = MESES_ABREV[m - 1];
            final double mVal = ingresosMensuales.getOrDefault(m, 0.0);
            XYChart.Data<String, Number> dato = new XYChart.Data<>(mesLabel, mVal);
            dato.nodeProperty().addListener((obs, o, node) -> {
                if (node == null) return;
                String barColor  = esMesActual ? "#3563e9" : "#93c5fd";
                String glowBase  = esMesActual ? "rgba(53,99,233,0.35)" : "rgba(147,197,253,0.28)";
                String glowHover = esMesActual ? "rgba(53,99,233,0.65)" : "rgba(147,197,253,0.55)";
                String baseStyle = "-fx-bar-fill:" + barColor + ";" +
                        "-fx-background-radius:6px 6px 0 0;" +
                        "-fx-effect:dropshadow(gaussian," + glowBase + ",8,0,0,3);";
                String hoverStyle = "-fx-bar-fill:" + barColor + ";" +
                        "-fx-background-radius:6px 6px 0 0;" +
                        "-fx-effect:dropshadow(gaussian," + glowHover + ",14,0.15,0,5);";
                node.setStyle(baseStyle);
                Tooltip tip = new Tooltip(mesLabel + " · " + formatCOP(mVal));
                tip.setStyle("-fx-background-color:#1e293b; -fx-text-fill:white;" +
                        "-fx-background-radius:10px; -fx-padding:8px 13px;" +
                        "-fx-font-size:12px; -fx-font-weight:600;");
                Tooltip.install(node, tip);
                node.setOnMouseEntered(e -> node.setStyle(hoverStyle));
                node.setOnMouseExited(e -> node.setStyle(baseStyle));
            });
            serieMensual.getData().add(dato);
        }
        barMensual.getData().add(serieMensual);

        Label anioLabel2 = new Label(String.valueOf(anioActual));
        anioLabel2.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill:#64748b;" +
                "-fx-background-color:#f1f5f9; -fx-background-radius:20px; -fx-padding:4px 12px;");
        VBox cardMensual = enCardConExtraPremium("Ingresos mensuales", anioLabel2, barMensual);
        HBox.setHgrow(cardMensual, Priority.ALWAYS);

        CategoryAxis xA = new CategoryAxis();
        NumberAxis   yA = new NumberAxis();
        xA.setLabel(""); yA.setLabel("$ Ingresos");
        yA.setMinorTickVisible(false);
        yA.setTickLabelFormatter(new StringConverter<Number>() {
            @Override public String toString(Number n) {
                return String.format("%,.0f", n.doubleValue()).replace(',', '.');
            }
            @Override public Number fromString(String s) { return 0; }
        });
        BarChart<String, Number> barAnual = new BarChart<>(xA, yA);
        barAnual.setLegendVisible(false);
        barAnual.setPrefHeight(250);
        barAnual.setAnimated(true);
        barAnual.getStyleClass().add("bar-chart-hotel");
        barAnual.setCategoryGap(14);
        barAnual.setBarGap(3);

        XYChart.Series<String, Number> serieAnual = new XYChart.Series<>();
        for (int i = 4; i >= 0; i--) {
            int anio = anioActual - i;
            final boolean esActual = (i == 0);
            final String anioStr = String.valueOf(anio);
            final double aVal = facturasPagadas.stream()
                    .filter(f -> f.getFechaEmision().getYear() == anio)
                    .mapToDouble(Factura::getTotal).sum();
            XYChart.Data<String, Number> dato = new XYChart.Data<>(anioStr, aVal);
            dato.nodeProperty().addListener((obs, o, node) -> {
                if (node == null) return;
                String barColor  = esActual ? "#3563e9" : "#bfdbfe";
                String glowBase  = esActual ? "rgba(53,99,233,0.40)" : "rgba(191,219,254,0.20)";
                String glowHover = esActual ? "rgba(53,99,233,0.70)" : "rgba(191,219,254,0.45)";
                String baseStyle = "-fx-bar-fill:" + barColor + ";" +
                        "-fx-background-radius:6px 6px 0 0;" +
                        "-fx-effect:dropshadow(gaussian," + glowBase + ",8,0,0,3);";
                String hoverStyle = "-fx-bar-fill:" + barColor + ";" +
                        "-fx-background-radius:6px 6px 0 0;" +
                        "-fx-effect:dropshadow(gaussian," + glowHover + ",14,0.15,0,5);";
                node.setStyle(baseStyle);
                Tooltip tip = new Tooltip(anioStr + " · " + formatCOP(aVal));
                tip.setStyle("-fx-background-color:#1e293b; -fx-text-fill:white;" +
                        "-fx-background-radius:10px; -fx-padding:8px 13px;" +
                        "-fx-font-size:12px; -fx-font-weight:600;");
                Tooltip.install(node, tip);
                node.setOnMouseEntered(e -> node.setStyle(hoverStyle));
                node.setOnMouseExited(e -> node.setStyle(baseStyle));
            });
            serieAnual.getData().add(dato);
        }
        barAnual.getData().add(serieAnual);

        Label trend5yLabel = new Label("Últimos 5 años");
        trend5yLabel.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill:#64748b;" +
                "-fx-background-color:#f1f5f9; -fx-background-radius:20px; -fx-padding:4px 12px;");
        VBox cardAnual = enCardConExtraPremium("Ingresos anuales", trend5yLabel, barAnual);
        HBox.setHgrow(cardAnual, Priority.ALWAYS);

        HBox fila2 = new HBox(20, cardMensual, cardAnual);
        chartsContainer.getChildren().addAll(fila1, fila2);

        animarEntradaFila(fila1, 100);
        animarEntradaFila(fila2, 260);
    }

    // ── FILA INFERIOR ─────────────────────────────────────────────────────────

    private static final String BOTTOM_CARD_STYLE =
            "-fx-background-color:white; -fx-background-radius:14px;" +
            "-fx-border-color:#e5e7eb; -fx-border-width:1; -fx-border-radius:14px;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),14,0,0,3);";

    private HBox crearFilaInferior() {
        VBox leftCard = new VBox(14);
        leftCard.setPadding(new Insets(20));
        leftCard.setStyle(BOTTOM_CARD_STYLE);
        leftCard.getChildren().addAll(tituloCard("Reservas recientes"), loadingLbl());
        HBox.setHgrow(leftCard, Priority.ALWAYS);

        VBox rightCard = new VBox(0);
        rightCard.setPadding(new Insets(20));
        rightCard.setStyle(BOTTOM_CARD_STYLE);
        rightCard.getChildren().addAll(tituloCard("Actividad reciente"), loadingLbl());
        HBox.setHgrow(rightCard, Priority.ALWAYS);

        return new HBox(20, leftCard, rightCard);
    }

    private void actualizarFilaInferior(HBox row, List<Reserva> reservas, List<Factura> facturas) {
        actualizarCardReservasRecientes((VBox) row.getChildren().get(0), reservas);
        actualizarCardActividad((VBox) row.getChildren().get(1), reservas, facturas);
    }

    private void actualizarCardReservasRecientes(VBox card, List<Reserva> reservas) {
        card.getChildren().clear();
        card.setSpacing(12);
        card.getChildren().add(tituloCard("Reservas recientes"));

        TableView<Reserva> tabla = new TableView<>();
        tabla.setStyle(
            "-fx-background-color:transparent; -fx-border-color:transparent;" +
            "-fx-table-cell-border-color:rgba(241,245,249,0.9);");
        tabla.setFixedCellSize(42);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Reserva, String> cId = new TableColumn<>("#");
        cId.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getId()));
        cId.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); }
                else { setText(v); setStyle("-fx-text-fill:#64748B; -fx-font-size:12px;"); }
            }
        });
        cId.setPrefWidth(70);

        TableColumn<Reserva, String> cHuesped = col("HUÉSPED",
            r -> r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—", 120);

        TableColumn<Reserva, String> cHab = col("HABITACIÓN", r -> {
            if (r.getHabitacion() == null) return "—";
            String tipo = r.getHabitacion().getTipoHabitacion() != null
                    ? r.getHabitacion().getTipoHabitacion().obtenerEtiquetaTipo() : "?";
            return r.getHabitacion().getNumero() + " — " + tipo;
        }, 150);

        TableColumn<Reserva, String> cIn = col("CHECK-IN",
            r -> r.getFechaEntrada() != null ? fmtFechaCorta(r.getFechaEntrada()) : "—", 90);

        TableColumn<Reserva, String> cOut = col("CHECK-OUT",
            r -> r.getFechaSalida() != null ? fmtFechaCorta(r.getFechaSalida()) : "—", 90);

        TableColumn<Reserva, String> cEstado = new TableColumn<>("ESTADO");
        cEstado.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEstado() != null ? d.getValue().getEstado().name() : "—"));
        cEstado.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(labelEstadoReserva(v));
                badge.setStyle(estiloEstadoBadge(v));
                setGraphic(badge); setText(null);
                setAlignment(Pos.CENTER);
            }
        });
        cEstado.setPrefWidth(100);

        tabla.getColumns().addAll(cId, cHuesped, cHab, cIn, cOut, cEstado);

        List<Reserva> ultimas = reservas.stream()
                .sorted((a, b) -> Integer.compare(b.getId(), a.getId()))
                .limit(5)
                .collect(Collectors.toList());
        tabla.getItems().addAll(ultimas);
        tabla.setPrefHeight(42 * Math.max(ultimas.size(), 1) + 38);

        card.getChildren().add(tabla);
        animarEntradaFila(card, 120);
    }

    private void actualizarCardActividad(VBox card, List<Reserva> reservas, List<Factura> facturas) {
        card.getChildren().clear();
        card.setSpacing(4);
        card.getChildren().add(tituloCard("Actividad reciente"));

        VBox itemsBox = new VBox(0);
        List<HBox> items = new ArrayList<>();

        reservas.stream()
            .filter(r -> r.getEstado() == Reserva.EstadoReserva.EN_PROCESO
                      && r.getCliente() != null && r.getHabitacion() != null)
            .sorted((a, b) -> Integer.compare(b.getId(), a.getId()))
            .limit(2)
            .forEach(r -> items.add(itemActividad("✓", "#DCFCE7", "#15803D",
                "Check-in completado",
                r.getCliente().obtenerNombreCompleto() + " · Hab. " + r.getHabitacion().getNumero(),
                tiempoRelativo(r.getFechaEntrada()))));

        reservas.stream()
            .filter(r -> r.getCliente() != null)
            .sorted((a, b) -> Integer.compare(b.getId(), a.getId()))
            .limit(1)
            .forEach(r -> items.add(itemActividad("📋", "#DBEAFE", "#1D4ED8",
                "Nueva reserva creada",
                r.getCliente().obtenerNombreCompleto() +
                (r.getHabitacion() != null ? " · Hab. " + r.getHabitacion().getNumero() : ""),
                tiempoRelativo(r.getFechaEntrada()))));

        reservas.stream()
            .filter(r -> r.getEstado() == Reserva.EstadoReserva.COMPLETADA
                      && r.getCliente() != null && r.getHabitacion() != null)
            .sorted((a, b) -> Integer.compare(b.getId(), a.getId()))
            .limit(1)
            .forEach(r -> items.add(itemActividad("→", "#DBEAFE", "#1D4ED8",
                "Check-out registrado",
                r.getCliente().obtenerNombreCompleto() + " · Hab. " + r.getHabitacion().getNumero(),
                tiempoRelativo(r.getFechaSalida()))));

        facturas.stream()
            .filter(f -> f.getEstadoPago() == Factura.EstadoPago.PAGADA && f.getCliente() != null)
            .sorted((a, b) -> Integer.compare(b.getId(), a.getId()))
            .limit(1)
            .forEach(f -> items.add(itemActividad("$", "#FEF3C7", "#A16207",
                "Pago recibido",
                f.getCliente().obtenerNombreCompleto() + " · " + formatCOP(f.getTotal()),
                tiempoRelativo(f.getFechaEmision()))));

        if (items.size() < 3) {
            items.add(itemActividad("○", "#F1F5F9", "#64748B",
                "Sistema activo",
                "Hotel Nativo · Panel en línea",
                "Hoy"));
        }

        items.stream().limit(5).forEach(itemsBox.getChildren()::add);
        card.getChildren().add(itemsBox);
        animarEntradaFila(card, 200);
    }

    private HBox itemActividad(String icono, String fondoCirculo, String colorIcono,
            String titulo, String subtitulo, String tiempo) {
        StackPane circle = new StackPane();
        circle.setMinSize(32, 32); circle.setMaxSize(32, 32);
        circle.setStyle("-fx-background-color:" + fondoCirculo + "; -fx-background-radius:50%;");
        Label ico = new Label(icono);
        ico.setStyle("-fx-font-size:13px; -fx-text-fill:" + colorIcono + ";");
        circle.getChildren().add(ico);

        Label tit = new Label(titulo);
        tit.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#1E293B;");
        Label sub = new Label(subtitulo);
        sub.setStyle("-fx-font-size:12px; -fx-text-fill:#94A3B8;");
        VBox texts = new VBox(2, tit, sub);
        HBox.setHgrow(texts, Priority.ALWAYS);

        Label time = new Label(tiempo);
        time.setStyle("-fx-font-size:12px; -fx-text-fill:#94A3B8;");

        HBox item = new HBox(12, circle, texts, time);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 4, 12, 4));
        item.setStyle(
            "-fx-border-color:transparent transparent #F1F5F9 transparent;" +
            "-fx-border-width:0 0 1 0;");
        return item;
    }

    private Label tituloCard(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#1E293B;");
        return l;
    }

    private Label loadingLbl() {
        Label l = new Label("Cargando...");
        l.setStyle("-fx-text-fill:#94A3B8; -fx-font-size:13px;");
        return l;
    }

    private String fmtFechaCorta(LocalDate fecha) {
        return fecha.format(DateTimeFormatter.ofPattern("d MMM", LOCALE_ES));
    }

    private String tiempoRelativo(LocalDate fecha) {
        if (fecha == null) return "—";
        long dias = LocalDate.now().toEpochDay() - fecha.toEpochDay();
        if (dias < 0)  return "Próximo";
        if (dias == 0) return "Hoy";
        if (dias == 1) return "Ayer";
        if (dias < 7)  return "Esta semana";
        return "Hace " + dias + " días";
    }

    private String labelEstadoReserva(String estado) {
        return switch (estado) {
            case "EN_PROCESO" -> "Activa";
            case "COMPLETADA" -> "Completada";
            case "PENDIENTE"  -> "Pendiente";
            case "CANCELADA"  -> "Cancelada";
            default           -> estado;
        };
    }

    private String estiloEstadoBadge(String estado) {
        String base = "-fx-background-radius:20; -fx-padding:3 10; " +
                      "-fx-font-size:10px; -fx-font-weight:700;";
        return switch (estado) {
            case "EN_PROCESO" -> base + "-fx-background-color:#DCFCE7; -fx-text-fill:#15803D;";
            case "COMPLETADA" -> base + "-fx-background-color:#DBEAFE; -fx-text-fill:#1D4ED8;";
            case "PENDIENTE"  -> base + "-fx-background-color:#FEF3C7; -fx-text-fill:#A16207;";
            default           -> base + "-fx-background-color:#F1F5F9; -fx-text-fill:#64748B;";
        };
    }

    private void cargarPanel(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/hotel/ui/fxml/" + fxmlName));
            Node panel = loader.load();
            setContenido(panel);
        } catch (Exception e) {
            System.out.println("ERROR cargando FXML: " + fxmlName + " -> " + e.getMessage());
            e.printStackTrace();
            setContenido(crearPlaceholder(fxmlName.replace(".fxml", "")));
        }
    }

    private VBox crearPlaceholder(String nombre) {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color:#f0f2f5;");
        Label ico = new Label("🚧");
        ico.setStyle("-fx-font-size:48px;");
        Label lbl = new Label("Módulo " + nombre);
        lbl.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#1e293b;");
        Label sub = new Label("Este panel está disponible en las clases de controlador.");
        sub.setStyle("-fx-font-size:13px; -fx-text-fill:#64748b;");
        sub.setWrapText(true);
        sub.setMaxWidth(400);
        sub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        box.getChildren().addAll(ico, lbl, sub);
        return box;
    }

    private void setContenido(Node node) {
        contentArea.getChildren().clear();
        node.setOpacity(0);
        contentArea.getChildren().add(node);
        FadeTransition ft = new FadeTransition(Duration.millis(250), node);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }

    private void setActivo(Button nav, String titulo, String breadcrumb) {
        if (navActivo != null) navActivo.getStyleClass().remove("active");
        nav.getStyleClass().add("active");
        navActivo = nav;
        topbarTitle.setText(titulo);
        topbarBreadcrumb.setText(breadcrumb);
    }

    private void aplicarRestriccionesRol() {
        ocultarSiSinPermiso(navReservas,     "CREAR_RESERVA");
        ocultarSiSinPermiso(navCheckin,      "CHECKIN");
        ocultarSiSinPermiso(navClientes,     "CREAR_CLIENTE");
        ocultarSiSinPermiso(navHabitaciones, "GESTIONAR_HABITACIONES");
        ocultarSiSinPermiso(navMantenimiento,"GESTIONAR_MANTENIMIENTO");
        ocultarSiSinPermiso(navFacturacion,  "GENERAR_FACTURA");
        ocultarSiSinPermiso(navReportes,     "VER_REPORTES");
        ocultarSiSinPermiso(navEmpleados,    "CREAR_EMPLEADO");
        if (!navReservas.isManaged() && !navCheckin.isManaged() && !navClientes.isManaged())
            ocultarNodo(sectionOperaciones);
        if (!navHabitaciones.isManaged() && !navMantenimiento.isManaged())
            ocultarNodo(sectionRecursos);
        if (!navFacturacion.isManaged() && !navReportes.isManaged() && !navEmpleados.isManaged())
            ocultarNodo(sectionAdministracion);
    }

    private void ocultarSiSinPermiso(Button btn, String permiso) {
        try { ctx.getAuthService().verificarPermiso(ctx.getTokenSesion(), permiso); }
        catch (Exception e) { ocultarNodo(btn); }
    }

    private void ocultarNodo(Node nodo) {
        nodo.setVisible(false);
        nodo.setManaged(false);
    }

    private <S> TableColumn<S, String> col(String titulo,
            Function<S, String> extractor, double width) {
        TableColumn<S, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(data -> new SimpleStringProperty(extractor.apply(data.getValue())));
        c.setPrefWidth(width);
        return c;
    }

    private <S> TableColumn<S, String> colS(String titulo, Function<S, String> extractor) {
        TableColumn<S, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(data -> new SimpleStringProperty(extractor.apply(data.getValue())));
        return c;
    }

    private VBox enCardPremium(String titulo, Node contenido) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20, 20, 16, 20));
        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 20px;" +
                "-fx-border-color: rgba(226,232,240,0.7);" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 20px;" +
                "-fx-effect: dropshadow(gaussian,rgba(15,23,42,0.09),28,0,0,8);");
        Label t = new Label(titulo);
        t.setStyle("-fx-font-size:13.5px; -fx-font-weight:800; -fx-text-fill:#0f172a;");
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: rgba(226,232,240,0.55);");
        card.getChildren().addAll(t, sep, contenido);
        return card;
    }

    private VBox enCardConExtraPremium(String titulo, Node badge, Node contenido) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20, 20, 16, 20));
        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 20px;" +
                "-fx-border-color: rgba(226,232,240,0.7);" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 20px;" +
                "-fx-effect: dropshadow(gaussian,rgba(15,23,42,0.09),28,0,0,8);");
        Label t = new Label(titulo);
        t.setStyle("-fx-font-size:13.5px; -fx-font-weight:800; -fx-text-fill:#0f172a;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, t, spacer, badge);
        header.setAlignment(Pos.CENTER_LEFT);
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: rgba(226,232,240,0.55);");
        card.getChildren().addAll(header, sep, contenido);
        return card;
    }

    private void animarEntradaFila(Node node, int delayMs) {
        node.setOpacity(0);
        node.setTranslateY(18);
        PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
        pause.setOnFinished(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(480), node);
            ft.setFromValue(0); ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(480), node);
            tt.setFromY(18); tt.setToY(0);
            tt.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(ft, tt).play();
        });
        pause.play();
    }

    private void configurarHoverNav(Button btn) {
        ScaleTransition stIn  = new ScaleTransition(Duration.millis(220), btn);
        ScaleTransition stOut = new ScaleTransition(Duration.millis(220), btn);
        stIn.setToX(1.05);  stIn.setToY(1.05);
        stOut.setToX(1.0);  stOut.setToY(1.0);
        stIn.setInterpolator(Interpolator.EASE_OUT);
        stOut.setInterpolator(Interpolator.EASE_OUT);

        btn.setOnMouseEntered(e -> { stOut.stop(); stIn.playFromStart(); });
        btn.setOnMouseExited(e  -> { stIn.stop();  stOut.playFromStart(); });
    }

    private VBox enCard(String titulo, Node contenido) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color:white; -fx-background-radius:14px;" +
                      "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),14,0,0,3);");
        Label t = new Label(titulo);
        t.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1e293b;");
        card.getChildren().addAll(t, new Separator(), contenido);
        return card;
    }

    // ─── INTERFAZ PARA TODAS LAS HABITACIONES (MODERNA) ─────────────────────────

    private void mostrarModalTodas(List<Habitacion> habitaciones) {
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
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 16px 16px 0 0;");
        
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(44, 44);
        iconBox.setMaxSize(44, 44);
        iconBox.setStyle("-fx-background-color: #1e3a5f; -fx-background-radius: 10px;");
        Label iconLabel = new Label("🏠");
        iconLabel.setStyle("-fx-font-size: 20px;");
        iconBox.getChildren().add(iconLabel);
        
        VBox headerTexts = new VBox(4);
        Label titleLabel = new Label("Inventario de Habitaciones");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        HBox totalBadge = new HBox(6);
        totalBadge.setAlignment(Pos.CENTER);
        totalBadge.setPadding(new Insets(4, 10, 4, 10));
        totalBadge.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 20px;");
        Label dotIndicator = new Label("●");
        dotIndicator.setStyle("-fx-text-fill: #1e3a5f; -fx-font-size: 8px;");
        Label totalLabel = new Label(habitaciones.size() + " HABITACIONES EN TOTAL");
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
        
        TableView<Habitacion> tabla = new TableView<>();
        tabla.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setFixedCellSize(52);
        
        TableColumn<Habitacion, String> colNumero = new TableColumn<>("NÚMERO");
        colNumero.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNumero()));
        colNumero.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else { setText(item); setStyle("-fx-font-weight: bold; -fx-text-fill: #1e3a5f; -fx-font-size: 14px;"); }
            }
        });
        colNumero.setPrefWidth(100);
        
        TableColumn<Habitacion, String> colTipo = new TableColumn<>("TIPO");
        colTipo.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTipoHabitacion() != null 
                ? d.getValue().getTipoHabitacion().obtenerEtiquetaTipo().toUpperCase() : "SIMPLE"));
        colTipo.setPrefWidth(120);
        
        TableColumn<Habitacion, String> colEstado = new TableColumn<>("ESTADO");
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado().name()));
        colEstado.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label();
                String badgeStyle;
                switch (item) {
                    case "DISPONIBLE":
                        badge.setText("Disponible");
                        badgeStyle = "-fx-background-color: #e0e7ff; -fx-text-fill: #1e3a5f; " +
                                     "-fx-background-radius: 20px; -fx-padding: 4px 12px; " +
                                     "-fx-font-size: 11px; -fx-font-weight: 600;";
                        break;
                    case "RESERVADA":
                        badge.setText("Reservada");
                        badgeStyle = "-fx-background-color: #bfdbfe; -fx-text-fill: #1e40af; " +
                                     "-fx-background-radius: 20px; -fx-padding: 4px 12px; " +
                                     "-fx-font-size: 11px; -fx-font-weight: 600;";
                        break;
                    case "OCUPADA":
                        badge.setText("Ocupada");
                        badgeStyle = "-fx-background-color: #1e293b; -fx-text-fill: white; " +
                                     "-fx-background-radius: 20px; -fx-padding: 4px 12px; " +
                                     "-fx-font-size: 11px; -fx-font-weight: 600;";
                        break;
                    default:
                        badge.setText("Mantenimiento");
                        badgeStyle = "-fx-background-color: #475569; -fx-text-fill: white; " +
                                     "-fx-background-radius: 20px; -fx-padding: 4px 12px; " +
                                     "-fx-font-size: 11px; -fx-font-weight: 600;";
                }
                badge.setStyle(badgeStyle);
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });
        colEstado.setPrefWidth(130);
        
        TableColumn<Habitacion, String> colCamas = new TableColumn<>("CAMAS");
        colCamas.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNumCamas() + " cama(s)"));
        colCamas.setPrefWidth(100);
        
        TableColumn<Habitacion, String> colPrecio = new TableColumn<>("PRECIO BASE");
        colPrecio.setCellValueFactory(d -> new SimpleStringProperty(formatCOP(d.getValue().getPrecioBase())));
        colPrecio.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-font-weight: 600; -fx-text-fill: #1e293b; -fx-font-size: 13px;"); }
            }
        });
        colPrecio.setPrefWidth(120);
        
        tabla.getColumns().addAll(colNumero, colTipo, colEstado, colCamas, colPrecio);
        habitaciones.sort((a, b) -> {
            try { return Integer.compare(Integer.parseInt(a.getNumero()), Integer.parseInt(b.getNumero())); }
            catch (NumberFormatException e) { return a.getNumero().compareTo(b.getNumero()); }
        });
        tabla.getItems().addAll(habitaciones);
        
        double tablaAltura = Math.min(habitaciones.size() * 52 + 30, 400);
        tabla.setPrefHeight(tablaAltura);
        
        VBox footer = new VBox();
        footer.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16px 16px;");
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #e2e8f0;");
        HBox footerContent = new HBox();
        footerContent.setAlignment(Pos.CENTER_RIGHT);
        footerContent.setPadding(new Insets(16, 24, 20, 24));
        Button cerrarBtn = new Button("Cerrar");
        cerrarBtn.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: white; " +
                          "-fx-background-radius: 8px; -fx-padding: 8px 20px; " +
                          "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        cerrarBtn.setOnAction(e -> dialog.close());
        footerContent.getChildren().add(cerrarBtn);
        footer.getChildren().addAll(divider, footerContent);
        
        mainContainer.getChildren().addAll(header, tabla, footer);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().setPrefSize(750, 500);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        
        Button defaultCloseButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        defaultCloseButton.setVisible(false);
        defaultCloseButton.setManaged(false);
        
        dialog.showAndWait();
    }

    // ─── INTERFAZ PARA HABITACIONES DISPONIBLES (MODERNA) ───────────────────────

    private void mostrarModalDisponibles(List<Habitacion> habitaciones) {
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
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 16px 16px 0 0;");
        
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(44, 44);
        iconBox.setMaxSize(44, 44);
        iconBox.setStyle("-fx-background-color: #1e3a5f; -fx-background-radius: 10px;");
        Label iconLabel = new Label("✅");
        iconLabel.setStyle("-fx-font-size: 20px;");
        iconBox.getChildren().add(iconLabel);
        
        VBox headerTexts = new VBox(4);
        Label titleLabel = new Label("Habitaciones Disponibles");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        HBox totalBadge = new HBox(6);
        totalBadge.setAlignment(Pos.CENTER);
        totalBadge.setPadding(new Insets(4, 10, 4, 10));
        totalBadge.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 20px;");
        Label dotIndicator = new Label("●");
        dotIndicator.setStyle("-fx-text-fill: #1e3a5f; -fx-font-size: 8px;");
        Label totalLabel = new Label(habitaciones.size() + " HABITACIONES DISPONIBLES");
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
        
        TableView<Habitacion> tabla = new TableView<>();
        tabla.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setFixedCellSize(52);
        
        TableColumn<Habitacion, String> colNumero = new TableColumn<>("NÚMERO");
        colNumero.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNumero()));
        colNumero.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else { setText(item); setStyle("-fx-font-weight: bold; -fx-text-fill: #1e3a5f; -fx-font-size: 14px;"); }
            }
        });
        colNumero.setPrefWidth(100);
        
        TableColumn<Habitacion, String> colTipo = new TableColumn<>("TIPO");
        colTipo.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTipoHabitacion() != null 
                ? d.getValue().getTipoHabitacion().obtenerEtiquetaTipo().toUpperCase() : "SIMPLE"));
        colTipo.setPrefWidth(120);
        
        TableColumn<Habitacion, String> colEstado = new TableColumn<>("ESTADO");
        colEstado.setCellValueFactory(d -> new SimpleStringProperty("DISPONIBLE"));
        colEstado.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label("Disponible");
                badge.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #1e3a5f; " +
                              "-fx-background-radius: 20px; -fx-padding: 4px 12px; " +
                              "-fx-font-size: 11px; -fx-font-weight: 600;");
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });
        colEstado.setPrefWidth(130);
        
        TableColumn<Habitacion, String> colCamas = new TableColumn<>("CAMAS");
        colCamas.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNumCamas() + " cama(s)"));
        colCamas.setPrefWidth(100);
        
        TableColumn<Habitacion, String> colPrecio = new TableColumn<>("PRECIO BASE");
        colPrecio.setCellValueFactory(d -> new SimpleStringProperty(formatCOP(d.getValue().getPrecioBase())));
        colPrecio.setPrefWidth(120);
        
        tabla.getColumns().addAll(colNumero, colTipo, colEstado, colCamas, colPrecio);
        habitaciones.sort((a, b) -> {
            try { return Integer.compare(Integer.parseInt(a.getNumero()), Integer.parseInt(b.getNumero())); }
            catch (NumberFormatException e) { return a.getNumero().compareTo(b.getNumero()); }
        });
        tabla.getItems().addAll(habitaciones);
        
        double tablaAltura = Math.min(habitaciones.size() * 52 + 30, 400);
        tabla.setPrefHeight(tablaAltura);
        
        VBox footer = new VBox();
        footer.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16px 16px;");
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #e2e8f0;");
        HBox footerContent = new HBox();
        footerContent.setAlignment(Pos.CENTER_RIGHT);
        footerContent.setPadding(new Insets(16, 24, 20, 24));
        Button cerrarBtn = new Button("Cerrar");
        cerrarBtn.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: white; " +
                          "-fx-background-radius: 8px; -fx-padding: 8px 20px; " +
                          "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        cerrarBtn.setOnAction(e -> dialog.close());
        footerContent.getChildren().add(cerrarBtn);
        footer.getChildren().addAll(divider, footerContent);
        
        mainContainer.getChildren().addAll(header, tabla, footer);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().setPrefSize(750, 500);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        
        Button defaultCloseButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        defaultCloseButton.setVisible(false);
        defaultCloseButton.setManaged(false);
        
        dialog.showAndWait();
    }

    // ─── INTERFAZ PARA HABITACIONES OCUPADAS (MODERNA) ─────────────────────────

    private void mostrarModalOcupadas(List<Habitacion> habitaciones) {
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
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 16px 16px 0 0;");
        
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(44, 44);
        iconBox.setMaxSize(44, 44);
        iconBox.setStyle("-fx-background-color: #1e3a5f; -fx-background-radius: 10px;");
        Label iconLabel = new Label("🔑");
        iconLabel.setStyle("-fx-font-size: 20px;");
        iconBox.getChildren().add(iconLabel);
        
        VBox headerTexts = new VBox(4);
        Label titleLabel = new Label("Habitaciones Ocupadas");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        HBox totalBadge = new HBox(6);
        totalBadge.setAlignment(Pos.CENTER);
        totalBadge.setPadding(new Insets(4, 10, 4, 10));
        totalBadge.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 20px;");
        Label dotIndicator = new Label("●");
        dotIndicator.setStyle("-fx-text-fill: #1e3a5f; -fx-font-size: 8px;");
        Label totalLabel = new Label(habitaciones.size() + " HABITACIONES OCUPADAS");
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
        
        TableView<Habitacion> tabla = new TableView<>();
        tabla.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setFixedCellSize(52);
        
        TableColumn<Habitacion, String> colNumero = new TableColumn<>("NÚMERO");
        colNumero.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNumero()));
        colNumero.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else { setText(item); setStyle("-fx-font-weight: bold; -fx-text-fill: #1e3a5f; -fx-font-size: 14px;"); }
            }
        });
        colNumero.setPrefWidth(100);
        
        TableColumn<Habitacion, String> colTipo = new TableColumn<>("TIPO");
        colTipo.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTipoHabitacion() != null 
                ? d.getValue().getTipoHabitacion().obtenerEtiquetaTipo().toUpperCase() : "SIMPLE"));
        colTipo.setPrefWidth(120);
        
        TableColumn<Habitacion, String> colEstado = new TableColumn<>("ESTADO");
        colEstado.setCellValueFactory(d -> new SimpleStringProperty("OCUPADA"));
        colEstado.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label("Ocupada");
                badge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; " +
                              "-fx-background-radius: 20px; -fx-padding: 4px 12px; " +
                              "-fx-font-size: 11px; -fx-font-weight: 600;");
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });
        colEstado.setPrefWidth(130);
        
        TableColumn<Habitacion, String> colCamas = new TableColumn<>("CAMAS");
        colCamas.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNumCamas() + " cama(s)"));
        colCamas.setPrefWidth(100);
        
        TableColumn<Habitacion, String> colPrecio = new TableColumn<>("PRECIO BASE");
        colPrecio.setCellValueFactory(d -> new SimpleStringProperty(formatCOP(d.getValue().getPrecioBase())));
        colPrecio.setPrefWidth(120);
        
        tabla.getColumns().addAll(colNumero, colTipo, colEstado, colCamas, colPrecio);
        habitaciones.sort((a, b) -> {
            try { return Integer.compare(Integer.parseInt(a.getNumero()), Integer.parseInt(b.getNumero())); }
            catch (NumberFormatException e) { return a.getNumero().compareTo(b.getNumero()); }
        });
        tabla.getItems().addAll(habitaciones);
        
        double tablaAltura = Math.min(habitaciones.size() * 52 + 30, 400);
        tabla.setPrefHeight(tablaAltura);
        
        VBox footer = new VBox();
        footer.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16px 16px;");
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #e2e8f0;");
        HBox footerContent = new HBox();
        footerContent.setAlignment(Pos.CENTER_RIGHT);
        footerContent.setPadding(new Insets(16, 24, 20, 24));
        Button cerrarBtn = new Button("Cerrar");
        cerrarBtn.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: white; " +
                          "-fx-background-radius: 8px; -fx-padding: 8px 20px; " +
                          "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        cerrarBtn.setOnAction(e -> dialog.close());
        footerContent.getChildren().add(cerrarBtn);
        footer.getChildren().addAll(divider, footerContent);
        
        mainContainer.getChildren().addAll(header, tabla, footer);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().setPrefSize(750, 500);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        
        Button defaultCloseButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        defaultCloseButton.setVisible(false);
        defaultCloseButton.setManaged(false);
        
        dialog.showAndWait();
    }

    // ─── INTERFAZ PARA CLIENTES (MODERNA) ─────────────────────────────────────

    private void mostrarModalClientes(List<Cliente> clientes) {
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
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 16px 16px 0 0;");
        
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(44, 44);
        iconBox.setMaxSize(44, 44);
        iconBox.setStyle("-fx-background-color: #1e3a5f; -fx-background-radius: 10px;");
        Label iconLabel = new Label("👥");
        iconLabel.setStyle("-fx-font-size: 20px;");
        iconBox.getChildren().add(iconLabel);
        
        VBox headerTexts = new VBox(4);
        Label titleLabel = new Label("Clientes Registrados");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        long vips = clientes.stream().filter(Cliente::isEsVip).count();
        HBox totalBadge = new HBox(6);
        totalBadge.setAlignment(Pos.CENTER);
        totalBadge.setPadding(new Insets(4, 10, 4, 10));
        totalBadge.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 20px;");
        Label dotIndicator = new Label("●");
        dotIndicator.setStyle("-fx-text-fill: #1e3a5f; -fx-font-size: 8px;");
        Label totalLabel = new Label(clientes.size() + " CLIENTES • " + vips + " VIP");
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
        
        TableView<Cliente> tabla = new TableView<>();
        tabla.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setFixedCellSize(52);
        
        TableColumn<Cliente, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getId()));
        colId.setPrefWidth(120);
        
        TableColumn<Cliente, String> colNombre = new TableColumn<>("NOMBRE");
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().obtenerNombreCompleto()));
        colNombre.setPrefWidth(180);
        
        TableColumn<Cliente, String> colDocumento = new TableColumn<>("DOCUMENTO");
        colDocumento.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDocumento() != null ? d.getValue().getDocumento() : "—"));
        colDocumento.setPrefWidth(130);
        
        TableColumn<Cliente, String> colEmail = new TableColumn<>("EMAIL");
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail() != null ? d.getValue().getEmail() : "—"));
        colEmail.setPrefWidth(200);
        
        TableColumn<Cliente, String> colTelefono = new TableColumn<>("TELÉFONO");
        colTelefono.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTelefono() != null ? d.getValue().getTelefono() : "—"));
        colTelefono.setPrefWidth(120);
        
        TableColumn<Cliente, String> colVip = new TableColumn<>("VIP");
        colVip.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isEsVip() ? "⭐ VIP" : ""));
        colVip.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold;");
            }
        });
        colVip.setPrefWidth(80);
        
        TableColumn<Cliente, String> colNacionalidad = new TableColumn<>("NACIONALIDAD");
        colNacionalidad.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNacionalidad() != null ? d.getValue().getNacionalidad() : "—"));
        colNacionalidad.setPrefWidth(120);
        
        tabla.getColumns().addAll(colId, colNombre, colDocumento, colEmail, colTelefono, colVip, colNacionalidad);
        tabla.getItems().addAll(clientes);
        
        double tablaAltura = Math.min(clientes.size() * 52 + 30, 450);
        tabla.setPrefHeight(tablaAltura);
        
        VBox footer = new VBox();
        footer.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16px 16px;");
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #e2e8f0;");
        HBox footerContent = new HBox();
        footerContent.setAlignment(Pos.CENTER_RIGHT);
        footerContent.setPadding(new Insets(16, 24, 20, 24));
        Button cerrarBtn = new Button("Cerrar");
        cerrarBtn.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: white; " +
                          "-fx-background-radius: 8px; -fx-padding: 8px 20px; " +
                          "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        cerrarBtn.setOnAction(e -> dialog.close());
        footerContent.getChildren().add(cerrarBtn);
        footer.getChildren().addAll(divider, footerContent);
        
        mainContainer.getChildren().addAll(header, tabla, footer);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().setPrefSize(1000, 600);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        
        Button defaultCloseButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        defaultCloseButton.setVisible(false);
        defaultCloseButton.setManaged(false);
        
        dialog.showAndWait();
    }

    // ─── INTERFAZ PARA INGRESOS TOTALES (MODERNA) ─────────────────────────────

    private void mostrarModalIngresos(List<Factura> facturas) {
        List<Factura> pagadas = facturas.stream()
                .filter(f -> f.getEstadoPago() == Factura.EstadoPago.PAGADA)
                .sorted((a, b) -> {
                    if (a.getFechaEmision() == null) return 1;
                    if (b.getFechaEmision() == null) return -1;
                    return b.getFechaEmision().compareTo(a.getFechaEmision());
                })
                .collect(Collectors.toList());
        double total = pagadas.stream().mapToDouble(Factura::getTotal).sum();
        
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
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 16px 16px 0 0;");
        
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(44, 44);
        iconBox.setMaxSize(44, 44);
        iconBox.setStyle("-fx-background-color: #1e3a5f; -fx-background-radius: 10px;");
        Label iconLabel = new Label("💲");
        iconLabel.setStyle("-fx-font-size: 20px;");
        iconBox.getChildren().add(iconLabel);
        
        VBox headerTexts = new VBox(4);
        Label titleLabel = new Label("Detalle de Ingresos");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        HBox totalBadge = new HBox(6);
        totalBadge.setAlignment(Pos.CENTER);
        totalBadge.setPadding(new Insets(4, 10, 4, 10));
        totalBadge.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 20px;");
        Label dotIndicator = new Label("●");
        dotIndicator.setStyle("-fx-text-fill: #1e3a5f; -fx-font-size: 8px;");
        Label totalLabel = new Label(pagadas.size() + " FACTURAS PAGADAS");
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
        
        // Total acumulado
        HBox totalBox = new HBox();
        totalBox.setAlignment(Pos.CENTER_LEFT);
        totalBox.setPadding(new Insets(20, 24, 0, 24));
        
        Label totalText = new Label("Total acumulado:");
        totalText.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        
        Label totalValue = new Label(formatCOP(total));
        totalValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f; -fx-background-color: #e0e7ff; " +
                           "-fx-background-radius: 20px; -fx-padding: 6px 16px;");
        
        Region totalSpacer = new Region();
        HBox.setHgrow(totalSpacer, Priority.ALWAYS);
        totalBox.getChildren().addAll(totalText, totalSpacer, totalValue);
        
        TableView<Factura> tabla = new TableView<>();
        tabla.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setFixedCellSize(52);
        
        TableColumn<Factura, String> colId = new TableColumn<>("# FACTURA");
        colId.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getId()));
        colId.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-font-weight: bold; -fx-text-fill: #1e3a5f;"); }
            }
        });
        colId.setPrefWidth(120);
        
        TableColumn<Factura, String> colFecha = new TableColumn<>("FECHA");
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFechaEmision() != null ? d.getValue().getFechaEmision().toString() : "—"));
        colFecha.setPrefWidth(150);
        
        TableColumn<Factura, String> colTotal = new TableColumn<>("TOTAL");
        colTotal.setCellValueFactory(d -> new SimpleStringProperty(formatCOP(d.getValue().getTotal())));
        colTotal.setPrefWidth(150);
        
        TableColumn<Factura, String> colEstado = new TableColumn<>("ESTADO");
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstadoPago().name()));
        colEstado.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label("PAGADA");
                badge.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #059669; " +
                              "-fx-background-radius: 20px; -fx-padding: 4px 12px; " +
                              "-fx-font-size: 11px; -fx-font-weight: 600;");
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });
        colEstado.setPrefWidth(120);
        
        tabla.getColumns().addAll(colId, colFecha, colTotal, colEstado);
        tabla.getItems().addAll(pagadas);
        
        double tablaAltura = Math.min(pagadas.size() * 52 + 30, 400);
        tabla.setPrefHeight(tablaAltura);
        
        VBox footer = new VBox();
        footer.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16px 16px;");
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #e2e8f0;");
        HBox footerContent = new HBox();
        footerContent.setAlignment(Pos.CENTER_RIGHT);
        footerContent.setPadding(new Insets(16, 24, 20, 24));
        Button cerrarBtn = new Button("Cerrar");
        cerrarBtn.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: white; " +
                          "-fx-background-radius: 8px; -fx-padding: 8px 20px; " +
                          "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        cerrarBtn.setOnAction(e -> dialog.close());
        footerContent.getChildren().add(cerrarBtn);
        footer.getChildren().addAll(divider, footerContent);
        
        VBox content = new VBox(0, totalBox, tabla);
        mainContainer.getChildren().addAll(header, content, footer);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().setPrefSize(750, 500);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        
        Button defaultCloseButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        defaultCloseButton.setVisible(false);
        defaultCloseButton.setManaged(false);
        
        dialog.showAndWait();
    }

    // ─── INTERFAZ PARA SALIDAS HOY (MODERNA) ──────────────────────────────────

   private void mostrarModalSalidasHoy(List<Reserva> reservas, List<CheckInOut> registros) {
    LocalDate hoy = LocalDate.now();

    // Checkouts realizados hoy (de la tabla CheckInOut)
    List<CheckInOut> realizadosHoy = registros.stream()
            .filter(c -> c.getFechaHoraCheckout() != null
                    && c.getFechaHoraCheckout().toLocalDate().equals(hoy))
            .collect(Collectors.toList());

    // Salidas pendientes hoy (reservas que terminan hoy pero no tienen checkout)
    List<Reserva> pendientesHoy = reservas.stream()
            .filter(r -> hoy.equals(r.getFechaSalida())
                    && r.getEstado() == Reserva.EstadoReserva.EN_PROCESO)
            .collect(Collectors.toList());

    int total = realizadosHoy.size() + pendientesHoy.size();
    String fechaStr = hoy.format(DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM yyyy", LOCALE_ES));
    String fechaCap = fechaStr.substring(0, 1).toUpperCase() + fechaStr.substring(1);

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
    iconBox.setMinSize(44, 44);
    iconBox.setMaxSize(44, 44);
    iconBox.setStyle("-fx-background-color: #1e3a5f; -fx-background-radius: 10px;");
    Label iconLabel = new Label("➡");
    iconLabel.setStyle("-fx-font-size: 20px;");
    iconBox.getChildren().add(iconLabel);
    
    VBox headerTexts = new VBox(4);
    Label titleLabel = new Label("Salidas de Hoy");
    titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
    
    HBox totalBadge = new HBox(6);
    totalBadge.setAlignment(Pos.CENTER);
    totalBadge.setPadding(new Insets(4, 10, 4, 10));
    totalBadge.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 20px;");
    Label dotIndicator = new Label("●");
    dotIndicator.setStyle("-fx-text-fill: #1e3a5f; -fx-font-size: 8px;");
    Label totalLabel = new Label(total + " SALIDAS • " + fechaCap);
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
    
    // CONTENIDO SCROLLABLE
    VBox contentBox = new VBox(20);
    contentBox.setPadding(new Insets(20, 24, 20, 24));
    
    // Sección 1: Checkouts realizados hoy
    Label hdrRealizados = new Label("✅  Checkouts realizados hoy  (" + realizadosHoy.size() + ")");
    hdrRealizados.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #059669; " +
                          "-fx-background-color: #dcfce7; -fx-background-radius: 8px; -fx-padding: 6px 12px;");
    
    TableView<CheckInOut> tablaRealizados = new TableView<>();
    tablaRealizados.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");
    tablaRealizados.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    tablaRealizados.setFixedCellSize(48);
    tablaRealizados.setPrefHeight(realizadosHoy.isEmpty() ? 80 : Math.min(realizadosHoy.size() * 48 + 30, 250));
    tablaRealizados.setPlaceholder(new Label("Sin checkouts realizados hoy"));
    
    TableColumn<CheckInOut, String> cReserva = new TableColumn<>("RESERVA");
    cReserva.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReserva() != null ? "#" + d.getValue().getReserva().getId() : "—"));
    cReserva.setPrefWidth(80);
    
    TableColumn<CheckInOut, String> cCliente = new TableColumn<>("CLIENTE");
    cCliente.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getReserva() != null && d.getValue().getReserva().getCliente() != null
            ? d.getValue().getReserva().getCliente().obtenerNombreCompleto() : "—"));
    cCliente.setPrefWidth(180);
    
    TableColumn<CheckInOut, String> cHabitacion = new TableColumn<>("HABITACIÓN");
    cHabitacion.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getReserva() != null && d.getValue().getReserva().getHabitacion() != null
            ? d.getValue().getReserva().getHabitacion().getNumero() : "—"));
    cHabitacion.setPrefWidth(100);
    
    TableColumn<CheckInOut, String> cHora = new TableColumn<>("HORA CHECKOUT");
    cHora.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getFechaHoraCheckout() != null
            ? d.getValue().getFechaHoraCheckout().toLocalTime().toString().substring(0, 5) : "—"));
    cHora.setPrefWidth(120);
    
    TableColumn<CheckInOut, String> cTotal = new TableColumn<>("TOTAL");
    cTotal.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getReserva() != null ? formatCOP(d.getValue().getReserva().getPrecioTotal()) : "—"));
    cTotal.setPrefWidth(120);
    
    tablaRealizados.getColumns().addAll(cReserva, cCliente, cHabitacion, cHora, cTotal);
    tablaRealizados.getItems().addAll(realizadosHoy);
    
    // Sección 2: Salidas pendientes hoy
    Label hdrPendientes = new Label("⏳  Salidas pendientes hoy  (" + pendientesHoy.size() + ")");
    hdrPendientes.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #b45309; " +
                          "-fx-background-color: #fef3c7; -fx-background-radius: 8px; -fx-padding: 6px 12px;");
    
    TableView<Reserva> tablaPendientes = new TableView<>();
    tablaPendientes.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");
    tablaPendientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    tablaPendientes.setFixedCellSize(48);
    tablaPendientes.setPrefHeight(pendientesHoy.isEmpty() ? 80 : Math.min(pendientesHoy.size() * 48 + 30, 250));
    tablaPendientes.setPlaceholder(new Label("Sin salidas pendientes para hoy"));
    
    TableColumn<Reserva, String> pReserva = new TableColumn<>("RESERVA");
    pReserva.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getId()));
    pReserva.setPrefWidth(80);
    
    TableColumn<Reserva, String> pCliente = new TableColumn<>("CLIENTE");
    pCliente.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getCliente() != null ? d.getValue().getCliente().obtenerNombreCompleto() : "—"));
    pCliente.setPrefWidth(180);
    
    TableColumn<Reserva, String> pHabitacion = new TableColumn<>("HABITACIÓN");
    pHabitacion.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getHabitacion() != null ? d.getValue().getHabitacion().getNumero() : "—"));
    pHabitacion.setPrefWidth(100);
    
    TableColumn<Reserva, String> pEntrada = new TableColumn<>("ENTRADA");
    pEntrada.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getFechaEntrada() != null ? d.getValue().getFechaEntrada().toString() : "—"));
    pEntrada.setPrefWidth(110);
    
    TableColumn<Reserva, String> pSalida = new TableColumn<>("SALIDA");
    pSalida.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getFechaSalida() != null ? d.getValue().getFechaSalida().toString() : "—"));
    pSalida.setPrefWidth(110);
    
    TableColumn<Reserva, String> pTotal = new TableColumn<>("TOTAL");
    pTotal.setCellValueFactory(d -> new SimpleStringProperty(formatCOP(d.getValue().getPrecioTotal())));
    pTotal.setPrefWidth(120);
    
    tablaPendientes.getColumns().addAll(pReserva, pCliente, pHabitacion, pEntrada, pSalida, pTotal);
    tablaPendientes.getItems().addAll(pendientesHoy);
    
    contentBox.getChildren().addAll(hdrRealizados, tablaRealizados, hdrPendientes, tablaPendientes);
    
    ScrollPane scrollPane = new ScrollPane(contentBox);
    scrollPane.setFitToWidth(true);
    scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
    
    // FOOTER
    VBox footer = new VBox();
    footer.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16px 16px;");
    
    Region divider = new Region();
    divider.setPrefHeight(1);
    divider.setStyle("-fx-background-color: #e2e8f0;");
    
    HBox footerContent = new HBox();
    footerContent.setAlignment(Pos.CENTER_RIGHT);
    footerContent.setPadding(new Insets(16, 24, 20, 24));
    
    Button cerrarBtn = new Button("Cerrar");
    cerrarBtn.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: white; " +
                      "-fx-background-radius: 8px; -fx-padding: 8px 20px; " +
                      "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
    cerrarBtn.setOnAction(e -> dialog.close());
    footerContent.getChildren().add(cerrarBtn);
    
    footer.getChildren().addAll(divider, footerContent);
    
    mainContainer.getChildren().addAll(header, scrollPane, footer);
    dialog.getDialogPane().setContent(mainContainer);
    dialog.getDialogPane().setPrefSize(950, 650);
    dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
    
    Button defaultCloseButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
    defaultCloseButton.setVisible(false);
    defaultCloseButton.setManaged(false);
    
    dialog.showAndWait();
}

    private VBox crearMapaHabitaciones(List<Habitacion> habitaciones, List<Reserva> reservas) {
        VBox section = new VBox(16);
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label titulo = new Label("Mapa de Habitaciones");
        titulo.setStyle("-fx-font-size:17px; -fx-font-weight:800; -fx-text-fill:#0f172a;");
        long total    = habitaciones.size();
        long libres   = habitaciones.stream().filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.DISPONIBLE).count();
        long ocupadas = habitaciones.stream().filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.OCUPADA).count();
        Label meta = new Label(total + " habitaciones · " + libres + " libres · " + ocupadas + " ocupadas");
        meta.setStyle("-fx-font-size:12.5px; -fx-text-fill:#94a3b8;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(titulo, meta, sp);

        Map<String, Reserva> reservaActiva = reservas.stream()
                .filter(r -> r.getHabitacion() != null
                        && (r.getEstado() == Reserva.EstadoReserva.EN_PROCESO
                         || r.getEstado() == Reserva.EstadoReserva.COMPLETADA))
                .collect(Collectors.toMap(
                        r -> r.getHabitacion().getNumero(),
                        r -> r,
                        (a, b) -> a));

        FlowPane grid = new FlowPane();
        grid.setHgap(14);
        grid.setVgap(14);

        for (Habitacion hab : habitaciones) {
            VBox card = construirRoomCard(hab, reservaActiva.get(hab.getNumero()));
            grid.getChildren().add(card);
        }

        section.getChildren().addAll(header, grid);
        return section;
    }

    private VBox construirRoomCard(Habitacion hab, Reserva reserva) {
        Habitacion.EstadoHabitacion estado = hab.getEstado();

        String stripeColor, bgColor, borderColor, badgeText, badgeStyle;
        switch (estado) {
            case DISPONIBLE:
                stripeColor = "linear-gradient(to right,#22c55e,#4ade80)";
                bgColor     = "linear-gradient(160deg,#ffffff 55%,#f0fdf4 100%)";
                borderColor = "rgba(34,197,94,0.20)";
                badgeText   = "LIBRE";
                badgeStyle  = "-fx-background-color:#dcfce7; -fx-text-fill:#15803d;";
                break;
            case OCUPADA:
                stripeColor = "linear-gradient(to right,#f43f5e,#fb7185)";
                bgColor     = "linear-gradient(160deg,#ffffff 55%,#fff1f2 100%)";
                borderColor = "rgba(244,63,94,0.20)";
                badgeText   = "OCUPADA";
                badgeStyle  = "-fx-background-color:#ffe4e6; -fx-text-fill:#be123c;";
                break;
            case RESERVADA:
                stripeColor = "linear-gradient(to right,#f97316,#fb923c)";
                bgColor     = "linear-gradient(160deg,#ffffff 55%,#fff7ed 100%)";
                borderColor = "rgba(249,115,22,0.20)";
                badgeText   = "RESERVADA";
                badgeStyle  = "-fx-background-color:#ffedd5; -fx-text-fill:#c2410c;";
                break;
            default:
                stripeColor = "linear-gradient(to right,#f59e0b,#fcd34d)";
                bgColor     = "linear-gradient(160deg,#ffffff 55%,#fffbeb 100%)";
                borderColor = "rgba(245,158,11,0.20)";
                badgeText   = "MANTENIM.";
                badgeStyle  = "-fx-background-color:#fef9c3; -fx-text-fill:#a16207;";
        }

        VBox card = new VBox(0);
        card.setPrefWidth(190);
        card.setStyle(
                "-fx-background-color:" + bgColor + ";" +
                "-fx-background-radius:16px;" +
                "-fx-border-color:" + borderColor + ";" +
                "-fx-border-width:1.5px; -fx-border-radius:16px;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),12,0,0,3);" +
                "-fx-cursor:hand;");

        Pane stripe = new Pane();
        stripe.setPrefHeight(4);
        stripe.setStyle("-fx-background-color:" + stripeColor + "; -fx-background-radius:14px 14px 0 0;");

        VBox body = new VBox(8);
        body.setPadding(new Insets(12, 14, 14, 14));

        HBox top = new HBox();
        top.setAlignment(Pos.TOP_LEFT);
        VBox numBox = new VBox(1);
        Label numLbl = new Label(hab.getNumero());
        numLbl.setStyle("-fx-font-size:24px; -fx-font-weight:900; -fx-text-fill:#0f172a;");
        String tipo = hab.getTipoHabitacion() != null
                ? hab.getTipoHabitacion().obtenerEtiquetaTipo().toUpperCase() : "SIMPLE";
        Label typeLbl = new Label(tipo);
        typeLbl.setStyle("-fx-font-size:9px; -fx-text-fill:#94a3b8; -fx-font-weight:700;");
        numBox.getChildren().addAll(numLbl, typeLbl);
        Region spacerTop = new Region();
        HBox.setHgrow(spacerTop, Priority.ALWAYS);
        Label badge = new Label(badgeText);
        badge.setStyle(badgeStyle +
                "-fx-background-radius:20px; -fx-padding:3px 8px;" +
                "-fx-font-size:9px; -fx-font-weight:700;");
        top.getChildren().addAll(numBox, spacerTop, badge);
        body.getChildren().add(top);

        if (estado == Habitacion.EstadoHabitacion.DISPONIBLE) {
            StackPane checkCircle = new StackPane();
            checkCircle.setPrefSize(46, 46);
            checkCircle.setMaxSize(46, 46);
            checkCircle.setStyle("-fx-background-color:linear-gradient(135deg,#dcfce7,#bbf7d0);" +
                    "-fx-background-radius:50%;" +
                    "-fx-effect:dropshadow(gaussian,rgba(34,197,94,0.22),8,0,0,2);");
            Label check = new Label("✓");
            check.setStyle("-fx-font-size:20px; -fx-text-fill:#16a34a; -fx-font-weight:bold;");
            checkCircle.getChildren().add(check);
            HBox cb = new HBox(checkCircle);
            cb.setAlignment(Pos.CENTER);
            cb.setPadding(new Insets(4, 0, 4, 0));
            Label dispTxt = new Label("Disponible");
            dispTxt.setStyle("-fx-font-size:12px; -fx-font-weight:600; -fx-text-fill:#16a34a;");
            HBox dtb = new HBox(dispTxt);
            dtb.setAlignment(Pos.CENTER);
            body.getChildren().addAll(cb, dtb);

        } else if (estado == Habitacion.EstadoHabitacion.MANTENIMIENTO) {
            Label iconLbl = new Label("🔧");
            iconLbl.setStyle("-fx-font-size:30px;");
            HBox ib = new HBox(iconLbl);
            ib.setAlignment(Pos.CENTER);
            ib.setPadding(new Insets(4, 0, 2, 0));
            Label txt = new Label("En Mantenimiento");
            txt.setStyle("-fx-font-size:11.5px; -fx-font-weight:600; -fx-text-fill:#a16207;");
            HBox tb = new HBox(txt);
            tb.setAlignment(Pos.CENTER);
            body.getChildren().addAll(ib, tb);

        } else {
            Label bedIco = new Label(estado == Habitacion.EstadoHabitacion.OCUPADA ? "🛏️" : "🔔");
            bedIco.setStyle("-fx-font-size:28px; -fx-opacity:0.85;");
            HBox bedBox = new HBox(bedIco);
            bedBox.setAlignment(Pos.CENTER);
            bedBox.setPadding(new Insets(2, 0, 4, 0));
            body.getChildren().add(bedBox);

            if (reserva != null && reserva.getCliente() != null) {
                Label guest = new Label(reserva.getCliente().obtenerNombreCompleto());
                guest.setStyle("-fx-font-size:11.5px; -fx-font-weight:600; -fx-text-fill:#334155;");
                guest.setWrapText(true);
                body.getChildren().add(guest);

                if (reserva.getFechaEntrada() != null && reserva.getFechaSalida() != null) {
                    HBox times = new HBox(8);
                    VBox entBox = new VBox(2);
                    Label entLbl = new Label("ENTRADA");
                    entLbl.setStyle("-fx-font-size:8px; -fx-font-weight:700; -fx-text-fill:#3b82f6;");
                    Label entVal = new Label(reserva.getFechaEntrada().toString());
                    entVal.setStyle("-fx-font-size:10px; -fx-font-weight:600; -fx-text-fill:#334155;");
                    entBox.getChildren().addAll(entLbl, entVal);
                    Region timeSp = new Region();
                    HBox.setHgrow(timeSp, Priority.ALWAYS);
                    VBox salBox = new VBox(2);
                    salBox.setAlignment(Pos.TOP_RIGHT);
                    Label salLbl = new Label("SALIDA");
                    salLbl.setStyle("-fx-font-size:8px; -fx-font-weight:700; -fx-text-fill:#f43f5e;");
                    Label salVal = new Label(reserva.getFechaSalida().toString());
                    salVal.setStyle("-fx-font-size:10px; -fx-font-weight:600; -fx-text-fill:#334155;");
                    salBox.getChildren().addAll(salLbl, salVal);
                    times.getChildren().addAll(entBox, timeSp, salBox);
                    body.getChildren().add(times);
                }
            }
        }

        card.getChildren().addAll(stripe, body);
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.025); st.setToY(1.025);
            st.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });

        return card;
    }

    private VBox enCardConExtra(String titulo, Node extra, Node contenido) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color:white; -fx-background-radius:14px;" +
                      "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),14,0,0,3);");
        Label t = new Label(titulo);
        t.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1e293b;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox hdr = new HBox(8, t, sp, extra);
        hdr.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().addAll(hdr, new Separator(), contenido);
        return card;
    }
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Controlador principal del Dashboard.
 * Gestiona navegación lateral, KPIs con hover+doble clic, gráficos mejorados y chatbot flotante.
 */
public class DashboardController {
    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private StackPane centerRoot;
    @FXML private StackPane contentArea;
    @FXML private Label     labelUsuario, labelRol, labelFecha;
    @FXML private Label     topbarTitle, topbarBreadcrumb;
    @FXML private Label     userAvatarLabel;
    @FXML private HBox      sidebarUserHBox;

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
   
     // ── Init ──────────────────────────────────────────────────────────────────

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
        navActivo = navDashboard;
        cargarPanelDashboard();
        aplicarRestriccionesRol();
        List.of(navDashboard, navReservas, navCheckin, navClientes,
                navHabitaciones, navMantenimiento, navFacturacion, navReportes, navEmpleados)
                .forEach(this::configurarHoverNav);
        Platform.runLater(this::initChatbot);
    }
    
      // ── Navegación ────────────────────────────────────────────────────────────

    @FXML public void irADashboard()    { setActivo(navDashboard,    "Dashboard",    "Inicio > Dashboard");       cargarPanelDashboard(); }
    @FXML public void irAReservas()     { setActivo(navReservas,     "Reservas",     "Operaciones > Reservas");    cargarPanel("Reservas.fxml"); }
    @FXML public void irACheckin()      { setActivo(navCheckin,      "Check-in/out", "Operaciones > Check-in");    cargarPanel("CheckInOut.fxml"); }
    @FXML public void irAClientes()     { setActivo(navClientes,     "Clientes",     "Operaciones > Clientes");    cargarPanel("Clientes.fxml"); }
    @FXML public void irAHabitaciones() { setActivo(navHabitaciones, "Habitaciones", "Recursos > Habitaciones");   cargarPanel("Habitaciones.fxml"); }
    @FXML public void irAMantenimiento(){ setActivo(navMantenimiento,"Mantenimiento","Recursos > Mantenimiento");  cargarPanel("Mantenimiento.fxml"); }
    @FXML public void irAFacturacion()  { setActivo(navFacturacion,  "Facturación",  "Admin > Facturación");        cargarPanel("Facturacion.fxml"); }
    @FXML public void irAReportes()     { setActivo(navReportes,     "Reportes",     "Admin > Reportes");           cargarPanel("Reportes.fxml"); }
    @FXML public void irAEmpleados()    { setActivo(navEmpleados,    "Empleados",    "Admin > Empleados");          cargarPanel("Empleados.fxml"); }

    @FXML
    public void handleLogout() {
        ctx.getAuthService().logout(ctx.getTokenSesion());
        ctx.setTokenSesion(null);
        ctx.setEmpleadoActual(null);
        NavigatorUtil.irAlLogin();
    }
    
    
       // ── Chatbot ───────────────────────────────────────────────────────────────

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
        Button btnCerrar = new Button("✕");
        btnCerrar.setStyle("-fx-background-color:transparent; -fx-text-fill:white;" +
                           "-fx-font-size:14px; -fx-cursor:hand; -fx-padding:2px 8px;");
        btnCerrar.setOnAction(e -> toggleChat());
        HBox header = new HBox(8, titulo, spacerH, btnCerrar);
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
    
     // ── Panel Dashboard ───────────────────────────────────────────────────────

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
        VBox recentTable     = crearTablaRecientes();

        panel.getChildren().addAll(
                crearSaludoHeader(), kpiRow,
                chartsContainer, recentTable);
        scroll.setContent(panel);
        setContenido(scroll);
        
           Thread hiloDashboard = new Thread(() -> {
            try {
                List<Reserva>    reservas     = ctx.getReservaService().listarTodasLasReservas();
                List<Habitacion> habitaciones = ctx.getHabitacionService().listarTodasLasHabitaciones();
                List<Cliente>    clientes     = ctx.getClienteService().listarTodosLosClientes();
                List<Factura>    facturas     = ctx.getFacturaService().listarTodasLasFacturas();

                long disponibles = habitaciones.stream()
                        .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.DISPONIBLE).count();
                long ocupadas    = habitaciones.stream()
                        .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.OCUPADA).count();
                double ingresos  = facturas.stream()
                        .filter(f -> f.getEstadoPago() == Factura.EstadoPago.PAGADA)
                        .mapToDouble(Factura::getTotal).sum();
                long checkoutsHoy = reservas.stream()
                        .filter(r -> LocalDate.now().equals(r.getFechaSalida())
                                  && (r.getEstado() == Reserva.EstadoReserva.EN_PROCESO
                                   || r.getEstado() == Reserva.EstadoReserva.COMPLETADA))
                        .count();

                Platform.runLater(() -> {
                    actualizarKPIs(kpiRow,
                            habitaciones.size(), disponibles, ocupadas,
                            clientes.size(), ingresos, checkoutsHoy,
                            habitaciones, clientes, facturas, reservas);

                    actualizarChartsRow(chartsContainer, reservas, habitaciones, facturas);
                    actualizarTablaRecientes(recentTable, reservas);
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
        saludo.setStyle("-fx-font-size:24px; -fx-font-weight:900; -fx-text-fill:#0f172a;");
        Label sub = new Label("Aquí tienes el resumen del hotel en tiempo real");
        sub.setStyle("-fx-font-size:13.5px; -fx-text-fill:#64748b;");

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
       // ── KPI Cards ─────────────────────────────────────────────────────────────

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

    private VBox construirKpiCard(String value, String label, String icon,
            String gradientBg, String shadowColor,
            Runnable onDoubleClick, int delayMs) {

        VBox card = new VBox(0);
        card.setPadding(new Insets(22, 22, 18, 22));

        String baseStyle =
                "-fx-background-color: " + gradientBg + "; " +
                "-fx-background-radius: 18px;" +
                "-fx-effect: dropshadow(gaussian," + shadowColor + ",22,0.10,0,6);" +
                "-fx-cursor: hand;";
        String hoverStyle =
                "-fx-background-color: " + gradientBg + "; " +
                "-fx-background-radius: 18px;" +
                "-fx-effect: dropshadow(gaussian," + shadowColor + ",36,0.28,0,10);" +
                "-fx-cursor: hand;";

        card.setStyle(baseStyle);
       // Ícono pill semitransparente
        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size:20px;" +
                     "-fx-background-color:rgba(255,255,255,0.17);" +
                     "-fx-background-radius:12px; -fx-padding:10px 12px;");

        Region spacer = new Region();
        spacer.setPrefHeight(10);

        // Valor principal: blanco, grande
        Label val = new Label(value);
        val.setStyle("-fx-font-size:28px; -fx-font-weight:900; -fx-text-fill:white;");
        val.setWrapText(false);

        // Etiqueta: blanco semitransparente en mayúsculas
        Label lbl = new Label(label.toUpperCase());
        lbl.setStyle("-fx-font-size:9.5px; -fx-font-weight:700;" +
                     "-fx-text-fill:rgba(255,255,255,0.65);" +
                     "-fx-padding:2px 0 0 0;");

        // Hint inferior
        Label hint = new Label("Doble clic para detalles ›");
        hint.setStyle("-fx-font-size:9.5px; -fx-text-fill:rgba(255,255,255,0.30);" +
                      "-fx-padding:8px 0 0 0;");

        card.getChildren().addAll(ico, spacer, val, lbl, hint);

        // Hover: escala + sombra más intensa
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), card);
            st.setToX(1.03); st.setToY(1.03);
            st.play();
            card.setStyle(hoverStyle);
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), card);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
            card.setStyle(baseStyle);
        });

        // Doble clic abre modal
        card.setOnMouseClicked((MouseEvent e) -> {
            if (e.getClickCount() == 2 && onDoubleClick != null) onDoubleClick.run();
        });

        // Animación de entrada escalonada (fadeUp)
        card.setOpacity(0);
        card.setTranslateY(22);
        PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
        pause.setOnFinished(ev -> {
            FadeTransition ft = new FadeTransition(Duration.millis(380), card);
            ft.setFromValue(0); ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(380), card);
            tt.setFromY(22); tt.setToY(0);
            new ParallelTransition(ft, tt).play();
        });
        pause.play();

        return card;
    }
    
    private void actualizarKPIs(HBox row,
            long totalHabs, long disponibles, long ocupadas, long clientes,
            double ingresos, long checkoutsHoy,
            List<Habitacion> habitaciones, List<Cliente> clientesList,
            List<Factura> facturasList, List<Reserva> reservasList) {

        row.getChildren().clear();

        // { valor, etiqueta, icono, gradienteBg, shadowColor, accion }
        Object[][] specs = {
            { String.valueOf(totalHabs),
              "Total Habitaciones", "🛏",
              "linear-gradient(to bottom right, #0f172a, #1a2744)",
              "rgba(15,23,42,0.45)",
              (Runnable)() -> abrirModalHabitaciones("Todas las Habitaciones", habitaciones, null) },

            { String.valueOf(disponibles),
              "Disponibles", "✅",
              "linear-gradient(to bottom right, #065f46, #059669)",
              "rgba(5,150,105,0.42)",
              (Runnable)() -> abrirModalHabitaciones("Habitaciones Disponibles",
                      habitaciones, Habitacion.EstadoHabitacion.DISPONIBLE) },

            { String.valueOf(ocupadas),
              "Ocupadas", "🔑",
              "linear-gradient(to bottom right, #1d4ed8, #3b82f6)",
              "rgba(59,130,246,0.42)",
              (Runnable)() -> abrirModalHabitaciones("Habitaciones Ocupadas",
                      habitaciones, Habitacion.EstadoHabitacion.OCUPADA) },

            { String.valueOf(clientes),
              "Clientes", "👥",
              "linear-gradient(to bottom right, #4c1d95, #8b5cf6)",
              "rgba(139,92,246,0.42)",
              (Runnable)() -> abrirModalClientes(clientesList) },

            { String.format("$%,.0f", ingresos),
              "Ingresos totales", "💰",
              "linear-gradient(to bottom right, #065f46, #10b981)",
              "rgba(16,185,129,0.42)",
              (Runnable)() -> abrirModalIngresos(facturasList) },

            { String.valueOf(checkoutsHoy),
              "Salidas hoy", "🚪",
              "linear-gradient(to bottom right, #9f1239, #f43f5e)",
              "rgba(244,63,94,0.42)",
              (Runnable)() -> abrirModalSalidasHoy(reservasList) }
        };

        int[] idx = {0};
        for (Object[] s : specs) {
            final String   value      = (String)   s[0];
            final String   label      = (String)   s[1];
            final String   icon       = (String)   s[2];
            final String   gradientBg = (String)   s[3];
            final String   shadowColor= (String)   s[4];
            final Runnable accion     = (Runnable) s[5];
            final int delay = idx[0]++ * 80;

            VBox card = construirKpiCard(value, label, icon, gradientBg, shadowColor, accion, delay);
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
    }
    
    
     // ── Modales de detalle (doble clic) ───────────────────────────────────────

    private void abrirModalHabitaciones(String titulo, List<Habitacion> todas,
            Habitacion.EstadoHabitacion filtro) {

        List<Habitacion> lista = filtro == null ? todas :
                todas.stream().filter(h -> h.getEstado() == filtro).collect(Collectors.toList());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(titulo);
        dialog.getDialogPane().setPrefSize(680, 500);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color:white;");

        TableView<Habitacion> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setStyle("-fx-background-color:transparent; -fx-border-color:transparent;");
        VBox.setVgrow(tabla, Priority.ALWAYS);

        TableColumn<Habitacion, String> colEst = colS("Estado", h -> h.getEstado().name());
        colEst.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                String color = switch (item) {
                    case "DISPONIBLE"    -> "#059669";
                    case "OCUPADA"       -> "#d97706";
                    case "RESERVADA"     -> "#2563eb";
                    case "MANTENIMIENTO" -> "#dc2626";
                    default              -> "#64748b";
                };
                setText(item);
                setStyle("-fx-text-fill:" + color + "; -fx-font-weight:bold;");
            }
        });

        tabla.getColumns().addAll(List.of(
            colS("Número",      h -> h.getNumero()),
            colS("Tipo",        h -> h.getTipoHabitacion() != null
                    ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "—"),
            colEst,
            colS("Camas",       h -> h.getNumCamas() > 0 ? h.getNumCamas() + " cama(s)" : "—"),
            colS("Precio base", h -> String.format("$%,.0f", h.getPrecioBase()))
        ));
        tabla.getItems().addAll(lista);

        VBox content = new VBox(14,
            modalHeader(titulo + "  (" + lista.size() + " habitaciones)"),
            new Separator(), tabla);
        content.setPadding(new Insets(22));
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }
    
     private void abrirModalClientes(List<Cliente> clientes) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Clientes registrados");
        dialog.getDialogPane().setPrefSize(760, 520);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color:white;");

        TableView<Cliente> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setStyle("-fx-background-color:transparent; -fx-border-color:transparent;");
        VBox.setVgrow(tabla, Priority.ALWAYS);

        // Columna VIP con badge
        TableColumn<Cliente, String> colVip = colS("VIP", c -> c.isEsVip() ? "⭐ VIP" : "");
        colVip.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-text-fill:#d97706; -fx-font-weight:bold;");
            }
        });

        tabla.getColumns().addAll(List.of(
            colS("ID",           c -> "#" + c.getId()),
            colS("Nombre",       c -> c.obtenerNombreCompleto()),
            colS("Documento",    c -> c.getDocumento() != null ? c.getDocumento() : "—"),
            colS("Email",        c -> c.getEmail() != null ? c.getEmail() : "—"),
            colS("Teléfono",     c -> c.getTelefono() != null ? c.getTelefono() : "—"),
            colVip,
            colS("Nacionalidad", c -> c.getNacionalidad() != null ? c.getNacionalidad() : "—")
        ));
        tabla.getItems().addAll(clientes);

        long vips = clientes.stream().filter(Cliente::isEsVip).count();
        Label badge = new Label("⭐ " + vips + " clientes VIP");
        badge.setStyle("-fx-font-size:12px; -fx-text-fill:#d97706; -fx-font-weight:600;" +
                "-fx-background-color:#fef3c7; -fx-background-radius:6px; -fx-padding:4px 10px;");

        HBox subHdr = new HBox(12, badge);
        subHdr.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(12,
            modalHeader("Clientes registrados  (" + clientes.size() + ")"),
            subHdr, new Separator(), tabla);
        content.setPadding(new Insets(22));
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }
     
      private void abrirModalIngresos(List<Factura> facturas) {
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
        dialog.setTitle("Detalle de ingresos");
        dialog.getDialogPane().setPrefSize(680, 520);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color:white;");

        Label totalLbl = new Label("Total acumulado:  " + String.format("$%,.2f", total));
        totalLbl.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#059669;" +
                "-fx-background-color:#d1fae5; -fx-background-radius:8px; -fx-padding:8px 16px;");

        TableView<Factura> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setStyle("-fx-background-color:transparent; -fx-border-color:transparent;");
        VBox.setVgrow(tabla, Priority.ALWAYS);

        tabla.getColumns().addAll(List.of(
            colS("# Factura",  f -> "#" + f.getId()),
            colS("Fecha",      f -> f.getFechaEmision() != null ? f.getFechaEmision().toString() : "—"),
            colS("Total",      f -> String.format("$%,.0f", f.getTotal())),
            colS("Estado",     f -> f.getEstadoPago().name())
        ));
        tabla.getItems().addAll(pagadas);

        VBox content = new VBox(12,
            modalHeader("Facturas pagadas  (" + pagadas.size() + " registros)"),
            totalLbl, new Separator(), tabla);
        content.setPadding(new Insets(22));
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void abrirModalSalidasHoy(List<Reserva> reservas) {
        LocalDate hoy = LocalDate.now();
        List<Reserva> salidas = reservas.stream()
                .filter(r -> hoy.equals(r.getFechaSalida()))
                .collect(Collectors.toList());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Salidas programadas hoy");
        dialog.getDialogPane().setPrefSize(820, 500);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color:white;");
        
        
        TableView<Reserva> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setStyle("-fx-background-color:transparent; -fx-border-color:transparent;");
        VBox.setVgrow(tabla, Priority.ALWAYS);

        // Columna de estado con colores
        TableColumn<Reserva, String> colEst = colS("Estado", r -> r.getEstado().name());
        colEst.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                String color = switch (item) {
                    case "EN_PROCESO"  -> "#2563eb";
                    case "COMPLETADA"  -> "#059669";
                    case "CANCELADA"   -> "#dc2626";
                    default            -> "#64748b";
                };
                setText(item);
                setStyle("-fx-text-fill:" + color + "; -fx-font-weight:bold;");
            }
        });

        tabla.getColumns().addAll(List.of(
            colS("Reserva",    r -> "#" + r.getId()),
            colS("Cliente",    r -> r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—"),
            colS("Habitación", r -> r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—"),
            colS("Entrada",    r -> r.getFechaEntrada() != null ? r.getFechaEntrada().toString() : "—"),
            colS("Salida",     r -> r.getFechaSalida()  != null ? r.getFechaSalida().toString()  : "—"),
            colEst,
            colS("Total",      r -> String.format("$%,.0f", r.getPrecioTotal()))
        ));
        tabla.getItems().addAll(salidas);

        String fechaStr = hoy.format(DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM yyyy", LOCALE_ES));
        Label sub = new Label(salidas.isEmpty()
                ? "No hay salidas programadas para hoy"
                : salidas.size() + " salida(s) — " + fechaStr);
        sub.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        VBox content = new VBox(10,
            modalHeader("Salidas de hoy  (" + salidas.size() + ")"),
            sub, new Separator(), tabla);
        content.setPadding(new Insets(22));
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private Label modalHeader(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;");
        return l;
    }
    
      // ── Gráficos ──────────────────────────────────────────────────────────────

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

        // ── Datos de ocupación para el centro del donut ───────────────────────
        long totalHabsCount = habs.size();
        long ocupadasCount  = habs.stream()
                .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.OCUPADA).count();
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
        String[] COLORES_PIE = { "#10b981",    "#f43f5e",   "#f97316",   "#8b5cf6"       };
        String[] LABELS_PIE  = { "Disponible", "Ocupada",   "Reservada", "Mantenim."     };
        
        or (int i = 0; i < ORDEN_PIE.length; i++) {
            String est = ORDEN_PIE[i];
            if (!estados.containsKey(est)) continue;
            long count = estados.get(est);
            pie.getData().add(new PieChart.Data(LABELS_PIE[i] + "  " + count, count));
        }
        estados.forEach((est, count) -> {
            if (!Arrays.asList(ORDEN_PIE).contains(est))
                pie.getData().add(new PieChart.Data(est + "  " + count, count));
        });

        int[] ci = {0};
        pie.getData().forEach(d -> {
            int ci2 = ci[0]++;
            String color = ci2 < COLORES_PIE.length ? COLORES_PIE[ci2] : "#64748b";
            d.nodeProperty().addListener((obs, o, node) -> {
                if (node == null) return;
                node.setStyle("-fx-pie-color: " + color + ";");
                DropShadow glow = new DropShadow();
                glow.setColor(Color.web(color));
                glow.setRadius(18);
                glow.setSpread(0.35);
                long pieTotal = (long) pie.getData().stream().mapToDouble(PieChart.Data::getPieValue).sum();
                Tooltip tip = new Tooltip(
                        d.getName().split("  ")[0] + "\n" +
                        (long) d.getPieValue() + " habitaciones\n" +
                        String.format("%.1f%%", d.getPieValue() * 100.0 / Math.max(1, pieTotal)));
                tip.setStyle("-fx-background-color:#1e293b; -fx-text-fill:white;" +
                        "-fx-background-radius:10px; -fx-padding:8px 13px;" +
                        "-fx-font-size:12px; -fx-font-weight:600;");
                Tooltip.install(node, tip);
                node.setOnMouseEntered(e -> {
                    node.setEffect(glow);
                    ScaleTransition st = new ScaleTransition(Duration.millis(160), node);
                    st.setToX(1.07); st.setToY(1.07); st.play();
                });
                node.setOnMouseExited(e -> {
                    node.setEffect(null);
                    ScaleTransition st = new ScaleTransition(Duration.millis(160), node);
                    st.setToX(1.0); st.setToY(1.0); st.play();
                });
            });
        });
    }
 }
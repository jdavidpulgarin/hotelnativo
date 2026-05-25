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
}
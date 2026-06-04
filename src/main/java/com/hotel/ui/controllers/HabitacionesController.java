package com.hotel.ui.controllers;

import com.hotel.AppContext;
import com.hotel.dto.HabitacionDTO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.Habitacion;
import com.hotel.model.Reserva;
import com.hotel.model.Cliente;
import com.hotel.ui.components.NotificationUtil;
import com.hotel.ui.components.ValidacionCampo;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Controlador de Habitaciones.
 * Grid de cards con filtros pill, badges de resumen y hover interactivo.
 * Modal moderno estilo Dashboard al hacer doble clic.
 */
public class HabitacionesController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private FlowPane   flowPane;
    @FXML private HBox       statsRow;
    @FXML private ProgressBar progressBar;

    // Filtros pill
    @FXML private Button btnTodas;
    @FXML private Button btnDisponibles;
    @FXML private Button btnReservadas;
    @FXML private Button btnOcupadas;
    @FXML private Button btnMantenimiento;

    // ── Estilos de cada botón filtro ──────────────────────────────────────────
    private static final String PILL_TODAS_OFF =
        "-fx-background-color:white; -fx-text-fill:#1e293b; -fx-font-size:13px; " +
        "-fx-font-weight:600; -fx-background-radius:25; -fx-border-radius:25; " +
        "-fx-border-color:#1e293b; -fx-border-width:1.5; -fx-padding:7 20; -fx-cursor:hand;";
    private static final String PILL_TODAS_ON =
        "-fx-background-color:#1e293b; -fx-text-fill:white; -fx-font-size:13px; " +
        "-fx-font-weight:600; -fx-background-radius:25; -fx-padding:7 20; -fx-cursor:hand;";

    private static final String PILL_DISP_OFF =
        "-fx-background-color:white; -fx-text-fill:#3b82f6; -fx-font-size:13px; " +
        "-fx-font-weight:600; -fx-background-radius:25; -fx-border-radius:25; " +
        "-fx-border-color:#3b82f6; -fx-border-width:1.5; -fx-padding:7 20; -fx-cursor:hand;";
    private static final String PILL_DISP_ON =
        "-fx-background-color:#3b82f6; -fx-text-fill:white; -fx-font-size:13px; " +
        "-fx-font-weight:600; -fx-background-radius:25; -fx-padding:7 20; -fx-cursor:hand;";

    private static final String PILL_RES_OFF =
        "-fx-background-color:white; -fx-text-fill:#f97316; -fx-font-size:13px; " +
        "-fx-font-weight:600; -fx-background-radius:25; -fx-border-radius:25; " +
        "-fx-border-color:#f97316; -fx-border-width:1.5; -fx-padding:7 20; -fx-cursor:hand;";
    private static final String PILL_RES_ON =
        "-fx-background-color:#f97316; -fx-text-fill:white; -fx-font-size:13px; " +
        "-fx-font-weight:600; -fx-background-radius:25; -fx-padding:7 20; -fx-cursor:hand;";

    private static final String PILL_OC_OFF =
        "-fx-background-color:white; -fx-text-fill:#ef4444; -fx-font-size:13px; " +
        "-fx-font-weight:600; -fx-background-radius:25; -fx-border-radius:25; " +
        "-fx-border-color:#ef4444; -fx-border-width:1.5; -fx-padding:7 20; -fx-cursor:hand;";
    private static final String PILL_OC_ON =
        "-fx-background-color:#ef4444; -fx-text-fill:white; -fx-font-size:13px; " +
        "-fx-font-weight:600; -fx-background-radius:25; -fx-padding:7 20; -fx-cursor:hand;";

    private static final String PILL_MANT_OFF =
        "-fx-background-color:white; -fx-text-fill:#8b5cf6; -fx-font-size:13px; " +
        "-fx-font-weight:600; -fx-background-radius:25; -fx-border-radius:25; " +
        "-fx-border-color:#8b5cf6; -fx-border-width:1.5; -fx-padding:7 20; -fx-cursor:hand;";
    private static final String PILL_MANT_ON =
        "-fx-background-color:#8b5cf6; -fx-text-fill:white; -fx-font-size:13px; " +
        "-fx-font-weight:600; -fx-background-radius:25; -fx-padding:7 20; -fx-cursor:hand;";

    // ── Estado ────────────────────────────────────────────────────────────────
    private final AppContext          ctx  = AppContext.getInstance();
    private       List<Habitacion>   todasLasHabitaciones;
    private       List<Reserva>      todasLasReservas;
    private       String             filtroActual   = "TODAS";
    private volatile long            generacion     = 0;

    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE_ES);

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        cargarDatos();
    }

    // ── Carga de datos (REFRESCO) ─────────────────────────────────────────────

    @FXML
    public void cargarDatos() {
        progressBar.setVisible(true);
        flowPane.getChildren().clear();

        final long gen = ++generacion;

        Thread t = new Thread(() -> {
            try {
                List<Habitacion> habitaciones = ctx.getHabitacionService().listarTodasLasHabitaciones();
                List<Reserva> reservas = ctx.getReservaService().listarTodasLasReservas();
                Platform.runLater(() -> {
                    if (gen != generacion) return;
                    todasLasHabitaciones = habitaciones;
                    todasLasReservas = reservas;
                    actualizarStats(habitaciones);
                    renderizarGrid(habitaciones);
                    progressBar.setVisible(false);
                    NotificationUtil.info("Datos actualizados correctamente");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (gen != generacion) return;
                    NotificationUtil.error("Error cargando habitaciones: " + e.getMessage());
                    progressBar.setVisible(false);
                });
            }
        }, "carga-habitaciones-" + gen);
        t.setDaemon(true);
        t.start();
    }

    // ─── MÉTODO DESHABILITADO - Solo para evitar error de FXML ─────────────────
    
    @FXML
    public void nuevaHabitacion() {
        // Método deshabilitado - la creación de nuevas habitaciones ya no está disponible
        NotificationUtil.info("La creación de nuevas habitaciones ha sido deshabilitada.");
    }

    // ── Filtros ───────────────────────────────────────────────────────────────

    @FXML public void filtrarTodas()         { activarFiltro("TODAS");        }
    @FXML public void filtrarDisponibles()   { activarFiltro("DISPONIBLE");   }
    @FXML public void filtrarReservadas()    { activarFiltro("RESERVADA");    }
    @FXML public void filtrarOcupadas()      { activarFiltro("OCUPADA");      }
    @FXML public void filtrarMantenimiento() { activarFiltro("MANTENIMIENTO");}

    private void activarFiltro(String estado) {
        filtroActual = estado;
        // Resetear todos los botones a estado "off"
        btnTodas.setStyle(PILL_TODAS_OFF);
        btnDisponibles.setStyle(PILL_DISP_OFF);
        btnReservadas.setStyle(PILL_RES_OFF);
        btnOcupadas.setStyle(PILL_OC_OFF);
        btnMantenimiento.setStyle(PILL_MANT_OFF);
        // Activar el seleccionado
        switch (estado) {
            case "TODAS":        btnTodas.setStyle(PILL_TODAS_ON);  break;
            case "DISPONIBLE":   btnDisponibles.setStyle(PILL_DISP_ON); break;
            case "RESERVADA":    btnReservadas.setStyle(PILL_RES_ON);   break;
            case "OCUPADA":      btnOcupadas.setStyle(PILL_OC_ON);      break;
            case "MANTENIMIENTO":btnMantenimiento.setStyle(PILL_MANT_ON);break;
        }
        if (todasLasHabitaciones == null) return;
        List<Habitacion> filtradas;
        if ("TODAS".equals(estado)) {
            filtradas = todasLasHabitaciones;
        } else {
            Habitacion.EstadoHabitacion est = Habitacion.EstadoHabitacion.valueOf(estado);
            filtradas = todasLasHabitaciones.stream()
                    .filter(h -> h.getEstado() == est)
                    .collect(Collectors.toList());
        }
        renderizarGrid(filtradas);
    }

    // ── Renderizado del grid ──────────────────────────────────────────────────

    private void renderizarGrid(List<Habitacion> habitaciones) {
        flowPane.getChildren().clear();
        if (habitaciones.isEmpty()) {
            Label empty = new Label("Sin habitaciones en este estado");
            empty.setStyle("-fx-text-fill:#9ca3af; -fx-font-size:14px;");
            flowPane.getChildren().add(empty);
            return;
        }
        for (Habitacion h : habitaciones) {
            flowPane.getChildren().add(crearCard(h));
        }
    }

    private VBox crearCard(Habitacion h) {
        VBox card = new VBox(0);
        card.setPrefWidth(220);
        card.setMinWidth(210);
        card.setMaxWidth(240);
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle(CARD_BASE);

        // Hover
        card.setOnMouseEntered(e -> card.setStyle(CARD_HOVER));
        card.setOnMouseExited(e  -> card.setStyle(CARD_BASE));
        card.setOnMouseClicked(e -> { if (e.getClickCount() == 2) mostrarModalModerno(h); });

        // ── Fila superior: ID (gris) + icono cama (derecha) ──────────────────
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(0, 0, 10, 0));

        Label idLabel = new Label("ID: " + h.getNumero());
        idLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#9ca3af;");
        HBox.setHgrow(idLabel, Priority.ALWAYS);

        Label iconoCama = new Label("🛏");
        iconoCama.setStyle("-fx-font-size:16px; -fx-text-fill:" + iconColorEstado(h.getEstado()) + ";");

        topRow.getChildren().addAll(idLabel, iconoCama);

        // ── Nombre hab (grande bold) ──────────────────────────────────────────
        Label numero = new Label("Hab. " + h.getNumero());
        numero.setStyle("-fx-font-size:20px; -fx-font-weight:800; " +
                        "-fx-text-fill:#1e293b; -fx-padding:0 0 5 0;");

        // ── Tipo ─────────────────────────────────────────────────────────────
        String tipoStr = h.getTipoHabitacion() != null
                ? h.getTipoHabitacion().obtenerEtiquetaTipo().toUpperCase() : "SIMPLE";
        Label tipo = new Label("🛏  " + tipoStr);
        tipo.setStyle("-fx-font-size:12px; -fx-font-weight:600; " +
                      "-fx-text-fill:#64748b; -fx-padding:0 0 3 0;");

        // ── Precio ───────────────────────────────────────────────────────────
        Label precio = new Label(String.format("$%,.0f/noche", h.calcularPrecioFinal()));
        precio.setStyle("-fx-font-size:15px; -fx-font-weight:700; " +
                        "-fx-text-fill:#1e293b; -fx-padding:0 0 12 0;");

        // ── Badge de estado ───────────────────────────────────────────────────
        Label badge = new Label(labelEstado(h.getEstado()));
        badge.setStyle(badgeStyle(h.getEstado()));

        card.getChildren().addAll(topRow, numero, tipo, precio, badge);
        return card;
    }
    
    // ─── MODAL MODERNO ESTILO DASHBOARD ────────────────────────────────────────

    private void mostrarModalModerno(Habitacion habitacion) {
        // Buscar reserva activa para esta habitación
        Reserva reservaActiva = null;
        if (todasLasReservas != null) {
            reservaActiva = todasLasReservas.stream()
                    .filter(r -> r.getHabitacion() != null 
                              && r.getHabitacion().getNumero().equals(habitacion.getNumero())
                              && (r.getEstado() == Reserva.EstadoReserva.EN_PROCESO 
                               || r.getEstado() == Reserva.EstadoReserva.CONFIRMADA))
                    .findFirst()
                    .orElse(null);
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
        
        // Icono cuadrado con fondo azul real según estado
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(48, 48);
        iconBox.setMaxSize(48, 48);
        iconBox.setStyle("-fx-background-color: #1e3a5f; -fx-background-radius: 12px;");
        
        String iconoTexto;
        String estadoColor;
        switch (habitacion.getEstado()) {
            case DISPONIBLE:
                iconoTexto = "✓";
                estadoColor = "#22c55e";
                break;
            case OCUPADA:
                iconoTexto = "🔑";
                estadoColor = "#ef4444";
                break;
            case RESERVADA:
                iconoTexto = "📅";
                estadoColor = "#f97316";
                break;
            default:
                iconoTexto = "🔧";
                estadoColor = "#8b5cf6";
        }
        
        Label iconLabel = new Label(iconoTexto);
        iconLabel.setStyle("-fx-font-size: 24px;");
        iconBox.getChildren().add(iconLabel);
        
        // Textos del header
        VBox headerTexts = new VBox(4);
        Label titleLabel = new Label("Habitación " + habitacion.getNumero());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // Badge con estado
        HBox estadoBadge = new HBox(6);
        estadoBadge.setAlignment(Pos.CENTER);
        estadoBadge.setPadding(new Insets(4, 10, 4, 10));
        estadoBadge.setStyle("-fx-background-color: " + estadoColor + "20; -fx-background-radius: 20px;");
        Label dotIndicator = new Label("●");
        dotIndicator.setStyle("-fx-text-fill: " + estadoColor + "; -fx-font-size: 8px;");
        String estadoTexto = switch (habitacion.getEstado()) {
            case DISPONIBLE -> "DISPONIBLE";
            case OCUPADA -> "OCUPADA";
            case RESERVADA -> "RESERVADA";
            default -> "MANTENIMIENTO";
        };
        Label estadoLabel = new Label(estadoTexto);
        estadoLabel.setStyle("-fx-text-fill: " + estadoColor + "; -fx-font-size: 11px; -fx-font-weight: 600;");
        estadoBadge.getChildren().addAll(dotIndicator, estadoLabel);
        headerTexts.getChildren().addAll(titleLabel, estadoBadge);
        
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
        
        // Información de la habitación en Grid
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(16);
        infoGrid.setVgap(14);
        infoGrid.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-padding: 16px;");
        
        // Fila 1: Tipo
        Label tipoLabel = new Label("🏷️ TIPO");
        tipoLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #64748b;");
        String tipoValor = habitacion.getTipoHabitacion() != null 
                ? habitacion.getTipoHabitacion().obtenerEtiquetaTipo() : "SIMPLE";
        Label tipoValorLabel = new Label(tipoValor);
        tipoValorLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");
        VBox tipoBox = new VBox(2, tipoLabel, tipoValorLabel);
        
        // Fila 2: Camas
        Label camasLabel = new Label("🛏️ CAMAS");
        camasLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #64748b;");
        Label camasValorLabel = new Label(habitacion.getNumCamas() + " cama" + (habitacion.getNumCamas() != 1 ? "s" : ""));
        camasValorLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");
        VBox camasBox = new VBox(2, camasLabel, camasValorLabel);
        
        // Fila 3: Precio
        Label precioLabel = new Label("💰 PRECIO POR NOCHE");
        precioLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #64748b;");
        Label precioValorLabel = new Label(formatCOP(habitacion.calcularPrecioFinal()));
        precioValorLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #1e3a5f;");
        VBox precioBox = new VBox(2, precioLabel, precioValorLabel);
        
        infoGrid.add(tipoBox, 0, 0);
        infoGrid.add(camasBox, 1, 0);
        infoGrid.add(precioBox, 2, 0);
        
        body.getChildren().add(infoGrid);
        
        // ─── INFORMACIÓN DEL HUÉSPED (si está ocupada o reservada) ─────────────
        if ((habitacion.getEstado() == Habitacion.EstadoHabitacion.OCUPADA || 
             habitacion.getEstado() == Habitacion.EstadoHabitacion.RESERVADA) && reservaActiva != null) {
            
            Cliente cliente = reservaActiva.getCliente();
            if (cliente != null) {
                VBox guestSection = new VBox(12);
                guestSection.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 12px; -fx-padding: 16px;");
                
                Label guestTitle = new Label(habitacion.getEstado() == Habitacion.EstadoHabitacion.OCUPADA ? "👤 HUÉSPED ACTUAL" : "👤 HUÉSPED RESERVADO");
                guestTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #166534;");
                
                Label guestName = new Label(cliente.obtenerNombreCompleto());
                guestName.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
                
                HBox guestDetails = new HBox(20);
                
                VBox docBox = new VBox(2);
                Label docLabel = new Label("DOCUMENTO");
                docLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: #64748b;");
                Label docValue = new Label(cliente.getDocumento() != null ? cliente.getDocumento() : "—");
                docValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
                docBox.getChildren().addAll(docLabel, docValue);
                
                VBox telBox = new VBox(2);
                Label telLabel = new Label("TELÉFONO");
                telLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: #64748b;");
                Label telValue = new Label(cliente.getTelefono() != null ? cliente.getTelefono() : "—");
                telValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
                telBox.getChildren().addAll(telLabel, telValue);
                
                VBox emailBox = new VBox(2);
                Label emailLabel = new Label("EMAIL");
                emailLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: #64748b;");
                Label emailValue = new Label(cliente.getEmail() != null ? cliente.getEmail() : "—");
                emailValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
                emailBox.getChildren().addAll(emailLabel, emailValue);
                
                guestDetails.getChildren().addAll(docBox, telBox, emailBox);
                
                // Fechas de estancia
                if (reservaActiva.getFechaEntrada() != null && reservaActiva.getFechaSalida() != null) {
                    HBox datesBox = new HBox(16);
                    VBox entradaBox = new VBox(2);
                    Label entradaLabel = new Label("📅 ENTRADA");
                    entradaLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: #3b82f6;");
                    Label entradaValue = new Label(reservaActiva.getFechaEntrada().format(FECHA_FORMATTER));
                    entradaValue.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");
                    entradaBox.getChildren().addAll(entradaLabel, entradaValue);
                    
                    VBox salidaBox = new VBox(2);
                    Label salidaLabel = new Label("🚪 SALIDA");
                    salidaLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: #ef4444;");
                    Label salidaValue = new Label(reservaActiva.getFechaSalida().format(FECHA_FORMATTER));
                    salidaValue.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");
                    salidaBox.getChildren().addAll(salidaLabel, salidaValue);
                    
                    datesBox.getChildren().addAll(entradaBox, salidaBox);
                    guestSection.getChildren().addAll(guestTitle, guestName, guestDetails, datesBox);
                } else {
                    guestSection.getChildren().addAll(guestTitle, guestName, guestDetails);
                }
                
                body.getChildren().add(guestSection);
            }
        }
        
        // ─── BOTONES DE ACCIÓN ────────────────────────────────────────────────
        HBox accionesBox = new HBox(12);
        accionesBox.setAlignment(Pos.CENTER);
        accionesBox.setPadding(new Insets(8, 0, 0, 0));
        
        // ─── BOTÓN ENVIAR A MANTENIMIENTO (PARA TODAS LAS HABITACIONES) ────────
        Button btnMantenimiento = new Button("🔧 Enviar a mantenimiento");
        btnMantenimiento.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; " +
                                 "-fx-background-radius: 8px; -fx-padding: 8px 16px; " +
                                 "-fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand;");
        
        btnMantenimiento.setOnAction(e -> {
            String mensajeAdvertencia = "";
            boolean necesitaAdvertencia = false;
            
            if (habitacion.getEstado() == Habitacion.EstadoHabitacion.OCUPADA) {
                mensajeAdvertencia = "⚠️ La habitación " + habitacion.getNumero() + " está OCUPADA.\n\n" +
                                     "Si la envías a mantenimiento, se perderá la reserva actual.\n\n" +
                                     "¿Estás seguro de continuar?";
                necesitaAdvertencia = true;
            } else if (habitacion.getEstado() == Habitacion.EstadoHabitacion.RESERVADA) {
                mensajeAdvertencia = "⚠️ La habitación " + habitacion.getNumero() + " está RESERVADA.\n\n" +
                                     "Si la envías a mantenimiento, se perderá la reserva actual.\n\n" +
                                     "¿Estás seguro de continuar?";
                necesitaAdvertencia = true;
            } else if (habitacion.getEstado() == Habitacion.EstadoHabitacion.DISPONIBLE) {
                mensajeAdvertencia = "¿Estás seguro de enviar la habitación " + habitacion.getNumero() + " a mantenimiento?\n\n" +
                                     "La habitación quedará fuera de servicio hasta que se repare.";
                necesitaAdvertencia = false;
            } else if (habitacion.getEstado() == Habitacion.EstadoHabitacion.MANTENIMIENTO) {
                mensajeAdvertencia = "⚠️ La habitación " + habitacion.getNumero() + " YA ESTÁ EN MANTENIMIENTO.\n\n" +
                                     "¿Deseas enviarla nuevamente a mantenimiento?";
                necesitaAdvertencia = false;
            }
            
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmar");
            confirm.setHeaderText("Enviar a mantenimiento");
            confirm.setContentText(mensajeAdvertencia);
            confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    dialog.close();
                    ejecutarEnviarMantenimiento(habitacion);
                }
            });
        });
        
        accionesBox.getChildren().add(btnMantenimiento);
        
        // ─── BOTÓN LIBERAR (solo para habitaciones NO disponibles) ─────────────
        if (habitacion.getEstado() != Habitacion.EstadoHabitacion.DISPONIBLE) {
            Button btnLiberar = new Button("✓ Liberar habitación");
            btnLiberar.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; " +
                               "-fx-background-radius: 8px; -fx-padding: 8px 16px; " +
                               "-fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand;");
            btnLiberar.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirmar");
                confirm.setHeaderText("Liberar habitación");
                confirm.setContentText("¿Estás seguro de liberar la habitación " + habitacion.getNumero() + "?\n\n" +
                                       "La habitación quedará disponible para nuevas reservas.");
                confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        dialog.close();
                        liberarHabitacion(habitacion);
                    }
                });
            });
            accionesBox.getChildren().add(btnLiberar);
        }
        
        body.getChildren().add(accionesBox);
        
        // ─── FOOTER ───────────────────────────────────────────────────────────
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
        
        mainContainer.getChildren().addAll(header, body, footer);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().setPrefSize(550, 550);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        
        Button defaultCloseButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        defaultCloseButton.setVisible(false);
        defaultCloseButton.setManaged(false);
        
        dialog.showAndWait();
    }
    
    // ─── EJECUTAR ENVÍO A MANTENIMIENTO ───────────────────────────────────────
    
    private void ejecutarEnviarMantenimiento(Habitacion habitacion) {
        new Thread(() -> {
            try {
                ctx.getHabitacionService().enviarHabitacionAMantenimiento(habitacion.getNumero());
                Platform.runLater(() -> {
                    NotificationUtil.exito("Habitación " + habitacion.getNumero() + " enviada a mantenimiento.");
                    cargarDatos();
                });
            } catch (ExcepcionNegocio ex) {
                Platform.runLater(() -> NotificationUtil.error(ex.getMessage()));
            }
        }, "mantenimiento").start();
    }
    
    // ─── LIBERAR HABITACIÓN ───────────────────────────────────────────────────
    
    private void liberarHabitacion(Habitacion habitacion) {
        new Thread(() -> {
            try {
                ctx.getHabitacionService().devolverHabitacionAServicio(habitacion.getNumero());
                Platform.runLater(() -> {
                    NotificationUtil.exito("Habitación " + habitacion.getNumero() + " liberada y disponible.");
                    cargarDatos();
                });
            } catch (ExcepcionNegocio ex) {
                Platform.runLater(() -> NotificationUtil.error(ex.getMessage()));
            }
        }, "liberar").start();
    }
    
    private String formatCOP(double valor) {
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(new Locale("es", "CO"));
        nf.setMaximumFractionDigits(0);
        nf.setGroupingUsed(true);
        return "$" + nf.format(valor);
    }
    
    // ─── STATS BADGES ─────────────────────────────────────────────────────────
    
    private void actualizarStats(List<Habitacion> lista) {
        statsRow.getChildren().clear();
        long total       = lista.size();
        long disponibles = lista.stream().filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.DISPONIBLE).count();
        long reservadas  = lista.stream().filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.RESERVADA).count();
        long ocupadas    = lista.stream().filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.OCUPADA).count();
        long mantenim    = lista.stream().filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.MANTENIMIENTO).count();
        double pct       = total > 0 ? ((ocupadas + reservadas) * 100.0 / total) : 0;
        
        statsRow.getChildren().addAll(
            badge("🏠  Total: "             + total,       "#93c5fd", "#1e40af"),
            badge("✓  Disponibles: "        + disponibles, "#86efac", "#166534"),
            badge("📅  Reservadas: "        + reservadas,  "#67e8f9", "#0e7490"),
            badge("🔑  Ocupadas: "          + ocupadas,    "#fcd34d", "#92400e"),
            badge("🔧  Mantenim.: "         + mantenim,    "#c4b5fd", "#5b21b6"),
            badge(String.format("📊  Ocupación: %.0f%%", pct), "#a5b4fc", "#3730a3")
        );
    }
    
    private Label badge(String texto, String borderColor, String textColor) {
        Label l = new Label(texto);
        l.setStyle(
            "-fx-background-color:white;" +
            "-fx-border-color:" + borderColor + ";" +
            "-fx-border-width:1.5;" +
            "-fx-border-radius:999;" +
            "-fx-background-radius:999;" +
            "-fx-padding:5 14;" +
            "-fx-font-size:12px;" +
            "-fx-font-weight:600;" +
            "-fx-text-fill:" + textColor + ";");
        return l;
    }
    
    // ─── ESTILOS Y AUXILIARES ─────────────────────────────────────────────────
    
    private static final String CARD_BASE =
        "-fx-background-color:white;" +
        "-fx-background-radius:14;" +
        "-fx-border-radius:14;" +
        "-fx-border-color:#e2e8f0;" +
        "-fx-border-width:1.5;" +
        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);" +
        "-fx-cursor:hand;";
    
    private static final String CARD_HOVER =
        "-fx-background-color:white;" +
        "-fx-background-radius:14;" +
        "-fx-border-radius:14;" +
        "-fx-border-color:#1e3a5f;" +
        "-fx-border-width:1.5;" +
        "-fx-effect:dropshadow(gaussian,rgba(30,58,95,0.15),12,0,0,4);" +
        "-fx-cursor:hand;" +
        "-fx-translate-y:-2;";
    
    private String iconColorEstado(Habitacion.EstadoHabitacion est) {
        switch (est) {
            case DISPONIBLE:    return "#22c55e";
            case RESERVADA:     return "#f97316";
            case OCUPADA:       return "#ef4444";
            case MANTENIMIENTO: return "#8b5cf6";
            default:            return "#6b7280";
        }
    }
    
    private String labelEstado(Habitacion.EstadoHabitacion est) {
        switch (est) {
            case DISPONIBLE:    return "✓ Disponible";
            case RESERVADA:     return "📅 Reservada";
            case OCUPADA:       return "🔑 Ocupada";
            case MANTENIMIENTO: return "🔧 Mantenimiento";
            default:            return est.name();
        }
    }
    
    private String badgeStyle(Habitacion.EstadoHabitacion est) {
        String base = "-fx-background-radius:999; -fx-padding:4 12; " +
                      "-fx-font-size:11px; -fx-font-weight:700;";
        switch (est) {
            case DISPONIBLE:    return base + "-fx-background-color:#dcfce7; -fx-text-fill:#166534;";
            case RESERVADA:     return base + "-fx-background-color:#ffedd5; -fx-text-fill:#c2410c;";
            case OCUPADA:       return base + "-fx-background-color:#fee2e2; -fx-text-fill:#991b1b;";
            case MANTENIMIENTO: return base + "-fx-background-color:#ede9fe; -fx-text-fill:#5b21b6;";
            default:            return base + "-fx-background-color:#f3f4f6; -fx-text-fill:#6b7280;";
        }
    }
}
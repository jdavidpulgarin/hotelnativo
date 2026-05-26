/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.controllers;

import com.hotel.AppContext;
import com.hotel.dto.HabitacionDTO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.Habitacion;
import com.hotel.ui.components.NotificationUtil;
import com.hotel.ui.components.ValidacionCampo;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador del módulo de Habitaciones.
 * Muestra las habitaciones como tarjetas visuales con código de color por estado.
 */
public class HabitacionesController {

    @FXML private FlowPane gridHabitaciones;
    @FXML private HBox     statsRow;
    @FXML private VBox     loadingBox;

    private final AppContext ctx = AppContext.getInstance();
    private List<Habitacion> todasLasHabitaciones;
    private String filtroActual = "TODAS";
    // Contador de generación: descarta resultados de cargas anteriores (anti race-condition)
    private volatile long generacionActual = 0;

    @FXML
    public void initialize() {
        cargarDatos();
    }

    @FXML
    public void cargarDatos() {
        gridHabitaciones.setVisible(false);
        loadingBox.setVisible(true);

        final long miGeneracion = ++generacionActual;

        Thread t = new Thread(() -> {
            try {
                List<Habitacion> lista = ctx.getHabitacionService().listarTodasLasHabitaciones();
                Platform.runLater(() -> {
                    if (miGeneracion != generacionActual) return; // resultado obsoleto
                    todasLasHabitaciones = lista;
                    actualizarStats(lista);
                    renderizarGrid(lista);
                    loadingBox.setVisible(false);
                    gridHabitaciones.setVisible(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (miGeneracion != generacionActual) return;
                    NotificationUtil.error("Error: " + e.getMessage());
                    loadingBox.setVisible(false);
                });
            }
        }, "carga-habitaciones-" + miGeneracion);
        t.setDaemon(true);
        t.start();
    }
     @FXML public void filtrarTodas()        { filtrarPorEstado("TODAS"); }
    @FXML public void filtrarDisponibles()  { filtrarPorEstado("DISPONIBLE"); }
    @FXML public void filtrarReservadas()   { filtrarPorEstado("RESERVADA"); }
    @FXML public void filtrarOcupadas()     { filtrarPorEstado("OCUPADA"); }
    @FXML public void filtrarMantenimiento(){ filtrarPorEstado("MANTENIMIENTO"); }

    /** Mantiene retrocompatibilidad con llamadas directas desde código. */
    @FXML
    public void filtrar() { filtrarPorEstado(filtroActual); }

    private void filtrarPorEstado(String estado) {
        if (todasLasHabitaciones == null) return;
        filtroActual = estado;
        List<Habitacion> filtradas;
        switch (filtroActual) {
            case "DISPONIBLE":
                filtradas = todasLasHabitaciones.stream()
                        .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.DISPONIBLE)
                        .collect(Collectors.toList());
                break;
            case "RESERVADA":
                filtradas = todasLasHabitaciones.stream()
                        .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.RESERVADA)
                        .collect(Collectors.toList());
                break;
            case "OCUPADA":
                filtradas = todasLasHabitaciones.stream()
                        .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.OCUPADA)
                        .collect(Collectors.toList());
                break;
            case "MANTENIMIENTO":
                filtradas = todasLasHabitaciones.stream()
                        .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.MANTENIMIENTO)
                        .collect(Collectors.toList());
                break;
            default:
                filtradas = todasLasHabitaciones;
        }
        renderizarGrid(filtradas);
    }

    @FXML
    public void nuevaHabitacion() {
        mostrarFormulario(null);
    }
     // ── Renderizado del grid ──────────────────────────────────────────────────

    private void renderizarGrid(List<Habitacion> habitaciones) {
        gridHabitaciones.getChildren().clear();
        for (Habitacion h : habitaciones) {
            gridHabitaciones.getChildren().add(crearTarjeta(h));
        }
        if (habitaciones.isEmpty()) {
            Label empty = new Label("Sin habitaciones en este estado");
            empty.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:14px;");
            gridHabitaciones.getChildren().add(empty);
        }
    }

    private VBox crearTarjeta(Habitacion h) {
        VBox card = new VBox(10);
        card.setPrefWidth(180);
        card.setPrefHeight(160);
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle(getCardStyle(h.getEstado()));
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) mostrarFormulario(h);
        });

        // ID de base de datos (pequeño, en la esquina)
        Label idLabel = new Label("ID: " + h.getId());
        idLabel.setStyle("-fx-font-size:9px; -fx-text-fill:" + getTextColor(h.getEstado()) + "; -fx-opacity:0.55;");

        // Número de habitación
        Label numero = new Label("Hab. " + h.getNumero());
        numero.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:" + getTextColor(h.getEstado()) + ";");

        // Tipo
        String tipoStr = h.getTipoHabitacion() != null ?
                h.getTipoHabitacion().obtenerEtiquetaTipo() : "—";
        Label tipo = new Label(getTipoIcon(tipoStr) + "  " + tipoStr);
        tipo.setStyle("-fx-font-size:12px; -fx-text-fill:" + getTextColor(h.getEstado()) + "; -fx-opacity:0.8;");

        // Piso
        Label piso = new Label("Piso " + (h.getPiso() != null ? h.getPiso().getNumeroPiso() : "?"));
        piso.setStyle("-fx-font-size:11px; -fx-text-fill:" + getTextColor(h.getEstado()) + "; -fx-opacity:0.65;");

        // Precio
        Label precio = new Label(String.format("$%,.0f/noche", h.calcularPrecioFinal()));
        precio.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:" + getTextColor(h.getEstado()) + ";");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Badge de estado
        Label badge = new Label(getEstadoLabel(h.getEstado()));
        badge.setStyle(getBadgeStyle(h.getEstado()));

        // Acciones al hover
        card.setOnMouseEntered(e -> card.setStyle(getCardStyleHover(h.getEstado())));
        card.setOnMouseExited(e ->  card.setStyle(getCardStyle(h.getEstado())));

        card.getChildren().addAll(idLabel, numero, tipo, piso, spacer, precio, badge);
        return card;
    }
        private void actualizarStats(List<Habitacion> lista) {
        statsRow.getChildren().clear();
        long disponibles   = lista.stream().filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.DISPONIBLE).count();
        long reservadas    = lista.stream().filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.RESERVADA).count();
        long ocupadas      = lista.stream().filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.OCUPADA).count();
        long mantenimiento = lista.stream().filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.MANTENIMIENTO).count();
        double ocupacion   = lista.isEmpty() ? 0 : ((ocupadas + reservadas) * 100.0) / lista.size();

        statsRow.getChildren().addAll(
            chip("🛏 Total: " + lista.size(),            "#dbeafe", "#1d4ed8"),
            chip("✓ Disponibles: " + disponibles,         "#dcfce7", "#15803d"),
            chip("📅 Reservadas: " + reservadas,          "#dbeafe", "#1d4ed8"),
            chip("👥 Ocupadas: " + ocupadas,               "#fef3c7", "#b45309"),
            chip("🔧 Mantenimiento: " + mantenimiento,    "#fee2e2", "#b91c1c"),
            chip(String.format("📊 Ocupación: %.0f%%", ocupacion), "#ede9fe", "#6d28d9")
        );
    }
            // ── Formulario modal ──────────────────────────────────────────────────────

    private void mostrarFormulario(Habitacion hab) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(hab == null ? "Nueva Habitación" : "Habitación " + hab.getNumero());

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(14);
        grid.setPadding(new Insets(20, 24, 20, 24));
        grid.setPrefWidth(400);

        TextField fNumero = new TextField(hab != null ? hab.getNumero() : "");
        fNumero.setPromptText("Ej: 101");

        ComboBox<String> fTipo = new ComboBox<>();
        fTipo.getItems().addAll("1 - Simple", "2 - Doble", "3 - Suite");
        if (hab != null && hab.getTipoHabitacion() != null) {
            fTipo.setValue(hab.getTipoHabitacion().getId() + " - " +
                           hab.getTipoHabitacion().obtenerEtiquetaTipo());
        }

        TextField fPiso = new TextField(hab != null && hab.getPiso() != null ?
                String.valueOf(hab.getPiso().getId()) : "");
        fPiso.setPromptText("ID del piso");

        TextField fPrecio = new TextField(hab != null ?
                String.valueOf(hab.getPrecioBase()) : "");
        fPrecio.setPromptText("Precio base");

        Label errNumero = new Label(); Label errPrecio = new Label();
        ValidacionCampo.aplicarSoloNumeros(fNumero, errNumero);
        ValidacionCampo.aplicarDecimal(fPrecio, errPrecio);

        VBox vbNumero = new VBox(2, fNumero, errNumero);
        VBox vbPrecio = new VBox(2, fPrecio, errPrecio);
        grid.addRow(0, lab("Número:"), vbNumero);
        grid.addRow(1, lab("Tipo:"),   fTipo);
        grid.addRow(2, lab("ID Piso:"),fPiso);
        grid.addRow(3, lab("Precio:"), vbPrecio);

        VBox content = new VBox(12);
        Label header = new Label(hab == null ? "➕ Registrar habitación" : "✏ Editar habitación");
        header.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;");
        Label errLabel = new Label("");
        errLabel.setStyle("-fx-text-fill:#dc2626; -fx-font-size:12px;");
          // Botones de acción rápida si hay habitación seleccionada
        if (hab != null) {
            HBox acciones = new HBox(8);
            Button btnMant = new Button("🔧 Enviar a Mantenimiento");
            btnMant.setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#b45309; -fx-background-radius:8px; -fx-cursor:hand; -fx-padding:6px 12px;");
            btnMant.setOnAction(e -> {
                btnMant.setDisable(true);
                Thread t = new Thread(() -> {
                    try {
                        ctx.getHabitacionService().enviarHabitacionAMantenimiento(hab.getId());
                        Platform.runLater(() -> {
                            NotificationUtil.info("Habitación enviada a mantenimiento.");
                            dialog.close();
                            cargarDatos();
                        });
                    } catch (ExcepcionNegocio ex) {
                        Platform.runLater(() -> { btnMant.setDisable(false); errLabel.setText(ex.getMessage()); });
                    }
                }, "accion-mantenimiento");
                t.setDaemon(true); t.start();
            });
            Button btnLiberar = new Button("✓ Liberar");
            btnLiberar.setStyle("-fx-background-color:#dcfce7; -fx-text-fill:#15803d; -fx-background-radius:8px; -fx-cursor:hand; -fx-padding:6px 12px;");
            btnLiberar.setOnAction(e -> {
                btnLiberar.setDisable(true);
                Thread t = new Thread(() -> {
                    try {
                        ctx.getHabitacionService().devolverHabitacionAServicio(hab.getId());
                        Platform.runLater(() -> {
                            NotificationUtil.exito("Habitación disponible.");
                            dialog.close();
                            cargarDatos();
                        });
                    } catch (ExcepcionNegocio ex) {
                        Platform.runLater(() -> { btnLiberar.setDisable(false); errLabel.setText(ex.getMessage()); });
                    }
                }, "accion-liberar");
                t.setDaemon(true); t.start();
            });
            acciones.getChildren().addAll(btnMant, btnLiberar);
            content.getChildren().addAll(header, new Separator(), acciones, new Separator(), grid, errLabel);
        } else {
            content.getChildren().addAll(header, new Separator(), grid, errLabel);
        }
              dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color:white; -fx-background-radius:12px;");

        Button btnGuardar = (Button) dialog.getDialogPane()
                .lookupButton(dialog.getDialogPane().getButtonTypes().get(0));
        btnGuardar.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (fNumero.getText().trim().isEmpty()) {
                errLabel.setText("El número de habitación es obligatorio.");
                event.consume(); return;
            }
            if (fTipo.getValue() == null) {
                errLabel.setText("Debes seleccionar un tipo de habitación.");
                event.consume(); return;
            }
            if (fPiso.getText().trim().isEmpty()) {
                errLabel.setText("El ID de piso es obligatorio.");
                event.consume(); return;
            }
            if (fPrecio.getText().trim().isEmpty()) {
                errLabel.setText("El precio base es obligatorio.");
                event.consume(); return;
            }
            try {
                Integer.parseInt(fPiso.getText().trim());
            } catch (NumberFormatException e) {
                errLabel.setText("El ID de piso debe ser un número entero válido.");
                event.consume(); return;
            }
            try {
                double precioCheck = Double.parseDouble(fPrecio.getText().trim());
                if (precioCheck <= 0) {
                    errLabel.setText("El precio debe ser mayor a cero.");
                    event.consume(); return;
                }
            } catch (NumberFormatException e) {
                errLabel.setText("El precio debe ser un valor numérico válido.");
                event.consume(); return;
            }
            try {
                int idTipo = Integer.parseInt(fTipo.getValue().split(" ")[0]);
                int idPiso = Integer.parseInt(fPiso.getText().trim());
                double precio = Double.parseDouble(fPrecio.getText().trim());
                HabitacionDTO dto = new HabitacionDTO(fNumero.getText().trim(), idTipo, idPiso, precio);

                if (hab == null) {
                    ctx.getHabitacionService().registrarHabitacion(dto);
                    NotificationUtil.exito("Habitación registrada.");
                } else {
                    ctx.getHabitacionService().actualizarHabitacion(hab.getId(), dto);
                    NotificationUtil.exito("Habitación actualizada.");
                }
                cargarDatos();
            } catch (Exception e) {
                errLabel.setText("Error: " + e.getMessage());
                event.consume();
            }
        });
        dialog.setResultConverter(btn -> btn);
        dialog.showAndWait();
    }
    
    // ── Estilos dinámicos ─────────────────────────────────────────────────────

    private String getCardStyle(Habitacion.EstadoHabitacion estado) {
        switch (estado) {
            case DISPONIBLE:   return "-fx-background-color:#f0fdf4; -fx-border-color:#86efac; -fx-border-width:1.5px; -fx-background-radius:12px; -fx-border-radius:12px; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,2); -fx-cursor:hand;";
            case RESERVADA:    return "-fx-background-color:#eff6ff; -fx-border-color:#93c5fd; -fx-border-width:1.5px; -fx-background-radius:12px; -fx-border-radius:12px; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,2); -fx-cursor:hand;";
            case OCUPADA:      return "-fx-background-color:#fffbeb; -fx-border-color:#fcd34d; -fx-border-width:1.5px; -fx-background-radius:12px; -fx-border-radius:12px; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,2); -fx-cursor:hand;";
            case MANTENIMIENTO:return "-fx-background-color:#fef2f2; -fx-border-color:#fca5a5; -fx-border-width:1.5px; -fx-background-radius:12px; -fx-border-radius:12px; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,2); -fx-cursor:hand;";
            default:           return "-fx-background-color:#f8fafc; -fx-border-color:#cbd5e1; -fx-border-width:1.5px; -fx-background-radius:12px; -fx-border-radius:12px; -fx-cursor:hand;";
        }
    }

    private String getCardStyleHover(Habitacion.EstadoHabitacion estado) {
        return getCardStyle(estado) + " -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.14),18,0,0,4); -fx-translate-y:-2;";
    }

    private String getTextColor(Habitacion.EstadoHabitacion estado) {
        switch (estado) {
            case DISPONIBLE:    return "#15803d";
            case RESERVADA:     return "#1d4ed8";
            case OCUPADA:       return "#b45309";
            case MANTENIMIENTO: return "#b91c1c";
            default:            return "#475569";
        }
    }

    private String getEstadoLabel(Habitacion.EstadoHabitacion estado) {
        switch (estado) {
            case DISPONIBLE:    return "✓ Disponible";
            case RESERVADA:     return "📅 Reservada";
            case OCUPADA:       return "👥 Ocupada";
            case MANTENIMIENTO: return "🔧 Mantenimiento";
            default:            return estado.name();
        }
    }

    private String getBadgeStyle(Habitacion.EstadoHabitacion estado) {
        switch (estado) {
            case DISPONIBLE:    return "-fx-background-color:#dcfce7; -fx-text-fill:#15803d; -fx-background-radius:20px; -fx-padding:3px 10px; -fx-font-size:10px; -fx-font-weight:bold;";
            case RESERVADA:     return "-fx-background-color:#dbeafe; -fx-text-fill:#1d4ed8; -fx-background-radius:20px; -fx-padding:3px 10px; -fx-font-size:10px; -fx-font-weight:bold;";
            case OCUPADA:       return "-fx-background-color:#fef3c7; -fx-text-fill:#b45309; -fx-background-radius:20px; -fx-padding:3px 10px; -fx-font-size:10px; -fx-font-weight:bold;";
            case MANTENIMIENTO: return "-fx-background-color:#fee2e2; -fx-text-fill:#b91c1c; -fx-background-radius:20px; -fx-padding:3px 10px; -fx-font-size:10px; -fx-font-weight:bold;";
            default:            return "-fx-background-color:#f1f5f9; -fx-text-fill:#475569; -fx-background-radius:20px; -fx-padding:3px 10px; -fx-font-size:10px;";
        }
    }

    private String getTipoIcon(String tipo) {
        if (tipo == null) return "🛏";
        switch (tipo.toUpperCase()) {
            case "SIMPLE": return "🛏";
            case "DOBLE":  return "🛏🛏";
            case "SUITE":  return "🌟";
            default:       return "🛏";
        }
    }

    private Label chip(String texto, String bg, String fg) {
        Label l = new Label(texto);
        l.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + ";" +
                   "-fx-background-radius:20px; -fx-padding:5px 12px; -fx-font-size:12px;");
        return l;
    }

    private Label lab(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#374151;");
        return l;
    }
}
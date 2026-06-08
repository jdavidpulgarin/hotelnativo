package com.hotel.ui.controllers;

import com.hotel.AppContext;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.Empleado;
import com.hotel.model.Factura;
import com.hotel.ui.components.FacturaTermicaView;
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
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

public class FacturacionController {

    // ── FXML — ids coinciden exactamente con Facturacion.fxml ────────────────
    @FXML private TableView<Factura>          tabla;
    @FXML private TableColumn<Factura,String> colId, colCliente, colReserva;
    @FXML private TableColumn<Factura,String> colEmision, colSubtotal, colImpuestos;
    @FXML private TableColumn<Factura,String> colTotal, colEstado, colMetodoPago;
    @FXML private TextField                   searchField;
    @FXML private HBox                        statsRow;
    @FXML private ProgressBar                 progressBar;

    private final AppContext               ctx      = AppContext.getInstance();
    private final ObservableList<Factura>  datos    = FXCollections.observableArrayList();
    private FilteredList<Factura>          filtradas;

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        configurarColumnas();
        filtradas = new FilteredList<>(datos, p -> true);
        tabla.setItems(filtradas);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cargarDatos();
    }

    // ── Columnas ──────────────────────────────────────────────────────────────

    private void configurarColumnas() {
        // ID — gris #64748b con prefijo #
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colId.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); }
                else { setText(v); setStyle("-fx-text-fill:#64748b; -fx-font-size:12px;"); }
            }
        });

        // Cliente
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCliente() != null
                        ? c.getValue().getCliente().obtenerNombreCompleto() : "—"));

        // Reserva — con prefijo #
        colReserva.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getReserva() != null
                        ? "#" + c.getValue().getReserva().getId() : "—"));

        // Fecha emisión
        colEmision.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaEmision() != null
                        ? c.getValue().getFechaEmision().toString() : "—"));

        // Subtotal
        colSubtotal.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("$%,.0f", c.getValue().getSubtotal())));

        // Impuestos
        colImpuestos.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("$%,.0f", c.getValue().getImpuestos())));

        // Total — bold
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("$%,.0f", c.getValue().getTotal())));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); }
                else { setText(v); setStyle("-fx-font-weight:700; -fx-text-fill:#1e293b;"); }
            }
        });

        // Método de pago
        colMetodoPago.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getMetodoPago() != null
                        ? c.getValue().getMetodoPago().name().replace("_", " ") : "—"));

        // Estado de pago — badge pill con ícono
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEstadoPago() != null
                        ? c.getValue().getEstadoPago().name() : "—"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null || "—".equals(v)) { setGraphic(null); setText(null); return; }
                Label badge = new Label(etiquetaEstado(v));
                badge.getStyleClass().add(badgeCssClass(v));
                setGraphic(badge); setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        tabla.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Factura f, boolean empty) {
                super.updateItem(f, empty);
                setStyle("");
            }
        });
    }

    // ── Carga de datos ────────────────────────────────────────────────────────

    @FXML
    public void cargarDatos() {
        progressBar.setVisible(true);
        Thread t = new Thread(() -> {
            try {
                List<Factura> lista = ctx.getFacturaService().listarTodasLasFacturas();
                Platform.runLater(() -> {
                    datos.setAll(lista);
                    actualizarStats(lista);
                    progressBar.setVisible(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    NotificationUtil.error("Error cargando facturas: " + e.getMessage());
                    progressBar.setVisible(false);
                });
            }
        }, "hilo-cargar-facturas");
        t.setDaemon(true);
        t.start();
    }

    private void actualizarStats(List<Factura> lista) {
        long pagadas = lista.stream()
                .filter(f -> f.getEstadoPago() == Factura.EstadoPago.PAGADA).count();
        statsRow.getChildren().clear();
        statsRow.getChildren().addAll(
            badge("📄  Total: "  + lista.size(), "badge-total"),
            badge("✓  Pagadas: " + pagadas,      "badge-pagadas")
        );
    }

    private Label badge(String texto, String cssClass) {
        Label l = new Label(texto);
        l.getStyleClass().add(cssClass);
        return l;
    }

    // ── Búsqueda ──────────────────────────────────────────────────────────────

    @FXML
    public void filtrar() {
        String texto = searchField.getText().toLowerCase().trim();
        filtradas.setPredicate(f -> {
            if (texto.isEmpty()) return true;
            return (f.getCliente() != null
                        && f.getCliente().obtenerNombreCompleto().toLowerCase().contains(texto))
                || (f.getReserva() != null
                        && String.valueOf(f.getReserva().getId()).contains(texto))
                || (f.getEstadoPago() != null
                        && f.getEstadoPago().name().toLowerCase().contains(texto));
        });
    }

    // ── Acciones FXML ─────────────────────────────────────────────────────────

    @FXML public void cobrar()           { generarFactura(); }
    @FXML public void enviarEmail()      { enviarFacturaPorEmail(); }
    @FXML public void generarHtml()      { verHTML(); }
    @FXML public void imprimirFactura()  { imprimirTermica(); }

    @FXML
    public void handleDobleClick(MouseEvent e) {
        if (e.getClickCount() == 2) {
            Factura sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) verDetalle(sel);
        }
    }

    private void verDetalle(Factura f) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Factura #" + f.getId());
        a.setHeaderText("Factura #" + f.getId() + " — "
                + (f.getEstadoPago() != null ? etiquetaEstado(f.getEstadoPago().name()) : "—"));
        a.setContentText(
            "Cliente: "  + (f.getCliente() != null ? f.getCliente().obtenerNombreCompleto() : "—") + "\n" +
            "Reserva: "  + (f.getReserva() != null ? "#" + f.getReserva().getId() : "—") + "\n" +
            "Emisión: "  + (f.getFechaEmision() != null ? f.getFechaEmision() : "—") + "\n" +
            "Subtotal: " + String.format("$%,.0f", f.getSubtotal()) + "\n" +
            "Impuestos: "+ String.format("$%,.0f", f.getImpuestos()) + "\n" +
            "Total: "    + String.format("$%,.0f", f.getTotal()) + "\n" +
            "Método: "   + (f.getMetodoPago() != null ? f.getMetodoPago().name().replace("_"," ") : "—"));
        a.showAndWait();
    }

    // ─── GENERAR FACTURA (CON DISEÑO MODERNO, MISMA LÓGICA) ─────────────────────

    @FXML
    public void generarFactura() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Sombra moderna
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.25));
        shadow.setRadius(24);
        shadow.setOffsetY(8);
        dialog.getDialogPane().setEffect(shadow);
        
        VBox mainContainer = new VBox(0);
        mainContainer.setStyle("-fx-background-color: white; -fx-background-radius: 16px;");
        
        // CABECERA MODERNA (fondo #1a1a2e)
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 16px 16px 0 0;");
        
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(44, 44);
        iconBox.setMaxSize(44, 44);
        iconBox.setStyle("-fx-background-color: #1e3a5f; -fx-background-radius: 10px;");
        Label iconLabel = new Label("💰");
        iconLabel.setStyle("-fx-font-size: 20px;");
        iconBox.getChildren().add(iconLabel);
        
        VBox headerTexts = new VBox(4);
        Label titleLabel = new Label("Generar Factura");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        HBox totalBadge = new HBox(6);
        totalBadge.setAlignment(Pos.CENTER);
        totalBadge.setPadding(new Insets(4, 10, 4, 10));
        totalBadge.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 20px;");
        Label dotIndicator = new Label("●");
        dotIndicator.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 8px;");
        Label totalLabel = new Label("REGISTRE LOS DATOS DE LA FACTURA");
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
        
        // CUERPO (con el mismo contenido que tenías originalmente)
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        
        VBox body = new VBox(14);
        body.setPadding(new Insets(24));
        
        // ID Reserva
        VBox reservaBox = new VBox(6);
        Label reservaLabel = new Label("📋 ID RESERVA *");
        reservaLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        TextField fIdReserva = tf("ID de la reserva COMPLETADA");
        reservaBox.getChildren().addAll(reservaLabel, fIdReserva);
        
        // Método de pago
        VBox metodoBox = new VBox(6);
        Label metodoLabel = new Label("💳 MÉTODO DE PAGO *");
        metodoLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        ComboBox<String> fMetodo = new ComboBox<>();
        fMetodo.getItems().addAll("EFECTIVO", "TARJETA_CREDITO", "TARJETA_DEBITO", "TRANSFERENCIA");
        fMetodo.setValue("EFECTIVO");
        fMetodo.setMaxWidth(Double.MAX_VALUE);
        fMetodo.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                         "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-padding: 8px 12px;");
        metodoBox.getChildren().addAll(metodoLabel, fMetodo);
        
        // Paneles de pago
        TextField fMonto = tf("Monto recibido del cliente ($)");
        Label lblCambio = new Label("Cambio estimado: —");
        lblCambio.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #15803d; " +
                           "-fx-background-color: #dcfce7; -fx-background-radius: 8px; -fx-padding: 8px 12px;");
        
        fMonto.textProperty().addListener((obs, oldVal, val) -> {
            if (val == null || val.isBlank()) { lblCambio.setText("Cambio estimado: —"); return; }
            try {
                double monto = Double.parseDouble(val.trim().replace(",", "."));
                lblCambio.setText(monto > 0 ? String.format("💰 Monto ingresado: $%,.0f", monto) : "Cambio estimado: —");
            } catch (NumberFormatException e) { lblCambio.setText("Cambio estimado: —"); }
        });
        
        GridPane efectivoGrid = new GridPane();
        efectivoGrid.setHgap(14); efectivoGrid.setVgap(10);
        efectivoGrid.addRow(0, lab("Monto recibido:"), fMonto);
        GridPane.setHgrow(fMonto, Priority.ALWAYS);
        VBox panelEfectivo = seccionPago("💵  Pago en Efectivo", efectivoGrid, lblCambio);
        
        ComboBox<String> fFranquiciaCredito = franquiciaCombo(true);
        Spinner<Integer> fCuotas = new Spinner<>(1, 36, 1);
        fCuotas.setEditable(true); fCuotas.setMaxWidth(Double.MAX_VALUE);
        fCuotas.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 1.5px; -fx-border-radius: 8px;");
        Label lblCuotaInfo = new Label("1 cuota = pago de contado");
        lblCuotaInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        fCuotas.valueProperty().addListener((obs, o, n) ->
                lblCuotaInfo.setText(n == 1 ? "1 cuota = pago de contado" : n + " cuotas mensuales"));
        GridPane creditoGrid = new GridPane();
        creditoGrid.setHgap(14); creditoGrid.setVgap(10);
        creditoGrid.addRow(0, lab("Franquicia:"), fFranquiciaCredito);
        creditoGrid.addRow(1, lab("Cuotas:"),     fCuotas);
        GridPane.setHgrow(fFranquiciaCredito, Priority.ALWAYS);
        GridPane.setHgrow(fCuotas, Priority.ALWAYS);
        VBox panelCredito = seccionPago("💳  Tarjeta de Crédito", creditoGrid, lblCuotaInfo);
        
        ComboBox<String> fFranquiciaDebito = franquiciaCombo(false);
        GridPane debitoGrid = new GridPane();
        debitoGrid.setHgap(14); debitoGrid.setVgap(10);
        debitoGrid.addRow(0, lab("Franquicia:"), fFranquiciaDebito);
        GridPane.setHgrow(fFranquiciaDebito, Priority.ALWAYS);
        VBox panelDebito = seccionPago("💳  Tarjeta de Débito", debitoGrid);
        
        TextField fReferencia = tf("Número de referencia bancaria");
        GridPane transGrid = new GridPane();
        transGrid.setHgap(14); transGrid.setVgap(10);
        transGrid.addRow(0, lab("Referencia:"), fReferencia);
        GridPane.setHgrow(fReferencia, Priority.ALWAYS);
        Label lblTransInfo = new Label("Ingresa el número de confirmación de la transferencia");
        lblTransInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        lblTransInfo.setWrapText(true);
        VBox panelTransfer = seccionPago("🏦  Transferencia Bancaria", transGrid, lblTransInfo);
        
        mostrarPanel(panelEfectivo, true);
        mostrarPanel(panelCredito,  false);
        mostrarPanel(panelDebito,   false);
        mostrarPanel(panelTransfer, false);
        fMetodo.valueProperty().addListener((obs, old, nuevo) -> {
            mostrarPanel(panelEfectivo, "EFECTIVO".equals(nuevo));
            mostrarPanel(panelCredito,  "TARJETA_CREDITO".equals(nuevo));
            mostrarPanel(panelDebito,   "TARJETA_DEBITO".equals(nuevo));
            mostrarPanel(panelTransfer, "TRANSFERENCIA".equals(nuevo));
        });
        
        body.getChildren().addAll(reservaBox, metodoBox, panelEfectivo, panelCredito, panelDebito, panelTransfer);
        
        // Error y loading
        Label errLbl = new Label("");
        errLbl.setStyle("-fx-text-fill:#dc2626; -fx-font-size:12px;");
        errLbl.setWrapText(true);
        errLbl.setMaxWidth(460);
        body.getChildren().add(errLbl);
        
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(22, 22); 
        spinner.setVisible(false);
        body.getChildren().add(spinner);
        
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
        
        ButtonType btnCrear = new ButtonType("Generar Factura", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnCrear, ButtonType.CANCEL);
        
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(btnCrear);
        okBtn.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: white; " +
                       "-fx-background-radius: 8px; -fx-padding: 10px 24px; " +
                       "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        
        footerContent.getChildren().addAll(cancelarBtn);
        footer.getChildren().addAll(divider, footerContent);
        
        mainContainer.getChildren().addAll(header, scrollPane, footer);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().setPrefWidth(650);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        
        // Animación de entrada
        dialog.setOnShown(ev -> {
            FadeTransition ft = new FadeTransition(Duration.millis(250), dialog.getDialogPane());
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        });
        
        // LA LÓGICA DE NEGOCIO SE MANTIENE IGUAL (SIN CAMBIOS)
        okBtn.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            errLbl.setText("");
            int idReserva;
            try {
                idReserva = Integer.parseInt(fIdReserva.getText().trim());
                if (idReserva <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                errLbl.setText("El ID de reserva debe ser un número entero positivo."); return;
            }
            String metodoStr = fMetodo.getValue();
            Factura.MetodoPago metodo = Factura.MetodoPago.valueOf(metodoStr);
            double montoFinal = 0;
            if ("EFECTIVO".equals(metodoStr)) {
                String montoTxt = fMonto.getText().trim();
                if (montoTxt.isEmpty()) { errLbl.setText("Ingresa el monto recibido."); return; }
                try {
                    montoFinal = Double.parseDouble(montoTxt.replace(",", "."));
                    if (montoFinal <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    errLbl.setText("El monto recibido debe ser un número positivo."); return;
                }
            }
            if ("TRANSFERENCIA".equals(metodoStr) && fReferencia.getText().trim().isEmpty()) {
                errLbl.setText("Ingresa el número de referencia."); return;
            }
            String franquicia = null; int cuotas = 1;
            if ("TARJETA_CREDITO".equals(metodoStr)) { franquicia = fFranquiciaCredito.getValue(); cuotas = fCuotas.getValue(); }
            else if ("TARJETA_DEBITO".equals(metodoStr)) { franquicia = fFranquiciaDebito.getValue(); }
            String referencia = "TRANSFERENCIA".equals(metodoStr) ? fReferencia.getText().trim() : null;

            final double montoRecibidoFinal = montoFinal;
            final String franquiciaFinal = franquicia;
            final int cuotasFinal = cuotas;
            final String refFinal = referencia;
            okBtn.setDisable(true); spinner.setVisible(true);

            Thread t = new Thread(() -> {
                try {
                    Factura f = ctx.getFacturaService().generarFactura(
                            idReserva, metodo, montoRecibidoFinal, franquiciaFinal, cuotasFinal, refFinal);
                    Platform.runLater(() -> {
                        String msg = "Factura #" + f.getId() + " generada. Total: "
                                + String.format("$%,.0f", f.getTotal());
                        if (metodo == Factura.MetodoPago.EFECTIVO && f.getCambio() > 0)
                            msg += "\nCambio a devolver: " + String.format("$%,.0f", f.getCambio());
                        NotificationUtil.exito(msg);
                        cargarDatos(); dialog.close();
                    });
                } catch (ExcepcionNegocio ex) {
                    Platform.runLater(() -> {
                        errLbl.setText("Error: " + ex.getMessage());
                        okBtn.setDisable(false); spinner.setVisible(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        errLbl.setText("Error inesperado: " + (ex.getMessage() != null
                                ? ex.getMessage() : ex.getClass().getSimpleName()));
                        okBtn.setDisable(false); spinner.setVisible(false);
                    });
                }
            }, "hilo-generar-factura");
            t.setDaemon(true); t.start();
        });
        
        dialog.showAndWait();
    }

    // ─── MÉTODOS ORIGINALES MANTENIDOS ────────────────────────────────────────

    @FXML
    public void enviarFacturaPorEmail() {
        Factura sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { NotificationUtil.advertencia("Selecciona una factura para enviar."); return; }
        if (sel.getCliente() == null || sel.getCliente().getEmail() == null
                || sel.getCliente().getEmail().isBlank()) {
            NotificationUtil.error("El cliente no tiene email registrado."); return;
        }
        String emailDestino = sel.getCliente().getEmail();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Enviar factura por correo");
        confirm.setHeaderText("¿Enviar Factura #" + sel.getId() + " al cliente?");
        confirm.setContentText("Destinatario: " + emailDestino);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            progressBar.setVisible(true);
            Thread t = new Thread(() -> {
                try {
                    String pdfPath = ctx.getPdfReporteService().generarFacturaPdf(sel.getId());
                    boolean enviado = ctx.getEmailService().enviarFacturaPorEmail(sel, pdfPath);
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        if (enviado) NotificationUtil.exito("Factura #" + sel.getId() + " enviada a " + emailDestino);
                        else NotificationUtil.advertencia("No se pudo enviar. Revisa la configuración en email.properties.");
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        NotificationUtil.error("Error al generar/enviar: " + ex.getMessage());
                    });
                }
            }, "hilo-enviar-factura-email");
            t.setDaemon(true); t.start();
        });
    }

    @FXML
    public void verHTML() {
        Factura sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { NotificationUtil.advertencia("Selecciona una factura para generar su HTML."); return; }
        Thread t = new Thread(() -> {
            try {
                String ruta = ctx.getReporteService().guardarFacturaHTML(sel.getId());
                Platform.runLater(() -> NotificationUtil.exito("Factura HTML guardada en:\n" + ruta));
            } catch (Exception e) {
                Platform.runLater(() -> NotificationUtil.error("Error generando HTML: " + e.getMessage()));
            }
        }, "hilo-factura-html");
        t.setDaemon(true); t.start();
    }

    @FXML
    public void imprimirTermica() {
        Factura sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { NotificationUtil.advertencia("Selecciona una factura para imprimir."); return; }
        Empleado cajero = ctx.getEmpleadoActual();
        Stage owner = (Stage) tabla.getScene().getWindow();
        new FacturaTermicaView(sel, cajero).mostrarVentanaPrevia(owner);
    }

    // ─── CONSTRUCTORES DE PANELES DE PAGO ─────────────────────────────────────

    private VBox seccionPago(String titulo, javafx.scene.Node... hijos) {
        Label lbl = new Label(titulo);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");
        VBox box = new VBox(10);
        box.setPadding(new Insets(12, 14, 12, 14));
        box.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0;" +
                     "-fx-border-width: 1.5px; -fx-border-radius: 10px; -fx-background-radius: 10px;");
        box.getChildren().add(lbl);
        for (javafx.scene.Node n : hijos) box.getChildren().add(n);
        return box;
    }

    private ComboBox<String> franquiciaCombo(boolean conAmex) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll("VISA", "MASTERCARD");
        if (conAmex) cb.getItems().addAll("AMEX", "DINNERS");
        cb.setValue("VISA");
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0;" +
                    "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-padding: 8px 12px;");
        return cb;
    }

    private static void mostrarPanel(VBox panel, boolean visible) {
        panel.setVisible(visible); 
        panel.setManaged(visible);
    }

    // ─── HELPERS ───────────────────────────────────────────────────────────────

    private String etiquetaEstado(String estado) {
        switch (estado) {
            case "PAGADA":    return "✓  Pagada";
            case "PENDIENTE": return "⚠  Pendiente";
            case "ANULADA":   return "⊘  Cancelada";
            default:          return estado;
        }
    }

    private String badgeCssClass(String estado) {
        switch (estado) {
            case "PAGADA":    return "estado-pagada";
            case "PENDIENTE": return "estado-pendiente";
            case "ANULADA":   return "estado-cancelada";
            default:          return "badge-no-show";
        }
    }

    private TextField tf(String placeholder) {
        TextField f = new TextField();
        f.setPromptText(placeholder);
        f.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0;" +
                   "-fx-border-width: 1.5px; -fx-border-radius: 8px;" +
                   "-fx-background-radius: 8px; -fx-padding: 8px 12px;");
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }

    private Label lab(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        l.setMinWidth(110);
        return l;
    }
}
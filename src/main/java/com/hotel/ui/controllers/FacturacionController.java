/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.controllers;

import com.hotel.AppContext;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.Empleado;
import com.hotel.model.Factura;
import com.hotel.ui.components.FacturaTermicaView;
import com.hotel.ui.components.NotificationUtil;
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
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class FacturacionController {

    @FXML private TableView<Factura>          tabla;
    @FXML private TableColumn<Factura,String> colId, colCliente, colReserva;
    @FXML private TableColumn<Factura,String> colFecha, colSubtotal, colImpuestos;
    @FXML private TableColumn<Factura,String> colTotal, colEstado, colMetodo;
    @FXML private TextField                   searchField;
    @FXML private Label                       lblTotal, lblPagadas;
    @FXML private ProgressBar                 progressBar;

    private final AppContext ctx = AppContext.getInstance();
    private final ObservableList<Factura> datos     = FXCollections.observableArrayList();
    private FilteredList<Factura>         filtradas;

    @FXML
    public void initialize() {
        configurarColumnas();
        filtradas = new FilteredList<>(datos, p -> true);
        tabla.setItems(filtradas);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cargarDatos();
    }
     private void configurarColumnas() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(
                "#" + c.getValue().getId()));
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCliente() != null
                        ? c.getValue().getCliente().obtenerNombreCompleto() : "—"));
        colReserva.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getReserva() != null
                        ? "#" + c.getValue().getReserva().getId() : "—"));
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaEmision() != null
                        ? c.getValue().getFechaEmision().toString() : "—"));
        colSubtotal.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("$%,.0f", c.getValue().getSubtotal())));
        colImpuestos.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("$%,.0f", c.getValue().getImpuestos())));
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("$%,.0f", c.getValue().getTotal())));
        colMetodo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getMetodoPago() != null
                        ? c.getValue().getMetodoPago().name().replace("_", " ") : "—"));

        colEstado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEstadoPago() != null
                        ? c.getValue().getEstadoPago().name() : "—"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                badge.setStyle(badgeEstilo(item));
                setGraphic(badge); setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        tabla.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Factura f, boolean empty) {
                super.updateItem(f, empty);
                if (f == null || empty || f.getEstadoPago() == null) { setStyle(""); return; }
                switch (f.getEstadoPago()) {
                    case PAGADA:    setStyle("-fx-background-color:#f0fdf4;"); break;
                    case PENDIENTE: setStyle("-fx-background-color:#fefce8;"); break;
                    case ANULADA:   setStyle("-fx-background-color:#fff1f2;"); break;
                    default:        setStyle(""); break;
                }
            }
        });
    }
       @FXML
    public void cargarDatos() {
        progressBar.setVisible(true);
        Thread t = new Thread(() -> {
            try {
                List<Factura> lista = ctx.getFacturaService().listarTodasLasFacturas();
                Platform.runLater(() -> {
                    datos.setAll(lista);
                    long pagadas = lista.stream()
                            .filter(f -> f.getEstadoPago() == Factura.EstadoPago.PAGADA)
                            .count();
                    lblTotal.setText("Total: " + lista.size());
                    lblPagadas.setText("Pagadas: " + pagadas);
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
      // ── Diálogo de generación de factura ─────────────────────────────────────

    @FXML
    public void generarFactura() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Generar Factura");
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setStyle("-fx-background-color:white; -fx-background-radius:12px;");

        // ── Encabezado ────────────────────────────────────────────────────────
        Label header = new Label("💰 Generar Factura");
        header.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;");

        // ── Campos base ───────────────────────────────────────────────────────
        TextField fIdReserva = tf("ID de la reserva COMPLETADA");

        ComboBox<String> fMetodo = new ComboBox<>();
        fMetodo.getItems().addAll("EFECTIVO", "TARJETA_CREDITO", "TARJETA_DEBITO", "TRANSFERENCIA");
        fMetodo.setValue("EFECTIVO");
        fMetodo.setMaxWidth(Double.MAX_VALUE);
        fMetodo.setStyle("-fx-background-color:#f8fafc; -fx-border-color:#e2e8f0;" +
                         "-fx-border-width:1.5px; -fx-border-radius:8px;");

        GridPane baseGrid = new GridPane();
        baseGrid.setHgap(14); baseGrid.setVgap(12);
        baseGrid.addRow(0, lab("ID Reserva:"),     fIdReserva);
        baseGrid.addRow(1, lab("Método de pago:"), fMetodo);
        GridPane.setHgrow(fIdReserva, Priority.ALWAYS);
        GridPane.setHgrow(fMetodo,    Priority.ALWAYS);
           // ── Panel EFECTIVO ────────────────────────────────────────────────────
        TextField fMonto   = tf("Monto recibido del cliente ($)");
        Label     lblCambio = new Label("Cambio estimado: —");
        lblCambio.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#15803d;" +
                           "-fx-padding:6px 10px; -fx-background-color:#f0fdf4;" +
                           "-fx-background-radius:8px; -fx-border-color:#86efac;" +
                           "-fx-border-width:1px; -fx-border-radius:8px;");
        fMonto.textProperty().addListener((obs, oldVal, val) -> {
            if (val == null || val.isBlank()) { lblCambio.setText("Cambio estimado: —"); return; }
            try {
                double monto = Double.parseDouble(val.trim().replace(",", "."));
                lblCambio.setText(monto > 0
                        ? String.format("Monto ingresado: $%,.0f", monto)
                        : "Cambio estimado: —");
            } catch (NumberFormatException e) {
                lblCambio.setText("Cambio estimado: —");
            }
        });

        GridPane efectivoGrid = new GridPane();
        efectivoGrid.setHgap(14); efectivoGrid.setVgap(10);
        efectivoGrid.addRow(0, lab("Monto recibido:"), fMonto);
        GridPane.setHgrow(fMonto, Priority.ALWAYS);

        VBox panelEfectivo = seccionPago("💵  Pago en Efectivo", efectivoGrid, lblCambio);

        // ── Panel TARJETA CRÉDITO ─────────────────────────────────────────────
        ComboBox<String> fFranquiciaCredito = franquiciaCombo(true);
        Spinner<Integer> fCuotas = new Spinner<>(1, 36, 1);
        fCuotas.setEditable(true);
        fCuotas.setMaxWidth(Double.MAX_VALUE);
        fCuotas.setStyle("-fx-background-color:#f8fafc;");

        Label lblCuotaInfo = new Label("1 cuota = pago de contado");
        lblCuotaInfo.setStyle("-fx-font-size:11px; -fx-text-fill:#6b7280;");
        fCuotas.valueProperty().addListener((obs, o, n) ->
                lblCuotaInfo.setText(n == 1 ? "1 cuota = pago de contado"
                        : n + " cuotas mensuales"));

        GridPane creditoGrid = new GridPane();
        creditoGrid.setHgap(14); creditoGrid.setVgap(10);
        creditoGrid.addRow(0, lab("Franquicia:"), fFranquiciaCredito);
        creditoGrid.addRow(1, lab("Cuotas:"),     fCuotas);
        GridPane.setHgrow(fFranquiciaCredito, Priority.ALWAYS);
        GridPane.setHgrow(fCuotas, Priority.ALWAYS);

        VBox panelCredito = seccionPago("💳  Tarjeta de Crédito", creditoGrid, lblCuotaInfo);

        // ── Panel TARJETA DÉBITO ──────────────────────────────────────────────
        ComboBox<String> fFranquiciaDebito = franquiciaCombo(false);

        GridPane debitoGrid = new GridPane();
        debitoGrid.setHgap(14); debitoGrid.setVgap(10);
        debitoGrid.addRow(0, lab("Franquicia:"), fFranquiciaDebito);
        GridPane.setHgrow(fFranquiciaDebito, Priority.ALWAYS);

        VBox panelDebito = seccionPago("💳  Tarjeta de Débito", debitoGrid);

        // ── Panel TRANSFERENCIA ───────────────────────────────────────────────
        TextField fReferencia = tf("Número de referencia bancaria");

        GridPane transGrid = new GridPane();
        transGrid.setHgap(14); transGrid.setVgap(10);
        transGrid.addRow(0, lab("Referencia:"), fReferencia);
        GridPane.setHgrow(fReferencia, Priority.ALWAYS);

        Label lblTransInfo = new Label("Ingresa el número de confirmación de la transferencia");
        lblTransInfo.setStyle("-fx-font-size:11px; -fx-text-fill:#6b7280;");
        lblTransInfo.setWrapText(true);

        VBox panelTransfer = seccionPago("🏦  Transferencia Bancaria", transGrid, lblTransInfo);

        // ── Control de visibilidad de paneles ─────────────────────────────────
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
        
 // ── Ensamblado ────────────────────────────────────────────────────────
        Label errLbl = new Label("");
        errLbl.setStyle("-fx-text-fill:#dc2626; -fx-font-size:12px;");
        errLbl.setWrapText(true);
        errLbl.setMaxWidth(460);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(22, 22);
        spinner.setVisible(false);

        VBox content = new VBox(14,
                header, new Separator(),
                baseGrid,
                panelEfectivo, panelCredito, panelDebito, panelTransfer,
                errLbl, spinner);
        content.setPadding(new Insets(12, 20, 8, 20));

        ButtonType btnCrear = new ButtonType("Generar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(btnCrear, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(btnCrear);

        okBtn.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            errLbl.setText("");

            // ── Validar ID de reserva ──────────────────────────────────────
            int idReserva;
            try {
                idReserva = Integer.parseInt(fIdReserva.getText().trim());
                if (idReserva <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                errLbl.setText("El ID de reserva debe ser un número entero positivo.");
                return;
            }

            // ── Recopilar detalles según método ───────────────────────────
            String metodoStr = fMetodo.getValue();
            Factura.MetodoPago metodo = Factura.MetodoPago.valueOf(metodoStr);

            double montoRecibido = 0;
            if ("EFECTIVO".equals(metodoStr)) {
                String montoTxt = fMonto.getText().trim();
                if (montoTxt.isEmpty()) {
                    errLbl.setText("Ingresa el monto recibido del cliente.");
                    return;
                }
                try {
                    montoRecibido = Double.parseDouble(montoTxt.replace(",", "."));
                    if (montoRecibido <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    errLbl.setText("El monto recibido debe ser un número positivo.");
                    return;
                }
            }

            if ("TRANSFERENCIA".equals(metodoStr) && fReferencia.getText().trim().isEmpty()) {
                errLbl.setText("Ingresa el número de referencia de la transferencia.");
                return;
            }

            String franquicia = null;
            int    cuotas     = 1;
            if ("TARJETA_CREDITO".equals(metodoStr)) {
                franquicia = fFranquiciaCredito.getValue();
                cuotas     = fCuotas.getValue();
            } else if ("TARJETA_DEBITO".equals(metodoStr)) {
                franquicia = fFranquiciaDebito.getValue();
            }

            String referencia = "TRANSFERENCIA".equals(metodoStr)
                    ? fReferencia.getText().trim() : null;

            // Capturar variables finales para el lambda
            final double  montoFinal      = montoRecibido;
            final String  franquiciaFinal = franquicia;
            final int     cuotasFinal     = cuotas;
            final String  refFinal        = referencia;

            okBtn.setDisable(true);
            spinner.setVisible(true);

            Thread t = new Thread(() -> {
                try {
                    Factura f = ctx.getFacturaService().generarFactura(
                            idReserva, metodo, montoFinal, franquiciaFinal, cuotasFinal, refFinal);

                    Platform.runLater(() -> {
                        String msg = "Factura #" + f.getId() + " generada.  "
                                + "Total: " + String.format("$%,.0f", f.getTotal());
                        if (metodo == Factura.MetodoPago.EFECTIVO && f.getCambio() > 0) {
                            msg += "\nCambio a devolver: "
                                    + String.format("$%,.0f", f.getCambio());
                        }
                        NotificationUtil.exito(msg);
                        cargarDatos();
                        dialog.close();
                    });
                } catch (ExcepcionNegocio ex) {
                    Platform.runLater(() -> {
                        errLbl.setText("Error: " + ex.getMessage());
                        okBtn.setDisable(false);
                        spinner.setVisible(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        String msg = ex.getMessage() != null
                                ? ex.getMessage() : ex.getClass().getSimpleName();
                        errLbl.setText("Error inesperado: " + msg);
                        okBtn.setDisable(false);
                        spinner.setVisible(false);
                    });
                }
            }, "hilo-generar-factura");
            t.setDaemon(true);
            t.start();
        });

        dialog.showAndWait();
    }
      // ── Acciones sobre tabla ──────────────────────────────────────────────────

    @FXML
    public void enviarFacturaPorEmail() {
        Factura sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { NotificationUtil.advertencia("Selecciona una factura para enviar."); return; }
        if (sel.getCliente() == null || sel.getCliente().getEmail() == null
                || sel.getCliente().getEmail().isBlank()) {
            NotificationUtil.error("El cliente no tiene email registrado.");
            return;
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
                        if (enviado) {
                            NotificationUtil.exito("Factura #" + sel.getId()
                                    + " enviada con PDF a " + emailDestino);
                        } else {
                            NotificationUtil.advertencia(
                                    "No se pudo enviar. Revisa la configuración en\n"
                                    + "src/com/hotel/email.properties");
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        NotificationUtil.error("Error al generar/enviar: " + ex.getMessage());
                    });
                }
            }, "hilo-enviar-factura-email");
            t.setDaemon(true);
            t.start();
        });
    }

    @FXML
    public void verHTML() {
        Factura sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) {
            NotificationUtil.advertencia("Selecciona una factura para generar su HTML.");
            return;
        }
        Thread t = new Thread(() -> {
            try {
                String ruta = ctx.getReporteService().guardarFacturaHTML(sel.getId());
                Platform.runLater(() ->
                    NotificationUtil.exito("Factura HTML guardada en:\n" + ruta));
            } catch (Exception e) {
                Platform.runLater(() ->
                    NotificationUtil.error("Error generando HTML: " + e.getMessage()));
            }
        }, "hilo-factura-html");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void imprimirTermica() {
        Factura sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) {
            NotificationUtil.advertencia("Selecciona una factura para imprimir.");
            return;
        }
        Empleado cajero = ctx.getEmpleadoActual();
        Stage owner = (Stage) tabla.getScene().getWindow();
        new FacturaTermicaView(sel, cajero).mostrarVentanaPrevia(owner);
    }
    // ── Constructores de paneles de pago ──────────────────────────────────────

    private VBox seccionPago(String titulo, javafx.scene.Node... hijos) {
        Label lbl = new Label(titulo);
        lbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#1e3a5f;");

        VBox box = new VBox(10);
        box.setPadding(new Insets(12, 14, 12, 14));
        box.setStyle("-fx-background-color:#f8fafc; -fx-border-color:#cbd5e1;" +
                     "-fx-border-width:1.5px; -fx-border-radius:10px;" +
                     "-fx-background-radius:10px;");
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
        cb.setStyle("-fx-background-color:#f8fafc; -fx-border-color:#e2e8f0;" +
                    "-fx-border-width:1.5px; -fx-border-radius:8px;");
        return cb;
    }

    private static void mostrarPanel(VBox panel, boolean visible) {
        panel.setVisible(visible);
        panel.setManaged(visible);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String badgeEstilo(String estado) {
        switch (estado) {
            case "PAGADA":    return "-fx-background-color:#dcfce7; -fx-text-fill:#15803d;" +
                    "-fx-background-radius:20px; -fx-padding:3px 10px; -fx-font-size:10px; -fx-font-weight:bold;";
            case "PENDIENTE": return "-fx-background-color:#fef3c7; -fx-text-fill:#b45309;" +
                    "-fx-background-radius:20px; -fx-padding:3px 10px; -fx-font-size:10px; -fx-font-weight:bold;";
            case "ANULADA":   return "-fx-background-color:#fee2e2; -fx-text-fill:#b91c1c;" +
                    "-fx-background-radius:20px; -fx-padding:3px 10px; -fx-font-size:10px; -fx-font-weight:bold;";
            default:          return "-fx-background-color:#f1f5f9; -fx-text-fill:#475569;" +
                    "-fx-background-radius:20px; -fx-padding:3px 10px; -fx-font-size:10px;";
        }
    }

    private TextField tf(String placeholder) {
        TextField f = new TextField();
        f.setPromptText(placeholder);
        f.setStyle("-fx-background-color:#f8fafc; -fx-border-color:#e2e8f0;" +
                "-fx-border-width:1.5px; -fx-border-radius:8px;" +
                "-fx-background-radius:8px; -fx-padding:8px 12px;");
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }

    private Label lab(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#374151;");
        l.setMinWidth(110);
        return l;
    }
}
    

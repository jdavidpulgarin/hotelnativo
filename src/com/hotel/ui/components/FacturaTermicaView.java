/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.components;

import com.hotel.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Genera y muestra la factura térmica de Hotel Nativo.
 * Optimizada para impresoras POS de 58mm y 80mm.
 *
 * GRASP: Fabricación Pura — responsabilidad única de construir el documento visual.
 */
public class FacturaTermicaView {

    // ── Constantes de diseño ──────────────────────────────────────────────────
    private static final double ANCHO   = 312;      // ≈ 80 mm en pantalla
    private static final int    COLS    = 42;       // caracteres por línea
    private static final String MONO    = "Courier New";
    private static final double FS_XL   = 17;
    private static final double FS_LG   = 13;
    private static final double FS_MD   = 10.5;
    private static final double FS_SM   =  9;

    private static final Locale         LOCALE_ES  = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FMT_DIA  =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FMT_LARGO =
            DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM yyyy", Locale.forLanguageTag("es-CO"));

    private static final String[] HOTEL_INFO = {
        "Cra. 10 No. 12-34, Centro",
        "Valledupar, Cesar - Colombia",
        "Tel: (605) 574-1234  Cel: 300 123-4567",
        "NIT: 901.234.567-8",
        "info@hotelnativo.com.co",
        "www.hotelnativo.com.co"
    };
    private static final String RES_DIAN  = "Res. DIAN No.18764003203 del 01/01/2026";
    private static final String RANGO     = "Rango: del 000001 al 999999";

    // ── Datos ─────────────────────────────────────────────────────────────────
    private final Factura  factura;
    private final Empleado cajero;

    public FacturaTermicaView(Factura factura, Empleado cajero) {
        this.factura = factura;
        this.cajero  = cajero;
    }
    
    
    
      /**
     * Abre la ventana de vista previa con controles de impresión y PDF.
     * @param owner ventana padre para la modality
     */
    public void mostrarVentanaPrevia(Stage owner) {
        VBox ticket = construirTicket();

        // Fondo gris para simular papel sobre superficie
        VBox wrapper = new VBox(ticket);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setPadding(new Insets(24, 20, 24, 20));
        wrapper.setStyle("-fx-background-color:#6b7280;");

        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:#6b7280; -fx-background-color:#6b7280;");

        // ── Toolbar ───────────────────────────────────────────────────────────
        Button btnPrint  = botonAccion("🖨  Imprimir",    "#1d4ed8", "#eff6ff");
        Button btnPdf    = botonAccion("📄  Guardar PDF", "#15803d", "#f0fdf4");
        Button btnCerrar = botonAccion("✕  Cerrar",       "#b91c1c", "#fff1f2");

        btnPrint.setOnAction(e  -> imprimir(ticket, (Stage) btnPrint.getScene().getWindow()));
        btnPdf.setOnAction(e    -> generarPdfExistente());
        btnCerrar.setOnAction(e -> ((Stage) btnCerrar.getScene().getWindow()).close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(10, btnPrint, btnPdf, spacer, btnCerrar);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(12, 18, 12, 18));
        toolbar.setStyle("-fx-background-color:white;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.14),8,0,0,-2);");

        // Título de la ventana
        Label titulo = new Label("Vista previa — Factura Térmica  " + numeroFactura());
        titulo.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#1e293b;");
        HBox topBar = new HBox(titulo);
        topBar.setPadding(new Insets(10, 18, 10, 18));
        topBar.setStyle("-fx-background-color:#f8fafc;" +
                "-fx-border-color:#e2e8f0; -fx-border-width:0 0 1px 0;");

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(scroll);
        root.setBottom(toolbar);

        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setTitle("Factura Térmica — Hotel Nativo");
        stage.setScene(new Scene(root, 470, 730));
        stage.setMinWidth(430);
        stage.show();
    }
    
      /** Construye el VBox completo del ticket listo para mostrar o imprimir. */
    public VBox construirTicket() {
        VBox t = new VBox(0);
        t.setPrefWidth(ANCHO);
        t.setMaxWidth(ANCHO);
        t.setMinWidth(ANCHO);
        t.setAlignment(Pos.TOP_LEFT);
        t.setStyle("-fx-background-color:white; -fx-padding:12px 10px;");

        // ── Secciones ─────────────────────────────────────────────────────────
        agregar(t, construirEncabezado());
        agregar(t, sep('═'));
        agregar(t, construirSeccionTipoDoc());
        agregar(t, sep('═'));
        agregar(t, construirSeccionInfoFactura());
        agregar(t, sep('─'));
        agregar(t, construirSeccionHuesped());
        agregar(t, sep('─'));
        agregar(t, construirTablaItems());
        agregar(t, construirSeccionTotales());
        agregar(t, sep('═'));
        agregar(t, construirSeccionLegal());
        agregar(t, sep('─'));
        agregar(t, construirSeccionCodigoQR());
        agregar(t, sep('─'));
        agregar(t, construirPieDePagina());
        agregar(t, sep('═'));
        agregar(t, espacio(6));

        return t;
    }
    
    
    private VBox construirEncabezado() {
        VBox v = new VBox(2);
        v.setAlignment(Pos.TOP_CENTER);
        v.setPadding(new Insets(4, 0, 8, 0));

        // Logo ASCII
        Label logo = labelMono(
                "  ╔════════════════════╗\n" +
                "  ║   H O T E L        ║\n" +
                "  ║   N A T I V O      ║\n" +
                "  ╚════════════════════╝",
                FS_MD, true);
        logo.setTextAlignment(TextAlignment.CENTER);
        logo.setAlignment(Pos.CENTER);
        logo.setMaxWidth(ANCHO);
        v.getChildren().add(logo);
        v.getChildren().add(espacio(5));

        for (String linea : HOTEL_INFO) {
            v.getChildren().add(labelCentrado(linea, FS_SM, false));
        }
        v.getChildren().add(espacio(4));
        Label estrellas = labelCentrado("★  ★  ★  ★  ★   Hotel 5 Estrellas", FS_SM, false);
        estrellas.setStyle(estrellas.getStyle() + " -fx-text-fill:#444444;");
        v.getChildren().add(estrellas);
        v.getChildren().add(espacio(4));
        return v;
    }

    private VBox construirSeccionTipoDoc() {
        VBox v = new VBox(3);
        v.setAlignment(Pos.TOP_CENTER);
        v.setPadding(new Insets(6, 0, 6, 0));
        v.getChildren().add(labelCentrado("FACTURA ELECTRÓNICA DE VENTA", FS_LG, true));
        v.getChildren().add(labelCentrado("Documento equivalente Régimen Simplificado", FS_SM, false));
        return v;
    }

    private VBox construirSeccionInfoFactura() {
        VBox v = new VBox(2);
        v.setPadding(new Insets(5, 0, 5, 0));

        String hora    = LocalTime.now().format(FMT_HORA);
        String fecha   = factura.getFechaEmision() != null
                ? factura.getFechaEmision().format(FMT_DIA) : LocalDate.now().format(FMT_DIA);
        String metodo  = factura.getMetodoPago() != null
                ? factura.getMetodoPago().name().replace("_", " ") : "EFECTIVO";
        String cajeroN = cajero != null ? cajero.obtenerNombreCompleto() : "Recepción";

        v.getChildren().addAll(
            filaKV("No. Factura:", numeroFactura()),
            filaKV("Fecha:",       fecha + "   " + hora),
            filaKV("Cajero:",      cajeroN),
            filaKV("Método pago:", metodo),
            espacio(4),
            construirBadgeEstado()
        );
        return v;
    }
    
    
       private VBox construirSeccionHuesped() {
        VBox v = new VBox(2);
        v.setPadding(new Insets(5, 0, 5, 0));

        Label titulo = labelMono("DATOS DEL HUÉSPED", FS_SM, true);
        titulo.setStyle(titulo.getStyle() + " -fx-underline:true;");
        v.getChildren().add(titulo);
        v.getChildren().add(espacio(2));

        Cliente c = factura.getCliente();
        Reserva r = factura.getReserva();

        if (c != null) {
            String nombre = c.obtenerNombreCompleto();
            if (c.isEsVip()) nombre += "   ★ VIP";
            v.getChildren().add(filaKV("Nombre:", nombre));
            if (c.getDocumento() != null)
                v.getChildren().add(filaKV("Documento:", c.getDocumento()));
            if (c.getNacionalidad() != null && !c.getNacionalidad().isBlank())
                v.getChildren().add(filaKV("Nac.:", c.getNacionalidad()));
        }
        if (r != null) {
            v.getChildren().add(filaKV("Reserva:", "#" + r.getId()));
            if (r.getHabitacion() != null) {
                String hab = r.getHabitacion().getNumero();
                if (r.getHabitacion().getTipoHabitacion() != null)
                    hab += " - " + r.getHabitacion().getTipoHabitacion().obtenerEtiquetaTipo();
                v.getChildren().add(filaKV("Habitación:", hab));
            }
            if (r.getFechaEntrada() != null)
                v.getChildren().add(filaKV("Check-in:", r.getFechaEntrada().format(FMT_DIA)));
            if (r.getFechaSalida() != null)
                v.getChildren().add(filaKV("Check-out:", r.getFechaSalida().format(FMT_DIA)));
            long noches = calcularNoches(r);
            if (noches > 0)
                v.getChildren().add(filaKV("Noches:", noches + " noche(s)"));
            v.getChildren().add(filaKV("Personas:", r.getNumPersonas() + " huésped(es)"));
        }
        return v;
    }

    private VBox construirTablaItems() {
        VBox v = new VBox(0);
        v.setPadding(new Insets(5, 0, 0, 0));

        // Encabezado de columnas
        v.getChildren().add(filaTabla("CONCEPTO", "CANT", "VALOR", true));
        v.getChildren().add(sep('─'));

        Reserva r     = factura.getReserva();
        long noches   = r != null ? calcularNoches(r) : 1;
        if (noches <= 0) noches = 1;

        // Precio base por noche (antes de descuento VIP)
        double precioBaseTotal = (r != null && r.getPrecioTotal() > 0)
                ? r.getPrecioTotal() : factura.getSubtotal();
        double precioPorNoche  = precioBaseTotal / noches;

        // ── Hospedaje ────────────────────────────────────────────────────────
        String tipoHab = "Hospedaje";
        if (r != null && r.getHabitacion() != null && r.getHabitacion().getTipoHabitacion() != null)
            tipoHab = acortar(r.getHabitacion().getTipoHabitacion().obtenerEtiquetaTipo(), 18);

        v.getChildren().add(filaTabla(tipoHab, String.valueOf(noches),
                fmt(precioBaseTotal), false));

        // Sub-descripción de noches
        Label subDesc = labelMono(
                "  " + noches + " noche(s) x " + fmt(precioPorNoche), FS_SM, false);
        subDesc.setStyle(subDesc.getStyle() + " -fx-text-fill:#555555;");
        v.getChildren().add(subDesc);

        // ── Descuento VIP ────────────────────────────────────────────────────
        Cliente c = factura.getCliente();
        boolean esVip = c != null && c.isEsVip();
        double descuentoVip = 0;
        if (esVip && r != null && r.getPrecioTotal() > 0) {
            descuentoVip = r.getPrecioTotal() - factura.getSubtotal();
            if (descuentoVip > 0.5) {
                v.getChildren().add(espacio(2));
                v.getChildren().add(filaTabla("Desc. VIP (-15%)", "1",
                        "-" + fmt(descuentoVip), false));
            }
        }

        v.getChildren().add(espacio(2));
        return v;
    }
    
    private VBox construirSeccionTotales() {
        VBox v = new VBox(2);
        v.setPadding(new Insets(0, 0, 5, 0));

        v.getChildren().add(sep('─'));

        int pctIva = (int) Math.round(factura.getTasaIva() * 100);

        v.getChildren().add(filaTotales("Subtotal:", fmt(factura.getSubtotal()), false));
        v.getChildren().add(filaTotales("IVA (" + pctIva + "%):", fmt(factura.getImpuestos()), false));

        Cliente c = factura.getCliente();
        if (c != null && c.isEsVip()) {
            v.getChildren().add(filaTotales("Tarifa VIP:", "Aplicada  ✓", false));
        }

        v.getChildren().add(espacio(5));
        v.getChildren().add(sep('─'));
        v.getChildren().add(espacio(3));

        // ── TOTAL A PAGAR (grande, centrado) ─────────────────────────────────
        Label lblTitTotal = new Label("TOTAL A PAGAR");
        lblTitTotal.setFont(Font.font(MONO, FontWeight.BOLD, FS_LG));
        lblTitTotal.setStyle("-fx-text-fill:#000000;");

        Label lblValTotal = new Label(fmt(factura.getTotal()));
        lblValTotal.setFont(Font.font(MONO, FontWeight.BOLD, FS_XL));
        lblValTotal.setStyle("-fx-text-fill:#000000;");

        VBox boxTotal = new VBox(3, lblTitTotal, lblValTotal);
        boxTotal.setAlignment(Pos.CENTER);
        boxTotal.setMaxWidth(ANCHO);
        boxTotal.setPadding(new Insets(6, 0, 6, 0));
        v.getChildren().add(boxTotal);

        v.getChildren().add(espacio(3));
        v.getChildren().add(sep('─'));
        v.getChildren().add(espacio(4));

        // Detalles del método de pago
        Factura.MetodoPago mp = factura.getMetodoPago();
        String metodo = mp != null ? mp.name().replace("_", " ") : "EFECTIVO";
        v.getChildren().add(filaTotales("Pagado con:", metodo, false));

        if (mp == Factura.MetodoPago.EFECTIVO) {
            if (factura.getMontoRecibido() > 0) {
                v.getChildren().add(filaTotales("Recibido:",
                        fmt(factura.getMontoRecibido()), false));
            }
            v.getChildren().add(filaTotales("Cambio:",
                    fmt(factura.getCambio()), false));

        } else if (mp == Factura.MetodoPago.TARJETA_CREDITO) {
            if (factura.getFranquiciaTarjeta() != null)
                v.getChildren().add(filaTotales("Franquicia:",
                        factura.getFranquiciaTarjeta(), false));
            v.getChildren().add(filaTotales("Cuotas:",
                    factura.getNumCuotas() + (factura.getNumCuotas() == 1
                            ? " cuota (contado)" : " cuotas"), false));

        } else if (mp == Factura.MetodoPago.TARJETA_DEBITO) {
            if (factura.getFranquiciaTarjeta() != null)
                v.getChildren().add(filaTotales("Franquicia:",
                        factura.getFranquiciaTarjeta(), false));

        } else if (mp == Factura.MetodoPago.TRANSFERENCIA) {
            String ref = factura.getReferenciaTransferencia();
            if (ref != null && !ref.isBlank())
                v.getChildren().add(filaTotales("Referencia:", ref, false));
        }

        v.getChildren().add(espacio(3));

        return v;
    }

    private VBox construirSeccionLegal() {
        VBox v = new VBox(2);
        v.setAlignment(Pos.TOP_CENTER);
        v.setPadding(new Insets(5, 0, 5, 0));

        v.getChildren().addAll(
            labelCentrado("DOCUMENTO SOPORTE FISCAL", FS_SM, true),
            espacio(3),
            labelCentrado(RES_DIAN, FS_SM, false),
            labelCentrado(RANGO, FS_SM, false),
            labelCentrado("Fecha resolución: 01/01/2026", FS_SM, false),
            espacio(4),
            labelCentrado("Conserve esta factura como soporte.", FS_SM, false),
            labelCentrado("Para quejas y reclamos:", FS_SM, false),
            labelCentrado("info@hotelnativo.com.co", FS_SM, false)
        );
        return v;
    }
    
     private VBox construirSeccionCodigoQR() {
        VBox v = new VBox(5);
        v.setAlignment(Pos.TOP_CENTER);
        v.setPadding(new Insets(8, 0, 8, 0));

        String payload = numeroFactura() + "|" + factura.getTotal() + "|" +
                (factura.getCliente() != null ? factura.getCliente().getDocumento() : "0");

        // QR visual
        Canvas qr = dibujarQR(payload, 88);
        v.getChildren().add(qr);
        v.getChildren().add(labelCentrado("Escanea para verificar", FS_SM, false));
        v.getChildren().add(espacio(8));

        // Código de barras
        Canvas barra = dibujarCodigoBarras(payload, 260, 38);
        HBox boxBarra = new HBox(barra);
        boxBarra.setAlignment(Pos.CENTER);
        v.getChildren().add(boxBarra);
        v.getChildren().add(labelCentrado(numeroFactura(), FS_SM, true));

        return v;
    }

    private VBox construirPieDePagina() {
        VBox v = new VBox(2);
        v.setAlignment(Pos.TOP_CENTER);
        v.setPadding(new Insets(8, 0, 6, 0));

        v.getChildren().addAll(
            espacio(4),
            labelCentrado("¡Gracias por su visita!", FS_LG, true),
            espacio(3),
            labelCentrado("Esperamos verle pronto en Hotel Nativo", FS_SM, false),
            espacio(5),
            labelCentrado("Síguenos:  @HotelNativoCol", FS_SM, false),
            labelCentrado("Calificanos en:  ★ ★ ★ ★ ★", FS_SM, false),
            espacio(5),
            labelCentrado("Sistema Hotel Nativo  v1.4", FS_SM, false),
            labelCentrado(LocalDate.now().format(FMT_LARGO), FS_SM, false)
        );
        return v;
    }
        /**
     * Genera el PDF de la factura y lo envía a la impresora del sistema
     * usando java.awt.Desktop (disponible sin dependencias adicionales).
     */
    public void imprimir(VBox ticket, Stage owner) {
        new Thread(() -> {
            try {
                String ruta = com.hotel.AppContext.getInstance()
                        .getPdfReporteService().generarFacturaPdf(factura.getId());
                java.io.File archivo = new java.io.File(ruta);
                if (!archivo.exists())
                    throw new IllegalStateException("No se generó el PDF en: " + ruta);

                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.PRINT)) {
                    desktop.print(archivo);
                    javafx.application.Platform.runLater(() ->
                            NotificationUtil.exito("Documento enviado a la impresora."));
                } else {
                    // Fallback: abre el PDF para que el usuario imprima manualmente
                    desktop.open(archivo);
                    javafx.application.Platform.runLater(() ->
                            NotificationUtil.advertencia(
                                    "Impresión directa no disponible.\n"
                                    + "Se abrió el PDF para imprimir manualmente."));
                }
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() ->
                        NotificationUtil.error("Error al imprimir: " + ex.getMessage()));
            }
        }).start();
    }

    private Canvas dibujarQR(String datos, int tamano) {
        final int N   = 21;       // QR Version 1: 21×21 módulos
        double    cel = (double) tamano / N;

        Canvas canvas = new Canvas(tamano, tamano);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, tamano, tamano);

        dibujarPatronDeteccion(gc, 0, 0, cel);
        dibujarPatronDeteccion(gc, N - 7, 0, cel);
        dibujarPatronDeteccion(gc, 0, N - 7, cel);

        for (int i = 8; i < N - 8; i++) {
            gc.setFill(i % 2 == 0 ? Color.BLACK : Color.WHITE);
            gc.fillRect(i * cel, 6 * cel, cel, cel);
            gc.fillRect(6 * cel, i * cel, cel, cel);
        }

        gc.setFill(Color.BLACK);
        gc.fillRect(8 * cel, 13 * cel, cel, cel);

        long seed = Math.abs((long) datos.hashCode()) ^ 0xCAFEBABEL;
        java.util.Random rnd = new java.util.Random(seed);
        for (int row = 0; row < N; row++) {
            for (int col = 0; col < N; col++) {
                if (esAreaReservada(row, col, N)) continue;
                gc.setFill(rnd.nextBoolean() ? Color.BLACK : Color.WHITE);
                gc.fillRect(col * cel, row * cel, cel, cel);
            }
        }

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        gc.strokeRect(0.75, 0.75, tamano - 1.5, tamano - 1.5);

        return canvas;
    }

    private void dibujarPatronDeteccion(GraphicsContext gc, int col, int row, double cel) {
        gc.setFill(Color.BLACK);
        gc.fillRect(col * cel, row * cel, 7 * cel, 7 * cel);
        gc.setFill(Color.WHITE);
        gc.fillRect((col + 1) * cel, (row + 1) * cel, 5 * cel, 5 * cel);
        gc.setFill(Color.BLACK);
        gc.fillRect((col + 2) * cel, (row + 2) * cel, 3 * cel, 3 * cel);
    }

    private boolean esAreaReservada(int row, int col, int n) {
        if (row < 8 && col < 8)       return true;
        if (row < 8 && col >= n - 8)  return true;
        if (row >= n - 8 && col < 8)  return true;
        if (row == 6 && col >= 8 && col < n - 8) return true;
        if (col == 6 && row >= 8 && row < n - 8) return true;
        return false;
    }

    private Canvas dibujarCodigoBarras(String datos, double ancho, double alto) {
        Canvas canvas = new Canvas(ancho, alto + 14);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, ancho, alto + 14);

        double x = 4;

        gc.setFill(Color.BLACK);
        gc.fillRect(x, 0, 2, alto); x += 3;
        gc.fillRect(x, 0, 1, alto); x += 2;
        gc.fillRect(x, 0, 2, alto); x += 5;

        long seed = Math.abs((long) datos.hashCode()) ^ 0xDEADL;
        java.util.Random rnd = new java.util.Random(seed);
        for (int i = 0; i < datos.length() && x < ancho - 16; i++) {
            int val = (int) datos.charAt(i);
            for (int bit = 7; bit >= 0 && x < ancho - 14; bit--) {
                boolean negro = ((val >> bit) & 1) == 1;
                double w = negro ? (rnd.nextBoolean() ? 3.0 : 2.0) : (rnd.nextBoolean() ? 2.0 : 1.5);
                gc.setFill(negro ? Color.BLACK : Color.WHITE);
                gc.fillRect(x, 0, w, alto);
                x += w;
            }
        }

        gc.setFill(Color.BLACK);
        gc.fillRect(x, 0, 2, alto); x += 4;
        gc.fillRect(x, 0, 1, alto); x += 2;
        gc.fillRect(x, 0, 2, alto);

        gc.setFill(Color.BLACK);
        gc.setFont(Font.font(MONO, 8));
        double tw = datos.length() * 4.8;
        gc.fillText(datos.length() > 30 ? datos.substring(0, 30) + "…" : datos,
                (ancho - tw) / 2, alto + 12);

        return canvas;
    }

    private HBox filaTabla(String desc, String cant, String valor, boolean bold) {
        Label lDesc  = labelMono(padD(desc,  20), FS_MD, bold);
        Label lCant  = labelMono(padC(cant,   5), FS_MD, bold);
        Label lValor = labelMono(padI(valor, 13), FS_MD, bold);
        HBox row = new HBox(lDesc, lCant, lValor);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox filaKV(String clave, String valor) {
        Label lK = labelMono(padD(clave, 13), FS_MD, true);
        Label lV = labelMono(valor, FS_MD, false);
        lV.setStyle(lV.getStyle() + " -fx-text-fill:#222222;");
        HBox row = new HBox(lK, lV);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox filaTotales(String clave, String valor, boolean grande) {
        double fs = grande ? FS_LG : FS_MD;
        Label lK = labelMono(padD(clave, 24), fs, grande);
        Label lV = labelMono(padI(valor, 15), fs, true);
        HBox row = new HBox(lK, lV);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Label construirBadgeEstado() {
        String estado = factura.getEstadoPago() != null
                ? factura.getEstadoPago().name() : "PAGADA";
        int lateral = (COLS - estado.length() - 4) / 2;
        lateral = Math.max(lateral, 0);
        String linea = "─".repeat(lateral) + "[ " + estado + " ]" + "─".repeat(lateral);
        Label l = labelMono(linea, FS_SM, true);
        l.setMaxWidth(ANCHO);
        l.setAlignment(Pos.CENTER);
        return l;
    }

    private Label labelCentrado(String texto, double fs, boolean bold) {
        Label l = labelMono(texto, fs, bold);
        l.setMaxWidth(ANCHO - 20);
        l.setAlignment(Pos.CENTER);
        l.setTextAlignment(TextAlignment.CENTER);
        return l;
    }

    private Label labelMono(String texto, double fs, boolean bold) {
        Label l = new Label(texto);
        l.setFont(bold
                ? Font.font(MONO, FontWeight.BOLD, fs)
                : Font.font(MONO, fs));
        l.setStyle("-fx-text-fill:#000000;");
        l.setWrapText(false);
        return l;
    }

    private Label sep(char c) {
        String linea = String.valueOf(c).repeat(COLS);
        Label l = new Label(linea);
        l.setFont(Font.font(MONO, FS_SM));
        l.setStyle("-fx-text-fill:#000000;");
        l.setPadding(new Insets(1, 0, 1, 0));
        return l;
    }

    private Region espacio(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        r.setMaxHeight(h);
        return r;
    }

    private Button botonAccion(String texto, String fg, String bg) {
        Button b = new Button(texto);
        b.setStyle(
            "-fx-background-color:" + bg + ";" +
            "-fx-text-fill:" + fg + ";" +
            "-fx-font-size:12px; -fx-font-weight:bold;" +
            "-fx-background-radius:8px; -fx-padding:8px 14px;" +
            "-fx-cursor:hand; -fx-border-color:" + fg + ";" +
            "-fx-border-width:1.5px; -fx-border-radius:8px;");
        return b;
    }

    private void agregar(VBox parent, javafx.scene.Node node) {
        parent.getChildren().add(node);
    }

    private String padD(String s, int n) {
        if (s == null) s = "";
        return s.length() >= n ? s.substring(0, n) : s + " ".repeat(n - s.length());
    }

    private String padI(String s, int n) {
        if (s == null) s = "";
        return s.length() >= n ? s.substring(0, n) : " ".repeat(n - s.length()) + s;
    }

    private String padC(String s, int n) {
        if (s == null) s = "";
        if (s.length() >= n) return s.substring(0, n);
        int left = (n - s.length()) / 2;
        return " ".repeat(left) + s + " ".repeat(n - s.length() - left);
    }

    private String acortar(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen - 1) + "." : s;
    }

    private String fmt(double valor) {
        return String.format("$%,.0f", valor);
    }

    private String numeroFactura() {
        return String.format("FE-%d-%06d", LocalDate.now().getYear(), factura.getId());
    }

    private long calcularNoches(Reserva r) {
        if (r == null || r.getFechaEntrada() == null || r.getFechaSalida() == null) return 1;
        long n = ChronoUnit.DAYS.between(r.getFechaEntrada(), r.getFechaSalida());
        return n > 0 ? n : 1;
    }

    private void generarPdfExistente() {
        new Thread(() -> {
            try {
                String ruta = com.hotel.AppContext.getInstance()
                        .getPdfReporteService().generarFacturaPdf(factura.getId());
                javafx.application.Platform.runLater(() ->
                        NotificationUtil.exito("PDF guardado en:\n" + ruta));
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() ->
                        NotificationUtil.error("Error al generar PDF: " + ex.getMessage()));
            }
        }).start();
    }
    
}
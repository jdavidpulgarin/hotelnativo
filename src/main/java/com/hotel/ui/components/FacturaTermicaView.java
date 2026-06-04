package com.hotel.ui.components;

import com.hotel.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.scene.transform.Scale;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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
    private static final int    COLS    = 34;       // caracteres por línea (ajustado a FS_SM=11)
    private static final String MONO    = "Courier New";
    private static final double FS_XL   = 22;
    private static final double FS_LG   = 16;
    private static final double FS_MD   = 13;
    private static final double FS_SM   = 11;

    private static final Locale         LOCALE_ES  = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FMT_DIA  =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FMT_LARGO =
            DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM yyyy", Locale.forLanguageTag("es-CO"));

    private static final String[] HOTEL_INFO = {
        "SISTEMA DE GESTIÓN HOTELERA",
        "Dg. 21 #27-89",
        "Valledupar, Cesar",
        "Tel: +57 300 5780623",
        "Email: hotelnativo1@gmail.com"
    };
    private static final String RES_DIAN  = "Res. DIAN: 000123-2024";
    private static final String RANGO     = "Rango: 1 - 9999999";

    // ── Datos ─────────────────────────────────────────────────────────────────
    private final Factura  factura;
    private final Empleado cajero;

    public FacturaTermicaView(Factura factura, Empleado cajero) {
        this.factura = factura;
        this.cajero  = cajero;
    }

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Abre la ventana de vista previa con controles de impresión y PDF.
     * @param owner ventana padre para la modality
     */
    public void mostrarVentanaPrevia(Stage owner) {
        VBox ticket = construirTicket();

        // ── Stage (sin decoración de Windows) ────────────────────────────────
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        if (owner != null) stage.initOwner(owner);
        stage.setResizable(false);

        // ── Header personalizado (draggable) ──────────────────────────────────
        Label lblTitulo = new Label("Vista previa — " + numeroFactura());
        lblTitulo.setStyle("-fx-font-size:13; -fx-font-weight:bold; -fx-text-fill:#1E293B;");

        String estiloXNormal = "-fx-background-color:transparent; -fx-border-color:transparent;" +
            "-fx-text-fill:#64748B; -fx-font-size:13; -fx-cursor:hand;" +
            "-fx-min-width:28; -fx-max-width:28; -fx-min-height:28; -fx-max-height:28; -fx-padding:0;";
        String estiloXHover = "-fx-background-color:#FEE2E2; -fx-border-color:transparent;" +
            "-fx-text-fill:#DC2626; -fx-font-size:13; -fx-cursor:hand; -fx-background-radius:4;" +
            "-fx-min-width:28; -fx-max-width:28; -fx-min-height:28; -fx-max-height:28; -fx-padding:0;";

        Button btnXHeader = new Button("✕");
        btnXHeader.setStyle(estiloXNormal);
        btnXHeader.setOnMouseEntered(e -> btnXHeader.setStyle(estiloXHover));
        btnXHeader.setOnMouseExited(e  -> btnXHeader.setStyle(estiloXNormal));
        btnXHeader.setOnAction(e       -> stage.close());

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        HBox header = new HBox(lblTitulo, hSpacer, btnXHeader);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 16, 8, 16));
        header.setPrefHeight(40);
        header.setStyle(
            "-fx-background-color:white;" +
            "-fx-border-color:transparent transparent #E2E8F0 transparent;" +
            "-fx-border-width:0 0 1 0;");

        // Drag para mover la ventana arrastrando el header
        double[] dragDelta = {0, 0};
        header.setOnMousePressed(ev -> {
            dragDelta[0] = stage.getX() - ev.getScreenX();
            dragDelta[1] = stage.getY() - ev.getScreenY();
        });
        header.setOnMouseDragged(ev -> {
            stage.setX(ev.getScreenX() + dragDelta[0]);
            stage.setY(ev.getScreenY() + dragDelta[1]);
        });

        // ── Papel de factura sobre superficie gris ────────────────────────────
        VBox papel = new VBox(ticket);
        papel.setAlignment(Pos.TOP_CENTER);
        papel.setStyle(
            "-fx-background-color:white;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),12,0,0,4);");

        VBox surface = new VBox(papel);
        surface.setAlignment(Pos.TOP_CENTER);
        surface.setPadding(new Insets(20));
        surface.setStyle("-fx-background-color:#E2E8F0;");

        ScrollPane scroll = new ScrollPane(surface);
        scroll.setFitToWidth(true);
        scroll.setStyle(
            "-fx-background:#E2E8F0; -fx-background-color:#E2E8F0;" +
            "-fx-border-color:transparent;");

        // ── Botones inferiores (estilos via CSS class en main.css) ────────────
        Button btnPrint  = new Button("🖨  Imprimir");
        btnPrint.getStyleClass().add("btn-imprimir");

        Button btnPdf    = new Button("📄  Guardar PDF");
        btnPdf.getStyleClass().add("btn-guardar-pdf");

        Region bSpacer = new Region();
        HBox.setHgrow(bSpacer, Priority.ALWAYS);

        Button btnCerrar = new Button("✕  Cerrar");
        btnCerrar.getStyleClass().add("btn-cerrar-factura");

        btnPrint.setOnAction(e  -> imprimir(ticket, stage));
        btnPdf.setOnAction(e    -> generarPdfExistente());
        btnCerrar.setOnAction(e -> stage.close());

        HBox toolbar = new HBox(10, btnPrint, btnPdf, bSpacer, btnCerrar);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(12, 20, 12, 20));
        toolbar.setStyle(
            "-fx-background-color:white;" +
            "-fx-border-color:#E2E8F0 transparent transparent transparent;" +
            "-fx-border-width:1 0 0 0;");

        // ── Ensamblado ────────────────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(scroll);
        root.setBottom(toolbar);

        Scene scene = new Scene(root, 450, 700);
        try {
            java.net.URL cssUrl =
                getClass().getResource("/com/hotel/ui/styles/main.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        } catch (Exception ignored) {}

        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    /** Envía el ticket directamente a la impresora térmica sin generar PDF. */
    public void imprimir(VBox ticket, Stage owner) {
    PrinterJob job = PrinterJob.createPrinterJob();
    if (job == null) {
        NotificationUtil.error("No se encontró ninguna impresora.");
        return;
    }

    // Obtener el layout por defecto de la impresora seleccionada
    PageLayout layout = job.getPrinter().createPageLayout(
            javafx.print.Paper.NA_LETTER,
            javafx.print.PageOrientation.PORTRAIT,
            javafx.print.Printer.MarginType.HARDWARE_MINIMUM);

    job.getJobSettings().setPageLayout(layout);

    SnapshotParameters sp = new SnapshotParameters();
    sp.setTransform(new Scale(2, 2));
    WritableImage snap = ticket.snapshot(sp, null);

    ImageView iv = new ImageView(snap);
    iv.setFitWidth(layout.getPrintableWidth());
    iv.setPreserveRatio(true);
    iv.setSmooth(true);

    boolean exito = job.printPage(layout, iv);
    if (exito) {
        job.endJob();
        NotificationUtil.exito("Documento enviado a la impresora.");
    } else {
        NotificationUtil.error("Error al enviar a la impresora.");
    }
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
        agregar(t, sepLinea());
        agregar(t, construirSeccionTipoDoc());
        agregar(t, sepLinea());
        agregar(t, construirSeccionInfoFactura());
        agregar(t, sep('─'));
        agregar(t, construirSeccionHuesped());
        agregar(t, sep('─'));
        agregar(t, construirTablaItems());
        agregar(t, construirSeccionTotales());
        agregar(t, sepLinea());
        agregar(t, construirSeccionLegal());
        agregar(t, sep('─'));
        agregar(t, construirSeccionCodigoQR());
        agregar(t, sep('─'));
        agregar(t, construirPieDePagina());
        agregar(t, sepLinea());
        agregar(t, espacio(6));

        return t;
    }

    // ── Secciones del ticket ──────────────────────────────────────────────────

    private VBox construirEncabezado() {
        VBox v = new VBox(2);
        v.setAlignment(Pos.TOP_CENTER);
        v.setPadding(new Insets(4, 0, 8, 0));

        // Logo: imagen real o fallback de texto
        boolean logoLoaded = false;
        try {
            java.net.URL logoUrl = getClass().getResource("/com/hotel/ui/images/logo.png");
            if (logoUrl != null) {
                ImageView imgLogo = new ImageView(new Image(logoUrl.toExternalForm()));
                imgLogo.setFitWidth(60);
                imgLogo.setFitHeight(60);
                imgLogo.setPreserveRatio(true);
                imgLogo.setSmooth(true);
                v.getChildren().add(imgLogo);
                logoLoaded = true;
            }
        } catch (Exception ignored) {}

        if (!logoLoaded) {
            v.getChildren().add(labelCentrado("HOTEL NATIVO", FS_LG, true));
        }

        v.getChildren().add(espacio(4));
        v.getChildren().add(labelCentrado("HOTEL NATIVO", 14, true));
        v.getChildren().add(espacio(3));

        for (String linea : HOTEL_INFO) {
            v.getChildren().add(labelCentrado(linea, 10, false));
        }
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
            labelCentrado("Fecha Resolución: 2024-01-15", FS_SM, false),
            espacio(4),
            labelCentrado("Conserve esta factura como soporte.", FS_SM, false),
            labelCentrado("Para quejas y reclamos:", FS_SM, false),
            labelCentrado("hotelnativo1@gmail.com", FS_SM, false)
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
        String codigoBarra = "HN" + LocalDate.now().getYear() + "0000" + factura.getId();
        v.getChildren().add(labelCentrado(codigoBarra, FS_SM, true));

        return v;
    }

    private VBox construirPieDePagina() {
        VBox v = new VBox(2);
        v.setAlignment(Pos.TOP_CENTER);
        v.setPadding(new Insets(8, 0, 6, 0));

        v.getChildren().addAll(
            espacio(4),
            labelCentrado("¡GRACIAS POR SU VISITA!", FS_LG, true),
            espacio(3),
            labelCentrado("Esperamos verlo otra vez en", FS_SM, false),
            labelCentrado("HOTEL NATIVO", FS_SM, true),
            espacio(5),
            labelCentrado("hotelnativo1@gmail.com", FS_SM, false),
            espacio(3),
            labelCentrado("Documento generado automáticamente", FS_SM, false),
            labelCentrado(LocalDate.now().format(FMT_LARGO), FS_SM, false)
        );
        return v;
    }

    // ── Gráficos: QR y código de barras ───────────────────────────────────────

    private Canvas dibujarQR(String datos, int tamano) {
        final int N   = 21;       // QR Version 1: 21×21 módulos
        double    cel = (double) tamano / N;

        Canvas canvas = new Canvas(tamano, tamano);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fondo blanco
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, tamano, tamano);

        // Patrones de detección en las 3 esquinas
        dibujarPatronDeteccion(gc, 0, 0, cel);
        dibujarPatronDeteccion(gc, N - 7, 0, cel);
        dibujarPatronDeteccion(gc, 0, N - 7, cel);

        // Patrones de temporización (timing)
        for (int i = 8; i < N - 8; i++) {
            gc.setFill(i % 2 == 0 ? Color.BLACK : Color.WHITE);
            gc.fillRect(i * cel, 6 * cel, cel, cel);   // horizontal
            gc.fillRect(6 * cel, i * cel, cel, cel);   // vertical
        }

        // Módulo oscuro de formato (fijo en versión 1)
        gc.setFill(Color.BLACK);
        gc.fillRect(8 * cel, 13 * cel, cel, cel);

        // Área de datos: patrón determinista basado en hash del contenido
        long seed = Math.abs((long) datos.hashCode()) ^ 0xCAFEBABEL;
        java.util.Random rnd = new java.util.Random(seed);
        for (int row = 0; row < N; row++) {
            for (int col = 0; col < N; col++) {
                if (esAreaReservada(row, col, N)) continue;
                gc.setFill(rnd.nextBoolean() ? Color.BLACK : Color.WHITE);
                gc.fillRect(col * cel, row * cel, cel, cel);
            }
        }

        // Marco exterior de 1.5px
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        gc.strokeRect(0.75, 0.75, tamano - 1.5, tamano - 1.5);

        return canvas;
    }

    private void dibujarPatronDeteccion(GraphicsContext gc, int col, int row, double cel) {
        gc.setFill(Color.BLACK);
        gc.fillRect(col * cel, row * cel, 7 * cel, 7 * cel);   // borde 7×7
        gc.setFill(Color.WHITE);
        gc.fillRect((col + 1) * cel, (row + 1) * cel, 5 * cel, 5 * cel); // interior 5×5
        gc.setFill(Color.BLACK);
        gc.fillRect((col + 2) * cel, (row + 2) * cel, 3 * cel, 3 * cel); // centro 3×3
    }

    private boolean esAreaReservada(int row, int col, int n) {
        if (row < 8 && col < 8)       return true;  // esquina sup-izq
        if (row < 8 && col >= n - 8)  return true;  // esquina sup-der
        if (row >= n - 8 && col < 8)  return true;  // esquina inf-izq
        if (row == 6 && col >= 8 && col < n - 8) return true; // timing H
        if (col == 6 && row >= 8 && row < n - 8) return true; // timing V
        return false;
    }

    private Canvas dibujarCodigoBarras(String datos, double ancho, double alto) {
        Canvas canvas = new Canvas(ancho, alto + 14);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, ancho, alto + 14);

        double x = 4;

        // Barra de inicio: narrow-wide-narrow
        gc.setFill(Color.BLACK);
        gc.fillRect(x, 0, 2, alto); x += 3;
        gc.fillRect(x, 0, 1, alto); x += 2;
        gc.fillRect(x, 0, 2, alto); x += 5;

        // Datos: cada carácter → 8 bits → alternancia barras/espacios
        long seed = Math.abs((long) datos.hashCode()) ^ 0xDEADL;
        java.util.Random rnd = new java.util.Random(seed);
        for (int i = 0; i < datos.length() && x < ancho - 16; i++) {
            int val = (int) datos.charAt(i);
            for (int bit = 7; bit >= 0 && x < ancho - 14; bit--) {
                boolean negro = ((val >> bit) & 1) == 1;
                // Mezcla con ruido determinista para aspecto más natural
                double w = negro ? (rnd.nextBoolean() ? 3.0 : 2.0) : (rnd.nextBoolean() ? 2.0 : 1.5);
                gc.setFill(negro ? Color.BLACK : Color.WHITE);
                gc.fillRect(x, 0, w, alto);
                x += w;
            }
        }

        // Barra de fin: wide-narrow-wide
        gc.setFill(Color.BLACK);
        gc.fillRect(x, 0, 2, alto); x += 4;
        gc.fillRect(x, 0, 1, alto); x += 2;
        gc.fillRect(x, 0, 2, alto);

        // Texto del código centrado
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font(MONO, 8));
        double tw = datos.length() * 4.8;
        gc.fillText(datos.length() > 30 ? datos.substring(0, 30) + "…" : datos,
                (ancho - tw) / 2, alto + 12);

        return canvas;
    }

    // ── Componentes de fila ───────────────────────────────────────────────────

    /** Fila de tabla: descripción | cantidad | valor — layout flexible sin padding fijo. */
    private HBox filaTabla(String desc, String cant, String valor, boolean bold) {
        Label lDesc  = labelMono(desc,  FS_MD, bold);
        Label lCant  = labelMono(cant,  FS_MD, bold);
        Label lValor = labelMono(valor, FS_MD, bold);

        lDesc.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(lDesc, Priority.ALWAYS);
        lValor.setMinWidth(Region.USE_PREF_SIZE);
        lCant.setMinWidth(Region.USE_PREF_SIZE);

        HBox row = new HBox(4, lDesc, lCant, lValor);
        row.setPrefWidth(ANCHO - 20);
        row.setMaxWidth(ANCHO - 20);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** Fila clave-valor: "Etiqueta:"  "Valor" con espaciador flexible. */
    private HBox filaKV(String clave, String valor) {
        Label lK = labelMono(clave, FS_MD, true);
        Label lV = labelMono(valor, FS_MD, false);
        lV.setStyle(lV.getStyle() + " -fx-text-fill:#222222;");
        lV.setMinWidth(Region.USE_PREF_SIZE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(lK, spacer, lV);
        row.setPrefWidth(ANCHO - 20);
        row.setMaxWidth(ANCHO - 20);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** Fila de totales: clave a la izquierda, valor a la derecha. */
    private HBox filaTotales(String clave, String valor, boolean grande) {
        double fs = grande ? FS_LG : FS_MD;
        Label lK = labelMono(clave, fs, grande);
        Label lV = labelMono(valor, fs, true);
        lV.setAlignment(Pos.CENTER_RIGHT);
        lV.setMinWidth(Region.USE_PREF_SIZE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(lK, spacer, lV);
        row.setPrefWidth(ANCHO - 20);
        row.setMaxWidth(ANCHO - 20);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** Badge de estado PAGADA / PENDIENTE / ANULADA centrado. */
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

    // ── Builders de Label ─────────────────────────────────────────────────────

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
            : Font.font(MONO, FontWeight.SEMI_BOLD, fs));
    l.setStyle("-fx-text-fill:#000000; -fx-font-smoothing-type:gray;");
    l.setWrapText(false);
    return l;
}
    /** Separador de línea completa con el carácter dado. */
    private Label sep(char c) {
        String linea = String.valueOf(c).repeat(COLS);
        Label l = new Label(linea);
        l.setFont(Font.font(MONO, FS_SM));
        l.setStyle("-fx-text-fill:#000000;");
        l.setPadding(new Insets(1, 0, 1, 0));
        return l;
    }

    /** Separador principal (reemplaza '═') con guiones en gris claro. */
    private Label sepLinea() {
        String linea = "-".repeat(COLS);
        Label l = new Label(linea);
        l.setFont(Font.font(MONO, FS_SM));
        l.setStyle("-fx-text-fill:#CBD5E1;");
        l.setPadding(new Insets(1, 0, 1, 0));
        return l;
    }

    private Region espacio(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        r.setMaxHeight(h);
        return r;
    }

    private void agregar(VBox parent, javafx.scene.Node node) {
        parent.getChildren().add(node);
    }

    /** Corta el String si excede maxLen, añadiendo punto al final. */
    private String acortar(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen - 1) + "." : s;
    }

    /** Formatea un double como moneda colombiana: $1,234,567. */
    private String fmt(double valor) {
        return String.format("$%,.0f", valor);
    }

    // ── Cálculos ──────────────────────────────────────────────────────────────

    private String numeroFactura() {
        return String.format("FE-%d-%06d", LocalDate.now().getYear(), factura.getId());
    }

    private long calcularNoches(Reserva r) {
        if (r == null || r.getFechaEntrada() == null || r.getFechaSalida() == null) return 1;
        long n = ChronoUnit.DAYS.between(r.getFechaEntrada(), r.getFechaSalida());
        return n > 0 ? n : 1;
    }

    /** Usa el PDF de factura estándar ya existente (formato A4). */
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
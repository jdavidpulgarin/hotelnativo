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
}
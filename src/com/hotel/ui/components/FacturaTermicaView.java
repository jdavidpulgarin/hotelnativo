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
}
package com.hotel.ui.controllers;

import com.hotel.AppContext;
import com.hotel.ui.components.NotificationUtil;
import com.hotel.util.ManejadorExcepciones;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Controlador del módulo de Reportes.
 * Gestiona la generación de reportes en formato HTML (existente) y PDF (nuevo).
 * Cada operación se ejecuta en un hilo secundario para no bloquear la UI de JavaFX.
 *
 * GRASP: Controlador – punto de entrada para las acciones del usuario en la vista.
 * SOLID: S – responsabilidad única: coordinar la generación de reportes desde la UI.
 */
public class ReportesController {

    // ── Campos del header ────────────────────────────────────────────────────
    @FXML private Label lblFechaBadge;
    @FXML private Label lblTurnoBadge;

    // ── Campos HTML ──────────────────────────────────────────────────────────
    @FXML private TextField fIdFactura;
    @FXML private TextField fAnio;
    @FXML private TextField fMes;

    // ── Campos PDF ───────────────────────────────────────────────────────────
    @FXML private TextField fIdFacturaPdf;
    @FXML private TextField fAnioDiario;
    @FXML private TextField fMesDiario;
    @FXML private TextField fAnioMensual;

    private final AppContext ctx = AppContext.getInstance();

    @FXML
    public void initialize() {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM",
                    Locale.forLanguageTag("es"));
            String fecha = LocalDate.now().format(fmt);
            fecha = Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1);
            lblFechaBadge.setText("📅  " + fecha);
            int hora = LocalTime.now().getHour();
            String turno = hora >= 6 && hora < 14 ? "Mañana"
                         : hora >= 14 && hora < 22 ? "Tarde" : "Noche";
            lblTurnoBadge.setText("⏰  Turno " + turno);
        } catch (Exception ignored) { }
    }

    // ── Reportes HTML (funcionalidad existente) ───────────────────────────────

    @FXML
    public void generarFacturaHTML() {
        String texto = fIdFactura.getText().trim();
        if (texto.isEmpty()) {
            NotificationUtil.advertencia("Ingresa el ID de la factura.");
            return;
        }
        try {
            int idFactura = Integer.parseInt(texto);
            Thread hilo = new Thread(() -> {
                try {
                    String ruta = ctx.getReporteService().guardarFacturaHTML(idFactura);
                    Platform.runLater(() ->
                        NotificationUtil.exito("Factura HTML generada:\n" + ruta));
                } catch (Exception e) {
                    Platform.runLater(() ->
                        NotificationUtil.error("Error: " + e.getMessage()));
                }
            });
            hilo.setName("hilo-reporte-factura-html");
            hilo.setDaemon(true);
            hilo.setUncaughtExceptionHandler(new ManejadorExcepciones());
            hilo.start();
        } catch (NumberFormatException e) {
            NotificationUtil.error("El ID debe ser un número entero.");
        }
    }

    @FXML
    public void generarReporteOcupacion() {
        String txtAnio = fAnio.getText().trim();
        String txtMes  = fMes.getText().trim();
        if (txtAnio.isEmpty() || txtMes.isEmpty()) {
            NotificationUtil.advertencia("Ingresa el año y el mes.");
            return;
        }
        try {
            int anio = Integer.parseInt(txtAnio);
            int mes  = Integer.parseInt(txtMes);
            if (mes < 1 || mes > 12) {
                NotificationUtil.error("El mes debe estar entre 1 y 12.");
                return;
            }
            Thread hilo = new Thread(() -> {
                try {
                    String ruta = ctx.getReporteService().guardarReporteOcupacion(anio, mes);
                    Platform.runLater(() ->
                        NotificationUtil.exito("Reporte de ocupación HTML generado:\n" + ruta));
                } catch (Exception e) {
                    Platform.runLater(() ->
                        NotificationUtil.error("Error: " + e.getMessage()));
                }
            });
            hilo.setName("hilo-reporte-ocupacion-html");
            hilo.setDaemon(true);
            hilo.setUncaughtExceptionHandler(new ManejadorExcepciones());
            hilo.start();
        } catch (NumberFormatException e) {
            NotificationUtil.error("Año y mes deben ser números enteros.");
        }
    }

    // ── Reportes PDF (nuevos) ─────────────────────────────────────────────────

    @FXML
    public void generarFacturaPDF() {
        String texto = fIdFacturaPdf.getText().trim();
        if (texto.isEmpty()) {
            NotificationUtil.advertencia("Ingresa el ID de la factura.");
            return;
        }
        try {
            int idFactura = Integer.parseInt(texto);
            Thread hilo = new Thread(() -> {
                try {
                    String ruta = ctx.getPdfReporteService().generarFacturaPdf(idFactura);
                    Platform.runLater(() ->
                        NotificationUtil.exito("Factura PDF generada:\n" + ruta));
                } catch (Exception e) {
                    Platform.runLater(() ->
                        NotificationUtil.error("Error generando PDF: " + e.getMessage()));
                }
            });
            hilo.setName("hilo-reporte-factura-pdf");
            hilo.setDaemon(true);
            hilo.setUncaughtExceptionHandler(new ManejadorExcepciones());
            hilo.start();
        } catch (NumberFormatException e) {
            NotificationUtil.error("El ID debe ser un número entero.");
        }
    }

    @FXML
    public void generarReporteOcupacionPDF() {
        String txtAnio = fAnio.getText().trim();
        String txtMes  = fMes.getText().trim();
        if (txtAnio.isEmpty() || txtMes.isEmpty()) {
            NotificationUtil.advertencia("Ingresa el año y el mes.");
            return;
        }
        try {
            int anio = Integer.parseInt(txtAnio);
            int mes  = Integer.parseInt(txtMes);
            if (mes < 1 || mes > 12) {
                NotificationUtil.error("El mes debe estar entre 1 y 12.");
                return;
            }
            Thread hilo = new Thread(() -> {
                try {
                    String ruta = ctx.getPdfReporteService().generarReporteOcupacionPdf(anio, mes);
                    Platform.runLater(() ->
                        NotificationUtil.exito("Reporte de ocupación PDF generado:\n" + ruta));
                } catch (Exception e) {
                    Platform.runLater(() ->
                        NotificationUtil.error("Error generando PDF: " + e.getMessage()));
                }
            });
            hilo.setName("hilo-reporte-ocupacion-pdf");
            hilo.setDaemon(true);
            hilo.setUncaughtExceptionHandler(new ManejadorExcepciones());
            hilo.start();
        } catch (NumberFormatException e) {
            NotificationUtil.error("Año y mes deben ser números enteros.");
        }
    }

    @FXML
    public void generarIngresosDiariosPDF() {
        String txtAnio = fAnioDiario.getText().trim();
        String txtMes  = fMesDiario.getText().trim();
        if (txtAnio.isEmpty() || txtMes.isEmpty()) {
            NotificationUtil.advertencia("Ingresa el año y el mes.");
            return;
        }
        try {
            int anio = Integer.parseInt(txtAnio);
            int mes  = Integer.parseInt(txtMes);
            if (mes < 1 || mes > 12) {
                NotificationUtil.error("El mes debe estar entre 1 y 12.");
                return;
            }
            Thread hilo = new Thread(() -> {
                try {
                    String ruta = ctx.getPdfReporteService().generarReporteIngresosDiariosPdf(anio, mes);
                    Platform.runLater(() ->
                        NotificationUtil.exito("Reporte de ingresos diarios PDF generado:\n" + ruta));
                } catch (Exception e) {
                    Platform.runLater(() ->
                        NotificationUtil.error("Error generando PDF: " + e.getMessage()));
                }
            });
            hilo.setName("hilo-reporte-ingresos-diarios-pdf");
            hilo.setDaemon(true);
            hilo.setUncaughtExceptionHandler(new ManejadorExcepciones());
            hilo.start();
        } catch (NumberFormatException e) {
            NotificationUtil.error("Año y mes deben ser números enteros.");
        }
    }

    @FXML
    public void generarIngresosMensualesPDF() {
        String txtAnio = fAnioMensual.getText().trim();
        if (txtAnio.isEmpty()) {
            NotificationUtil.advertencia("Ingresa el año.");
            return;
        }
        try {
            int anio = Integer.parseInt(txtAnio);
            Thread hilo = new Thread(() -> {
                try {
                    String ruta = ctx.getPdfReporteService().generarReporteIngresosMensualesPdf(anio);
                    Platform.runLater(() ->
                        NotificationUtil.exito("Reporte de ingresos mensuales PDF generado:\n" + ruta));
                } catch (Exception e) {
                    Platform.runLater(() ->
                        NotificationUtil.error("Error generando PDF: " + e.getMessage()));
                }
            });
            hilo.setName("hilo-reporte-ingresos-mensuales-pdf");
            hilo.setDaemon(true);
            hilo.setUncaughtExceptionHandler(new ManejadorExcepciones());
            hilo.start();
        } catch (NumberFormatException e) {
            NotificationUtil.error("El año debe ser un número entero.");
        }
    }

    @FXML
    public void generarIngresosAnualesPDF() {
        Thread hilo = new Thread(() -> {
            try {
                String ruta = ctx.getPdfReporteService().generarReporteIngresosAnualesPdf();
                Platform.runLater(() ->
                    NotificationUtil.exito("Reporte de ingresos anuales PDF generado:\n" + ruta));
            } catch (Exception e) {
                Platform.runLater(() ->
                    NotificationUtil.error("Error generando PDF: " + e.getMessage()));
            }
        });
        hilo.setName("hilo-reporte-ingresos-anuales-pdf");
        hilo.setDaemon(true);
        hilo.setUncaughtExceptionHandler(new ManejadorExcepciones());
        hilo.start();
    }
}

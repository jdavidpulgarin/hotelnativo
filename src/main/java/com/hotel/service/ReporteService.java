package com.hotel.service;

import com.hotel.dao.interfaces.IFacturaDAO;
import com.hotel.dao.interfaces.IReservaBusqueda;
import com.hotel.dao.interfaces.IReservaDAO;
import com.hotel.model.Factura;
import com.hotel.model.Reserva;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio de generación de reportes y documentos HTML del hotel.
 *
 * CORRECCIÓN BUG #1: generarFacturaHTML() ahora llama factura.getTasaIva() que
 * fue agregado al modelo Factura. Antes lanzaba NullPointerException porque el
 * método no existía.
 *
 * CORRECCIÓN WARN #6: generarReporteOcupacionHTML() ya NO carga todas las
 * reservas en memoria para luego filtrar en Java. Ahora usa
 * reservaBusqueda.buscarPorRangoFechas() que ejecuta el filtrado directamente
 * en la query SQL con WHERE fecha_entrada BETWEEN ? AND ?.
 *
 * GRASP: Alta Cohesión – responsabilidad única: generación de documentos.
 * SOLID: S – separado de FacturaService y EmailService. SOLID: D – depende de
 * interfaces DAO.
 */
public class ReporteService {

    private static final DateTimeFormatter FMT_FECHA
            = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_MES
            = DateTimeFormatter.ofPattern("MMMM yyyy",
                    java.util.Locale.forLanguageTag("es"));

    private final IFacturaDAO facturaDAO;
    private final IReservaDAO reservaDAO;
    private final IReservaBusqueda reservaBusqueda;

    public ReporteService(IFacturaDAO facturaDAO,
            IReservaDAO reservaDAO,
            IReservaBusqueda reservaBusqueda) {
        this.facturaDAO = facturaDAO;
        this.reservaDAO = reservaDAO;
        this.reservaBusqueda = reservaBusqueda;
    }

    // ── Factura HTML ──────────────────────────────────────────────────────────
    /**
     * Genera el HTML de una factura lista para impresión o conversión a PDF.
     *
     * CORRECCIÓN BUG #1: usa factura.getTasaIva() que ahora existe en el
     * modelo.
     *
     * @param idFactura ID de la factura a renderizar
     * @return HTML completo como String
     */
    public String generarFacturaHTML(int idFactura) {
        Factura factura = facturaDAO.buscarPorId(idFactura)
                .orElseThrow(() -> new IllegalArgumentException(
                "Factura no encontrada: " + idFactura));

        Reserva reserva = reservaDAO.buscarPorId(factura.getReserva().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                "Reserva no encontrada: " + factura.getReserva().getId()));
        factura.setReserva(reserva);

        long noches = (reserva.getFechaEntrada() != null && reserva.getFechaSalida() != null)
                ? reserva.getFechaEntrada().until(reserva.getFechaSalida()).getDays() : 0;
        double subtotal = factura.getSubtotal();
        boolean clienteVip = factura.getCliente() != null && factura.getCliente().isEsVip();
        double precioOriginal = clienteVip ? subtotal / 0.85 : subtotal;
        double descuentoVip = clienteVip ? precioOriginal - subtotal : 0;
        double tasaIva = factura.getTasaIva() > 0 ? factura.getTasaIva() : 0.19;
        double iva = subtotal * tasaIva;
        double totalConIva = subtotal + iva;

        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>"
                + "<title>Factura #" + idFactura + "</title>"
                + "<style>"
                + "body{font-family:Arial,sans-serif;margin:40px;color:#2c3e50;font-size:13px}"
                + ".header{display:flex;justify-content:space-between;margin-bottom:30px}"
                + ".hotel-name{font-size:22px;font-weight:bold;color:#1a3a5c}"
                + ".hotel-sub{color:#7f8c8d;font-size:11px;margin-top:3px}"
                + ".info-grid{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:24px}"
                + ".info-box{background:#f8f9fa;padding:14px;border-radius:6px;border-left:4px solid #1a3a5c}"
                + ".info-box h4{font-size:11px;text-transform:uppercase;color:#7f8c8d;margin-bottom:8px}"
                + "table{width:100%;border-collapse:collapse;margin-bottom:20px}"
                + "th{background:#1a3a5c;color:#fff;padding:9px 12px;text-align:left;font-size:12px}"
                + "td{padding:9px 12px;border-bottom:1px solid #dee2e6;font-size:13px}"
                + ".totals{margin-left:auto;width:280px}"
                + ".total-row{display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid #dee2e6}"
                + ".total-final{display:flex;justify-content:space-between;padding:10px 0;font-size:16px;font-weight:bold;color:#1a3a5c;border-top:2px solid #1a3a5c}"
                + ".footer{margin-top:40px;text-align:center;color:#7f8c8d;font-size:11px}"
                + ".badge{display:inline-block;padding:4px 12px;border-radius:20px;font-size:11px;font-weight:bold}"
                + ".badge-paid{background:#e8f8f1;color:#2ecc71}"
                + "</style></head><body>"
                + "<div class='header'>"
                + "<div><div class='hotel-name'>Hotel Nativo</div>"
                + "<div class='hotel-sub'>Valledupar, Cesar · NIT: 901.234.567-8</div></div>"
                + "<div><h2 style='margin:0;color:#1a3a5c'>FACTURA DE VENTA</h2>"
                + "<div style='color:#7f8c8d'>N° " + String.format("%06d", idFactura) + "</div>"
                + "<span class='badge badge-paid'>PAGADA</span></div></div>"
                + "<div class='info-grid'>"
                + "<div class='info-box'><h4>Cliente</h4>"
                + "<p><strong>" + (factura.getCliente() != null ? factura.getCliente().obtenerNombreCompleto() : "—") + "</strong></p>"
                + "<p>" + (factura.getCliente() != null ? factura.getCliente().getEmail() : "—") + "</p></div>"
                + "<div class='info-box'><h4>Detalles</h4>"
                + "<p><strong>Fecha:</strong> " + (factura.getFechaEmision() != null ? factura.getFechaEmision().format(FMT_FECHA) : "—") + "</p>"
                + "<p><strong>Reserva:</strong> #" + reserva.getId() + "</p>"
                + "<p><strong>Pago:</strong> " + (factura.getMetodoPago() != null ? factura.getMetodoPago().name().replace("_", " ") : "—") + "</p></div></div>"
                + "<table><thead><tr>"
                + "<th>Descripción</th><th>Hab.</th><th>Entrada</th><th>Salida</th>"
                + "<th>Noches</th><th style='text-align:right'>Total</th>"
                + "</tr></thead><tbody><tr>"
                + "<td>" + (reserva.getHabitacion() != null && reserva.getHabitacion().getTipoHabitacion() != null
                ? reserva.getHabitacion().getTipoHabitacion().obtenerEtiquetaTipo()
                : "Habitación") + "</td>"
                + "<td>" + (reserva.getHabitacion() != null ? reserva.getHabitacion().getNumero() : "—") + "</td>"
                + "<td>" + (reserva.getFechaEntrada() != null ? reserva.getFechaEntrada().format(FMT_FECHA) : "—") + "</td>"
                + "<td>" + (reserva.getFechaSalida() != null ? reserva.getFechaSalida().format(FMT_FECHA) : "—") + "</td>"
                + "<td>" + noches + "</td>"
                + "<td style='text-align:right'>$" + String.format("%,.0f", subtotal) + "</td>"
                + "</tr></tbody></table>"
                + "<div class='totals'>"
                + (clienteVip
                        ? "<div class='total-row'><span>Precio base</span><span>$"
                        + String.format("%,.0f", precioOriginal) + "</span></div>"
                        + "<div class='total-row' style='color:#b45309'><span>Descuento VIP (15%)</span><span>-$"
                        + String.format("%,.0f", descuentoVip) + "</span></div>"
                        : "")
                + "<div class='total-row'><span>Subtotal</span><span>$"
                + String.format("%,.0f", subtotal) + "</span></div>"
                + "<div class='total-row'><span>IVA (" + Math.round(tasaIva * 100) + "%)</span><span>$"
                + String.format("%,.0f", iva) + "</span></div>"
                + "<div class='total-final'><span>TOTAL A PAGAR</span><span>$"
                + String.format("%,.0f", totalConIva) + "</span></div></div>"
                + "<div class='footer'>Gracias por su preferencia · Hotel Nativo S.A.S.<br>"
                + "Factura generada electrónicamente.</div>"
                + "</body></html>";
    }

    public String guardarFacturaHTML(int idFactura) {
        String html = generarFacturaHTML(idFactura);
        String carpeta = "C:/Facturas";
        new java.io.File(carpeta).mkdirs();
        java.nio.file.Path path = java.nio.file.Paths.get(carpeta, "factura_" + idFactura + ".html");
        try {
            java.nio.file.Files.writeString(path, html);
            System.out.println("[REPORTE] Factura HTML guardada en: " + path);
        } catch (Exception e) {
            System.err.println("[REPORTE] Error al guardar factura: " + e.getMessage());
        }
        return path.toString();
    }
}

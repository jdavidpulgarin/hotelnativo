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

    // ── Reporte de ocupación mensual ──────────────────────────────────────────
    /**
     * Genera un reporte HTML de ocupación e ingresos para el mes indicado.
     *
     * CORRECCIÓN WARN #6: Antes se llamaba reservaDAO.listarTodas() cargando
     * TODAS las reservas históricas en memoria para luego filtrar en Java.
     * Ahora se usa reservaBusqueda.buscarPorRangoFechas() que ejecuta la query
     * SQL con WHERE fecha_entrada BETWEEN inicio AND fin. Esto es O(resultado)
     * en lugar de O(total_reservas).
     *
     * @param anio año del reporte
     * @param mes mes (1–12)
     * @return HTML del reporte
     */
    public String generarReporteOcupacionHTML(int anio, int mes) {
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth());

        List<Reserva> reservasDelMes = reservaBusqueda.buscarPorRangoFechas(inicio, fin);

        long totalReservas = reservasDelMes.size();
        long completadas = reservasDelMes.stream()
                .filter(r -> r.getEstado() == Reserva.EstadoReserva.COMPLETADA).count();
        long canceladas = reservasDelMes.stream()
                .filter(r -> r.getEstado() == Reserva.EstadoReserva.CANCELADA).count();
        double ingresos = reservasDelMes.stream()
                .filter(r -> r.getEstado() == Reserva.EstadoReserva.COMPLETADA)
                .mapToDouble(Reserva::getPrecioTotal).sum();

        String nombreMes = inicio.format(FMT_MES).toUpperCase();

        StringBuilder filas = new StringBuilder();
        reservasDelMes.forEach(r -> {
            long noches = (r.getFechaEntrada() != null && r.getFechaSalida() != null)
                    ? r.getFechaEntrada().until(r.getFechaSalida()).getDays() : 0;
            filas.append("<tr>")
                    .append("<td>#").append(r.getId()).append("</td>")
                    .append("<td>").append(r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—").append("</td>")
                    .append("<td>").append(r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—").append("</td>")
                    .append("<td>").append(r.getFechaEntrada() != null ? r.getFechaEntrada().format(FMT_FECHA) : "—").append("</td>")
                    .append("<td>").append(noches).append("</td>")
                    .append("<td>$").append(String.format("%,.0f", r.getPrecioTotal())).append("</td>")
                    .append("<td>").append(r.getEstado()).append("</td>")
                    .append("</tr>");
        });

        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>"
                + "<title>Reporte " + nombreMes + "</title>"
                + "<style>"
                + "body{font-family:Arial,sans-serif;margin:40px;color:#2c3e50;font-size:13px}"
                + "h1{color:#1a3a5c;border-bottom:2px solid #c9aa71;padding-bottom:10px}"
                + ".kpis{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin:24px 0}"
                + ".kpi{background:#f8f9fa;border-radius:8px;padding:16px;text-align:center;border-top:3px solid #1a3a5c}"
                + ".kpi-val{font-size:26px;font-weight:bold;color:#1a3a5c}"
                + ".kpi-lbl{font-size:11px;color:#7f8c8d;margin-top:4px;text-transform:uppercase}"
                + "table{width:100%;border-collapse:collapse;margin-top:16px}"
                + "th{background:#1a3a5c;color:#fff;padding:9px 12px;text-align:left;font-size:12px}"
                + "td{padding:8px 12px;border-bottom:1px solid #dee2e6}"
                + "</style></head><body>"
                + "<h1>Reporte de Ocupación – " + nombreMes + "</h1>"
                + "<p style='color:#7f8c8d'>Generado: " + LocalDate.now().format(FMT_FECHA)
                + " · Hotel Nativo S.A.S.</p>"
                + "<div class='kpis'>"
                + "<div class='kpi'><div class='kpi-val'>" + totalReservas + "</div>"
                + "<div class='kpi-lbl'>Reservas</div></div>"
                + "<div class='kpi'><div class='kpi-val'>" + completadas + "</div>"
                + "<div class='kpi-lbl'>Completadas</div></div>"
                + "<div class='kpi'><div class='kpi-val'>" + canceladas + "</div>"
                + "<div class='kpi-lbl'>Canceladas</div></div>"
                + "<div class='kpi'><div class='kpi-val'>$" + String.format("%,.0f", ingresos) + "</div>"
                + "<div class='kpi-lbl'>Ingresos</div></div></div>"
                + "<table><thead><tr>"
                + "<th>#</th><th>Cliente</th><th>Hab.</th><th>Entrada</th>"
                + "<th>Noches</th><th>Total</th><th>Estado</th>"
                + "</tr></thead><tbody>" + filas + "</tbody></table>"
                + "</body></html>";
    }

    public String guardarReporteOcupacion(int anio, int mes) {
        String html = generarReporteOcupacionHTML(anio, mes);
        String carpeta = "C:/Facturas";
        new java.io.File(carpeta).mkdirs();
        java.nio.file.Path path = java.nio.file.Paths.get(
                carpeta, "reporte_" + anio + "_" + String.format("%02d", mes) + ".html");
        try {
            java.nio.file.Files.writeString(path, html);
            System.out.println("[REPORTE] Reporte guardado en: " + path);
        } catch (Exception e) {
            System.err.println("[REPORTE] Error al guardar reporte: " + e.getMessage());
        }
        return path.toString();
    }
}

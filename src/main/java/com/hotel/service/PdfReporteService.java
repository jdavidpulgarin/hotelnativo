package com.hotel.service;

import com.hotel.dao.interfaces.IFacturaDAO;
import com.hotel.dao.interfaces.IReservaBusqueda;
import com.hotel.dao.interfaces.IReservaDAO;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.model.Factura;
import com.hotel.model.Reserva;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de generacion de reportes en formato PDF usando OpenPDF.
 * Guarda los archivos en ~/HotelReportes y los abre automaticamente
 * si el sistema operativo soporta la API java.awt.Desktop.
 *
 * GRASP: Fabricacion Pura - no existe en el dominio, responsabilidad unica de PDF.
 * SOLID: S - separado de ReporteService (HTML) y FacturaService.
 * SOLID: D - depende de interfaces DAO, no de implementaciones concretas.
 */
public class PdfReporteService {

    private static final String CARPETA_REPORTES =
            System.getProperty("user.home") + "/HotelReportes";

    private static final Color COLOR_AZUL_HOTEL  = new Color(0x1a, 0x3a, 0x5c);
    private static final Color COLOR_DORADO       = new Color(0xc9, 0xaa, 0x71);
    private static final Color COLOR_VERDE        = new Color(0x16, 0xa3, 0x4a);
    private static final Color COLOR_GRIS_HEADER  = new Color(0x37, 0x41, 0x51);
    private static final Color COLOR_FILA_PAR     = new Color(0xf8, 0xfa, 0xfc);

    private final IFacturaDAO      facturaDAO;
    private final IReservaBusqueda reservaBusqueda;

    public PdfReporteService(IFacturaDAO facturaDAO,
                             IReservaDAO reservaDAO,
                             IReservaBusqueda reservaBusqueda) {
        this.facturaDAO      = facturaDAO;
        this.reservaBusqueda = reservaBusqueda;
    }

    public String generarFacturaPdf(int idFactura) {
        Factura f = facturaDAO.buscarPorId(idFactura)
                .orElseThrow(() -> new ExcepcionBaseDatos(
                        "No existe la factura con ID " + idFactura));

        String nombreArchivo = "Factura_" + idFactura + "_" + LocalDate.now() + ".pdf";
        String ruta = crearRuta(nombreArchivo);

        Document doc = new Document(PageSize.A4);
        doc.setMargins(36, 36, 40, 36);
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(ruta));
            doc.open();
            agregarHeaderFact(doc, f);
            agregarLineaSep(doc, 15, 15);
            agregarInfoFact(doc, f);
            agregarDetallesFact(doc, f);
            agregarTotalesFact(doc, f);
            agregarFooterFact(doc, f);
            agregarLineaSep(doc, 8, 4);
            agregarDisclaimer(doc);
        } catch (Exception e) {
            throw new ExcepcionBaseDatos("Error generando PDF de factura: " + e.getMessage(), e);
        } finally {
            doc.close();
        }
        abrirPdf(ruta);
        return ruta;
    }

    public String generarReporteOcupacionPdf(int anio, int mes) {
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        LocalDate fin    = inicio.withDayOfMonth(inicio.lengthOfMonth());
        List<Reserva> reservas = reservaBusqueda.buscarPorRangoFechas(inicio, fin);
        String nombreMes = inicio.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));
        String nombreArchivo = "Ocupacion_" + anio + "_" + String.format("%02d", mes) + ".pdf";
        String ruta = crearRuta(nombreArchivo);

        Document doc = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(ruta));
            doc.open();
            agregarHeaderHotel(doc, "REPORTE DE OCUPACION - " + nombreMes.toUpperCase() + " " + anio);
            long confirmadas = reservas.stream().filter(r ->
                    r.getEstado() == Reserva.EstadoReserva.CONFIRMADA ||
                    r.getEstado() == Reserva.EstadoReserva.EN_PROCESO).count();
            long canceladas  = reservas.stream().filter(r ->
                    r.getEstado() == Reserva.EstadoReserva.CANCELADA).count();
            long completadas = reservas.stream().filter(r ->
                    r.getEstado() == Reserva.EstadoReserva.COMPLETADA).count();
            PdfPTable kpiTab = crearTabla(4, new float[]{1, 1, 1, 1});
            agregarKPI(kpiTab, "Total reservas", String.valueOf(reservas.size()), COLOR_AZUL_HOTEL);
            agregarKPI(kpiTab, "Activas",        String.valueOf(confirmadas),     COLOR_VERDE);
            agregarKPI(kpiTab, "Completadas",    String.valueOf(completadas),     COLOR_GRIS_HEADER);
            agregarKPI(kpiTab, "Canceladas",     String.valueOf(canceladas),      new Color(0xdc, 0x26, 0x26));
            doc.add(kpiTab);
            doc.add(Chunk.NEWLINE);
            PdfPTable tabla = crearTabla(6, new float[]{0.5f, 2f, 1.5f, 1.2f, 1.2f, 1.2f});
            agregarEncabezado(tabla, new String[]{"ID", "Cliente", "Habitacion", "Entrada", "Salida", "Estado"});
            boolean parImpar = false;
            for (Reserva r : reservas) {
                Color fondo = parImpar ? COLOR_FILA_PAR : Color.WHITE;
                Font fontFila = FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_GRIS_HEADER);
                agregarCelda(tabla, String.valueOf(r.getId()), fontFila, fondo, Element.ALIGN_CENTER);
                agregarCelda(tabla, r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "-", fontFila, fondo, Element.ALIGN_LEFT);
                agregarCelda(tabla, r.getHabitacion() != null ? "Hab. " + r.getHabitacion().getNumero() : "-", fontFila, fondo, Element.ALIGN_CENTER);
                agregarCelda(tabla, r.getFechaEntrada() != null ? r.getFechaEntrada().toString() : "-", fontFila, fondo, Element.ALIGN_CENTER);
                agregarCelda(tabla, r.getFechaSalida()  != null ? r.getFechaSalida().toString()  : "-", fontFila, fondo, Element.ALIGN_CENTER);
                agregarCelda(tabla, r.getEstado() != null ? r.getEstado().name() : "-", fontFila, fondo, Element.ALIGN_CENTER);
                parImpar = !parImpar;
            }
            if (reservas.isEmpty()) {
                PdfPCell vacio = new PdfPCell(new Phrase("Sin reservas en este periodo",
                        FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.GRAY)));
                vacio.setColspan(6);
                vacio.setHorizontalAlignment(Element.ALIGN_CENTER);
                vacio.setPadding(12);
                tabla.addCell(vacio);
            }
            doc.add(tabla);
            agregarPiePagina(doc);
        } catch (Exception e) {
            throw new ExcepcionBaseDatos("Error generando PDF de ocupacion: " + e.getMessage(), e);
        } finally {
            doc.close();
        }
        abrirPdf(ruta);
        return ruta;
    }

}

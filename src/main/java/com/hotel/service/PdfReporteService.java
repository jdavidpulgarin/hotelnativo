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

    public String generarReporteIngresosDiariosPdf(int anio, int mes) {
        List<Factura> todasLasFacturas = facturaDAO.listarTodas();
        String nombreMes = LocalDate.of(anio, mes, 1).getMonth()
                .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));
        Map<Integer, Double> porDia = todasLasFacturas.stream()
                .filter(f -> f.getEstadoPago() == Factura.EstadoPago.PAGADA)
                .filter(f -> f.getFechaEmision() != null
                          && f.getFechaEmision().getYear() == anio
                          && f.getFechaEmision().getMonthValue() == mes)
                .collect(Collectors.groupingBy(
                        f -> f.getFechaEmision().getDayOfMonth(),
                        Collectors.summingDouble(Factura::getTotal)));
        int diasEnMes = LocalDate.of(anio, mes, 1).lengthOfMonth();
        double totalMes = porDia.values().stream().mapToDouble(Double::doubleValue).sum();
        String nombreArchivo = "IngresosDiarios_" + anio + "_" + String.format("%02d", mes) + ".pdf";
        String ruta = crearRuta(nombreArchivo);
        Document doc = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(ruta));
            doc.open();
            agregarHeaderHotel(doc, "INGRESOS DIARIOS - " + nombreMes.toUpperCase() + " " + anio);
            PdfPTable tabla = crearTabla(3, new float[]{1f, 2f, 2f});
            agregarEncabezado(tabla, new String[]{"Dia", "Ingresos del dia", "Acumulado"});
            double acumulado = 0;
            boolean parImpar = false;
            for (int dia = 1; dia <= diasEnMes; dia++) {
                double ingresoDia = porDia.getOrDefault(dia, 0.0);
                acumulado += ingresoDia;
                Color fondo = parImpar ? COLOR_FILA_PAR : Color.WHITE;
                Font fontFila = FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_GRIS_HEADER);
                agregarCelda(tabla, String.valueOf(dia), fontFila, fondo, Element.ALIGN_CENTER);
                agregarCelda(tabla,
                        ingresoDia > 0 ? String.format("$ %,.2f", ingresoDia) : "-",
                        ingresoDia > 0 ? FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_VERDE) : fontFila,
                        fondo, Element.ALIGN_RIGHT);
                agregarCelda(tabla, String.format("$ %,.2f", acumulado), fontFila, fondo, Element.ALIGN_RIGHT);
                parImpar = !parImpar;
            }
            Font fontTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_VERDE);
            Color fondoTotal = new Color(0xf0, 0xfd, 0xf4);
            agregarCelda(tabla, "TOTAL",                               fontTotal, fondoTotal, Element.ALIGN_CENTER);
            agregarCelda(tabla, String.format("$ %,.2f", totalMes),   fontTotal, fondoTotal, Element.ALIGN_RIGHT);
            agregarCelda(tabla, String.format("$ %,.2f", totalMes),   fontTotal, fondoTotal, Element.ALIGN_RIGHT);
            doc.add(tabla);
            agregarPiePagina(doc);
        } catch (Exception e) {
            throw new ExcepcionBaseDatos("Error generando PDF de ingresos diarios: " + e.getMessage(), e);
        } finally {
            doc.close();
        }
        abrirPdf(ruta);
        return ruta;
    }

    public String generarReporteIngresosMensualesPdf(int anio) {
        List<Factura> todasLasFacturas = facturaDAO.listarTodas();
        String[] MESES = {"Enero","Febrero","Marzo","Abril","Mayo","Junio",
                          "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};
        Map<Integer, Double> porMes = todasLasFacturas.stream()
                .filter(f -> f.getEstadoPago() == Factura.EstadoPago.PAGADA)
                .filter(f -> f.getFechaEmision() != null && f.getFechaEmision().getYear() == anio)
                .collect(Collectors.groupingBy(
                        f -> f.getFechaEmision().getMonthValue(),
                        Collectors.summingDouble(Factura::getTotal)));
        double totalAnio = porMes.values().stream().mapToDouble(Double::doubleValue).sum();
        String nombreArchivo = "IngresosMensuales_" + anio + ".pdf";
        String ruta = crearRuta(nombreArchivo);
        Document doc = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(ruta));
            doc.open();
            agregarHeaderHotel(doc, "INGRESOS MENSUALES - " + anio);
            PdfPTable tabla = crearTabla(3, new float[]{2f, 2f, 1.5f});
            agregarEncabezado(tabla, new String[]{"Mes", "Ingresos", "% del total"});
            boolean parImpar = false;
            for (int m = 1; m <= 12; m++) {
                double ingreso  = porMes.getOrDefault(m, 0.0);
                double porcentaje = totalAnio > 0 ? (ingreso / totalAnio) * 100 : 0;
                Color fondo = parImpar ? COLOR_FILA_PAR : Color.WHITE;
                Font fontFila = FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_GRIS_HEADER);
                agregarCelda(tabla, MESES[m - 1], fontFila, fondo, Element.ALIGN_LEFT);
                agregarCelda(tabla,
                        ingreso > 0 ? String.format("$ %,.2f", ingreso) : "$ 0.00",
                        ingreso > 0 ? FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_VERDE) : fontFila,
                        fondo, Element.ALIGN_RIGHT);
                agregarCelda(tabla, String.format("%.1f%%", porcentaje), fontFila, fondo, Element.ALIGN_CENTER);
                parImpar = !parImpar;
            }
            Font fontTotal  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_VERDE);
            Color fondoTotal = new Color(0xf0, 0xfd, 0xf4);
            agregarCelda(tabla, "TOTAL ANUAL",                        fontTotal, fondoTotal, Element.ALIGN_LEFT);
            agregarCelda(tabla, String.format("$ %,.2f", totalAnio),  fontTotal, fondoTotal, Element.ALIGN_RIGHT);
            agregarCelda(tabla, "100.0%",                              fontTotal, fondoTotal, Element.ALIGN_CENTER);
            doc.add(tabla);
            agregarPiePagina(doc);
        } catch (Exception e) {
            throw new ExcepcionBaseDatos("Error generando PDF de ingresos mensuales: " + e.getMessage(), e);
        } finally {
            doc.close();
        }
        abrirPdf(ruta);
        return ruta;
    }

    public String generarReporteIngresosAnualesPdf() {
        List<Factura> todasLasFacturas = facturaDAO.listarTodas();
        int anioActual = LocalDate.now().getYear();
        String nombreArchivo = "IngresosAnuales_" + anioActual + ".pdf";
        String ruta = crearRuta(nombreArchivo);
        Document doc = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(ruta));
            doc.open();
            agregarHeaderHotel(doc, "INGRESOS ANUALES - ULTIMOS 5 ANOS");
            PdfPTable tabla = crearTabla(3, new float[]{1f, 2f, 2f});
            agregarEncabezado(tabla, new String[]{"Ano", "Ingresos totales", "Variacion vs. ano anterior"});
            double ingresoAnterior = 0;
            boolean parImpar = false;
            for (int i = 4; i >= 0; i--) {
                int anio = anioActual - i;
                final int anioFinal = anio;
                double ingreso = todasLasFacturas.stream()
                        .filter(f -> f.getEstadoPago() == Factura.EstadoPago.PAGADA)
                        .filter(f -> f.getFechaEmision() != null && f.getFechaEmision().getYear() == anioFinal)
                        .mapToDouble(Factura::getTotal).sum();
                double variacion = (ingresoAnterior > 0) ? ((ingreso - ingresoAnterior) / ingresoAnterior) * 100 : 0;
                String variacionStr = (i == 4 || ingresoAnterior == 0) ? "-" :
                        (variacion >= 0 ? String.format("+%.1f%%", variacion) : String.format("%.1f%%", variacion));
                Color fondo = parImpar ? COLOR_FILA_PAR : Color.WHITE;
                Font fontFila = FontFactory.getFont(FontFactory.HELVETICA, anio == anioActual ? 11 : 10,
                        anio == anioActual ? COLOR_AZUL_HOTEL : COLOR_GRIS_HEADER);
                Font fontVar  = variacion >= 0 && i < 4
                        ? FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_VERDE)
                        : FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(0xdc, 0x26, 0x26));
                agregarCelda(tabla, String.valueOf(anio), fontFila, fondo, Element.ALIGN_CENTER);
                agregarCelda(tabla, ingreso > 0 ? String.format("$ %,.2f", ingreso) : "$ 0.00",
                        FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_VERDE), fondo, Element.ALIGN_RIGHT);
                agregarCelda(tabla, variacionStr,
                        (i == 4 || ingresoAnterior == 0) ? fontFila : fontVar, fondo, Element.ALIGN_CENTER);
                ingresoAnterior = ingreso;
                parImpar = !parImpar;
            }
            doc.add(tabla);
            agregarPiePagina(doc);
        } catch (Exception e) {
            throw new ExcepcionBaseDatos("Error generando PDF de ingresos anuales: " + e.getMessage(), e);
        } finally {
            doc.close();
        }
        abrirPdf(ruta);
        return ruta;
    }

    private void agregarHeaderFact(Document doc, Factura f) throws DocumentException {
        Image logoImg = null;
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/com/hotel/ui/images/logo.png");
            if (is != null) { logoImg = Image.getInstance(is.readAllBytes()); logoImg.scaleToFit(80, 80); }
        } catch (Exception ignored) {}
        PdfPTable headerTab = new PdfPTable(3);
        headerTab.setWidthPercentage(100);
        headerTab.setWidths(new float[]{3, 4, 3});
        PdfPCell cellLogo = new PdfPCell();
        cellLogo.setBorder(Rectangle.NO_BORDER);
        cellLogo.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellLogo.setPadding(8);
        if (logoImg != null) { cellLogo.addElement(logoImg); }
        else {
            Paragraph pFallback = new Paragraph("HOTEL NATIVO",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_AZUL_HOTEL));
            pFallback.setAlignment(Element.ALIGN_CENTER);
            cellLogo.addElement(pFallback);
        }
        headerTab.addCell(cellLogo);
        PdfPCell cellInfo = new PdfPCell();
        cellInfo.setBorder(Rectangle.NO_BORDER);
        cellInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellInfo.setPadding(8);
        Font fNombreH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, COLOR_AZUL_HOTEL);
        Font fInfoGray = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
        Font fInfoDark = FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_GRIS_HEADER);
        Paragraph pNombre = new Paragraph("HOTEL NATIVO", fNombreH);
        pNombre.setAlignment(Element.ALIGN_CENTER);
        cellInfo.addElement(pNombre);
        String[] hotelLines = {"SISTEMA DE GESTION HOTELERA","Dg. 21 #27-89","Valledupar, Cesar","Colombia","Tel: +57 300 5780623","hotelnativo1@gmail.com"};
        for (String line : hotelLines) {
            Paragraph pl = new Paragraph(line, fInfoDark);
            pl.setAlignment(Element.ALIGN_CENTER);
            cellInfo.addElement(pl);
        }
        headerTab.addCell(cellInfo);
        PdfPCell cellQR = new PdfPCell();
        cellQR.setBorder(Rectangle.NO_BORDER);
        cellQR.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellQR.setPadding(8);
        Paragraph pNumFactura = new Paragraph("FACTURA #" + f.getId(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_AZUL_HOTEL));
        pNumFactura.setAlignment(Element.ALIGN_CENTER);
        cellQR.addElement(pNumFactura);
        headerTab.addCell(cellQR);
        doc.add(headerTab);
    }

}

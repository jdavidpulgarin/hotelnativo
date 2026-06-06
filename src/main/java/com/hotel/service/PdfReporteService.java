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

}

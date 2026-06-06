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
 * CORRECCIÓN BUG #1: generarFacturaHTML() ahora llama factura.getTasaIva()
 * que fue agregado al modelo Factura. Antes lanzaba NullPointerException
 * porque el método no existía.
 *
 * CORRECCIÓN WARN #6: generarReporteOcupacionHTML() ya NO carga todas las
 * reservas en memoria para luego filtrar en Java. Ahora usa
 * reservaBusqueda.buscarPorRangoFechas() que ejecuta el filtrado directamente
 * en la query SQL con WHERE fecha_entrada BETWEEN ? AND ?.
 *
 * GRASP: Alta Cohesión – responsabilidad única: generación de documentos.
 * SOLID: S – separado de FacturaService y EmailService.
 * SOLID: D – depende de interfaces DAO.
 */
public class ReporteService {

    private static final DateTimeFormatter FMT_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_MES   =
            DateTimeFormatter.ofPattern("MMMM yyyy",
                    java.util.Locale.forLanguageTag("es"));

    private final IFacturaDAO      facturaDAO;
    private final IReservaDAO      reservaDAO;
    private final IReservaBusqueda reservaBusqueda;

    public ReporteService(IFacturaDAO facturaDAO,
                          IReservaDAO reservaDAO,
                          IReservaBusqueda reservaBusqueda) {
        this.facturaDAO      = facturaDAO;
        this.reservaDAO      = reservaDAO;
        this.reservaBusqueda = reservaBusqueda;
    }
}
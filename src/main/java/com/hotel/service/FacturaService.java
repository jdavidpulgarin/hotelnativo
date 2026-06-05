/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.service;

import com.hotel.dao.interfaces.IFacturaDAO;
import com.hotel.dao.interfaces.IReservaDAO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.*;
import com.hotel.util.Constantes;
import com.hotel.util.ValidadorEntradas;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Pulgarin
 */
public class FacturaService {

    private final IFacturaDAO facturaDAO;
    private final IReservaDAO reservaDAO;

    public FacturaService(IFacturaDAO facturaDAO, IReservaDAO reservaDAO) {
        this.facturaDAO = facturaDAO;
        this.reservaDAO = reservaDAO;
    }

    /**
     * Genera la factura con detalles completos del pago.
     */
    public Factura generarFactura(int idReserva, Factura.MetodoPago metodoPago,
            double montoRecibido, String franquicia, int numCuotas, String referencia)
            throws ExcepcionNegocio {

        ValidadorEntradas.validarIdPositivo(idReserva, "reserva");

        Reserva reserva = obtenerReservaCompletadaOLanzarError(idReserva);

        // v2: prc_checkout crea la factura PENDIENTE automaticamente.
        // Si ya existe una factura pendiente, registrar el pago en lugar de crear una nueva.
        java.util.Optional<Factura> facturaExistente = facturaDAO.buscarPorReserva(idReserva);
        if (facturaExistente.isPresent()) {
            Factura f = facturaExistente.get();
            if (f.getEstadoPago() == Factura.EstadoPago.PAGADA) {
                throw new ExcepcionNegocio("FACTURA_YA_PAGADA",
                        "La factura de la reserva ya fue pagada.");
            }
            // Registrar pago: cambiar estado a PAGADA y fijar método
            f.setEstadoPago(Factura.EstadoPago.PAGADA);
            f.setMetodoPago(metodoPago);
            facturaDAO.actualizar(f);
            // Aplicar detalles transient para el ticket termico
            if (montoRecibido > 0) {
                if (montoRecibido < f.getTotal()) {
                    throw new ExcepcionNegocio("PAGO_INSUFICIENTE",
                            String.format("Monto insuficiente. Recibido: $%,.0f  —  Total: $%,.0f",
                                    montoRecibido, f.getTotal()));
                }
                f.setMontoRecibido(montoRecibido);
                f.setCambio(Math.max(Math.round((montoRecibido - f.getTotal()) * 100.0) / 100.0, 0));
            }
            if (franquicia != null && !franquicia.isBlank()) {
                f.setFranquiciaTarjeta(franquicia);
            }
            if (metodoPago == Factura.MetodoPago.TARJETA_CREDITO && numCuotas > 1) {
                f.setNumCuotas(numCuotas);
            }
            if (referencia != null && !referencia.isBlank()) {
                f.setReferenciaTransferencia(referencia);
            }
            return f;
        }

        // Si no existe factura previa (flujo legacy), crearla
        verificarQueNoTieneFactura(idReserva);
        Factura nuevaFactura = new Factura(0, reserva, reserva.getCliente(),
                LocalDate.now(), reserva.getPrecioTotal(), Constantes.TASA_IVA);
        nuevaFactura.setMetodoPago(metodoPago);
        nuevaFactura.setEstadoPago(Factura.EstadoPago.PAGADA);

        // ── Validar y registrar detalles según método ─────────────────────────
        if (metodoPago == Factura.MetodoPago.EFECTIVO) {
            if (montoRecibido > 0 && montoRecibido < nuevaFactura.getTotal()) {
                throw new ExcepcionNegocio("PAGO_INSUFICIENTE",
                        String.format("Monto insuficiente. Recibido: $%,.0f  —  Total: $%,.0f",
                                montoRecibido, nuevaFactura.getTotal()));
            }
            nuevaFactura.setMontoRecibido(montoRecibido);
            // Redondeo a 2 decimales para evitar errores de punto flotante
            double cambio = Math.round((montoRecibido - nuevaFactura.getTotal()) * 100.0) / 100.0;
            nuevaFactura.setCambio(Math.max(cambio, 0));
        }

        if ((metodoPago == Factura.MetodoPago.TARJETA_CREDITO
                || metodoPago == Factura.MetodoPago.TARJETA_DEBITO)
                && franquicia != null && !franquicia.isBlank()) {
            nuevaFactura.setFranquiciaTarjeta(franquicia);
        }

        if (metodoPago == Factura.MetodoPago.TARJETA_CREDITO && numCuotas > 1) {
            nuevaFactura.setNumCuotas(numCuotas);
        }

        if (metodoPago == Factura.MetodoPago.TRANSFERENCIA
                && referencia != null && !referencia.isBlank()) {
            nuevaFactura.setReferenciaTransferencia(referencia);
        }

        return facturaDAO.insertar(nuevaFactura);
    }

    /**
     * Sobrecarga de compatibilidad. Genera factura sin detalles de pago (útil
     * para llamadas internas o migraciones de datos existentes).
     */
    public Factura generarFactura(int idReserva, Factura.MetodoPago metodoPago)
            throws ExcepcionNegocio {
        return generarFactura(idReserva, metodoPago, 0, null, 1, null);
    }

    public Optional<Factura> buscarPorReserva(int idReserva) throws ExcepcionNegocio {
        ValidadorEntradas.validarIdPositivo(idReserva, "reserva");
        return facturaDAO.buscarPorReserva(idReserva);
    }

    public List<Factura> listarFacturasPorCliente(int idCliente) throws ExcepcionNegocio {
        ValidadorEntradas.validarIdPositivo(idCliente, "cliente");
        return facturaDAO.listarPorCliente(idCliente);
    }
}

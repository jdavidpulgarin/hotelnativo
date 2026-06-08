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
 * Lógica de negocio para generación y gestión de facturas. GRASP: Alta Cohesión
 * — responsabilidad única: ciclo de vida de facturas. SOLID: S — facturación
 * separada de reservas y check-in/out.
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
     *
     * <ul>
     * <li><b>EFECTIVO</b>: {@code montoRecibido} debe ser ≥ total. Si > 0 y
     * menor al total se lanza {@code PAGO_INSUFICIENTE}. El cambio (vuelto) se
     * calcula y almacena en la factura.</li>
     * <li><b>TARJETA_CREDITO / TARJETA_DEBITO</b>: {@code franquicia} ("VISA",
     * "MASTERCARD", …). Solo crédito usa {@code numCuotas}.</li>
     * <li><b>TRANSFERENCIA</b>: {@code referencia} es el número de confirmación
     * bancaria.</li>
     * </ul>
     *
     * @param idReserva reserva COMPLETADA a facturar
     * @param metodoPago forma de pago
     * @param montoRecibido efectivo entregado por el cliente (0 si no aplica)
     * @param franquicia franquicia de tarjeta, null si no aplica
     * @param numCuotas cuotas (1 = contado); ignorado si no es crédito
     * @param referencia número de referencia bancaria, null si no aplica
     * @return factura generada y persistida (con detalles de pago en memoria)
     * @throws ExcepcionNegocio si la reserva no está completada, ya tiene
     * factura, o el monto en efectivo es insuficiente
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

    public List<Factura> listarTodasLasFacturas() {
        return facturaDAO.listarTodas();
    }

    // ── Métodos privados de apoyo ─────────────────────────────────────────────
    private Reserva obtenerReservaCompletadaOLanzarError(int idReserva) throws ExcepcionNegocio {
        Reserva reserva = reservaDAO.buscarPorId(idReserva)
                .orElseThrow(() -> new ExcepcionNegocio("RESERVA_NOT_FOUND",
                "No se encontró la reserva con ID: " + idReserva));

        if (!Reserva.EstadoReserva.COMPLETADA.equals(reserva.getEstado())) {
            throw new ExcepcionNegocio("RESERVA_NO_COMPLETADA",
                    "Solo se pueden facturar reservas con estado COMPLETADA. "
                    + "Estado actual: " + reserva.getEstado());
        }
        return reserva;
    }

    private void verificarQueNoTieneFactura(int idReserva) throws ExcepcionNegocio {
        if (facturaDAO.buscarPorReserva(idReserva).isPresent()) {
            throw new ExcepcionNegocio("FACTURA_DUPLICADA",
                    "La reserva " + idReserva + " ya tiene una factura generada.");
        }
    }
}


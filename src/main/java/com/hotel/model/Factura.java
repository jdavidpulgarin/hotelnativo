
package com.hotel.model;

/**
 *
 * @author rober
 */
import java.time.LocalDate;

/**
 * Entidad Factura del hotel.
 * GRASP: Experto en Información — calcula impuestos y total con precisión.
 *
 * Campos de pago (montoRecibido, cambio, franquiciaTarjeta, numCuotas,
 * referenciaTransferencia) son transient: se usan en la sesión actual para
 * el ticket térmico y las notificaciones; no se persisten en la BD porque
 * la tabla FACTURA actual no tiene esas columnas. Agregar columnas opcionales
 * con ALTER TABLE para persistirlos en una versión futura.
 */
public class Factura {

    public enum EstadoPago  { PENDIENTE, PAGADA, ANULADA }
    public enum MetodoPago  { EFECTIVO, TARJETA_CREDITO, TARJETA_DEBITO, TRANSFERENCIA }

    private int        id;
    private Reserva    reserva;
    private Cliente    cliente;
    private LocalDate  fechaEmision;
    private double     subtotal;
    private double     impuestos;
    private double     total;
    private double     tasaIva;
    private EstadoPago estadoPago;
    private MetodoPago metodoPago;

    // ── Detalles de pago (transient — sesión actual) ──────────────────────────
    private double montoRecibido;          // efectivo: monto entregado por el cliente
    private double cambio;                  // efectivo: vuelto a devolver
    private String franquiciaTarjeta;      // "VISA", "MASTERCARD", "AMEX", "DINNERS"
    private int    numCuotas   = 1;        // tarjeta crédito: cuotas (1 = contado)
    private String referenciaTransferencia; // número de referencia bancaria

    public Factura() {}

    /**
     * Constructor principal. Redondea impuestos y total a 2 decimales para
     * evitar la acumulación de error flotante característica de double (ej.
     * 150000 * 0.19 = 28500.000000000004 sin redondeo).
     *
     * @param subtotal      precio antes de impuestos
     * @param tasaImpuesto  tasa decimal, ej. 0.19 para IVA 19 %
     */
    public Factura(int id, Reserva reserva, Cliente cliente,
                   LocalDate fechaEmision, double subtotal, double tasaImpuesto) {
        this.id           = id;
        this.reserva      = reserva;
        this.cliente      = cliente;
        this.fechaEmision = fechaEmision;
        this.subtotal     = subtotal;
        this.tasaIva      = tasaImpuesto;
        this.impuestos    = Math.round(subtotal * tasaImpuesto * 100.0) / 100.0;
        this.total        = Math.round((subtotal + this.impuestos) * 100.0) / 100.0;
        this.estadoPago   = EstadoPago.PENDIENTE;
    }
    
}
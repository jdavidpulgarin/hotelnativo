package com.hotel.view;

import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.Factura;
import com.hotel.service.FacturaService;
import com.hotel.util.Constantes;
import java.util.List;
import java.util.Scanner;

/** Vista consola para generación y consulta de facturas. CAPA PRESENTACIÓN. */
public class FacturaView {

    private final FacturaService facturaService;
    private final Scanner        scanner;

    public FacturaView(FacturaService facturaService, Scanner scanner) {
        this.facturaService = facturaService;
        this.scanner        = scanner;
    }

    public void mostrarMenu() {
        boolean continuar = true;
        while (continuar) {
            System.out.println("\n" + Constantes.SEPARADOR_MENU);
            System.out.println("  FACTURACIÓN");
            System.out.println(Constantes.SEPARADOR_MENU);
            System.out.println("  1. Generar factura");
            System.out.println("  2. Ver factura por reserva");
            System.out.println("  3. Facturas de un cliente");
            System.out.println("  4. Listar todas");
            System.out.println("  0. Volver");
            System.out.print("  Opción: ");
            int opcion = leerEntero();
            switch (opcion) {
                case 1: generarFactura();          break;
                case 2: verFacturaPorReserva();    break;
                case 3: facturasPorCliente();      break;
                case 4: listarTodas();             break;
                case 0: continuar = false;         break;
                default: System.out.println(Constantes.ETIQUETA_ERROR + "Opción inválida.");
            }
        }
    }

    private void generarFactura() {
        System.out.print("\n  ID Reserva: "); int idReserva = leerEntero();
        System.out.println("  Método de pago:");
        System.out.println("    1. Efectivo  2. Tarjeta Crédito  3. Tarjeta Débito  4. Transferencia");
        System.out.print("  Selección: ");
        Factura.MetodoPago metodo = resolverMetodoPago(leerEntero());
        try {
            Factura factura = facturaService.generarFactura(idReserva, metodo);
            imprimirFactura(factura);
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void verFacturaPorReserva() {
        System.out.print("\n  ID Reserva: "); int idReserva = leerEntero();
        try {
            facturaService.buscarPorReserva(idReserva).ifPresentOrElse(
                this::imprimirFactura,
                () -> System.out.println(Constantes.ETIQUETA_ADVERTENCIA + "Sin factura para esa reserva.")
            );
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void facturasPorCliente() {
        System.out.print("\n  ID Cliente: "); int idCliente = leerEntero();
        try {
            List<Factura> facturas = facturaService.listarFacturasPorCliente(idCliente);
            if (facturas.isEmpty()) { System.out.println("Sin facturas para ese cliente."); return; }
            facturas.forEach(this::imprimirResumenFactura);
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void listarTodas() {
        List<Factura> todas = facturaService.listarTodasLasFacturas();
        if (todas.isEmpty()) { System.out.println("Sin facturas registradas."); return; }
        todas.forEach(this::imprimirResumenFactura);
    }

    private void imprimirFactura(Factura f) {
        System.out.println("\n  ══════════ FACTURA #" + f.getId() + " ══════════");
        System.out.println("  Fecha emisión: " + f.getFechaEmision());
        System.out.println("  Reserva:       #" + f.getReserva().getId());
        System.out.printf( "  Subtotal:      $%.2f%n", f.getSubtotal());
        System.out.printf( "  IVA (19%%):     $%.2f%n", f.getImpuestos());
        System.out.printf( "  TOTAL:         $%.2f%n", f.getTotal());
        System.out.println("  Estado:        " + f.getEstadoPago());
        System.out.println("  Método pago:   " + f.getMetodoPago());
        System.out.println("  ═══════════════════════════════");
    }

    private void imprimirResumenFactura(Factura f) {
        System.out.printf("  [%3d] Reserva#%-4d | $%-10.2f | %s | %s%n",
            f.getId(), f.getReserva().getId(), f.getTotal(),
            f.getEstadoPago(), f.getFechaEmision());
    }

    private Factura.MetodoPago resolverMetodoPago(int opcion) {
        switch (opcion) {
            case 2: return Factura.MetodoPago.TARJETA_CREDITO;
            case 3: return Factura.MetodoPago.TARJETA_DEBITO;
            case 4: return Factura.MetodoPago.TRANSFERENCIA;
            default: return Factura.MetodoPago.EFECTIVO;
        }
    }

    private int leerEntero() {
        try { return Integer.parseInt(scanner.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }
}

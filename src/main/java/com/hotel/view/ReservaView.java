package com.hotel.view;

import com.hotel.dto.BusquedaDisponibilidadDTO;
import com.hotel.dto.ReservaDTO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.Habitacion;
import com.hotel.model.Reserva;
import com.hotel.service.ReservaService;
import com.hotel.util.Constantes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Vista consola para gestión de reservas.
 * CAPA PRESENTACIÓN: únicamente Scanner y System.out.
 * GRASP: Controlador - delega toda lógica a ReservaService.
 */
public class ReservaView {

    private final ReservaService reservaService;
    private final Scanner        scanner;

    public ReservaView(ReservaService reservaService, Scanner scanner) {
        this.reservaService = reservaService;
        this.scanner        = scanner;
    }

    public void mostrarMenu() {
        boolean continuar = true;
        while (continuar) {
            imprimirEncabezado("GESTIÓN DE RESERVAS");
            System.out.println("  1. Nueva reserva");
            System.out.println("  2. Buscar habitaciones disponibles");
            System.out.println("  3. Buscar reserva por ID");
            System.out.println("  4. Listar todas las reservas");
            System.out.println("  5. Confirmar reserva");
            System.out.println("  6. Cancelar reserva");
            System.out.println("  7. Reservas activas de un cliente");
            System.out.println("  0. Volver");
            System.out.print("  Opción: ");
            continuar = ejecutarOpcion(leerEntero());
        }
    }

    private boolean ejecutarOpcion(int opcion) {
        switch (opcion) {
            case 1: crearReserva();                    return true;
            case 2: buscarHabitacionesDisponibles();   return true;
            case 3: buscarReservaPorId();              return true;
            case 4: listarTodasLasReservas();          return true;
            case 5: confirmarReserva();                return true;
            case 6: cancelarReserva();                 return true;
            case 7: listarReservasCliente();           return true;
            case 0:                                    return false;
            default:
                System.out.println(Constantes.ETIQUETA_ERROR + "Opción inválida.");
                return true;
        }
    }

    private void crearReserva() {
        System.out.println("\n── Nueva Reserva ──");
        try {
            System.out.print("  ID Cliente:    "); int idCliente    = leerEntero();
            System.out.print("  Número habitación: "); String numHab = scanner.nextLine().trim();
            System.out.print("  Fecha entrada (yyyy-MM-dd): "); LocalDate entrada = leerFecha();
            System.out.print("  Fecha salida  (yyyy-MM-dd): "); LocalDate salida  = leerFecha();
            System.out.print("  N° personas:   ");              int personas      = leerEntero();
            System.out.print("  Canal (CAN01=Recepción, CAN02=Teléfono, CAN03=Web, CAN04=OTA, CAN05=Corp): ");
            String canal = scanner.nextLine().trim();
            if (canal.isEmpty()) canal = "CAN01";

            ReservaDTO dto = new ReservaDTO(idCliente, numHab, entrada, salida, personas, canal);
            Reserva creada = reservaService.crearReserva(dto);
            System.out.printf("%sReserva #%d creada. Total: $%.2f%n",
                Constantes.ETIQUETA_EXITO, creada.getId(), creada.getPrecioTotal());

        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void buscarHabitacionesDisponibles() {
        System.out.println("\n── Buscar Disponibilidad ──");
        try {
            System.out.print("  Fecha entrada (yyyy-MM-dd): "); LocalDate entrada  = leerFecha();
            System.out.print("  Fecha salida  (yyyy-MM-dd): "); LocalDate salida   = leerFecha();
            System.out.print("  N° personas:               ");  int personas       = leerEntero();

            BusquedaDisponibilidadDTO criterios = new BusquedaDisponibilidadDTO(entrada, salida, personas);
            List<Habitacion> disponibles = reservaService.buscarHabitacionesDisponibles(criterios);

            if (disponibles.isEmpty()) {
                System.out.println(Constantes.ETIQUETA_ADVERTENCIA + "Sin habitaciones disponibles.");
                return;
            }
            System.out.println("\n  Habitaciones disponibles:");
            disponibles.forEach(h -> System.out.printf(
                "  [%3d] Hab. %-5s | %-6s | Piso %d | $%.2f/noche%n",
                h.getId(), h.getNumero(),
                h.getTipoHabitacion().obtenerEtiquetaTipo(),
                h.getPiso().getNumeroPiso(),
                h.calcularPrecioFinal()));

        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void buscarReservaPorId() {
        System.out.print("\nID de reserva: ");
        int id = leerEntero();
        try {
            reservaService.buscarPorId(id).ifPresentOrElse(
                this::imprimirDetalleReserva,
                () -> System.out.println(Constantes.ETIQUETA_ADVERTENCIA + "Reserva no encontrada.")
            );
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void listarTodasLasReservas() {
        List<Reserva> reservas = reservaService.listarTodasLasReservas();
        if (reservas.isEmpty()) { System.out.println("Sin reservas registradas."); return; }
        System.out.println("\n── Reservas ──");
        reservas.forEach(this::imprimirResumenReserva);
    }

    private void confirmarReserva() {
        System.out.print("\nID de reserva a confirmar: ");
        int id = leerEntero();
        try {
            reservaService.confirmarReserva(id);
            System.out.println(Constantes.ETIQUETA_EXITO + "Reserva confirmada.");
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void cancelarReserva() {
        System.out.print("\nID de reserva a cancelar: ");
        int id = leerEntero();
        System.out.print("¿Confirmar cancelación? (s/n): ");
        if (!"s".equalsIgnoreCase(scanner.nextLine().trim())) { System.out.println("Cancelado."); return; }
        try {
            reservaService.cancelarReserva(id);
            System.out.println(Constantes.ETIQUETA_EXITO + "Reserva cancelada.");
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void listarReservasCliente() {
        System.out.print("\nID del cliente: ");
        int idCliente = leerEntero();
        try {
            List<Reserva> activas = reservaService.listarReservasActivasPorCliente(idCliente);
            if (activas.isEmpty()) { System.out.println("Sin reservas activas."); return; }
            System.out.println("\n── Reservas activas ──");
            activas.forEach(this::imprimirResumenReserva);
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void imprimirDetalleReserva(Reserva r) {
        System.out.printf("%n  Reserva #%d | Estado: %s%n", r.getId(), r.getEstado());
        System.out.println("  Cliente:    " + r.getCliente().obtenerNombreCompleto());
        System.out.println("  Habitación: " + r.getHabitacion().getNumero());
        System.out.println("  Entrada:    " + r.getFechaEntrada());
        System.out.println("  Salida:     " + r.getFechaSalida());
        System.out.println("  Días:       " + r.calcularTotalDiasReserva());
        System.out.printf( "  Total:      $%.2f%n", r.getPrecioTotal());
    }

    private void imprimirResumenReserva(Reserva r) {
        System.out.printf("  [%3d] %-12s | Hab. %-5s | %s → %s | $%.2f%n",
            r.getId(), r.getEstado(), r.getHabitacion().getNumero(),
            r.getFechaEntrada(), r.getFechaSalida(), r.getPrecioTotal());
    }

    private void imprimirEncabezado(String titulo) {
        System.out.println("\n" + Constantes.SEPARADOR_MENU);
        System.out.println("  " + titulo);
        System.out.println(Constantes.SEPARADOR_MENU);
    }

    private LocalDate leerFecha() {
        try { return LocalDate.parse(scanner.nextLine().trim()); }
        catch (DateTimeParseException e) {
            System.out.println(Constantes.ETIQUETA_ERROR + "Formato incorrecto, use yyyy-MM-dd.");
            return LocalDate.now().plusDays(1);
        }
    }

    private int leerEntero() {
        try { return Integer.parseInt(scanner.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }
}

package com.hotel.view;

import com.hotel.dto.ClienteDTO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.CheckInOut;
import com.hotel.model.Cliente;
import com.hotel.service.CheckInOutService;
import com.hotel.service.ClienteService;
import com.hotel.util.Constantes;

import java.util.Optional;
import java.util.Scanner;

/**
 * Vista consola para check-in y check-out.
 *
 * MEJORA 4: formulario completo de check-in con búsqueda por documento y
 * registro de nuevo cliente si no existe en el sistema.
 *
 * CAPA PRESENTACIÓN — no contiene lógica de negocio.
 */
public class CheckInOutView {

    private final CheckInOutService checkInOutService;
    private final ClienteService    clienteService;
    private final Scanner           scanner;

    public CheckInOutView(CheckInOutService checkInOutService,
                          ClienteService    clienteService,
                          Scanner           scanner) {
        this.checkInOutService = checkInOutService;
        this.clienteService    = clienteService;
        this.scanner           = scanner;
    }

    public void mostrarMenu() {
        boolean continuar = true;
        while (continuar) {
            System.out.println("\n" + Constantes.SEPARADOR_MENU);
            System.out.println("  CHECK-IN / CHECK-OUT");
            System.out.println(Constantes.SEPARADOR_MENU);
            System.out.println("  1. Realizar Check-in");
            System.out.println("  2. Realizar Check-out");
            System.out.println("  0. Volver");
            System.out.print("  Opción: ");
            int opcion = leerEntero();
            switch (opcion) {
                case 1: realizarCheckin();  break;
                case 2: realizarCheckout(); break;
                case 0: continuar = false;  break;
                default: System.out.println(Constantes.ETIQUETA_ERROR + "Opción inválida.");
            }
        }
    }

    // ── Check-in ─────────────────────────────────────────────────────────────

    private void realizarCheckin() {
        System.out.println("\n  --- REGISTRO DE CHECK-IN ---");

        // 1. Buscar o registrar al cliente por documento
        Cliente cliente = buscarORegistrarCliente();
        if (cliente == null) return;

        System.out.println(Constantes.ETIQUETA_EXITO + "Cliente: "
                + cliente.obtenerNombreCompleto()
                + " | Email: " + cliente.getEmail()
                + " | VIP: " + (cliente.isEsVip() ? "Sí" : "No"));

        // 2. Datos de la reserva
        System.out.print("  ID Reserva:  "); int idReserva  = leerEntero();
        System.out.print("  ID Empleado: "); int idEmpleado = leerEntero();
        System.out.print("  Observaciones (Enter para omitir): ");
        String obs = scanner.nextLine().trim();

        try {
            CheckInOut registro = checkInOutService.realizarCheckin(
                    idReserva, idEmpleado, obs.isEmpty() ? null : obs);
            System.out.println(Constantes.ETIQUETA_EXITO
                    + "Check-in registrado #" + registro.getId()
                    + " — Hora de entrada: " + registro.getFechaHoraCheckin());
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private Cliente buscarORegistrarCliente() {
        System.out.print("  Email del huésped: ");
        String email = scanner.nextLine().trim();
        if (email.isEmpty()) {
            System.out.println(Constantes.ETIQUETA_ERROR + "Email requerido.");
            return null;
        }

        try {
            Optional<Cliente> encontrado = clienteService.listarTodosLosClientes().stream()
                    .filter(c -> email.equalsIgnoreCase(c.getEmail()))
                    .findFirst();
            if (encontrado.isPresent()) {
                return encontrado.get();
            }
        } catch (Exception e) {
            System.out.println(Constantes.ETIQUETA_ADVERTENCIA
                    + "Error buscando cliente: " + e.getMessage());
            return null;
        }

        System.out.println(Constantes.ETIQUETA_ADVERTENCIA
                + "Cliente no encontrado. ¿Desea registrarlo ahora? (s/n): ");
        String respuesta = scanner.nextLine().trim();
        if (!respuesta.equalsIgnoreCase("s")) return null;

        return registrarNuevoCliente(email);
    }

    private Cliente registrarNuevoCliente(String email) {
        System.out.println("\n  --- REGISTRO DE NUEVO CLIENTE ---");
        try {
            System.out.print("  Cédula:           "); String cedula = scanner.nextLine().trim();
            System.out.print("  Primer nombre:    "); String nombre = scanner.nextLine().trim();
            System.out.print("  Segundo nombre:   "); String sn     = scanner.nextLine().trim();
            System.out.print("  Primer apellido:  "); String ap1    = scanner.nextLine().trim();
            System.out.print("  Segundo apellido: "); String ap2    = scanner.nextLine().trim();
            System.out.print("  Teléfono:         "); String tel    = scanner.nextLine().trim();
            System.out.print("  Nacionalidad:     "); String nac    = scanner.nextLine().trim();
            System.out.print("  Ciudad de origen: "); String ciudad = scanner.nextLine().trim();

            ClienteDTO dto = new ClienteDTO(cedula, nombre,
                    sn.isEmpty() ? null : sn,
                    ap1,
                    ap2.isEmpty() ? null : ap2,
                    email, tel, nac,
                    ciudad.isEmpty() ? null : ciudad);

            Cliente nuevo = clienteService.registrarCliente(dto);
            System.out.println(Constantes.ETIQUETA_EXITO
                    + "Cliente registrado con ID: " + nuevo.getId());
            return nuevo;

        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
            return null;
        }
    }

    // ── Check-out ────────────────────────────────────────────────────────────

    private void realizarCheckout() {
        System.out.print("\n  ID Reserva: "); int idReserva = leerEntero();
        System.out.print("  Observaciones finales (Enter para omitir): ");
        String obs = scanner.nextLine().trim();
        try {
            CheckInOut registro = checkInOutService.realizarCheckout(
                    idReserva, obs.isEmpty() ? null : obs);
            System.out.println(Constantes.ETIQUETA_EXITO
                    + "Check-out registrado — Hora de salida: "
                    + registro.getFechaHoraCheckout());
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private int leerEntero() {
        try { return Integer.parseInt(scanner.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }
}

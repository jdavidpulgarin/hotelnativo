package com.hotel.view;

import com.hotel.dto.ClienteDTO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.Cliente;
import com.hotel.service.ClienteService;
import com.hotel.util.Constantes;

import java.util.List;
import java.util.Scanner;

/**
 * Vista consola para gestión de clientes.
 * CAPA PRESENTACIÓN: únicamente Scanner y System.out. Sin lógica de negocio.
 * SOLID: S - responsabilidad única: I/O de usuario para clientes.
 */
public class ClienteView {

    private final ClienteService clienteService;
    private final Scanner        scanner;

    public ClienteView(ClienteService clienteService, Scanner scanner) {
        this.clienteService = clienteService;
        this.scanner        = scanner;
    }

    public void mostrarMenu() {
        boolean continuar = true;
        while (continuar) {
            imprimirEncabezado("GESTIÓN DE CLIENTES");
            System.out.println("  1. Registrar nuevo cliente");
            System.out.println("  2. Buscar por ID");
            System.out.println("  3. Buscar por documento");
            System.out.println("  4. Listar todos");
            System.out.println("  5. Actualizar cliente");
            System.out.println("  6. Eliminar cliente");
            System.out.println("  7. Clientes VIP");
            System.out.println("  0. Volver");
            System.out.print("  Opción: ");
            continuar = ejecutarOpcion(leerEntero());
        }
    }

    private boolean ejecutarOpcion(int opcion) {
        switch (opcion) {
            case 1: registrarCliente();          return true;
            case 2: buscarPorId();               return true;
            case 3: buscarPorDocumento();        return true;
            case 4: listarTodos();               return true;
            case 5: actualizarCliente();         return true;
            case 6: eliminarCliente();           return true;
            case 7: listarVip();                 return true;
            case 0:                              return false;
            default:
                System.out.println(Constantes.ETIQUETA_ERROR + "Opción inválida.");
                return true;
        }
    }

    private void registrarCliente() {
        System.out.println("\n── Registrar Cliente ──");
        try {
            Cliente creado = clienteService.registrarCliente(leerDatosCliente());
            System.out.println(Constantes.ETIQUETA_EXITO + "Cliente #" + creado.getId() + " registrado.");
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void buscarPorId() {
        System.out.print("\nID del cliente: ");
        try {
            clienteService.buscarPorId(leerEntero()).ifPresentOrElse(
                this::imprimirDetalle,
                () -> System.out.println(Constantes.ETIQUETA_ADVERTENCIA + "No encontrado.")
            );
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void buscarPorDocumento() {
        System.out.print("\nDocumento: ");
        String doc = scanner.nextLine().trim();
        try {
            clienteService.buscarPorDocumento(doc).ifPresentOrElse(
                this::imprimirDetalle,
                () -> System.out.println(Constantes.ETIQUETA_ADVERTENCIA + "No encontrado.")
            );
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void listarTodos() {
        List<Cliente> lista = clienteService.listarTodosLosClientes();
        if (lista.isEmpty()) { System.out.println("Sin clientes registrados."); return; }
        System.out.println("\n── Clientes ──");
        lista.forEach(this::imprimirResumen);
    }

    private void actualizarCliente() {
        System.out.print("\nID a actualizar: ");
        int id = leerEntero();
        try {
            clienteService.actualizarCliente(id, leerDatosCliente());
            System.out.println(Constantes.ETIQUETA_EXITO + "Actualizado correctamente.");
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void eliminarCliente() {
        System.out.print("\nID a eliminar: ");
        int id = leerEntero();
        System.out.print("¿Confirmar? (s/n): ");
        if (!"s".equalsIgnoreCase(scanner.nextLine().trim())) { System.out.println("Cancelado."); return; }
        try {
            clienteService.eliminarCliente(id);
            System.out.println(Constantes.ETIQUETA_EXITO + "Eliminado.");
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void listarVip() {
        List<Cliente> vip = clienteService.listarClientesVip();
        if (vip.isEmpty()) { System.out.println("Sin clientes VIP."); return; }
        System.out.println("\n── Clientes VIP ──");
        vip.forEach(this::imprimirResumen);
    }

    private ClienteDTO leerDatosCliente() {
        System.out.print("  Cédula:           "); String cedula    = scanner.nextLine().trim();
        System.out.print("  Primer nombre:    "); String nombre    = scanner.nextLine().trim();
        System.out.print("  Segundo nombre:   "); String sn        = scanner.nextLine().trim();
        System.out.print("  Primer apellido:  "); String apellido  = scanner.nextLine().trim();
        System.out.print("  Segundo apellido: "); String a2        = scanner.nextLine().trim();
        System.out.print("  Email:            "); String email     = scanner.nextLine().trim();
        System.out.print("  Teléfono:         "); String telefono  = scanner.nextLine().trim();
        System.out.print("  Nacionalidad:     "); String nac       = scanner.nextLine().trim();
        System.out.print("  Ciudad origen:    "); String ciudad    = scanner.nextLine().trim();
        return new ClienteDTO(cedula, nombre,
                sn.isEmpty() ? null : sn,
                apellido,
                a2.isEmpty() ? null : a2,
                email, telefono, nac,
                ciudad.isEmpty() ? null : ciudad);
    }

    private void imprimirDetalle(Cliente c) {
        System.out.println("\n  ID: " + c.getId() + " | " + c.obtenerNombreCompleto()
            + " | " + c.getEmail()
            + " | " + (c.isEsVip() ? "★ VIP" : "Standard"));
    }

    private void imprimirResumen(Cliente c) {
        System.out.printf("  [%3d] %-30s | %-25s | %s%n",
            c.getId(), c.obtenerNombreCompleto(), c.getEmail(),
            c.isEsVip() ? "★ VIP" : "");
    }

    private void imprimirEncabezado(String titulo) {
        System.out.println("\n" + Constantes.SEPARADOR_MENU);
        System.out.println("  " + titulo);
        System.out.println(Constantes.SEPARADOR_MENU);
    }

    private int leerEntero() {
        try { return Integer.parseInt(scanner.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }
}

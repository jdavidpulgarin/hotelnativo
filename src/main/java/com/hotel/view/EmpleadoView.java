package com.hotel.view;

import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.Empleado;
import com.hotel.service.EmpleadoService;
import com.hotel.util.Constantes;

import java.util.List;
import java.util.Scanner;

/**
 * Vista consola para gestión de empleados.
 *
 * NUEVO: Esta vista faltaba. Ahora conecta EmpleadoService con la interfaz de usuario.
 * Solo un ADMINISTRADOR puede acceder (verificado via token en MainMenuView).
 *
 * CAPA PRESENTACIÓN: únicamente Scanner y System.out. Sin lógica de negocio.
 * SOLID: S – responsabilidad única: I/O de usuario para empleados.
 */
public class EmpleadoView {

    private final EmpleadoService empleadoService;
    private final Scanner         scanner;

    public EmpleadoView(EmpleadoService empleadoService, Scanner scanner) {
        this.empleadoService = empleadoService;
        this.scanner         = scanner;
    }

    public void mostrarMenu() {
        boolean continuar = true;
        while (continuar) {
            imprimirEncabezado("GESTIÓN DE EMPLEADOS");
            System.out.println("  1. Registrar nuevo empleado");
            System.out.println("  2. Listar todos los empleados");
            System.out.println("  3. Buscar por ID");
            System.out.println("  4. Actualizar empleado");
            System.out.println("  5. Eliminar empleado");
            System.out.println("  6. Resetear contraseña");
            System.out.println("  7. Buscar por cargo");
            System.out.println("  0. Volver");
            System.out.print("  Opción: ");
            continuar = ejecutarOpcion(leerEntero());
        }
    }

    private boolean ejecutarOpcion(int opcion) {
        switch (opcion) {
            case 1: registrarEmpleado();   return true;
            case 2: listarTodos();         return true;
            case 3: buscarPorId();         return true;
            case 4: actualizarEmpleado();  return true;
            case 5: eliminarEmpleado();    return true;
            case 6: resetearPassword();    return true;
            case 7: buscarPorCargo();      return true;
            case 0:                        return false;
            default:
                System.out.println(Constantes.ETIQUETA_ERROR + "Opción inválida.");
                return true;
        }
    }

    private void registrarEmpleado() {
        System.out.println("\n── Registrar Empleado ──");
        System.out.print("  Cédula:             "); String cedula        = scanner.nextLine().trim();
        System.out.print("  Primer nombre:      "); String nombre        = scanner.nextLine().trim();
        System.out.print("  Segundo nombre:     "); String segNombre     = scanner.nextLine().trim();
        System.out.print("  Primer apellido:    "); String apellido      = scanner.nextLine().trim();
        System.out.print("  Segundo apellido:   "); String apellido2     = scanner.nextLine().trim();
        System.out.print("  Email:              "); String email         = scanner.nextLine().trim();
        System.out.print("  Teléfono:           "); String telefono      = scanner.nextLine().trim();
        System.out.print("  ID Cargo (1=Recep 2=Gerente 3=Mant): "); int idCargo = leerEntero();
        System.out.print("  Contraseña inicial: "); String pass          = scanner.nextLine().trim();
        try {
            Empleado creado = empleadoService.registrarEmpleado(
                    cedula, nombre, segNombre, apellido, apellido2, email, telefono, idCargo, pass,
                    0, "TC01", "TP01", null);
            System.out.println(Constantes.ETIQUETA_EXITO
                    + "Empleado #" + creado.getId() + " registrado.");
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void listarTodos() {
        List<Empleado> lista = empleadoService.listarTodosLosEmpleados();
        if (lista.isEmpty()) { System.out.println("Sin empleados registrados."); return; }
        System.out.println("\n── Empleados ──");
        lista.forEach(e -> System.out.printf("  [%3d] %-30s | %-20s | %s%n",
                e.getId(), e.obtenerNombreCompleto(), e.getEmail(),
                e.getCargo() != null ? e.getCargo().getNombreCargo() : "Sin cargo"));
    }

    private void buscarPorId() {
        System.out.print("\n  ID Empleado: "); int id = leerEntero();
        try {
            empleadoService.buscarPorId(id).ifPresentOrElse(
                e -> System.out.printf("  [%d] %s | %s | Cargo: %s%n",
                        e.getId(), e.obtenerNombreCompleto(), e.getEmail(),
                        e.getCargo() != null ? e.getCargo().getNombreCargo() : "Sin cargo"),
                () -> System.out.println(Constantes.ETIQUETA_ADVERTENCIA + "No encontrado.")
            );
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void actualizarEmpleado() {
        System.out.print("\n  ID a actualizar:    "); int id           = leerEntero();
        System.out.print("  Primer nombre:      "); String nombre     = scanner.nextLine().trim();
        System.out.print("  Segundo nombre:     "); String segNombre  = scanner.nextLine().trim();
        System.out.print("  Primer apellido:    "); String apellido   = scanner.nextLine().trim();
        System.out.print("  Segundo apellido:   "); String apellido2  = scanner.nextLine().trim();
        System.out.print("  Email:              "); String email      = scanner.nextLine().trim();
        System.out.print("  Teléfono:           "); String tel        = scanner.nextLine().trim();
        System.out.print("  ID Cargo:           "); int cargo         = leerEntero();
        try {
            empleadoService.actualizarEmpleado(id, nombre, segNombre, apellido, apellido2, email, tel, cargo,
                    null, 0, "INDEFINIDO", "MENSUAL", null);
            System.out.println(Constantes.ETIQUETA_EXITO + "Empleado actualizado.");
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void eliminarEmpleado() {
        System.out.print("\n  ID a eliminar: "); int id = leerEntero();
        System.out.print("  ¿Confirmar? (s/n): ");
        if (!"s".equalsIgnoreCase(scanner.nextLine().trim())) {
            System.out.println("Cancelado."); return;
        }
        try {
            empleadoService.eliminarEmpleado(id);
            System.out.println(Constantes.ETIQUETA_EXITO + "Empleado eliminado.");
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void resetearPassword() {
        System.out.print("\n  ID Empleado:       "); int id = leerEntero();
        System.out.print("  Nueva contraseña: ");   String pass = scanner.nextLine().trim();
        try {
            empleadoService.resetearPassword(id, pass);
            System.out.println(Constantes.ETIQUETA_EXITO + "Contraseña actualizada.");
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void buscarPorCargo() {
        System.out.print("\n  ID Cargo: "); int idCargo = leerEntero();
        try {
            List<Empleado> lista = empleadoService.buscarPorCargo(idCargo);
            if (lista.isEmpty()) { System.out.println("Sin empleados con ese cargo."); return; }
            lista.forEach(e -> System.out.printf("  [%3d] %s | %s%n",
                    e.getId(), e.obtenerNombreCompleto(), e.getEmail()));
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
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

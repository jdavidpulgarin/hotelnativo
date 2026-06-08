package com.hotel.view;

import com.hotel.dto.HabitacionDTO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.Habitacion;
import com.hotel.service.HabitacionService;
import com.hotel.util.Constantes;
import java.util.List;
import java.util.Scanner;

/** Vista consola para gestión de habitaciones. CAPA PRESENTACIÓN. */
public class HabitacionView {
    private final HabitacionService habitacionService;
    private final Scanner scanner;

    public HabitacionView(HabitacionService habitacionService, Scanner scanner) {
        this.habitacionService = habitacionService;
        this.scanner = scanner;
    }

    public void mostrarMenu() {
        boolean continuar = true;
        while (continuar) {
            System.out.println("\n" + Constantes.SEPARADOR_MENU);
            System.out.println("  GESTIÓN DE HABITACIONES");
            System.out.println(Constantes.SEPARADOR_MENU);
            System.out.println("  1. Registrar habitación");
            System.out.println("  2. Listar todas");
            System.out.println("  3. Buscar por ID");
            System.out.println("  4. Actualizar habitación");
            System.out.println("  5. Enviar a mantenimiento");
            System.out.println("  6. Devolver a servicio");
            System.out.println("  7. Eliminar habitación");
            System.out.println("  0. Volver");
            System.out.print("  Opción: ");
            int op = leerEntero();
            switch (op) {
                case 1: registrar();               break;
                case 2: listarTodas();             break;
                case 3: buscarPorId();             break;
                case 4: actualizar();              break;
                case 5: enviarMantenimiento();     break;
                case 6: devolverServicio();        break;
                case 7: eliminar();                break;
                case 0: continuar = false;         break;
                default: System.out.println(Constantes.ETIQUETA_ERROR + "Opción inválida.");
            }
        }
    }

    private void registrar() {
        System.out.println("\n── Registrar Habitación ──");
        System.out.print("  Número:         "); String numero = scanner.nextLine().trim();
        System.out.print("  Tipo (1=Simple 2=Doble 3=Suite): "); int tipo = leerEntero();
        System.out.print("  ID Piso:        "); int piso = leerEntero();
        System.out.print("  Precio base:    "); double precio = leerDouble();
        try {
            Habitacion h = habitacionService.registrarHabitacion(new HabitacionDTO(numero, tipo, piso, precio));
            System.out.println(Constantes.ETIQUETA_EXITO + "Habitación #" + h.getNumero() + " registrada.");
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void listarTodas() {
        List<Habitacion> lista = habitacionService.listarTodasLasHabitaciones();
        if (lista.isEmpty()) { System.out.println("Sin habitaciones registradas."); return; }
        System.out.println("\n── Habitaciones ──");
        lista.forEach(h -> System.out.printf("  [%-4s] %-6s | Piso %d | %-12s | $%.2f%n",
            h.getNumero(), h.getTipoHabitacion().obtenerEtiquetaTipo(),
            h.getPiso().getNumeroPiso(), h.getEstado(), h.calcularPrecioFinal()));
    }

    private void buscarPorId() {
        System.out.print("\n  Número de habitación: "); String numero = scanner.nextLine().trim();
        habitacionService.buscarPorId(numero).ifPresentOrElse(
            h -> System.out.println("  " + h),
            () -> System.out.println(Constantes.ETIQUETA_ADVERTENCIA + "No encontrada.")
        );
    }

    private void actualizar() {
        System.out.print("\n  Número a actualizar: "); String numero = scanner.nextLine().trim();
        System.out.print("  Tipo (1=Simple 2=Doble 3=Suite): "); int tipo = leerEntero();
        System.out.print("  ID Piso:         "); int piso = leerEntero();
        System.out.print("  Precio base:     "); double precio = leerDouble();
        try {
            habitacionService.actualizarHabitacion(numero, new HabitacionDTO(numero, tipo, piso, precio));
            System.out.println(Constantes.ETIQUETA_EXITO + "Habitación actualizada.");
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void enviarMantenimiento() {
        System.out.print("\n  Número de habitación: "); String numero = scanner.nextLine().trim();
        try {
            habitacionService.enviarHabitacionAMantenimiento(numero);
            System.out.println(Constantes.ETIQUETA_EXITO + "Habitación enviada a mantenimiento.");
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void devolverServicio() {
        System.out.print("\n  Número de habitación: "); String numero = scanner.nextLine().trim();
        try {
            habitacionService.devolverHabitacionAServicio(numero);
            System.out.println(Constantes.ETIQUETA_EXITO + "Habitación disponible nuevamente.");
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private void eliminar() {
        System.out.print("\n  Número a eliminar: "); String numero = scanner.nextLine().trim();
        System.out.print("  ¿Confirmar? (s/n): ");
        if (!"s".equalsIgnoreCase(scanner.nextLine().trim())) { System.out.println("Cancelado."); return; }
        try {
            habitacionService.eliminarHabitacion(numero);
            System.out.println(Constantes.ETIQUETA_EXITO + "Eliminada.");
        } catch (ExcepcionNegocio e) {
            System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
        }
    }

    private int leerEntero() {
        try { return Integer.parseInt(scanner.nextLine().trim()); } catch (NumberFormatException e) { return -1; }
    }
    private double leerDouble() {
        try { return Double.parseDouble(scanner.nextLine().trim()); } catch (NumberFormatException e) { return 0; }
    }
}

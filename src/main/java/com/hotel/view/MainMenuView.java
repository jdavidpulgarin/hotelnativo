package com.hotel.view;

import com.hotel.dao.impl.*;
import com.hotel.model.Empleado;
import com.hotel.service.*;
import com.hotel.util.Constantes;

import java.util.List;
import java.util.Scanner;

/**
 * Punto de entrada de la aplicación — Composition Root.
 *
 * MEJORA 7: opción 10 para checkout automático (solo administradores).
 * CheckInOutView ahora recibe ClienteService para el formulario completo
 * de check-in (MEJORA 4).
 *
 * GRASP: Controlador — único punto de composición.
 * SOLID: D — instancia implementaciones concretas solo aquí.
 */
public class MainMenuView {

    private final Scanner scanner = new Scanner(System.in);

    // ── Servicios ──────────────────────────────────────────────────────────────
    private final AuthService        authService;
    private final ReporteService     reporteService;
    private final CheckInOutService  checkInOutService;

    // ── Vistas ────────────────────────────────────────────────────────────────
    private final ClienteView    clienteView;
    private final ReservaView    reservaView;
    private final HabitacionView habitacionView;
    private final CheckInOutView checkInOutView;
    private final FacturaView    facturaView;
    private final ChatbotView    chatbotView;
    private final EmpleadoView   empleadoView;

    /** Token de sesión del empleado autenticado. */
    private String tokenSesion;

    public MainMenuView() {
        // ── DAOs concretos ──────────────────────────────────────────────────────
        ClienteDAOImpl       clienteDAO       = new ClienteDAOImpl();
        ReservaDAOImpl       reservaDAO       = new ReservaDAOImpl();
        HabitacionDAOImpl    habitacionDAO    = new HabitacionDAOImpl();
        EmpleadoDAOImpl      empleadoDAO      = new EmpleadoDAOImpl();
        FacturaDAOImpl       facturaDAO       = new FacturaDAOImpl();
        CheckInOutDAOImpl    checkInOutDAO    = new CheckInOutDAOImpl();
        MantenimientoDAOImpl mantenimientoDAO = new MantenimientoDAOImpl();
        // ── Services ────────────────────────────────────────────────────────────
        EmailService emailService = new EmailService();

        this.authService = new AuthService();
        cargarCredencialesDesdeDB(empleadoDAO);

        ClienteService    clienteService    = new ClienteService(clienteDAO, clienteDAO, reservaDAO);
        ReservaService    reservaService    = new ReservaService(reservaDAO, reservaDAO,
                                                habitacionDAO, clienteDAO, emailService);
        HabitacionService habitacionService = new HabitacionService(habitacionDAO);

        this.checkInOutService = new CheckInOutService(checkInOutDAO, reservaDAO, empleadoDAO);

        FacturaService    facturaService    = new FacturaService(facturaDAO, reservaDAO);
        GeminiApiService geminiApiService = new GeminiApiService(leerApiKeyGemini());
        ChatbotService chatbotService = new ChatbotService(habitacionDAO, reservaDAO, reservaDAO, clienteDAO, 
                   facturaDAO, empleadoDAO, mantenimientoDAO, geminiApiService);
        this.reporteService = new ReporteService(facturaDAO, reservaDAO, reservaDAO);

        EmpleadoService empleadoService = new EmpleadoService(empleadoDAO, this.authService);

        // ── Vistas ──────────────────────────────────────────────────────────────
        this.clienteView    = new ClienteView(clienteService, scanner);
        this.reservaView    = new ReservaView(reservaService, scanner);
        this.habitacionView = new HabitacionView(habitacionService, scanner);
        // MEJORA 4: CheckInOutView recibe ClienteService para búsqueda/registro de huéspedes
        this.checkInOutView = new CheckInOutView(checkInOutService, clienteService, scanner);
        this.facturaView    = new FacturaView(facturaService, scanner);
        this.chatbotView    = new ChatbotView(chatbotService, scanner);
        this.empleadoView   = new EmpleadoView(empleadoService, scanner);
    }

    // ── Arranque ──────────────────────────────────────────────────────────────
    

    public void iniciar() {
        System.out.println(Constantes.SEPARADOR_MENU);
        System.out.println("           HOTEL NATIVO");
        System.out.println("  Sistema de Gestión Hotelera");
        System.out.println(Constantes.SEPARADOR_MENU);

        if (!realizarLogin()) {
            System.out.println("No se pudo autenticar. Sistema cerrado.");
            return;
        }

        boolean continuar = true;
        while (continuar) {
            imprimirMenuPrincipal();
            continuar = ejecutarOpcion(leerEntero());
        }

        if (tokenSesion != null) authService.logout(tokenSesion);
        com.hotel.util.ConexionBaseDatos.obtenerInstancia().cerrarPool();
        System.out.println("Sesión cerrada. ¡Hasta pronto!");
        scanner.close();
    }

    // ── Carga de credenciales desde BD ────────────────────────────────────────

    private void cargarCredencialesDesdeDB(EmpleadoDAOImpl empleadoDAO) {
        try {
            List<Empleado> empleados = empleadoDAO.listarTodos();
            empleados.forEach(emp -> {
                try {
                    authService.registrarCredenciales(emp, emp.getEmail());
                } catch (Exception ignored) {}
            });
            System.out.println("[AUTH] " + empleados.size() + " empleado(s) cargados correctamente.");
        } catch (Exception e) {
            System.out.println("[AUTH] Error al cargar credenciales: " + e.getMessage());
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    private boolean realizarLogin() {
        System.out.println("\n  INICIO DE SESIÓN");
        System.out.println(Constantes.SEPARADOR_MENU);
        for (int i = 0; i < 3; i++) {
            System.out.print("  Email:      "); String email = scanner.nextLine().trim();
            System.out.print("  Contraseña: "); String pass  = scanner.nextLine().trim();
            try {
                tokenSesion = authService.login(email, pass);
                System.out.println("\n" + Constantes.ETIQUETA_EXITO + "Sesión iniciada correctamente.\n");
                return true;
            } catch (AuthService.AuthException e) {
                System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
                if (e.getCodigo().equals("CUENTA_BLOQUEADA")) return false;
            }
        }
        return false;
    }
    private String leerApiKeyGemini() {
    String envKey = System.getenv("GEMINI_API_KEY");
    if (envKey != null && !envKey.isBlank()) return envKey;
    try (java.io.InputStream is = new java.io.FileInputStream("config/chatbot.properties")) {
        java.util.Properties props = new java.util.Properties();
        props.load(is);
        return props.getProperty("gemini.api.key", "");
    } catch (Exception e) {
        return "";
    }
}

    // ── Menú principal ────────────────────────────────────────────────────────

    private void imprimirMenuPrincipal() {
        authService.obtenerEmpleadoActual(tokenSesion).ifPresent(emp ->
            System.out.println("\n  Usuario: " + emp.obtenerNombreCompleto()
                    + " | " + (emp.getCargo() != null ? emp.getCargo().getNombreCargo() : ""))
        );
        System.out.println("\n" + Constantes.SEPARADOR_MENU);
        System.out.println("  MENÚ PRINCIPAL — HOTEL NATIVO");
        System.out.println(Constantes.SEPARADOR_MENU);
        System.out.println("  1.  Gestión de Clientes");
        System.out.println("  2.  Gestión de Reservas");
        System.out.println("  3.  Gestión de Habitaciones");
        System.out.println("  4.  Check-in / Check-out");
        System.out.println("  5.  Facturación");
        System.out.println("  6.  Asistente Virtual");
        System.out.println("  7.  Reportes y Documentos");
        System.out.println("  8.  Gestión de Empleados");
        System.out.println("  9.  Cerrar sesión");
        System.out.println("  10. Checkout Automático 11:00 AM  [ADMIN]");
        System.out.println("  0.  Salir del sistema");
        System.out.print("  Opción: ");
    }

    private boolean ejecutarOpcion(int opcion) {
        try {
            switch (opcion) {
                case 1:  clienteView.mostrarMenu();       return true;
                case 2:  reservaView.mostrarMenu();       return true;
                case 3:  habitacionView.mostrarMenu();    return true;
                case 4:  checkInOutView.mostrarMenu();    return true;
                case 5:  facturaView.mostrarMenu();       return true;
                case 6:  chatbotView.iniciarChat();       return true;
                case 7:  mostrarMenuReportes();           return true;
                case 8:  mostrarMenuEmpleados();          return true;
                case 9:  return realizarLogoutYRelogin();
                case 10: ejecutarCheckoutAutomatico();    return true;
                case 0:  return false;
                default:
                    System.out.println(Constantes.ETIQUETA_ERROR + "Opción inválida.");
                    return true;
            }
        } catch (AuthService.AuthException e) {
            System.out.println(Constantes.ETIQUETA_ERROR + "Acceso denegado: " + e.getMessage());
            return true;
        }
    }

    // ── Checkout automático (MEJORA 7) ────────────────────────────────────────

    private void ejecutarCheckoutAutomatico() throws AuthService.AuthException {
        authService.verificarPermiso(tokenSesion, "CREAR_EMPLEADO"); // permiso de admin
        System.out.println("\n  Procesando checkouts automáticos para hoy...");
        int procesados = checkInOutService.procesarCheckoutsAutomaticos();
        System.out.println(Constantes.ETIQUETA_EXITO
                + procesados + " checkout(s) automático(s) procesado(s).");
    }

    // ── Menú reportes ─────────────────────────────────────────────────────────

    private void mostrarMenuReportes() throws AuthService.AuthException {
        authService.verificarPermiso(tokenSesion, "VER_REPORTES");
        System.out.println("\n" + Constantes.SEPARADOR_MENU);
        System.out.println("  REPORTES Y DOCUMENTOS");
        System.out.println(Constantes.SEPARADOR_MENU);
        System.out.println("  1. Generar factura HTML");
        System.out.println("  2. Reporte de ocupación mensual");
        System.out.println("  0. Volver");
        System.out.print("  Opción: ");
        int op = leerEntero();
        switch (op) {
            case 1:
                System.out.print("  ID de la factura: ");
                try {
                    String ruta = reporteService.guardarFacturaHTML(leerEntero());
                    System.out.println(Constantes.ETIQUETA_EXITO + "Factura guardada en: " + ruta);
                } catch (Exception e) {
                    System.out.println(Constantes.ETIQUETA_ERROR + e.getMessage());
                }
                break;
            case 2:
                System.out.print("  Año: ");        int anio = leerEntero();
                System.out.print("  Mes (1-12): "); int mes  = leerEntero();
                String ruta = reporteService.guardarReporteOcupacion(anio, mes);
                System.out.println(Constantes.ETIQUETA_EXITO + "Reporte guardado en: " + ruta);
                break;
            default: break;
        }
    }

    // ── Menú empleados ────────────────────────────────────────────────────────

    private void mostrarMenuEmpleados() throws AuthService.AuthException {
        authService.verificarPermiso(tokenSesion, "CREAR_EMPLEADO");
        empleadoView.mostrarMenu();
    }

    // ── Logout / Relogin ──────────────────────────────────────────────────────

    private boolean realizarLogoutYRelogin() {
        authService.logout(tokenSesion);
        tokenSesion = null;
        System.out.println(Constantes.ETIQUETA_EXITO + "Sesión cerrada.\n");
        return realizarLogin();
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private int leerEntero() {
        try { return Integer.parseInt(scanner.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    public static void main(String[] args) {
        new MainMenuView().iniciar();
    }
}
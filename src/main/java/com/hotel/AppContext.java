package com.hotel;

import com.hotel.dao.impl.*;
import com.hotel.dao.interfaces.*;
import com.hotel.model.Empleado;
import com.hotel.service.*;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Contexto global de la aplicación. Patrón Service Locator: instancia única de
 * todos los servicios, accesible desde cualquier controlador JavaFX sin
 * re-crear DAOs.
 *
 * Se inicializa UNA sola vez al arrancar y se reutiliza toda la sesión. Evita
 * crear N conexiones por cada pantalla (problema de rendimiento).
 */
public class AppContext {

    // volatile garantiza visibilidad entre hilos y evita el broken DCL
    private static volatile AppContext instance;

    // ── DAOs ──────────────────────────────────────────────────────────────────
    private final IClienteDAO clienteDAO;
    private final IClienteBusqueda clienteBusqueda;
    private final IReservaDAO reservaDAO;
    private final IReservaBusqueda reservaBusqueda;
    private final IHabitacionDAO habitacionDAO;
    private final IEmpleadoDAO empleadoDAO;
    private final IFacturaDAO facturaDAO;
    private final ICheckInOutDAO checkInOutDAO;
    private final IMantenimientoDAO mantenimientoDAO;
    private final IAuditoriaDAO auditoriaDAO;

    // ── Servicios ─────────────────────────────────────────────────────────────
    private final AuthService authService;
    private final ClienteService clienteService;
    private final ReservaService reservaService;
    private final HabitacionService habitacionService;
    private final CheckInOutService checkInOutService;
    private final FacturaService facturaService;
    private final MantenimientoService mantenimientoService;
    private final ReporteService reporteService;
    private final PdfReporteService pdfReporteService;
    private final EmpleadoService empleadoService;
    private final EmailService emailService;
    private final AuditoriaService auditoriaService;
    private final ChatbotService chatbotService;
    private final GeminiApiService geminiApiService;

    /**
     * Se completa cuando todas las credenciales están cargadas desde la BD.
     */
    private final CompletableFuture<Void> credencialesCargadas = new CompletableFuture<>();

    /**
     * Token de sesión activo (volatile: se escribe en hilo de login, se lee en
     * hilo JavaFX).
     */
    private volatile String tokenSesion;
    /**
     * Empleado logueado actualmente (volatile: misma razón que tokenSesion).
     */
    private volatile Empleado empleadoActual;

    private AppContext() {
        // Instanciar DAOs (una instancia concreta expuesta vía múltiples interfaces)
        ClienteDAOImpl clienteImpl = new ClienteDAOImpl();
        clienteDAO = clienteImpl;
        clienteBusqueda = clienteImpl;

        ReservaDAOImpl reservaImpl = new ReservaDAOImpl();
        reservaDAO = reservaImpl;
        reservaBusqueda = reservaImpl;

        habitacionDAO = new HabitacionDAOImpl();
        empleadoDAO = new EmpleadoDAOImpl();
        facturaDAO = new FacturaDAOImpl();
        checkInOutDAO = new CheckInOutDAOImpl();
        mantenimientoDAO = new MantenimientoDAOImpl();
        auditoriaDAO = new AuditoriaDAOImpl();

        // Instanciar servicios
        emailService = new EmailService();
        authService = new AuthService();
        auditoriaService = new AuditoriaService(auditoriaDAO);
        clienteService = new ClienteService(clienteDAO, clienteBusqueda, reservaBusqueda);
        reservaService = new ReservaService(reservaDAO, reservaBusqueda,
                habitacionDAO, clienteDAO, emailService);
        habitacionService = new HabitacionService(habitacionDAO);
        checkInOutService = new CheckInOutService(checkInOutDAO, reservaDAO, empleadoDAO);
        facturaService = new FacturaService(facturaDAO, reservaDAO);
        mantenimientoService = new MantenimientoService(mantenimientoDAO,
                habitacionDAO, empleadoDAO, reservaBusqueda, emailService);
        reporteService = new ReporteService(facturaDAO, reservaDAO, reservaBusqueda);
        pdfReporteService = new PdfReporteService(facturaDAO, reservaDAO, reservaBusqueda);
        empleadoService = new EmpleadoService(empleadoDAO, authService);
        geminiApiService = new GeminiApiService(leerApiKeyGemini());
        chatbotService = new ChatbotService(habitacionDAO, reservaDAO, reservaBusqueda, clienteDAO,
                facturaDAO, empleadoDAO, mantenimientoDAO, geminiApiService);

        // Precargar credenciales en hilo daemon para no impedir el cierre de la JVM
        Thread tCredenciales = new Thread(this::cargarCredenciales, "init-credenciales");
        tCredenciales.setDaemon(true);
        tCredenciales.start();
    }

    public static AppContext getInstance() {
        if (instance == null) {
            synchronized (AppContext.class) {
                if (instance == null) {
                    instance = new AppContext();
                }
            }
        }
        return instance;
    }

    private void cargarCredenciales() {
        try {
            List<Empleado> empleados = empleadoDAO.listarTodos();
            empleados.forEach(emp -> {
                try {
                    String hash = emp.getHashContrasena();
                    if (hash != null && !hash.isBlank()) {
                        // Hash ya persistido en BD: cargar directamente sin re-hashear.
                        // Si el hash sigue siendo el email inicial y el flag está en 0
                        // (bug de migración con DEFAULT 0), re-activar el flag.
                        if (!emp.isDebeCambiarContrasena()
                                && hash.startsWith("$2")
                                && BCrypt.checkpw(emp.getEmail(), hash)) {
                            emp.setDebeCambiarContrasena(true);
                            empleadoDAO.actualizarDebeCambiarPassword(emp.getId(), true);
                            System.out.println("[APP] Contraseña inicial detectada, flag reactivado: "
                                    + emp.getEmail());
                        }
                        authService.registrarCredencialesConHash(emp, hash);
                    } else {
                        // Primer arranque o empleado sin hash: usar email como contraseña inicial,
                        // guardar el hash en BD y marcar que debe cambiar la contraseña.
                        String hashInicial = authService.generarHash(emp.getEmail());
                        emp.setDebeCambiarContrasena(true);
                        authService.registrarCredencialesConHash(emp, hashInicial);
                        empleadoDAO.actualizarPasswordHash(emp.getId(), hashInicial);
                        empleadoDAO.actualizarDebeCambiarPassword(emp.getId(), true);
                        System.out.println("[APP] Hash inicial creado - ID: " + emp.getId());
                    }
                } catch (Exception e) {
                    System.err.println("[APP] Error cargando credenciales - ID: " + emp.getId()
                            + ": " + e.getMessage());
                }
            });
            System.out.println("[APP] " + empleados.size() + " empleado(s) cargados.");
            credencialesCargadas.complete(null);
        } catch (Exception e) {
            System.err.println("[APP] Error cargando credenciales: " + e.getMessage());
            credencialesCargadas.completeExceptionally(e);
        }
    }

    public CompletableFuture<Void> getCredencialesCargadas() {
        return credencialesCargadas;
    }

    private String leerApiKeyGemini() {
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey;
        }
        try (java.io.InputStream is = new java.io.FileInputStream("config/chatbot.properties")) {
            java.util.Properties props = new java.util.Properties();
            props.load(is);
            return props.getProperty("gemini.api.key", "");
        } catch (Exception e) {
            return "";
        }
    }

    // ── Getters de servicios ──────────────────────────────────────────────────
    public AuthService getAuthService() {
        return authService;
    }

    public EmailService getEmailService() {
        return emailService;
    }

    public ClienteService getClienteService() {
        return clienteService;
    }

    public ReservaService getReservaService() {
        return reservaService;
    }

    public HabitacionService getHabitacionService() {
        return habitacionService;
    }

    public CheckInOutService getCheckInOutService() {
        return checkInOutService;
    }

    public FacturaService getFacturaService() {
        return facturaService;
    }

    public MantenimientoService getMantenimientoService() {
        return mantenimientoService;
    }

    public ReporteService getReporteService() {
        return reporteService;
    }

    public PdfReporteService getPdfReporteService() {
        return pdfReporteService;
    }

    public EmpleadoService getEmpleadoService() {
        return empleadoService;
    }

    public AuditoriaService getAuditoriaService() {
        return auditoriaService;
    }

    public ChatbotService getChatbotService() {
        return chatbotService;
    }

    public GeminiApiService getGeminiApiService() {
        return geminiApiService;
    }

    // ── Sesión ────────────────────────────────────────────────────────────────
    public String getTokenSesion() {
        return tokenSesion;
    }

    public void setTokenSesion(String token) {
        this.tokenSesion = token;
    }

    public Empleado getEmpleadoActual() {
        return empleadoActual;
    }

    public void setEmpleadoActual(Empleado e) {
        this.empleadoActual = e;
    }
}

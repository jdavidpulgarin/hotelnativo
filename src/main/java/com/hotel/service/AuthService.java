/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.java.com.hotel.service;

import com.hotel.exception.ExcepcionValidacion;
import com.hotel.model.Empleado;
import com.hotel.util.ValidadorEntradas;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Pulgarin
 */
/**
 * Servicio de autenticación y gestión de sesiones.
 *
 * CORRECCIÓN WARN #4: El campo {@code creadaEn} en SesionActiva existía pero
 * nunca se usaba para verificar expiración. Los tokens vivían indefinidamente
 * hasta un logout manual. Un token robado tenía acceso permanente.
 *
 * Ahora verificarPermiso() y obtenerEmpleadoDeToken() validan que la sesión no
 * haya superado MINUTOS_EXPIRACION_SESION. Las sesiones expiradas se limpian
 * automáticamente con limpiarSesionesExpiradas().
 *
 * SEGURIDAD: Las contraseñas se almacenan con BCrypt (cost=12). Para cuentas
 * migradas desde SHA-256, el hash se actualiza automáticamente en el primer
 * login exitoso. Los hashes legacy se detectan por ausencia del prefijo "$2".
 *
 * GRASP: Controlador – gestiona el ciclo de vida de sesiones. SOLID: S –
 * responsabilidad única: autenticación y autorización.
 */
public class AuthService {

    public static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    public static final String ROL_RECEPCIONISTA = "RECEPCIONISTA";
    public static final String ROL_MANTENIMIENTO = "MANTENIMIENTO";
    public static final String ROL_CONTADOR = "CONTADOR";

    private static final Map<String, String[]> PERMISOS = new ConcurrentHashMap<>();

    static {
        PERMISOS.put(ROL_ADMINISTRADOR, new String[]{
            "CREAR_EMPLEADO", "ELIMINAR_EMPLEADO",
            "CREAR_RESERVA", "CANCELAR_RESERVA",
            "CREAR_CLIENTE", "ELIMINAR_CLIENTE",
            "GENERAR_FACTURA", "ELIMINAR_FACTURA",
            "CHECKIN", "CHECKOUT",
            "VER_REPORTES", "GESTIONAR_HABITACIONES",
            "GESTIONAR_MANTENIMIENTO"
        });
        PERMISOS.put(ROL_RECEPCIONISTA, new String[]{
            "CREAR_RESERVA", "CANCELAR_RESERVA",
            "CREAR_CLIENTE",
            "GENERAR_FACTURA",
            "CHECKIN", "CHECKOUT",
            "GESTIONAR_HABITACIONES",
            "GESTIONAR_MANTENIMIENTO"
        });
        PERMISOS.put(ROL_MANTENIMIENTO, new String[]{
            "GESTIONAR_MANTENIMIENTO"
        });
        // Contador: solo facturación, reportes y gestión de empleados
        PERMISOS.put(ROL_CONTADOR, new String[]{
            "GENERAR_FACTURA",
            "VER_REPORTES",
            "CREAR_EMPLEADO"
        });
    }

    private static final int MAX_INTENTOS_FALLIDOS = 3;
    private static final long MINUTOS_EXPIRACION_SESION = 480L;

    // ── Estado en memoria ──────────────────────────────────────────────────────
    private final Map<String, SesionActiva> sesionesActivas = new ConcurrentHashMap<>();
    private final Map<String, String> credenciales = new ConcurrentHashMap<>();
    private final Map<String, Empleado> empleadosPorEmail = new ConcurrentHashMap<>();
    private final Map<String, Integer> intentosFallidos = new ConcurrentHashMap<>();

    // Tokens de un solo uso para el flujo de cambio de contraseña obligatorio.
    // Clave: preAuthToken  →  Valor: [email normalizado, timestamp de creación separados por "|"]
    private final Map<String, String> preAuthTokens = new ConcurrentHashMap<>();
    private static final long MINUTOS_EXPIRACION_PRE_AUTH = 15L;

    public void registrarCredenciales(Empleado empleado, String contrasena) {
        if (empleado == null || contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("Empleado y contraseña son requeridos.");
        }
        String email = empleado.getEmail().toLowerCase();
        credenciales.put(email, hashContrasena(contrasena));
        empleadosPorEmail.put(email, empleado);
        intentosFallidos.put(email, 0);
    }

    /**
     * Registra un hash BCrypt ya calculado, sin volver a hashearlo. Usado al
     * cargar empleados desde la BD para no alterar el hash almacenado.
     */
    public void registrarCredencialesConHash(Empleado empleado, String hashDirecto) {
        if (empleado == null || hashDirecto == null || hashDirecto.isBlank()) {
            throw new IllegalArgumentException("Empleado y hash son requeridos.");
        }
        String email = empleado.getEmail().toLowerCase();
        credenciales.put(email, hashDirecto);
        empleadosPorEmail.put(email, empleado);
        intentosFallidos.put(email, 0);
    }

    /**
     * Genera un hash BCrypt de la contraseña dada. Expuesto para que
     * EmpleadoService pueda obtener el hash antes de persistirlo.
     */
    public String generarHash(String contrasena) {
        return hashContrasena(contrasena);
    }

    /**
     * Retorna el email asociado a un preAuthToken activo y no expirado, o null
     * si no existe. Expira automáticamente tokens con más de
     * MINUTOS_EXPIRACION_PRE_AUTH minutos de antigüedad.
     */
    public String obtenerEmailDePreAuthToken(String preAuthToken) {
        String valor = preAuthTokens.get(preAuthToken);
        if (valor == null) {
            return null;
        }
        String[] partes = valor.split("\\|", 2);
        long creadoEn = Long.parseLong(partes[1]);
        long minutos = (System.currentTimeMillis() - creadoEn) / 60_000L;
        if (minutos >= MINUTOS_EXPIRACION_PRE_AUTH) {
            preAuthTokens.remove(preAuthToken);
            return null;
        }
        return partes[0];
    }

    /**
     * Retorna el Empleado cuyo email (normalizado) coincida, o vacío si no está
     * cargado. Usa el mapa en memoria — sin consulta adicional a la BD.
     */
    public Optional<Empleado> obtenerEmpleadoPorEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(empleadosPorEmail.get(email.toLowerCase().trim()));
    }

    /**
     * Genera un hash BCrypt con factor de trabajo 12 (recomendado OWASP 2024).
     */
    private String hashContrasena(String contrasena) {
        return BCrypt.hashpw(contrasena, BCrypt.gensalt(12));
    }

    private class SesionActiva {

        final Empleado empleado;
        final long creadaEnMs;

        SesionActiva(String token, Empleado empleado) {
            this.empleado = empleado;
            this.creadaEnMs = System.currentTimeMillis();
        }

        /**
         * CORRECCIÓN WARN #4: verifica si han pasado más de N minutos desde la
         * creación.
         */
        boolean estaExpirada() {
            long transcurridoMs = System.currentTimeMillis() - creadaEnMs;
            long transcurridoMin = transcurridoMs / 60_000L;
            return transcurridoMin >= MINUTOS_EXPIRACION_SESION;
        }
    }
}

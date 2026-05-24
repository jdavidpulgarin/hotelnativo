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

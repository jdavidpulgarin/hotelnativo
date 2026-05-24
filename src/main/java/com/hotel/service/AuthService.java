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

    public String login(String email, String contrasena) throws AuthException {
        String emailNorm = email.toLowerCase().trim();

        if (!credenciales.containsKey(emailNorm)) {
            throw new AuthException("CREDENCIALES_INVALIDAS", "Credenciales incorrectas.");
        }

        int intentos = intentosFallidos.getOrDefault(emailNorm, 0);
        if (intentos >= MAX_INTENTOS_FALLIDOS) {
            throw new AuthException("CUENTA_BLOQUEADA",
                    "Cuenta bloqueada por " + MAX_INTENTOS_FALLIDOS
                    + " intentos fallidos. Contacte al administrador.");
        }

        String hashAlmacenado = credenciales.get(emailNorm);
        if (!verificarContrasena(contrasena, hashAlmacenado)) {
            int nuevoIntentos = intentos + 1;
            intentosFallidos.put(emailNorm, nuevoIntentos);
            // Delay exponencial para dificultar fuerza bruta (máx 8 s)
            try {
                Thread.sleep(Math.min(1000L * nuevoIntentos, 8000L));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            int restantes = MAX_INTENTOS_FALLIDOS - nuevoIntentos;
            throw new AuthException("CREDENCIALES_INVALIDAS",
                    restantes > 0
                            ? "Contraseña incorrecta. Intentos restantes: " + restantes
                            : "Cuenta bloqueada. Contacte al administrador.");
        }

        // Migración gradual: si el hash es SHA-256 legacy, actualizar a BCrypt
        if (!esBCrypt(hashAlmacenado)) {
            credenciales.put(emailNorm, hashContrasena(contrasena));
        }

        intentosFallidos.put(emailNorm, 0);
        limpiarSesionesExpiradas();

        Empleado empleado = empleadosPorEmail.get(emailNorm);

        // Verificar si el empleado debe cambiar su contraseña en este primer acceso
        if (empleado.isDebeCambiarContrasena()) {
            limpiarPreAuthTokensExpirados();
            String preAuthToken = UUID.randomUUID().toString();
            // Valor: "email|timestampMs" para detectar expiración sin estructura extra
            preAuthTokens.put(preAuthToken, emailNorm + "|" + System.currentTimeMillis());
            throw new AuthException("CAMBIO_PASSWORD_REQUERIDO",
                    "Debe cambiar su contraseña antes de continuar.",
                    preAuthToken);
        }

        String token = UUID.randomUUID().toString();
        sesionesActivas.put(token, new SesionActiva(token, empleado));

        System.out.println("[AUTH] Login exitoso - ID: " + empleado.getId()
                + " | Rol: " + obtenerRol(empleado));
        return token;
    }

    public void logout(String token) {
        SesionActiva sesion = sesionesActivas.remove(token);
        if (sesion != null) {
            System.out.println("[AUTH] Logout - ID: " + sesion.empleado.getId());
        }
    }

    private boolean verificarContrasena(String contrasenaPlana, String hashAlmacenado) {
        if (hashAlmacenado == null) {
            return false;
        }
        if (esBCrypt(hashAlmacenado)) {
            try {
                return BCrypt.checkpw(contrasenaPlana, hashAlmacenado);
            } catch (Exception e) {
                return false;
            }
        }
        return hashAlmacenado.equals(hashSHA256Legacy(contrasenaPlana));
    }

    private boolean esBCrypt(String hash) {
        return hash != null && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }

    private String hashSHA256Legacy(String contrasena) {
        try {
            java.security.MessageDigest md
                    = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(
                    contrasena.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }

    private void limpiarSesionesExpiradas() {
        sesionesActivas.entrySet().removeIf(e -> e.getValue().estaExpirada());
    }

    private void limpiarPreAuthTokensExpirados() {
        long ahora = System.currentTimeMillis();
        preAuthTokens.entrySet().removeIf(e -> {
            String[] p = e.getValue().split("\\|", 2);
            if (p.length < 2) {
                return true;
            }
            long minutos = (ahora - Long.parseLong(p[1])) / 60_000L;
            return minutos >= MINUTOS_EXPIRACION_PRE_AUTH;
        });
    }

    private String obtenerRol(Empleado empleado) {
        if (empleado.getCargo() == null) {
            return "";
        }
        String raw = empleado.getCargo().getNombreCargo();
        if (raw == null) {
            return "";
        }
        String nombreCargo = raw
                .toUpperCase()
                .replace("Á", "A").replace("É", "E").replace("Í", "I")
                .replace("Ó", "O").replace("Ú", "U");
        if (nombreCargo.contains("ADMIN")) {
            return ROL_ADMINISTRADOR;
        }
        if (nombreCargo.contains("GEREN")) {
            return ROL_ADMINISTRADOR;
        }
        if (nombreCargo.contains("DIRECTOR")) {
            return ROL_ADMINISTRADOR;
        }
        if (nombreCargo.contains("CONTAD")) {
            return ROL_CONTADOR;
        }
        if (nombreCargo.contains("RECEP")) {
            return ROL_RECEPCIONISTA;
        }
        if (nombreCargo.contains("MANTEN")) {
            return ROL_MANTENIMIENTO;
        }
        if (nombreCargo.contains("TECNICO")) {
            return ROL_MANTENIMIENTO;
        }
        if (nombreCargo.contains("ELECTRI")) {
            return ROL_MANTENIMIENTO;
        }
        if (nombreCargo.contains("PLOME")) {
            return ROL_MANTENIMIENTO;
        }
        return nombreCargo;
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

    // ── Cambio de contraseña obligatorio ──────────────────────────────────────
    /**
     * Cambia la contraseña del empleado identificado por el preAuthToken. Solo
     * funciona con tokens generados durante el flujo de primer login.
     *
     * @param preAuthToken token temporal emitido cuando
     * debeCambiarContrasena==true
     * @param contrasenaActual la contraseña actual (para re-verificación)
     * @param contrasenaNueva la nueva contraseña que debe cumplir la política
     * de complejidad
     */
    public void cambiarPassword(String preAuthToken, String contrasenaActual, String contrasenaNueva)
            throws AuthException, ExcepcionValidacion {

        String emailNorm = obtenerEmailDePreAuthToken(preAuthToken);
        if (emailNorm == null) {
            throw new AuthException("TOKEN_INVALIDO",
                    "La sesión de cambio de contraseña es inválida o ha expirado.");
        }

        String hashAlmacenado = credenciales.get(emailNorm);
        if (!verificarContrasena(contrasenaActual, hashAlmacenado)) {
            throw new AuthException("CREDENCIALES_INVALIDAS",
                    "La contraseña actual es incorrecta.");
        }

        // Validar complejidad de la nueva contraseña
        ValidadorEntradas.validarPassword(contrasenaNueva, "nueva contraseña");

        // La nueva contraseña debe ser diferente a la actual
        if (verificarContrasena(contrasenaNueva, hashAlmacenado)) {
            throw new AuthException("PASSWORD_IGUAL",
                    "La nueva contraseña debe ser diferente a la contraseña actual.");
        }

        credenciales.put(emailNorm, hashContrasena(contrasenaNueva));

        Empleado empleado = empleadosPorEmail.get(emailNorm);
        if (empleado != null) {
            empleado.setDebeCambiarContrasena(false);
        }

        preAuthTokens.remove(preAuthToken);
        System.out.println("[AUTH] Contraseña actualizada - ID: "
                + (empleado != null ? empleado.getId() : "desconocido"));
    }

    // ── Autorización ──────────────────────────────────────────────────────────
    /**
     * Verifica permiso y que la sesión no haya expirado.
     *
     * CORRECCIÓN WARN #4: ahora lanza AuthException si la sesión superó
     * MINUTOS_EXPIRACION_SESION minutos desde que fue creada.
     */
    public void verificarPermiso(String token, String accion) throws AuthException {
        Empleado empleado = obtenerEmpleadoDeToken(token);
        String rol = obtenerRol(empleado);

        String[] permisosDelRol = PERMISOS.getOrDefault(rol, new String[0]);
        for (String permiso : permisosDelRol) {
            if (permiso.equalsIgnoreCase(accion)) {
                return;
            }
        }

        throw new AuthException("ACCESO_DENEGADO",
                "El rol '" + rol + "' no tiene permiso para: " + accion);
    }

    public Optional<Empleado> obtenerEmpleadoActual(String token) {
        SesionActiva sesion = sesionesActivas.get(token);
        if (sesion == null || sesion.estaExpirada()) {
            return Optional.empty();
        }
        return Optional.of(sesion.empleado);
    }

    public boolean esSesionValida(String token) {
        if (token == null) {
            return false;
        }
        SesionActiva sesion = sesionesActivas.get(token);
        return sesion != null && !sesion.estaExpirada();
    }

    public void desbloquearCuenta(String tokenAdmin, String emailADesbloquear) throws AuthException {
        verificarPermiso(tokenAdmin, "CREAR_EMPLEADO");
        intentosFallidos.put(emailADesbloquear.toLowerCase(), 0);
        System.out.println("[AUTH] Cuenta desbloqueada por admin.");
    }

    // ── Métodos privados ──────────────────────────────────────────────────────
    /**
     * CORRECCIÓN WARN #4: verifica expiración de la sesión antes de retornar el
     * empleado.
     */
    private Empleado obtenerEmpleadoDeToken(String token) throws AuthException {
        SesionActiva sesion = sesionesActivas.get(token);
        if (sesion == null) {
            throw new AuthException("SESION_INVALIDA",
                    "La sesión no existe o ha expirado. Por favor inicia sesión de nuevo.");
        }
        if (sesion.estaExpirada()) {
            sesionesActivas.remove(token);
            throw new AuthException("SESION_EXPIRADA",
                    "Tu sesión ha expirado después de " + MINUTOS_EXPIRACION_SESION
                    + " minutos. Por favor inicia sesión de nuevo.");
        }
        return sesion.empleado;
    }

}

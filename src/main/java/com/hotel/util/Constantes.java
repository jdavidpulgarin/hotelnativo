/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.util;

/**
 *
 * @author Pulgarin
 */
/**
 * Constantes del sistema hotelero.
 *
 * ACTUALIZACIONES: - MINUTOS_EXPIRACION_SESION: nueva constante para expiración
 * de tokens (Warn #4). - TAMANO_POOL_CONEXIONES: nueva constante para el pool
 * de BD (Bug #2).
 *
 * SOLID: S – responsabilidad única: repositorio de constantes del dominio.
 */
public final class Constantes {

    // ── Impuestos ─────────────────────────────────────────────────────────────
    /**
     * IVA colombiano aplicado a servicios de hotelería
     */
    public static final double TASA_IVA = 0.19;

    // ── Reservas ──────────────────────────────────────────────────────────────
    public static final int DIAS_MAXIMOS_RESERVA = 30;
    public static final int HORAS_LIMITE_CHECKIN = 14;   // 2:00 PM
    public static final int HORAS_LIMITE_CHECKOUT = 12;   // 12:00 PM

    // ── Descuentos ────────────────────────────────────────────────────────────
    public static final double DESCUENTO_CLIENTE_VIP = 0.15;  // 15%
    public static final double DESCUENTO_TEMPORADA_BAJA = 0.10;  // 10%
    // ── Sesiones (NUEVO) ──────────────────────────────────────────────────────
    /**
     * Minutos hasta que una sesión expira automáticamente. 480 = 8 horas (turno
     * laboral completo). Reducir a 60 en entornos de mayor seguridad.
     */
    public static final long MINUTOS_EXPIRACION_SESION = 480L;

    // ── Pool de conexiones (NUEVO) ────────────────────────────────────────────
    /**
     * Número de conexiones en el pool de BD. 5 es suficiente para consola;
     * aumentar si se migra a servidor web.
     */
    public static final int TAMANO_POOL_CONEXIONES = 5;
    /**
     * Segundos máximos de espera para obtener conexión del pool.
     */
    public static final int TIMEOUT_CONEXION_SEGUNDOS = 10;
    // ── Paginación ────────────────────────────────────────────────────────────
    public static final int REGISTROS_POR_PAGINA = 10;

    // ── Menú ──────────────────────────────────────────────────────────────────
    public static final int OPCION_SALIR = 0;
    // ── Mensajes de sistema ───────────────────────────────────────────────────
    public static final String SEPARADOR_MENU = "═══════════════════════════════════════";
    public static final String ETIQUETA_EXITO = "[OK] ";
    public static final String ETIQUETA_ERROR = "[ERROR] ";
    public static final String ETIQUETA_ADVERTENCIA = "[AVISO] ";

    private Constantes() {
    }
}

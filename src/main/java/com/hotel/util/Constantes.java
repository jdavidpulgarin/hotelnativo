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

    private Constantes() {
    }
}

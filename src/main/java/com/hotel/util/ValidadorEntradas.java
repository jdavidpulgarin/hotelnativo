package com.hotel.util;

import com.hotel.exception.ExcepcionValidacion;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Validaciones reutilizables de entrada de datos.
 *
 * GRASP: Fabricación Pura - no existe en el dominio, pero centraliza
 * validaciones evitando duplicación en múltiples servicios. SOLID: S -
 * responsabilidad única: validar entradas.
 */
public final class ValidadorEntradas {

    private static final Pattern PATRON_EMAIL = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PATRON_TELEFONO = Pattern.compile("^[+]?[0-9]{7,15}$");
    private static final int LARGO_MINIMO_NOMBRE = 2;
    private static final int LARGO_MAXIMO_NOMBRE = 100;

    private ValidadorEntradas() {
    }

    public static void validarCampoRequerido(String valor, String nombreCampo)
            throws ExcepcionValidacion {
        if (valor == null || valor.trim().isEmpty()) {
            throw new ExcepcionValidacion(nombreCampo,
                    "El campo '" + nombreCampo + "' es obligatorio y no puede estar vacío.");
        }
    }
}

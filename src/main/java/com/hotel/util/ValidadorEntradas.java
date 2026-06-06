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

    public static void validarFormatoEmail(String email) throws ExcepcionValidacion {
        validarCampoRequerido(email, "email");
        if (!PATRON_EMAIL.matcher(email).matches()) {
            throw new ExcepcionValidacion("email",
                    "El email '" + email + "' no tiene un formato válido.");
        }
    }

    public static void validarFormatoTelefono(String telefono) throws ExcepcionValidacion {
        validarCampoRequerido(telefono, "telefono");
        if (!PATRON_TELEFONO.matcher(telefono).matches()) {
            throw new ExcepcionValidacion("telefono",
                    "El teléfono '" + telefono + "' no tiene un formato válido (7-15 dígitos).");
        }
    }

    public static void validarRangoFechas(LocalDate fechaEntrada, LocalDate fechaSalida)
            throws ExcepcionValidacion {
        if (fechaEntrada == null) {
            throw new ExcepcionValidacion("fechaEntrada", "La fecha de entrada es obligatoria.");
        }
        if (fechaSalida == null) {
            throw new ExcepcionValidacion("fechaSalida", "La fecha de salida es obligatoria.");
        }
        if (!fechaEntrada.isBefore(fechaSalida)) {
            throw new ExcepcionValidacion("fechas",
                    "La fecha de entrada debe ser anterior a la fecha de salida.");
        }
        if (fechaEntrada.isBefore(LocalDate.now())) {
            throw new ExcepcionValidacion("fechaEntrada",
                    "La fecha de entrada no puede ser en el pasado.");
        }
    }
}

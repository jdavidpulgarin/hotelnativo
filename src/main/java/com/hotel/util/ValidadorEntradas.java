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

    public static void validarIdPositivo(int id, String nombreEntidad) throws ExcepcionValidacion {
        if (id <= 0) {
            throw new ExcepcionValidacion("id",
                    "El ID de " + nombreEntidad + " debe ser un número positivo.");
        }
    }

    public static void validarPrecioPositivo(double precio, String nombreCampo)
            throws ExcepcionValidacion {
        if (precio <= 0) {
            throw new ExcepcionValidacion(nombreCampo,
                    "El campo '" + nombreCampo + "' debe ser mayor a cero.");
        }
    }

    public static void validarLargoNombre(String nombre, String nombreCampo)
            throws ExcepcionValidacion {
        validarCampoRequerido(nombre, nombreCampo);
        int largo = nombre.trim().length();
        if (largo < LARGO_MINIMO_NOMBRE || largo > LARGO_MAXIMO_NOMBRE) {
            throw new ExcepcionValidacion(nombreCampo,
                    "El campo '" + nombreCampo + "' debe tener entre "
                    + LARGO_MINIMO_NOMBRE + " y " + LARGO_MAXIMO_NOMBRE + " caracteres.");
        }
    }

    /**
     * Valida la complejidad de una contraseña según la política de seguridad:
     * mínimo 8 caracteres, máximo 128, al menos una mayúscula, una minúscula,
     * un dígito y un carácter especial.
     *
     * @param contrasena contraseña a validar
     * @param nombreCampo nombre del campo para los mensajes de error
     * @throws ExcepcionValidacion si la contraseña no cumple la política
     */
    public static void validarPassword(String contrasena, String nombreCampo)
            throws ExcepcionValidacion {
        validarCampoRequerido(contrasena, nombreCampo);
        int largo = contrasena.length();
        if (largo < 8) {
            throw new ExcepcionValidacion(nombreCampo,
                    "La contraseña debe tener al menos 8 caracteres.");
        }
        if (largo > 128) {
            throw new ExcepcionValidacion(nombreCampo,
                    "La contraseña no puede superar los 128 caracteres.");
        }
        if (!contrasena.chars().anyMatch(Character::isUpperCase)) {
            throw new ExcepcionValidacion(nombreCampo,
                    "La contraseña debe contener al menos una letra mayúscula.");
        }
        if (!contrasena.chars().anyMatch(Character::isLowerCase)) {
            throw new ExcepcionValidacion(nombreCampo,
                    "La contraseña debe contener al menos una letra minúscula.");
        }
        if (!contrasena.chars().anyMatch(Character::isDigit)) {
            throw new ExcepcionValidacion(nombreCampo,
                    "La contraseña debe contener al menos un número.");
        }
        boolean tieneEspecial = contrasena.chars()
                .anyMatch(c -> !Character.isLetterOrDigit(c));
        if (!tieneEspecial) {
            throw new ExcepcionValidacion(nombreCampo,
                    "La contraseña debe contener al menos un carácter especial (!@#$%^&* etc.).");
        }
    }
}

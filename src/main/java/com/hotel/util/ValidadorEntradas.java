package com.hotel.util;

import com.hotel.exception.ExcepcionValidacion;

public class ValidadorEntradas {

    public static void validarCampoRequerido(String valor, String campo) {
        if (valor == null || valor.isBlank())
            throw new ExcepcionValidacion("El campo '" + campo + "' es obligatorio.");
    }

    public static void validarIdPositivo(int id, String entidad) {
        if (id <= 0)
            throw new ExcepcionValidacion("El ID de " + entidad + " debe ser mayor a cero.");
    }

    public static void validarSoloNumeros(String valor, String campo) {
        if (!valor.matches("\\d+"))
            throw new ExcepcionValidacion("El campo '" + campo + "' solo acepta números.");
    }

    public static void validarLargoNombre(String valor, String campo) {
        validarCampoRequerido(valor, campo);
        if (valor.length() < 2 || valor.length() > 50)
            throw new ExcepcionValidacion("El campo '" + campo + "' debe tener entre 2 y 50 caracteres.");
    }

    public static void validarFormatoEmail(String email) {
        validarCampoRequerido(email, "email");
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$"))
            throw new ExcepcionValidacion("El formato del email es inválido.");
    }

    public static void validarFormatoTelefono(String tel) {
        if (tel != null && !tel.isBlank() && !tel.matches("[+\\d\\s()-]{7,20}"))
            throw new ExcepcionValidacion("El formato del teléfono es inválido.");
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 *
 * @author Pulgarin
 */
/**
 * Utilidad temporal para generar el hash BCrypt de la clave inicial. Ejecutar
 * una sola vez, copiar el hash impreso en el SQL de actualización. Borrar este
 * archivo después de usarlo.
 */
public class GeneradorHash {

    public static void main(String[] args) {
        String clave = "Hotel2024!";
        String hash = BCrypt.hashpw(clave, BCrypt.gensalt(10));

        System.out.println("=== HASH GENERADO ===");
        System.out.println("Clave : " + clave);
        System.out.println("Hash  : " + hash);
        System.out.println();
        System.out.println("-- Copiar y pegar en SQL*Plus:");
        System.out.println("UPDATE EMPLEADO SET password_hash = '" + hash + "';");
        System.out.println("COMMIT;");
        System.out.println();
        System.out.println("-- Verificacion:");
        System.out.println("Valido: " + BCrypt.checkpw(clave, hash));
    }
}

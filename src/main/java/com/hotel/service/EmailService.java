/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author Pulgarin
 */
/**
 * Servicio de notificaciones por correo electrónico. Usa Jakarta Mail
 * (angus-mail) con SMTP (Gmail por defecto).
 *
 * Configurar el remitente en src/com/hotel/email.properties. Para Gmail:
 * generar una "Contraseña de aplicación" en
 * https://myaccount.google.com/apppasswords (requiere 2FA activo).
 *
 * SOLID: S – responsabilidad única: envío de correos. GRASP: Alta Cohesión –
 * solo gestiona notificaciones.
 */
public class EmailService {

    private final Properties smtpProps = new Properties();
    private String fromAddress;
    private String password;
    private boolean configurado = false;
    
        public EmailService() {
        cargarConfiguracion();
    }
        
        private void cargarConfiguracion() {
        try (InputStream is = getClass().getResourceAsStream("/com/hotel/email.properties")) {
            if (is == null) {
                System.err.println("[EMAIL] email.properties no encontrado en classpath.");
                return;
            }
            Properties cfg = new Properties();
            cfg.load(is);

            fromAddress = cfg.getProperty("mail.from", "").trim();
            password    = cfg.getProperty("mail.password", "").trim();
}

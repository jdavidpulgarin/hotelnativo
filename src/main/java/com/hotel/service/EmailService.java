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
            password = cfg.getProperty("mail.password", "").trim();

            smtpProps.put("mail.smtp.host", cfg.getProperty("mail.smtp.host", "smtp.gmail.com"));
            smtpProps.put("mail.smtp.port", cfg.getProperty("mail.smtp.port", "587"));
            smtpProps.put("mail.smtp.auth", cfg.getProperty("mail.smtp.auth", "true"));
            smtpProps.put("mail.smtp.starttls.enable", cfg.getProperty("mail.smtp.starttls.enable", "true"));
            smtpProps.put("mail.smtp.ssl.trust", cfg.getProperty("mail.smtp.host", "smtp.gmail.com"));

            configurado = !fromAddress.isEmpty()
                    && !password.isEmpty()
                    && !password.startsWith("xxxx");

            if (!configurado) {
                System.out.println("[EMAIL] Configuración SMTP pendiente. Edita src/com/hotel/email.properties.");
            } else {
                System.out.println("[EMAIL] SMTP configurado: " + fromAddress);
            }
        } catch (IOException e) {
            System.err.println("[EMAIL] Error leyendo email.properties: " + e.getMessage());
        }
    }

    // ── Notificaciones de Reserva ─────────────────────────────────────────────
    public void notificarConfirmacionReserva(Cliente cliente, Reserva reserva) {
        enviarEmail(cliente.getEmail(),
                "Confirmación Reserva #" + reserva.getId() + " - Hotel Nativo",
                construirCuerpoConfirmacion(cliente, reserva), null);
    }
}

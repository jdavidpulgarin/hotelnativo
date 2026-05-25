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

    public void notificarCancelacionReserva(Cliente cliente, Reserva reserva) {
        enviarEmail(cliente.getEmail(),
                "Cancelación Reserva #" + reserva.getId() + " - Hotel Nativo",
                "Estimado " + cliente.obtenerNombreCompleto()
                + ", su reserva #" + reserva.getId() + " fue cancelada exitosamente.",
                null);
    }

    // ── Notificaciones de Factura ─────────────────────────────────────────────
    public void notificarFacturaGenerada(Cliente cliente, int idFactura, double total) {
        enviarEmail(cliente.getEmail(),
                "Factura #" + idFactura + " generada - Hotel Nativo",
                "Estimado " + cliente.obtenerNombreCompleto()
                + ", su factura #" + idFactura
                + " por $" + String.format("%,.0f", total) + " está lista.",
                null);
    }

    /**
     * Envía la factura al correo del cliente con el PDF adjunto.
     *
     * @param factura datos de la factura
     * @param pdfPath ruta del PDF a adjuntar (null = solo texto sin adjunto)
     * @return true si el envío fue exitoso
     */
    public boolean enviarFacturaPorEmail(Factura factura, String pdfPath) {
        if (factura.getCliente() == null || factura.getCliente().getEmail() == null) {
            System.err.println("[EMAIL] Factura sin cliente/email — no se envía.");
            return false;
        }
        String asunto = "Factura #" + factura.getId() + " — Hotel Nativo";
        String cuerpo = construirCuerpoFactura(factura);
        return enviarEmail(factura.getCliente().getEmail(), asunto, cuerpo, pdfPath);
    }

    // ── Notificaciones de Mantenimiento ──────────────────────────────────────
    public void notificarNuevoMantenimiento(Empleado empleado,
            Habitacion habitacion,
            Mantenimiento.TipoMantenimiento tipo,
            String descripcion,
            int idMantenimiento) {
        String cuerpo = "Estimado " + empleado.obtenerNombreCompleto() + ",\n"
                + "Se le ha asignado una solicitud de mantenimiento #" + idMantenimiento + ".\n"
                + "Habitación: " + habitacion.getNumero() + "\n"
                + "Tipo: " + tipo + "\n"
                + "Descripción: " + descripcion + "\n"
                + "La habitación ha sido bloqueada automáticamente (estado: MANTENIMIENTO).\n"
                + "Por favor atender a la brevedad posible.";
        enviarEmail(empleado.getEmail(),
                "Nueva solicitud de mantenimiento #" + idMantenimiento
                + " - Hab. " + habitacion.getNumero(),
                cuerpo, null);
    }

    public void notificarMantenimientoCompletado(Mantenimiento mantenimiento) {
        String cuerpo = "El mantenimiento #" + mantenimiento.getId()
                + " de la habitación " + mantenimiento.getHabitacion().getNumero()
                + " fue completado.\n"
                + "Costo: $" + String.format("%,.0f", mantenimiento.getCosto()) + "\n"
                + "La habitación está disponible nuevamente.";
        enviarEmail(fromAddress.isEmpty() ? "recepcion@hotel.com" : fromAddress,
                "Mantenimiento completado – Hab. " + mantenimiento.getHabitacion().getNumero(),
                cuerpo, null);
    }

    // ── Implementación SMTP ───────────────────────────────────────────────────
    /**
     * Envía un email SMTP con cuerpo de texto y, opcionalmente, un PDF adjunto.
     * Si el servicio no está configurado, imprime en consola (modo prueba).
     *
     * @param pdfPath ruta del PDF a adjuntar, null para no adjuntar nada
     * @return true si se envió realmente, false en modo simulado o error
     */
    private boolean enviarEmail(String destinatario, String asunto, String cuerpo, String pdfPath) {
        System.out.println("[EMAIL] Para:   " + destinatario);
        System.out.println("[EMAIL] Asunto: " + asunto);

        if (!configurado) {
            System.out.println("[EMAIL] (simulado — configura email.properties para envío real)");
            System.out.println("[EMAIL] ─────────────────────────────────────────");
            return false;
        }

        try {
            Session session = Session.getInstance(smtpProps, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(fromAddress, password);
                }
            });

            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(fromAddress, "Hotel Nativo"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            msg.setSubject(asunto, "UTF-8");
            msg.setHeader("X-Mailer", "Hotel Nativo System 1.0");
            msg.setSentDate(new java.util.Date());

            if (pdfPath != null && new java.io.File(pdfPath).exists()) {
                MimeMultipart multipart = new MimeMultipart();

                MimeBodyPart textoPart = new MimeBodyPart();
                textoPart.setText(cuerpo, "UTF-8");
                multipart.addBodyPart(textoPart);

                MimeBodyPart adjuntoPart = new MimeBodyPart();
                adjuntoPart.attachFile(new java.io.File(pdfPath));
                adjuntoPart.setFileName("Factura_Hotel_Nativo.pdf");
                multipart.addBodyPart(adjuntoPart);

                msg.setContent(multipart);
            } else {
                msg.setText(cuerpo, "UTF-8");
            }

            Transport.send(msg);
            System.out.println("[EMAIL] Enviado exitosamente a: " + destinatario);
            System.out.println("[EMAIL] ─────────────────────────────────────────");
            return true;
        } catch (Exception e) {
            System.err.println("[EMAIL] Error enviando a " + destinatario + ": " + e.getMessage());
            return false;
        }
    }

    // ── Constructores de cuerpo ───────────────────────────────────────────────
    private String construirCuerpoConfirmacion(Cliente cliente, Reserva reserva) {
        return "Estimado " + cliente.obtenerNombreCompleto() + ",\n\n"
                + "Su reserva #" + reserva.getId() + " fue confirmada.\n"
                + "Habitación: " + (reserva.getHabitacion() != null ? reserva.getHabitacion().getNumero() : "—") + "\n"
                + "Entrada: " + reserva.getFechaEntrada() + "\n"
                + "Salida: " + reserva.getFechaSalida() + "\n"
                + "Total: $" + String.format("%,.0f", reserva.getPrecioTotal()) + "\n\n"
                + "Gracias por elegir Hotel Nativo.\n"
                + "Para consultas: recepcion@hotelnativo.com";
    }

    private String construirCuerpoFactura(Factura factura) {
        StringBuilder sb = new StringBuilder();
        sb.append("══════════════════════════════════════════\n");
        sb.append("          HOTEL NATIVO — FACTURA\n");
        sb.append("══════════════════════════════════════════\n\n");
        sb.append("Factura N.°: #").append(factura.getId()).append("\n");
        sb.append("Fecha:       ").append(factura.getFechaEmision() != null
                ? factura.getFechaEmision() : "—").append("\n\n");

        if (factura.getCliente() != null) {
            sb.append("CLIENTE\n");
            sb.append("  Nombre:   ").append(factura.getCliente().obtenerNombreCompleto()).append("\n");
            sb.append("  Email:    ").append(factura.getCliente().getEmail()).append("\n");
            if (factura.getCliente().getTelefono() != null) {
                sb.append("  Teléfono: ").append(factura.getCliente().getTelefono()).append("\n");
            }
            sb.append("\n");
        }

        if (factura.getReserva() != null) {
            Reserva r = factura.getReserva();
            sb.append("RESERVA #").append(r.getId()).append("\n");
            if (r.getHabitacion() != null) {
                sb.append("  Habitación: ").append(r.getHabitacion().getNumero()).append("\n");
            }
            sb.append("  Entrada:    ").append(r.getFechaEntrada() != null ? r.getFechaEntrada() : "—").append("\n");
            sb.append("  Salida:     ").append(r.getFechaSalida() != null ? r.getFechaSalida() : "—").append("\n");
            sb.append("  Noches:     ").append(r.calcularTotalDiasReserva()).append("\n\n");
        }

        sb.append("──────────────────────────────────────────\n");
        sb.append(String.format("  Subtotal:   $%,14.0f%n", factura.getSubtotal()));
        sb.append(String.format("  Impuestos:  $%,14.0f%n", factura.getImpuestos()));
        sb.append("──────────────────────────────────────────\n");
        sb.append(String.format("  TOTAL:      $%,14.0f%n", factura.getTotal()));
        sb.append("══════════════════════════════════════════\n\n");
        sb.append("Método de pago: ").append(factura.getMetodoPago() != null
                ? factura.getMetodoPago().name().replace("_", " ") : "—").append("\n");
        sb.append("Estado:         ").append(factura.getEstadoPago() != null
                ? factura.getEstadoPago().name() : "—").append("\n\n");
        sb.append("Gracias por hospedarse en Hotel Nativo.\n");
        sb.append("Recepción: +57 XXX XXX XXXX | recepcion@hotelnativo.com\n");
        return sb.toString();
    }

    /**
     * Indica si el SMTP está configurado con credenciales reales.
     */
    public boolean isConfigurado() {
        return configurado;
    }

}

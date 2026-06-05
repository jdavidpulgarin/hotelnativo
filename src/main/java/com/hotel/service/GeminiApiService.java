/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Pulgarin
 */
/**
 * Cliente HTTP para la API de Google Gemini con soporte de historial de
 * conversación.
 *
 * Mantiene un historial de turnos (usuario/modelo) para que Gemini recuerde el
 * contexto de la conversación en curso.
 *
 * Uso: 1. Pon tu API key en config/chatbot.properties
 * (gemini.api.key=AQ.Ab8R...) o en la variable de entorno GEMINI_API_KEY. 2.
 * Cada sesión de chat crea una instancia nueva (o llama a limpiarHistorial()).
 * 3. ChatbotController llama a consultar(contexto, pregunta) por cada turno.
 */
public class GeminiApiService {

    private static final String URL_BASE
            = "https://generativelanguage.googleapis.com/v1beta/models/"
            + "gemini-2.0-flash:generateContent?key=";

    private static final int MAX_TURNOS_HISTORIAL = 10; // máximo pares usuario/modelo a recordar

    // Cada String del historial es un bloque JSON de un turno: {"role":"user","parts":[...]}
    private final List<String> historial = new ArrayList<>();

    private final HttpClient http;
    private final String apiKey;

    public GeminiApiService(String apiKey) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * true si la clave está configurada y no es el placeholder.
     */
    public boolean estaConfigurado() {
        return !apiKey.isBlank()
                && !apiKey.equalsIgnoreCase("TU_API_KEY_AQUI");
    }

    /**
     * Envía la pregunta a Gemini con contexto real de la BD e historial
     * completo.
     *
     * @param contextoHotel Datos reales de BD (disponibilidad, reservas, etc.)
     * @param pregunta Mensaje original del usuario.
     * @return Respuesta de Gemini en texto plano.
     * @throws Exception Si la llamada HTTP falla o la respuesta es inválida.
     */
    public String consultar(String contextoHotel, String pregunta) throws Exception {
        // Construir el turno del usuario (incluye contexto de BD si hay)
        String mensajeUsuario = construirMensajeUsuario(contextoHotel, pregunta);

        // Agregar turno usuario al historial
        historial.add("{\"role\":\"user\",\"parts\":[{\"text\":" + encodeJson(mensajeUsuario) + "}]}");

        // Recortar historial si supera el máximo (conserva pares completos)
        recortarHistorial();

        // Construir el cuerpo JSON completo con historial
        String cuerpoJson = construirCuerpoJson();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + apiKey))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpoJson))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            // Remover el último turno del historial para no contaminar el contexto
            historial.remove(historial.size() - 1);
            throw new RuntimeException("Gemini respondió HTTP " + resp.statusCode()
                    + ". Verifica tu API key en config/chatbot.properties.\nDetalle: " + resp.body());
        }

        String respuesta = extraerTexto(resp.body());

        // Agregar turno del modelo al historial
        historial.add("{\"role\":\"model\",\"parts\":[{\"text\":" + encodeJson(respuesta) + "}]}");

        return respuesta;
    }

    /**
     * Limpia el historial de conversación (para iniciar una nueva sesión).
     */
    public void limpiarHistorial() {
        historial.clear();
    }

    /**
     * Número de turnos en el historial actual.
     */
    public int cantidadTurnos() {
        return historial.size();
    }

    private String construirMensajeUsuario(String contexto, String pregunta) {
        if (contexto != null && !contexto.isBlank()) {
            return "=== DATOS DEL SISTEMA (información real de la BD) ===\n"
                    + contexto
                    + "\n=== FIN DATOS ===\n\n"
                    + pregunta;
        }
        return pregunta;
    }

    private String construirCuerpoJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // System instruction (personalidad del bot)
        sb.append("\"system_instruction\":{\"parts\":[{\"text\":");
        sb.append(encodeJson(obtenerSystemPrompt()));
        sb.append("}]},");

        // Historial de conversación (contents)
        sb.append("\"contents\":[");
        for (int i = 0; i < historial.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(historial.get(i));
        }
        sb.append("],");

        // Configuración de generación
        sb.append("\"generationConfig\":{");
        sb.append("\"maxOutputTokens\":700,");
        sb.append("\"temperature\":0.65,");
        sb.append("\"topP\":0.9");
        sb.append("}");

        sb.append("}");
        return sb.toString();
    }

    private String obtenerSystemPrompt() {
        return "Eres Nova, la asistente virtual inteligente del Hotel Nativo (Valledupar, Colombia). "
                + "Tu función es ayudar al personal de recepción con consultas operacionales en tiempo real. "
                + "Tienes acceso a datos reales del sistema de gestión del hotel (reservas, habitaciones, clientes, facturación). "
                + "REGLAS ESTRICTAS:\n"
                + "1. Responde SIEMPRE en español, de forma clara, concisa y profesional.\n"
                + "2. Usa los datos reales del sistema cuando estén disponibles en el mensaje. NO inventes datos.\n"
                + "3. Si no tienes información sobre algo, dilo claramente y sugiere cómo obtenerla.\n"
                + "4. Puedes reformular y complementar la información del sistema para hacerla más útil.\n"
                + "5. Recuerdas el contexto de la conversación actual para dar respuestas coherentes.\n"
                + "6. Usa emojis con moderación (máximo 2-3 por respuesta).\n"
                + "7. Sé directo y operacional: el personal necesita información rápida y precisa.\n"
                + "8. Si el usuario saluda o hace preguntas generales del hotel, responde amablemente.\n"
                + "9. Para consultas que requieren acciones (crear reserva, hacer check-in), "
                + "indica los pasos a seguir en el sistema.\n"
                + "Tu nombre es Nova. Si te preguntan quién eres, dilo con orgullo.";
    }

    // ── Gestión del historial ─────────────────────────────────────────────────
    private void recortarHistorial() {
        // Cada turno completo = 2 elementos (usuario + modelo)
        int maxElementos = MAX_TURNOS_HISTORIAL * 2;
        while (historial.size() > maxElementos) {
            // Eliminar el par más antiguo (índices 0 y 1)
            historial.remove(0);
            if (!historial.isEmpty()) {
                historial.remove(0);
            }
        }
    }

    // ── Parseo de la respuesta ────────────────────────────────────────────────
    /**
     * Extrae el campo text del JSON de Gemini sin dependencias externas.
     * Estructura esperada: candidates[0].content.parts[0].text
     */
    private String extraerTexto(String json) {
        int idx = json.indexOf("\"text\":");
        if (idx < 0) {
            throw new RuntimeException("Respuesta inesperada de Gemini: " + json);
        }

        int inicio = json.indexOf('"', idx + 7) + 1;
        StringBuilder sb = new StringBuilder();
        int i = inicio;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                break;
            }
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case 'n' -> {
                        sb.append('\n');
                        i += 2;
                        continue;
                    }
                    case 't' -> {
                        sb.append('\t');
                        i += 2;
                        continue;
                    }
                    case '"' -> {
                        sb.append('"');
                        i += 2;
                        continue;
                    }
                    case '\\' -> {
                        sb.append('\\');
                        i += 2;
                        continue;
                    }
                    case 'r' -> {
                        i += 2;
                        continue;
                    } // ignorar \r
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString().trim();
    }

    /**
     * Serializa un String Java como literal JSON.
     */
    private String encodeJson(String s) {
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "  ") + "\"";
    }
}

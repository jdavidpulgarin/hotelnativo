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
}

package com.hotel.view;

import com.hotel.service.ChatbotService;
import com.hotel.util.Constantes;
import java.util.Scanner;

/** Vista consola para el chatbot de consultas. CAPA PRESENTACIÓN. */
public class ChatbotView {
    private final ChatbotService chatbotService;
    private final Scanner scanner;

    public ChatbotView(ChatbotService chatbotService, Scanner scanner) {
        this.chatbotService = chatbotService;
        this.scanner = scanner;
    }

    public void iniciarChat() {
        System.out.println("\n" + Constantes.SEPARADOR_MENU);
        System.out.println("  ASISTENTE VIRTUAL DEL HOTEL");
        System.out.println(Constantes.SEPARADOR_MENU);
        System.out.println(chatbotService.obtenerMensajeBienvenida());
        System.out.println("\nEscribe 'salir' para volver al menú principal.");

        String mensajeUsuario;
        do {
            System.out.print("\nTú: ");
            mensajeUsuario = scanner.nextLine().trim();
            if (!"salir".equalsIgnoreCase(mensajeUsuario)) {
                String respuesta = chatbotService.procesarMensaje(mensajeUsuario);
                System.out.println("Hotel Bot: " + respuesta);
            }
        } while (!"salir".equalsIgnoreCase(mensajeUsuario));
        System.out.println("¡Hasta luego!");
    }
}

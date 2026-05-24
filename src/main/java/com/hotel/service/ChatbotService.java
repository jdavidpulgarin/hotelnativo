/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.java.com.hotel.service;

import com.hotel.dao.interfaces.IClienteDAO;
import com.hotel.dao.interfaces.IHabitacionDAO;
import com.hotel.dao.interfaces.IReservaBusqueda;
import com.hotel.dao.interfaces.IReservaDAO;
import com.hotel.dto.BusquedaDisponibilidadDTO;
import com.hotel.model.Cliente;
import com.hotel.model.Habitacion;
import com.hotel.model.Reserva;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * @author Pulgarin
 */
/**
 * Asistente interno de recepción para el Hotel Nativo. Responde consultas
 * operacionales en tiempo real desde la BD: check-ins/checkouts del día, estado
 * de habitaciones, reservas, clientes.
 *
 * GRASP: Alta Cohesión – responsabilidad única: interpretar y responder
 * consultas de recepción. SOLID: D – depende de interfaces de DAO, no de
 * implementaciones concretas.
 */
public class ChatbotService {
    // ── Patrones NLP ──────────────────────────────────────────────────────────

    private static final Pattern PATRON_FECHA_ISO = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern PATRON_FECHA_DMY = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");
    private static final Pattern PATRON_NUMERO = Pattern.compile("\\b(\\d+)\\b");
    private static final Pattern PATRON_ID_RESERVA = Pattern.compile(
            "(?:reserva|booking|numero|id)\\s*[:#]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    // ── Intenciones y sus palabras clave ─────────────────────────────────────
    private static final Map<String, String[]> SINONIMOS = new HashMap<>();

    static {
        SINONIMOS.put("SALUDO",
                new String[]{"hola", "buenas", "buenos", "hi", "hey", "saludos"});
        SINONIMOS.put("AYUDA",
                new String[]{"ayuda", "help", "que puedes", "que podes", "comandos",
                    "opciones", "como funciona", "menu"});
        SINONIMOS.put("CHECKINS_HOY",
                new String[]{"check-in hoy", "checkin hoy", "llegadas hoy", "quien llega hoy",
                    "quien llega", "entradas hoy", "llegadas de hoy", "entradas de hoy",
                    "que llegadas hay", "quien hace check-in", "que checkins hay"});
        SINONIMOS.put("CHECKOUTS_HOY",
                new String[]{"check-out hoy", "checkout hoy", "salidas hoy", "quien sale hoy",
                    "quien se va hoy", "salidas de hoy", "checkouts hoy",
                    "que salidas hay", "que checkouts hay"});
        SINONIMOS.put("ESTADO_HABITACIONES",
                new String[]{"estado habitaciones", "estado de las habitaciones",
                    "mapa habitaciones", "habitaciones ocupadas", "cuales estan ocupadas",
                    "cuales habitaciones", "resumen habitaciones", "que habitaciones hay",
                    "ver habitaciones", "todas las habitaciones", "habitaciones en mantenimiento",
                    "cuantas habitaciones", "ocupacion habitaciones", "mapa de ocupacion"});
        SINONIMOS.put("DISPONIBILIDAD",
                new String[]{"disponible", "disponibilidad", "hay habitaciones", "hay cuartos",
                    "habitaciones libres", "cuartos libres", "buscar habitacion", "para reservar"});
        SINONIMOS.put("ESTADO_RESERVA",
                new String[]{"reserva #", "estado reserva", "buscar reserva", "consultar reserva",
                    "ver reserva", "informacion reserva", "datos reserva", "detalle reserva"});
        SINONIMOS.put("RESERVAS_HOY",
                new String[]{"reservas hoy", "reservas del dia", "movimiento de hoy",
                    "que reservas hay hoy", "actividad de hoy"});
        SINONIMOS.put("RESERVAS_ACTIVAS",
                new String[]{"reservas pendientes", "reservas confirmadas", "reservas activas",
                    "reservas vigentes", "reservas abiertas", "todas las reservas activas",
                    "listar reservas", "ver reservas"});
        SINONIMOS.put("BUSCAR_CLIENTE",
                new String[]{"buscar cliente", "cliente llamado", "cliente con nombre",
                    "datos del cliente", "informacion del cliente", "buscar huesped",
                    "quien es el cliente"});
    }
    
        // ── Dependencias ──────────────────────────────────────────────────────────
    private final IHabitacionDAO   habitacionDAO;
    private final IReservaDAO      reservaDAO;
    private final IReservaBusqueda reservaBusqueda;
    private final IClienteDAO      clienteDAO;

    public ChatbotService(IHabitacionDAO habitacionDAO, IReservaDAO reservaDAO,
                          IReservaBusqueda reservaBusqueda, IClienteDAO clienteDAO) {
        this.habitacionDAO   = habitacionDAO;
        this.reservaDAO      = reservaDAO;
        this.reservaBusqueda = reservaBusqueda;
        this.clienteDAO      = clienteDAO;
    }
}

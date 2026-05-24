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
}

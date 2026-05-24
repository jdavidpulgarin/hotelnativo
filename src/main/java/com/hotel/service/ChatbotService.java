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
    private final IHabitacionDAO habitacionDAO;
    private final IReservaDAO reservaDAO;
    private final IReservaBusqueda reservaBusqueda;
    private final IClienteDAO clienteDAO;

    public ChatbotService(IHabitacionDAO habitacionDAO, IReservaDAO reservaDAO,
            IReservaBusqueda reservaBusqueda, IClienteDAO clienteDAO) {
        this.habitacionDAO = habitacionDAO;
        this.reservaDAO = reservaDAO;
        this.reservaBusqueda = reservaBusqueda;
        this.clienteDAO = clienteDAO;
    }

    // ── API pública ───────────────────────────────────────────────────────────
    public String obtenerMensajeBienvenida() {
        LocalDate hoy = LocalDate.now();
        String ej1 = hoy.plusDays(3).toString();
        String ej2 = hoy.plusDays(6).toString();
        return "¡Hola! Soy tu asistente de recepción del Hotel Nativo 🏨\n"
                + "Consulto la base de datos en tiempo real para darte información precisa.\n\n"
                + "¿Qué necesitas saber?\n"
                + "  🔑 'check-in hoy'           → llegadas del día\n"
                + "  🔓 'checkout hoy'           → salidas del día\n"
                + "  🛏 'estado habitaciones'    → mapa de ocupación actual\n"
                + "  📅 'disponibilidad " + ej1 + " " + ej2 + "'\n"
                + "  📋 'reserva #123'           → detalle de una reserva\n"
                + "  📋 'reservas pendientes'    → todas las reservas activas\n"
                + "  👤 'buscar cliente García'  → datos del cliente\n\n"
                + "Escribe 'ayuda' para ver todos los comandos disponibles. 😊";
    }

    public String procesarMensaje(String mensajeUsuario) {
        if (mensajeUsuario == null || mensajeUsuario.trim().isEmpty()) {
            return "Por favor escribe tu consulta. Escribe 'ayuda' para ver las opciones.";
        }
        String normalizado = normalizar(mensajeUsuario);
        String intencion = detectarIntencion(normalizado);

        switch (intencion) {
            case "SALUDO":
                return obtenerMensajeBienvenida();
            case "AYUDA":
                return obtenerTextoAyuda();
            case "CHECKINS_HOY":
                return manejarCheckinsHoy();
            case "CHECKOUTS_HOY":
                return manejarCheckoutsHoy();
            case "ESTADO_HABITACIONES":
                return manejarEstadoHabitaciones();
            case "DISPONIBILIDAD":
                return manejarDisponibilidad(mensajeUsuario, normalizado);
            case "ESTADO_RESERVA":
                return manejarEstadoReserva(mensajeUsuario);
            case "RESERVAS_HOY":
                return manejarReservasHoy();
            case "RESERVAS_ACTIVAS":
                return manejarReservasActivas();
            case "BUSCAR_CLIENTE":
                return manejarBuscarCliente(mensajeUsuario);
            default:
                return respuestaNoEntendida();
        }
    }

    // ── Detección de intención (NLP básico) ───────────────────────────────────
    private String detectarIntencion(String normalizado) {
        for (Map.Entry<String, String[]> entrada : SINONIMOS.entrySet()) {
            for (String clave : entrada.getValue()) {
                if (normalizado.contains(clave)) {
                    return entrada.getKey();
                }
            }
        }
        if (contarFechas(normalizado) >= 2) {
            return "DISPONIBILIDAD";
        }
        if (PATRON_ID_RESERVA.matcher(normalizado).find()) {
            return "ESTADO_RESERVA";
        }
        if (normalizado.contains("reserva") && PATRON_NUMERO.matcher(normalizado).find()) {
            return "ESTADO_RESERVA";
        }
        return "DESCONOCIDO";
    }

    private String normalizar(String texto) {
        return texto.trim().toLowerCase()
                .replaceAll("[áàä]", "a").replaceAll("[éèë]", "e")
                .replaceAll("[íìï]", "i").replaceAll("[óòö]", "o")
                .replaceAll("[úùü]", "u").replaceAll("[ñ]", "n");
    }
    // ── Handler: check-ins de hoy ─────────────────────────────────────────────

    private String manejarCheckinsHoy() {
        try {
            LocalDate hoy = LocalDate.now();
            List<Reserva> llegadas = reservaBusqueda.buscarPorRangoFechas(hoy, hoy)
                    .stream()
                    .filter(r -> r.getEstado() == Reserva.EstadoReserva.PENDIENTE
                    || r.getEstado() == Reserva.EstadoReserva.CONFIRMADA)
                    .collect(Collectors.toList());

            List<Reserva> yaCheckIn = reservaBusqueda.buscarPorRangoFechas(hoy, hoy)
                    .stream()
                    .filter(r -> r.getEstado() == Reserva.EstadoReserva.EN_PROCESO)
                    .collect(Collectors.toList());

            StringBuilder sb = new StringBuilder();
            sb.append("🔑 Check-ins de hoy (").append(hoy).append("):\n\n");

            if (llegadas.isEmpty() && yaCheckIn.isEmpty()) {
                return sb.append("No hay llegadas registradas para hoy.").toString();
            }

            if (!llegadas.isEmpty()) {
                sb.append("⏳ Pendientes de hacer check-in (").append(llegadas.size()).append("):\n");
                for (Reserva r : llegadas) {
                    long noches = r.getFechaEntrada().until(r.getFechaSalida()).getDays();
                    sb.append(String.format("  📋 Res. #%-4d │ %-25s │ Hab. %-5s │ %d noche(s) │ Sale: %s\n",
                            r.getId(),
                            r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—",
                            r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—",
                            noches, r.getFechaSalida()));
                }
            }

            if (!yaCheckIn.isEmpty()) {
                if (!llegadas.isEmpty()) {
                    sb.append("\n");
                }
                sb.append("✅ Ya hicieron check-in (").append(yaCheckIn.size()).append("):\n");
                for (Reserva r : yaCheckIn) {
                    sb.append(String.format("  📋 Res. #%-4d │ %-25s │ Hab. %-5s │ Sale: %s\n",
                            r.getId(),
                            r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—",
                            r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—",
                            r.getFechaSalida()));
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "⚠ No pude consultar los check-ins de hoy. Verifica la conexión.";
        }
    }

    // ── Handler: checkouts de hoy ─────────────────────────────────────────────
    private String manejarCheckoutsHoy() {
        try {
            LocalDate hoy = LocalDate.now();
            List<Reserva> todas = reservaDAO.listarTodas();

            List<Reserva> pendientes = todas.stream()
                    .filter(r -> hoy.equals(r.getFechaSalida())
                    && r.getEstado() == Reserva.EstadoReserva.EN_PROCESO)
                    .collect(Collectors.toList());

            List<Reserva> completadas = todas.stream()
                    .filter(r -> hoy.equals(r.getFechaSalida())
                    && r.getEstado() == Reserva.EstadoReserva.COMPLETADA)
                    .collect(Collectors.toList());

            StringBuilder sb = new StringBuilder();
            sb.append("🔓 Checkouts de hoy (").append(hoy).append("):\n\n");

            if (pendientes.isEmpty() && completadas.isEmpty()) {
                return sb.append("No hay salidas registradas para hoy.").toString();
            }

            if (!pendientes.isEmpty()) {
                sb.append("⏳ Pendientes de hacer checkout (").append(pendientes.size()).append("):\n");
                for (Reserva r : pendientes) {
                    sb.append(String.format("  📋 Res. #%-4d │ %-25s │ Hab. %-5s │ Total: $%,.0f\n",
                            r.getId(),
                            r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—",
                            r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—",
                            r.getPrecioTotal()));
                }
            }

            if (!completadas.isEmpty()) {
                if (!pendientes.isEmpty()) {
                    sb.append("\n");
                }
                sb.append("✅ Ya realizaron checkout (").append(completadas.size()).append("):\n");
                for (Reserva r : completadas) {
                    sb.append(String.format("  📋 Res. #%-4d │ %-25s │ Hab. %-5s\n",
                            r.getId(),
                            r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—",
                            r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—"));
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "⚠ No pude consultar los checkouts de hoy. Verifica la conexión.";
        }
    }

    // ── Handler: estado/mapa de habitaciones ──────────────────────────────────
    private String manejarEstadoHabitaciones() {
        try {
            List<Habitacion> todas = habitacionDAO.listarTodas();
            Map<Habitacion.EstadoHabitacion, List<Habitacion>> porEstado
                    = todas.stream().collect(Collectors.groupingBy(Habitacion::getEstado));

            List<Habitacion> disponibles = porEstado.getOrDefault(Habitacion.EstadoHabitacion.DISPONIBLE, List.of());
            List<Habitacion> ocupadas = porEstado.getOrDefault(Habitacion.EstadoHabitacion.OCUPADA, List.of());
            List<Habitacion> reservadas = porEstado.getOrDefault(Habitacion.EstadoHabitacion.RESERVADA, List.of());
            List<Habitacion> mantenimiento = porEstado.getOrDefault(Habitacion.EstadoHabitacion.MANTENIMIENTO, List.of());

            StringBuilder sb = new StringBuilder();
            sb.append("🏨 Estado de habitaciones (").append(todas.size()).append(" en total):\n\n");
            sb.append(String.format("  🟢 Disponibles:   %2d\n", disponibles.size()));
            sb.append(String.format("  🔴 Ocupadas:      %2d\n", ocupadas.size()));
            sb.append(String.format("  📅 Reservadas:    %2d\n", reservadas.size()));
            sb.append(String.format("  🔧 Mantenimiento: %2d\n", mantenimiento.size()));

            if (!disponibles.isEmpty()) {
                sb.append("\n🟢 Disponibles ahora:\n");
                for (Habitacion h : disponibles) {
                    sb.append(String.format("   Hab. %-5s │ %-22s │ $%,.0f/noche\n",
                            h.getNumero(),
                            h.getTipoHabitacion() != null ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "—",
                            h.calcularPrecioFinal()));
                }
            }

            if (!ocupadas.isEmpty()) {
                sb.append("\n🔴 Ocupadas actualmente:\n");
                for (Habitacion h : ocupadas) {
                    sb.append(String.format("   Hab. %-5s │ %s\n",
                            h.getNumero(),
                            h.getTipoHabitacion() != null ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "—"));
                }
            }

            if (!reservadas.isEmpty()) {
                sb.append("\n📅 Reservadas (próximamente ocupadas):\n");
                for (Habitacion h : reservadas) {
                    sb.append(String.format("   Hab. %-5s │ %s\n",
                            h.getNumero(),
                            h.getTipoHabitacion() != null ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "—"));
                }
            }

            if (!mantenimiento.isEmpty()) {
                sb.append("\n🔧 En mantenimiento:\n");
                for (Habitacion h : mantenimiento) {
                    sb.append(String.format("   Hab. %-5s │ %s\n",
                            h.getNumero(),
                            h.getTipoHabitacion() != null ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "—"));
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "⚠ No pude consultar el estado de las habitaciones.";
        }
    }

    // ── Handler: disponibilidad por fechas ────────────────────────────────────
    private String manejarDisponibilidad(String original, String normalizado) {
        List<LocalDate> fechas = extraerFechas(original);

        // Sin fechas → mostrar mapa de ocupación actual
        if (fechas.size() < 2) {
            return manejarEstadoHabitaciones();
        }

        LocalDate entrada = fechas.get(0);
        LocalDate salida = fechas.get(1);

        if (!salida.isAfter(entrada)) {
            return "⚠ La fecha de salida debe ser posterior a la de entrada.\n"
                    + "Ejemplo: 'disponibilidad 2026-06-01 2026-06-05'";
        }

        int personas = extraerNumeroPersonas(normalizado, 1);

        try {
            BusquedaDisponibilidadDTO criterios = new BusquedaDisponibilidadDTO(entrada, salida, personas);
            List<Habitacion> disponibles = habitacionDAO.buscarDisponibles(criterios);
            return formatearDisponibilidad(disponibles, entrada, salida, personas);
        } catch (Exception e) {
            return "⚠ Error al consultar disponibilidad: " + e.getMessage();
        }
    }

    private String formatearDisponibilidad(List<Habitacion> disponibles,
            LocalDate entrada, LocalDate salida, int personas) {
        long noches = entrada.until(salida).getDays();
        StringBuilder sb = new StringBuilder();
        sb.append("📅 Disponibilidad: ").append(entrada).append(" → ").append(salida)
                .append("  (").append(noches).append(noches == 1 ? " noche" : " noches")
                .append(", ").append(personas).append(personas == 1 ? " persona" : " personas").append(")\n\n");

        if (disponibles.isEmpty()) {
            String alt1 = entrada.plusDays(2).toString();
            String alt2 = salida.plusDays(2).toString();
            return sb.append("❌ No hay habitaciones disponibles para esas fechas.\n\n")
                    .append("Prueba con otras fechas:\n")
                    .append("  'disponibilidad ").append(alt1).append(" ").append(alt2).append("'")
                    .toString();
        }

        sb.append("✅ ").append(disponibles.size())
                .append(disponibles.size() == 1 ? " habitación disponible:" : " habitaciones disponibles:").append("\n\n");
        for (Habitacion hab : disponibles) {
            double precio = hab.calcularPrecioFinal();
            sb.append(String.format("  🛏 Hab. %-5s │ %-22s │ $%,.0f/noche │ Total: $%,.0f\n",
                    hab.getNumero(),
                    hab.getTipoHabitacion() != null ? hab.getTipoHabitacion().obtenerEtiquetaTipo() : "—",
                    precio, precio * noches));
        }
        return sb.toString().trim();
    }

}

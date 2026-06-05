package com.hotel.service;

import com.hotel.dao.interfaces.IClienteDAO;
import com.hotel.dao.interfaces.IEmpleadoDAO;
import com.hotel.dao.interfaces.IMantenimientoDAO;
import com.hotel.dao.interfaces.IFacturaDAO;
import com.hotel.dao.interfaces.IHabitacionDAO;
import com.hotel.dao.interfaces.IReservaBusqueda;
import com.hotel.dao.interfaces.IReservaDAO;
import com.hotel.dto.BusquedaDisponibilidadDTO;
import com.hotel.model.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class ChatbotService {

    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String LINEA = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

    private static final Pattern PATRON_FECHA_ISO = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern PATRON_FECHA_DMY = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");
    private static final Pattern PATRON_NUMERO = Pattern.compile("\\b(\\d+)\\b");
    private static final Pattern PATRON_ID_RESERVA = Pattern.compile("(?:reserva|booking|numero|id)\\s*[:#]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATRON_ID_FACTURA = Pattern.compile("(?:factura|fac|recibo)\\s*[:#]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATRON_PERSONAS = Pattern.compile("(\\d+)\\s*(?:persona|personas|huesped|huespedes|adulto|adultos)");
    private static final Pattern PATRON_NOMBRE_CLIENTE = Pattern.compile("cliente\\s+([a-záéíóúñ\\s]+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATRON_HABITACION = Pattern.compile("habitacion\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATRON_DOCUMENTO = Pattern.compile("documento\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATRON_TELEFONO = Pattern.compile("telefono\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    // Sinónimos actualizados con todas las consultas
    private static final Map<String, String[]> SINONIMOS = new HashMap<>();

    static {
        // Consultas existentes
        SINONIMOS.put("SALUDO", new String[]{"hola", "buenas", "buenos", "hi", "hey", "saludos", "buen dia", "buenas tardes", "buenas noches"});
        SINONIMOS.put("AYUDA", new String[]{"ayuda", "help", "que puedes", "comandos", "opciones", "como funciona", "menu", "que haces"});
        SINONIMOS.put("CHECKINS_HOY", new String[]{"check-in hoy", "checkin hoy", "llegadas hoy", "quien llega hoy", "entradas hoy"});
        SINONIMOS.put("CHECKOUTS_HOY", new String[]{"check-out hoy", "checkout hoy", "salidas hoy", "quien sale hoy"});
        SINONIMOS.put("ESTADO_HABITACIONES", new String[]{"estado habitaciones", "estado de las habitaciones", "mapa habitaciones", "habitaciones ocupadas", "ver habitaciones"});
        SINONIMOS.put("DISPONIBILIDAD", new String[]{"disponible", "disponibilidad", "hay habitaciones", "habitaciones libres"});
        SINONIMOS.put("ESTADO_RESERVA", new String[]{"estado reserva", "buscar reserva", "consultar reserva", "ver reserva"});
        SINONIMOS.put("RESERVAS_HOY", new String[]{"reservas hoy", "reservas del dia", "movimiento de hoy"});
        SINONIMOS.put("RESERVAS_ACTIVAS", new String[]{"reservas activas", "reservas vigentes"});
        SINONIMOS.put("BUSCAR_CLIENTE", new String[]{"buscar cliente", "cliente llamado", "datos del cliente", "informacion del cliente"});
        SINONIMOS.put("OCUPACION", new String[]{"ocupacion", "porcentaje de ocupacion", "como estamos hoy", "resumen del hotel"});
        SINONIMOS.put("FACTURA_POR_ID", new String[]{"factura #", "ver factura", "buscar factura", "detalle factura"});
        SINONIMOS.put("FACTURA_POR_RESERVA", new String[]{"factura de la reserva", "factura reserva", "cobro de la reserva"});
        SINONIMOS.put("FACTURAS_CLIENTE", new String[]{"facturas del cliente", "historial de pagos", "pagos del cliente"});
        SINONIMOS.put("FACTURAS_PENDIENTES", new String[]{"facturas pendientes", "facturas sin pagar", "cuentas pendientes", "quien debe"});
        SINONIMOS.put("INGRESOS_HOY", new String[]{"ingresos hoy", "recaudacion hoy", "cuanto ingreso hoy", "total facturado hoy"});
        SINONIMOS.put("RESUMEN_FACTURACION", new String[]{"resumen facturacion", "total facturado", "ingresos totales"});
        SINONIMOS.put("EMPLEADOS_ACTIVOS", new String[]{"empleados activos", "personal activo", "quienes trabajan", "lista de empleados"});
        SINONIMOS.put("EMPLEADOS_POR_CARGO", new String[]{"empleados de recepcion", "recepcionistas", "personal de limpieza"});
        SINONIMOS.put("BUSCAR_EMPLEADO", new String[]{"buscar empleado", "empleado llamado", "informacion del empleado"});
        SINONIMOS.put("TURNOS_HOY", new String[]{"quien esta de turno", "turno de hoy", "quien trabaja hoy"});
        SINONIMOS.put("CARGOS_DISPONIBLES", new String[]{"cargos disponibles", "que cargos hay", "lista de cargos"});
        SINONIMOS.put("MANTENIMIENTO_PENDIENTE", new String[]{"mantenimientos pendientes", "mantenimiento pendiente", "que mantenimientos hay"});
        SINONIMOS.put("MANTENIMIENTO_POR_HABITACION", new String[]{"mantenimiento habitacion", "historial mantenimiento", "reparaciones habitacion"});
        SINONIMOS.put("MANTENIMIENTO_TODOS", new String[]{"todos los mantenimientos", "historial mantenimientos", "ver mantenimientos"});
        SINONIMOS.put("MANTENIMIENTO_COSTO", new String[]{"costo mantenimiento", "gastos mantenimiento", "cuanto se ha gastado"});

        // ─── CONSULTAS DE RESERVAS ─────────────────────────────────────────────
        SINONIMOS.put("RESERVAS_CONFIRMADAS", new String[]{"reservas confirmadas", "reservas confirmada", "lista reservas confirmadas", "confirmadas"});
        SINONIMOS.put("RESERVAS_PENDIENTES", new String[]{"reservas pendientes", "reservas pendiente", "pendientes", "lista reservas pendientes"});
        SINONIMOS.put("RESERVAS_COMPLETADAS", new String[]{"reservas completadas", "reservas completada", "historial reservas", "completadas"});
        SINONIMOS.put("RESERVAS_CANCELADAS", new String[]{"reservas canceladas", "reservas cancelada", "canceladas", "lista reservas canceladas"});
        SINONIMOS.put("RESERVAS_MES", new String[]{"reservas del mes", "reservas este mes", "reservas mes actual", "reservas en el mes"});
        SINONIMOS.put("RESERVAS_MAÑANA", new String[]{"reservas mañana", "llegadas mañana", "check-in mañana", "para mañana"});
        SINONIMOS.put("RESERVAS_PROXIMA_SEMANA", new String[]{"proxima semana", "reservas proxima semana", "siguiente semana", "próximos 7 días", "los proximos 7 dias"});
        SINONIMOS.put("RESERVAS_POR_CLIENTE", new String[]{"reservas por cliente", "reservas del cliente", "buscar reservas por cliente"});
        SINONIMOS.put("RESERVAS_POR_HABITACION", new String[]{"reservas por habitacion", "historial de reservas de la habitacion", "reservas habitacion"});
        SINONIMOS.put("RESERVA_MAS_CARA", new String[]{"reserva mas cara", "mayor valor", "reserva mas costosa", "reserva de mayor precio"});
        SINONIMOS.put("RESERVA_MAS_LARGA", new String[]{"reserva mas larga", "mayor duracion", "reserva de mas noches", "estancia mas larga"});

        // ─── NUEVAS CONSULTAS DE HABITACIONES ──────────────────────────────────
        SINONIMOS.put("HABITACIONES_LIBRES_HOY", new String[]{"habitaciones libres hoy", "habitaciones disponibles hoy", "libres hoy", "disponibles hoy"});
        SINONIMOS.put("HABITACIONES_POR_TIPO", new String[]{"habitaciones por tipo", "agrupar habitaciones por tipo", "tipos de habitacion", "clasificar habitaciones"});
        SINONIMOS.put("HABITACIONES_MANTENIMIENTO", new String[]{"habitaciones en mantenimiento", "habitaciones mantenimiento", "habitaciones en reparacion"});
        SINONIMOS.put("HABITACIONES_RESERVADAS", new String[]{"habitaciones reservadas", "habitaciones reservadas hoy", "que habitaciones estan reservadas"});
        SINONIMOS.put("PRECIO_HABITACION", new String[]{"precio habitacion", "costo habitacion", "valor habitacion", "cuanto cuesta la habitacion"});
        SINONIMOS.put("TIPOS_HABITACION", new String[]{"tipos de habitacion", "que tipos hay", "categorias habitaciones", "tipos disponibles"});
        SINONIMOS.put("CAPACIDAD_MAXIMA", new String[]{"capacidad maxima", "capacidad de habitaciones", "personas por habitacion", "maximo de personas"});
        SINONIMOS.put("TARIFAS", new String[]{"tarifas", "precios por tipo", "tabla de precios", "lista de precios"});
        SINONIMOS.put("HABITACIONES_DISPONIBLES_FECHAS", new String[]{"habitaciones disponibles fechas", "disponibilidad en fechas"});

        // ─── NUEVAS CONSULTAS DE CLIENTES ─────────────────────────────────────
        SINONIMOS.put("CLIENTES_VIP", new String[]{"clientes vip", "clientes VIP", "clientes especiales", "clientes destacados"});
        SINONIMOS.put("CLIENTES_FRECUENTES", new String[]{"clientes frecuentes", "clientes mas activos", "clientes con mas reservas", "mejores clientes"});
        SINONIMOS.put("CLIENTE_MAS_FIEL", new String[]{"cliente mas fiel", "cliente con mas noches", "cliente que mas se queda"});
        SINONIMOS.put("CLIENTES_REGISTRADOS", new String[]{"clientes registrados", "total clientes", "cuantos clientes hay", "numero de clientes"});
        SINONIMOS.put("CLIENTE_POR_DOCUMENTO", new String[]{"cliente por documento", "buscar cliente por documento", "cedula del cliente"});
        SINONIMOS.put("CLIENTE_POR_TELEFONO", new String[]{"cliente por telefono", "buscar cliente por telefono", "numero de telefono cliente"});
        SINONIMOS.put("CLIENTES_POR_NACIONALIDAD", new String[]{"clientes por nacionalidad", "nacionalidad de clientes", "de donde son los clientes"});
    }

    private final IHabitacionDAO habitacionDAO;
    private final IReservaDAO reservaDAO;
    private final IReservaBusqueda reservaBusqueda;
    private final IClienteDAO clienteDAO;
    private final IFacturaDAO facturaDAO;
    private final IEmpleadoDAO empleadoDAO;
    private final IMantenimientoDAO mantenimientoDAO;
    private final GeminiApiService gemini;

    public ChatbotService(IHabitacionDAO habitacionDAO, IReservaDAO reservaDAO,
            IReservaBusqueda reservaBusqueda, IClienteDAO clienteDAO,
            IFacturaDAO facturaDAO, IEmpleadoDAO empleadoDAO,
            IMantenimientoDAO mantenimientoDAO, GeminiApiService gemini) {
        this.habitacionDAO = habitacionDAO;
        this.reservaDAO = reservaDAO;
        this.reservaBusqueda = reservaBusqueda;
        this.clienteDAO = clienteDAO;
        this.facturaDAO = facturaDAO;
        this.empleadoDAO = empleadoDAO;
        this.mantenimientoDAO = mantenimientoDAO;
        this.gemini = gemini;
    }

    public String obtenerMensajeBienvenida() {
        if (gemini != null && gemini.estaConfigurado()) {
            try {
                String contexto = obtenerResumenGeneral();
                return gemini.consultar(contexto, "Preséntate brevemente como Nova. Máximo 3 líneas.");
            } catch (Exception e) {
            }
        }
        return mensajeBienvenidaEstatico();
    }

    public String procesarMensaje(String mensajeUsuario) {
        if (mensajeUsuario == null || mensajeUsuario.trim().isEmpty()) {
            return "⚠️ Escribe tu consulta. Escribe 'ayuda' para ver las opciones.";
        }

        String normalizado = normalizar(mensajeUsuario);
        String intencion = detectarIntencion(normalizado, mensajeUsuario);
        String contextoBD = obtenerContexto(intencion, mensajeUsuario, normalizado);

        if (gemini != null && gemini.estaConfigurado()) {
            try {
                return gemini.consultar(contextoBD, mensajeUsuario);
            } catch (Exception e) {
            }
        }

        return contextoBD.isBlank() ? respuestaNoEntendida() : contextoBD;
    }

    public void limpiarHistorial() {
        if (gemini != null) {
            gemini.limpiarHistorial();
        }
    }

    private String fmtMoneda(double monto) {
        return String.format("$%,.0f", monto);
    }

    private String fmtFecha(LocalDate fecha) {
        return fecha != null ? fecha.format(FECHA_FORMATTER) : "—";
    }

    private String detectarIntencion(String normalizado, String original) {
        for (Map.Entry<String, String[]> e : SINONIMOS.entrySet()) {
            for (String clave : e.getValue()) {
                if (normalizado.contains(clave)) {
                    return e.getKey();
                }
            }
        }
        if (PATRON_ID_RESERVA.matcher(normalizado).find()) {
            return "ESTADO_RESERVA";
        }
        if (PATRON_ID_FACTURA.matcher(normalizado).find()) {
            return "FACTURA_POR_ID";
        }
        if (PATRON_HABITACION.matcher(original).find()) {
            return "PRECIO_HABITACION";
        }
        if (PATRON_DOCUMENTO.matcher(original).find()) {
            return "CLIENTE_POR_DOCUMENTO";
        }
        if (PATRON_TELEFONO.matcher(original).find()) {
            return "CLIENTE_POR_TELEFONO";
        }
        if (PATRON_NOMBRE_CLIENTE.matcher(original).find()) {
            return "RESERVAS_POR_CLIENTE";
        }
        if (contarFechas(original) >= 2) {
            return "DISPONIBILIDAD";
        }
        return "GENERAL";
    }

    private String normalizar(String texto) {
        return texto.trim().toLowerCase()
                .replaceAll("[áàä]", "a").replaceAll("[éèë]", "e")
                .replaceAll("[íìï]", "i").replaceAll("[óòö]", "o")
                .replaceAll("[úùü]", "u").replaceAll("[ñ]", "n");
    }

    private String obtenerContexto(String intencion, String original, String norm) {
        try {
            switch (intencion) {
                // Generales
                case "SALUDO":
                    return obtenerResumenGeneral();
                case "AYUDA":
                    return textoAyuda();
                case "OCUPACION":
                    return obtenerResumenGeneral();

                // Reservas existentes
                case "CHECKINS_HOY":
                    return contextoCheckinsHoy();
                case "CHECKOUTS_HOY":
                    return contextoCheckoutsHoy();
                case "ESTADO_RESERVA":
                    return contextoEstadoReserva(original);
                case "RESERVAS_HOY":
                    return contextoReservasHoy();
                case "RESERVAS_ACTIVAS":
                    return contextoReservasActivas();

                // Nuevas consultas de RESERVAS
                case "RESERVAS_CONFIRMADAS":
                    return contextoReservasPorEstado("CONFIRMADA", "CONFIRMADAS");
                case "RESERVAS_PENDIENTES":
                    return contextoReservasPorEstado("PENDIENTE", "PENDIENTES");
                case "RESERVAS_COMPLETADAS":
                    return contextoReservasPorEstado("COMPLETADA", "COMPLETADAS");
                case "RESERVAS_CANCELADAS":
                    return contextoReservasPorEstado("CANCELADA", "CANCELADAS");
                case "RESERVAS_MES":
                    return contextoReservasDelMes();
                case "RESERVAS_MAÑANA":
                    return contextoReservasManana();
                case "RESERVAS_PROXIMA_SEMANA":
                    return contextoReservasProximaSemana();
                case "RESERVAS_POR_CLIENTE":
                    return contextoReservasPorCliente(original);
                case "RESERVAS_POR_HABITACION":
                    return contextoReservasPorHabitacion(original);
                case "RESERVA_MAS_CARA":
                    return contextoReservaMasCara();
                case "RESERVA_MAS_LARGA":
                    return contextoReservaMasLarga();

                // Nuevas consultas de HABITACIONES
                case "ESTADO_HABITACIONES":
                    return contextoEstadoHabitaciones();
                case "DISPONIBILIDAD":
                    return contextoDisponibilidad(original);
                case "HABITACIONES_LIBRES_HOY":
                    return contextoHabitacionesLibresHoy();
                case "HABITACIONES_POR_TIPO":
                    return contextoHabitacionesPorTipo();
                case "HABITACIONES_MANTENIMIENTO":
                    return contextoHabitacionesMantenimiento();
                case "HABITACIONES_RESERVADAS":
                    return contextoHabitacionesReservadas();
                case "PRECIO_HABITACION":
                    return contextoPrecioHabitacion(original);
                case "TIPOS_HABITACION":
                    return contextoTiposHabitacion();
                case "CAPACIDAD_MAXIMA":
                    return contextoCapacidadMaxima();
                case "TARIFAS":
                    return contextoTarifas();

                // Nuevas consultas de CLIENTES
                case "BUSCAR_CLIENTE":
                    return contextoBuscarCliente(original);
                case "CLIENTES_VIP":
                    return contextoClientesVip();
                case "CLIENTES_FRECUENTES":
                    return contextoClientesFrecuentes();
                case "CLIENTE_MAS_FIEL":
                    return contextoClienteMasFiel();
                case "CLIENTES_REGISTRADOS":
                    return contextoClientesRegistrados();
                case "CLIENTE_POR_DOCUMENTO":
                    return contextoClientePorDocumento(original);
                case "CLIENTE_POR_TELEFONO":
                    return contextoClientePorTelefono(original);
                case "CLIENTES_POR_NACIONALIDAD":
                    return contextoClientesPorNacionalidad();

                // Facturación
                case "FACTURA_POR_ID":
                    return contextoFacturaPorId(original);
                case "FACTURA_POR_RESERVA":
                    return contextoFacturaPorReserva(original);
                case "FACTURAS_CLIENTE":
                    return contextoFacturasCliente(original);
                case "FACTURAS_PENDIENTES":
                    return contextoFacturasPendientes();
                case "INGRESOS_HOY":
                    return contextoIngresosHoy();
                case "RESUMEN_FACTURACION":
                    return contextoResumenFacturacion();

                // Empleados
                case "EMPLEADOS_ACTIVOS":
                    return contextoEmpleadosActivos();
                case "EMPLEADOS_POR_CARGO":
                    return contextoEmpleadosPorCargo(original);
                case "BUSCAR_EMPLEADO":
                    return contextoBuscarEmpleado(original);
                case "TURNOS_HOY":
                    return contextoTurnosHoy();
                case "CARGOS_DISPONIBLES":
                    return contextoCargosDisponibles();

                // Mantenimiento
                case "MANTENIMIENTO_PENDIENTE":
                    return contextoMantenimientoPendiente();
                case "MANTENIMIENTO_POR_HABITACION":
                    return contextoMantenimientoPorHabitacion(original);
                case "MANTENIMIENTO_TODOS":
                    return contextoTodosMantenimientos();
                case "MANTENIMIENTO_COSTO":
                    return contextoResumenCostoMantenimiento();

                default:
                    return obtenerResumenGeneral();
            }
        } catch (Exception e) {
            return "❌ Error: " + e.getMessage();
        }
    }

    // ==================== CONSULTAS DE RESERVAS ====================
    private String contextoReservasPorEstado(String estado, String titulo) {
        List<Reserva> reservas = reservaBusqueda.buscarPorEstado(estado);
        if (reservas.isEmpty()) {
            return "📋 No hay reservas " + titulo.toLowerCase() + " en este momento.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📋 RESERVAS %s (%d)\n", titulo, reservas.size()));
        sb.append(LINEA).append("\n");
        for (Reserva r : reservas) {
            sb.append(String.format("   • #%d | %s | Hab.%s | %s→%s | %d noches | %s\n",
                    r.getId(),
                    r.getCliente() != null ? truncar(r.getCliente().obtenerNombreCompleto(), 25) : "—",
                    r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—",
                    fmtFecha(r.getFechaEntrada()), fmtFecha(r.getFechaSalida()),
                    r.getFechaEntrada().until(r.getFechaSalida()).getDays(),
                    fmtMoneda(r.getPrecioTotal())));
        }
        return sb.toString();
    }

    private String contextoReservasDelMes() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate finMes = hoy.withDayOfMonth(hoy.lengthOfMonth());

        List<Reserva> reservas = reservaDAO.listarTodas().stream()
                .filter(r -> !r.getFechaEntrada().isBefore(inicioMes) && !r.getFechaEntrada().isAfter(finMes))
                .sorted(Comparator.comparing(Reserva::getFechaEntrada))
                .collect(Collectors.toList());

        if (reservas.isEmpty()) {
            return "📋 No hay reservas en el mes de " + hoy.getMonth().getDisplayName(java.time.format.TextStyle.FULL, new Locale("es", "ES"));
        }

        Map<String, Long> porEstado = reservas.stream()
                .collect(Collectors.groupingBy(r -> r.getEstado().name(), Collectors.counting()));
        double totalFacturado = reservas.stream().mapToDouble(Reserva::getPrecioTotal).sum();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📋 RESERVAS DEL MES DE %s (%d)\n",
                hoy.getMonth().getDisplayName(java.time.format.TextStyle.FULL, new Locale("es", "ES")).toUpperCase(),
                reservas.size()));
        sb.append(LINEA).append("\n");
        sb.append("   📊 Resumen:\n");
        for (Map.Entry<String, Long> e : porEstado.entrySet()) {
            sb.append(String.format("      • %s: %d\n", traducirEstadoReserva(Reserva.EstadoReserva.valueOf(e.getKey())), e.getValue()));
        }
        sb.append(String.format("   💰 Total facturado: %s\n\n", fmtMoneda(totalFacturado)));
        sb.append("   📅 Detalle:\n");
        for (Reserva r : reservas) {
            sb.append(String.format("      • #%d | %s | Hab.%s | %s | %s\n",
                    r.getId(),
                    r.getCliente() != null ? truncar(r.getCliente().obtenerNombreCompleto(), 20) : "—",
                    r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—",
                    fmtFecha(r.getFechaEntrada()),
                    traducirEstadoReserva(r.getEstado())));
        }
        return sb.toString();
    }

    private String contextoReservasManana() {
        LocalDate manana = LocalDate.now().plusDays(1);
        List<Reserva> reservas = reservaBusqueda.buscarPorRangoFechas(manana, manana);
        if (reservas.isEmpty()) {
            return "📋 No hay llegadas programadas para mañana " + fmtFecha(manana);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📋 LLEGADAS MAÑANA (%s)\n", fmtFecha(manana)));
        sb.append(LINEA).append("\n");
        for (Reserva r : reservas) {
            sb.append(String.format("   • #%d | %s | Hab.%s | %d noches | [%s]\n",
                    r.getId(),
                    r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—",
                    r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—",
                    r.getFechaEntrada().until(r.getFechaSalida()).getDays(),
                    traducirEstadoReserva(r.getEstado())));
        }
        return sb.toString();
    }

    private String contextoReservasProximaSemana() {
        LocalDate hoy = LocalDate.now();
        LocalDate dentroDe7Dias = hoy.plusDays(7);
        List<Reserva> reservas = reservaDAO.listarTodas().stream()
                .filter(r -> !r.getFechaEntrada().isBefore(hoy) && !r.getFechaEntrada().isAfter(dentroDe7Dias))
                .sorted(Comparator.comparing(Reserva::getFechaEntrada))
                .collect(Collectors.toList());

        if (reservas.isEmpty()) {
            return "📋 No hay reservas en los próximos 7 días.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📋 RESERVAS PRÓXIMOS 7 DÍAS (%d)\n", reservas.size()));
        sb.append(LINEA).append("\n");
        sb.append("   📅 " + fmtFecha(hoy) + " → " + fmtFecha(dentroDe7Dias) + "\n\n");
        for (Reserva r : reservas) {
            sb.append(String.format("   • #%d | %s | Hab.%s | %s | %s\n",
                    r.getId(),
                    r.getCliente() != null ? truncar(r.getCliente().obtenerNombreCompleto(), 20) : "—",
                    r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—",
                    fmtFecha(r.getFechaEntrada()),
                    traducirEstadoReserva(r.getEstado())));
        }
        return sb.toString();
    }

    private String contextoReservasPorCliente(String mensaje) {
        Matcher m = PATRON_NOMBRE_CLIENTE.matcher(mensaje);
        String nombreCliente = "";
        if (m.find()) {
            nombreCliente = m.group(1).trim();
        } else {
            String termino = mensaje.replaceAll("(?i)reservas por cliente|reservas del cliente|buscar reservas por cliente", "").trim();
            if (!termino.isEmpty()) {
                nombreCliente = termino;
            }
        }

        if (nombreCliente.isEmpty()) {
            return "⚠️ Escribe el nombre del cliente. Ej: 'reservas por cliente Pedro'";
        }

        String norm = normalizar(nombreCliente);
        Optional<Cliente> cliente = clienteDAO.listarTodos().stream()
                .filter(c -> normalizar(c.obtenerNombreCompleto()).contains(norm))
                .findFirst();

        if (cliente.isEmpty()) {
            return "❌ No se encontró el cliente: '" + nombreCliente + "'";
        }

        List<Reserva> reservas = reservaDAO.listarTodas().stream()
                .filter(r -> r.getCliente() != null && r.getCliente().getId() == cliente.get().getId())
                .sorted(Comparator.comparing(Reserva::getId).reversed())
                .collect(Collectors.toList());

        if (reservas.isEmpty()) {
            return "📋 El cliente " + cliente.get().obtenerNombreCompleto() + " no tiene reservas registradas.";
        }

        double totalGastado = reservas.stream().mapToDouble(Reserva::getPrecioTotal).sum();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📋 RESERVAS DE %s (%d)\n", cliente.get().obtenerNombreCompleto(), reservas.size()));
        sb.append(LINEA).append("\n");
        sb.append(String.format("   💰 Total gastado: %s\n\n", fmtMoneda(totalGastado)));
        for (Reserva r : reservas) {
            sb.append(String.format("   • #%d | Hab.%s | %s→%s | %s | %s\n",
                    r.getId(),
                    r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—",
                    fmtFecha(r.getFechaEntrada()), fmtFecha(r.getFechaSalida()),
                    fmtMoneda(r.getPrecioTotal()),
                    traducirEstadoReserva(r.getEstado())));
        }
        return sb.toString();
    }

    private String contextoReservasPorHabitacion(String mensaje) {
        Matcher m = PATRON_HABITACION.matcher(mensaje);
        String numHab = "";
        if (m.find()) {
            numHab = m.group(1);
        } else {
            String termino = mensaje.replaceAll("(?i)reservas por habitacion|historial de reservas de la habitacion|reservas habitacion", "").trim();
            if (!termino.isEmpty()) {
                numHab = termino;
            }
        }

        if (numHab.isEmpty()) {
            return "⚠️ Escribe el número de habitación. Ej: 'reservas por habitacion 101'";
        }

        final String numeroHab = numHab.trim();

        List<Reserva> reservas = reservaDAO.listarTodas().stream()
                .filter(r -> r.getHabitacion() != null && numeroHab.equals(r.getHabitacion().getNumero()))
                .sorted(Comparator.comparing(Reserva::getFechaEntrada).reversed())
                .collect(Collectors.toList());

        if (reservas.isEmpty()) {
            return "📋 La habitación " + numeroHab + " no tiene reservas registradas.";
        }

        double totalFacturado = reservas.stream().mapToDouble(Reserva::getPrecioTotal).sum();
        long totalNoches = reservas.stream()
                .mapToLong(r -> r.getFechaEntrada().until(r.getFechaSalida()).getDays())
                .sum();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📋 HISTORIAL DE RESERVAS - Habitación %s (%d reservas)\n", numeroHab, reservas.size()));
        sb.append(LINEA).append("\n");
        sb.append(String.format("   💰 Total facturado: %s | 📅 Total noches: %d\n\n", fmtMoneda(totalFacturado), totalNoches));
        for (Reserva r : reservas) {
            sb.append(String.format("   • #%d | %s | %s→%s | %d noches | %s | %s\n",
                    r.getId(),
                    r.getCliente() != null ? truncar(r.getCliente().obtenerNombreCompleto(), 20) : "—",
                    fmtFecha(r.getFechaEntrada()), fmtFecha(r.getFechaSalida()),
                    r.getFechaEntrada().until(r.getFechaSalida()).getDays(),
                    fmtMoneda(r.getPrecioTotal()),
                    traducirEstadoReserva(r.getEstado())));
        }
        return sb.toString();
    }

    private String contextoReservaMasCara() {
        List<Reserva> todas = reservaDAO.listarTodas();
        if (todas.isEmpty()) {
            return "📋 No hay reservas registradas.";
        }

        Optional<Reserva> max = todas.stream()
                .max(Comparator.comparingDouble(Reserva::getPrecioTotal));

        if (max.isEmpty()) {
            return "📋 No se pudo determinar la reserva más cara.";
        }

        Reserva r = max.get();
        StringBuilder sb = new StringBuilder();
        sb.append("💰 RESERVA MÁS CARA\n");
        sb.append(LINEA).append("\n");
        sb.append(String.format("   • Reserva #%d\n", r.getId()));
        sb.append(String.format("   • Cliente: %s\n", r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—"));
        sb.append(String.format("   • Habitación: %s\n", r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—"));
        sb.append(String.format("   • Período: %s → %s (%d noches)\n",
                fmtFecha(r.getFechaEntrada()), fmtFecha(r.getFechaSalida()),
                r.getFechaEntrada().until(r.getFechaSalida()).getDays()));
        sb.append(String.format("   • TOTAL: %s\n", fmtMoneda(r.getPrecioTotal())));
        sb.append(String.format("   • Estado: %s", traducirEstadoReserva(r.getEstado())));
        return sb.toString();
    }

    private String contextoReservaMasLarga() {
        List<Reserva> todas = reservaDAO.listarTodas();
        if (todas.isEmpty()) {
            return "📋 No hay reservas registradas.";
        }

        Optional<Reserva> max = todas.stream()
                .max(Comparator.comparingLong(r -> r.getFechaEntrada().until(r.getFechaSalida()).getDays()));

        if (max.isEmpty()) {
            return "📋 No se pudo determinar la reserva más larga.";
        }

        Reserva r = max.get();
        long noches = r.getFechaEntrada().until(r.getFechaSalida()).getDays();

        StringBuilder sb = new StringBuilder();
        sb.append("📅 RESERVA MÁS LARGA\n");
        sb.append(LINEA).append("\n");
        sb.append(String.format("   • Reserva #%d\n", r.getId()));
        sb.append(String.format("   • Cliente: %s\n", r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—"));
        sb.append(String.format("   • Habitación: %s\n", r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—"));
        sb.append(String.format("   • Período: %s → %s\n", fmtFecha(r.getFechaEntrada()), fmtFecha(r.getFechaSalida())));
        sb.append(String.format("   • Duración: %d noches\n", noches));
        sb.append(String.format("   • Total: %s\n", fmtMoneda(r.getPrecioTotal())));
        sb.append(String.format("   • Estado: %s", traducirEstadoReserva(r.getEstado())));
        return sb.toString();
    }

    // ==================== CONSULTAS DE HABITACIONES ====================
    private String contextoEstadoHabitaciones() {
        List<Habitacion> todas = habitacionDAO.listarTodas();
        Map<Habitacion.EstadoHabitacion, List<Habitacion>> porEstado
                = todas.stream().collect(Collectors.groupingBy(Habitacion::getEstado));

        List<Habitacion> disponibles = porEstado.getOrDefault(Habitacion.EstadoHabitacion.DISPONIBLE, List.of());
        List<Habitacion> ocupadas = porEstado.getOrDefault(Habitacion.EstadoHabitacion.OCUPADA, List.of());
        List<Habitacion> reservadas = porEstado.getOrDefault(Habitacion.EstadoHabitacion.RESERVADA, List.of());
        List<Habitacion> mantenimiento = porEstado.getOrDefault(Habitacion.EstadoHabitacion.MANTENIMIENTO, List.of());

        StringBuilder sb = new StringBuilder();
        sb.append("🏨 ESTADO DE HABITACIONES\n");
        sb.append(LINEA).append("\n");
        sb.append(String.format("   Total: %d   |   Disp: %d   |   Ocup: %d   |   Reserv: %d   |   Mant: %d\n\n",
                todas.size(), disponibles.size(), ocupadas.size(), reservadas.size(), mantenimiento.size()));

        if (!disponibles.isEmpty()) {
            sb.append("✅ DISPONIBLES:\n");
            for (Habitacion h : disponibles) {
                sb.append(String.format("   • Hab.%s | %s | %s/noche\n",
                        h.getNumero(),
                        h.getTipoHabitacion() != null ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "—",
                        fmtMoneda(h.calcularPrecioFinal())));
            }
        }

        if (!ocupadas.isEmpty()) {
            sb.append("\n🔑 OCUPADAS:\n");
            for (Habitacion h : ocupadas) {
                sb.append(String.format("   • Hab.%s | %s\n",
                        h.getNumero(),
                        h.getTipoHabitacion() != null ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "—"));
            }
        }

        if (!reservadas.isEmpty()) {
            sb.append("\n📅 RESERVADAS:\n");
            for (Habitacion h : reservadas) {
                sb.append(String.format("   • Hab.%s | %s\n",
                        h.getNumero(),
                        h.getTipoHabitacion() != null ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "—"));
            }
        }

        if (!mantenimiento.isEmpty()) {
            sb.append("\n🔧 EN MANTENIMIENTO:\n");
            for (Habitacion h : mantenimiento) {
                sb.append(String.format("   • Hab.%s | %s\n",
                        h.getNumero(),
                        h.getTipoHabitacion() != null ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "—"));
            }
        }
        return sb.toString();
    }

    private String contextoDisponibilidad(String mensaje) {
        List<LocalDate> fechas = extraerFechas(mensaje);
        if (fechas.size() < 2) {
            return contextoEstadoHabitaciones();
        }

        LocalDate entrada = fechas.get(0);
        LocalDate salida = fechas.get(1);
        if (!salida.isAfter(entrada)) {
            return "⚠️ La fecha de salida debe ser posterior a la entrada.";
        }

        int personas = extraerNumeroPersonas(mensaje, 1);
        List<Habitacion> disponibles = habitacionDAO.buscarDisponibles(
                new BusquedaDisponibilidadDTO(entrada, salida, personas));
        long noches = entrada.until(salida).getDays();

        if (disponibles.isEmpty()) {
            return String.format("🔍 No hay habitaciones disponibles del %s al %s (%d noches, %d personas)",
                    fmtFecha(entrada), fmtFecha(salida), noches, personas);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🔍 DISPONIBILIDAD: %s → %s (%d noches, %d personas)\n",
                fmtFecha(entrada), fmtFecha(salida), noches, personas));
        sb.append(LINEA).append("\n");
        for (Habitacion h : disponibles) {
            double precio = h.calcularPrecioFinal();
            sb.append(String.format("   • Hab.%s | %s | %s/noche | Total: %s\n",
                    h.getNumero(),
                    h.getTipoHabitacion() != null ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "—",
                    fmtMoneda(precio), fmtMoneda(precio * noches)));
        }
        sb.append(LINEA).append("\n");
        sb.append(String.format("✅ %d habitación(es) disponible(s)", disponibles.size()));
        return sb.toString();
    }

    private String contextoHabitacionesLibresHoy() {
        LocalDate hoy = LocalDate.now();
        List<Habitacion> libresHoy = habitacionDAO.listarTodas().stream()
                .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.DISPONIBLE)
                .collect(Collectors.toList());

        if (libresHoy.isEmpty()) {
            return "🏨 No hay habitaciones libres para hoy " + fmtFecha(hoy);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🏨 HABITACIONES LIBRES HOY (%s)\n", fmtFecha(hoy)));
        sb.append(LINEA).append("\n");
        for (Habitacion h : libresHoy) {
            sb.append(String.format("   • Hab.%s | %s | %s/noche\n",
                    h.getNumero(),
                    h.getTipoHabitacion() != null ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "—",
                    fmtMoneda(h.calcularPrecioFinal())));
        }
        return sb.toString();
    }

    private String contextoHabitacionesPorTipo() {
        List<Habitacion> todas = habitacionDAO.listarTodas();
        Map<String, List<Habitacion>> porTipo = todas.stream()
                .collect(Collectors.groupingBy(h -> h.getTipoHabitacion() != null
                ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "SIMPLE"));

        StringBuilder sb = new StringBuilder();
        sb.append("🏨 HABITACIONES POR TIPO\n");
        sb.append(LINEA).append("\n");

        for (Map.Entry<String, List<Habitacion>> entry : porTipo.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
            sb.append(String.format("📌 %s (%d habitaciones):\n", entry.getKey().toUpperCase(), entry.getValue().size()));
            for (Habitacion h : entry.getValue()) {
                sb.append(String.format("   • Hab.%s | %s | %s\n",
                        h.getNumero(),
                        h.getEstado().name(),
                        fmtMoneda(h.calcularPrecioFinal())));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String contextoHabitacionesMantenimiento() {
        List<Habitacion> mantenimiento = habitacionDAO.listarTodas().stream()
                .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.MANTENIMIENTO)
                .collect(Collectors.toList());

        if (mantenimiento.isEmpty()) {
            return "🔧 No hay habitaciones en mantenimiento.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🔧 HABITACIONES EN MANTENIMIENTO\n");
        sb.append(LINEA).append("\n");
        for (Habitacion h : mantenimiento) {
            sb.append(String.format("   • Hab.%s | %s\n",
                    h.getNumero(),
                    h.getTipoHabitacion() != null ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "—"));
        }
        return sb.toString();
    }

    private String contextoHabitacionesReservadas() {
        List<Habitacion> reservadas = habitacionDAO.listarTodas().stream()
                .filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.RESERVADA)
                .collect(Collectors.toList());

        if (reservadas.isEmpty()) {
            return "📅 No hay habitaciones reservadas en este momento.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📅 HABITACIONES RESERVADAS\n");
        sb.append(LINEA).append("\n");
        for (Habitacion h : reservadas) {
            sb.append(String.format("   • Hab.%s | %s | %s/noche\n",
                    h.getNumero(),
                    h.getTipoHabitacion() != null ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "—",
                    fmtMoneda(h.calcularPrecioFinal())));
        }
        return sb.toString();
    }

    private String contextoPrecioHabitacion(String mensaje) {
        Matcher m = PATRON_HABITACION.matcher(mensaje);
        String numHab = "";
        if (m.find()) {
            numHab = m.group(1);
        } else {
            String termino = mensaje.replaceAll("(?i)precio habitacion|costo habitacion|valor habitacion|cuanto cuesta la habitacion", "").trim();
            if (!termino.isEmpty()) {
                numHab = termino;
            }
        }

        if (numHab.isEmpty()) {
            return "⚠️ Escribe el número de habitación. Ej: 'precio habitacion 101'";
        }

        final String numeroHab = numHab;

        Optional<Habitacion> habitacion = habitacionDAO.listarTodas().stream()
                .filter(h -> numeroHab != null && numeroHab.equals(h.getNumero()))
                .findFirst();

        if (habitacion.isEmpty()) {
            return "❌ No se encontró la habitación " + numHab;
        }

        Habitacion h = habitacion.get();
        return String.format("💰 PRECIO HABITACIÓN %s\n" + LINEA + "\n"
                + "   • Tipo: %s\n"
                + "   • Precio por noche: %s\n"
                + "   • Capacidad: %d personas\n"
                + "   • Estado: %s",
                numHab,
                h.getTipoHabitacion() != null ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "SIMPLE",
                fmtMoneda(h.calcularPrecioFinal()),
                h.getTipoHabitacion() != null ? h.getTipoHabitacion().getCapacidadMaxima() : 2,
                h.getEstado().name());
    }

    private String contextoTiposHabitacion() {
        List<Habitacion> todas = habitacionDAO.listarTodas();
        Map<String, List<Habitacion>> porTipo = todas.stream()
                .collect(Collectors.groupingBy(h -> h.getTipoHabitacion() != null
                ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "SIMPLE"));

        StringBuilder sb = new StringBuilder();
        sb.append("🏨 TIPOS DE HABITACIÓN\n");
        sb.append(LINEA).append("\n");

        for (Map.Entry<String, List<Habitacion>> entry : porTipo.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
            double precioPromedio = entry.getValue().stream()
                    .mapToDouble(Habitacion::calcularPrecioFinal)
                    .average()
                    .orElse(0);
            sb.append(String.format("📌 %s\n", entry.getKey().toUpperCase()));
            sb.append(String.format("   • Cantidad: %d habitaciones\n", entry.getValue().size()));
            sb.append(String.format("   • Precio promedio: %s/noche\n", fmtMoneda(precioPromedio)));
            sb.append(String.format("   • Capacidad: %d personas\n\n",
                    entry.getValue().get(0).getTipoHabitacion() != null
                    ? entry.getValue().get(0).getTipoHabitacion().getCapacidadMaxima() : 2));
        }
        return sb.toString();
    }

    private String contextoCapacidadMaxima() {
        List<Habitacion> todas = habitacionDAO.listarTodas();
        int maxCapacidad = todas.stream()
                .mapToInt(h -> h.getTipoHabitacion() != null ? h.getTipoHabitacion().getCapacidadMaxima() : 2)
                .max()
                .orElse(0);

        List<Habitacion> conMaxCapacidad = todas.stream()
                .filter(h -> (h.getTipoHabitacion() != null ? h.getTipoHabitacion().getCapacidadMaxima() : 2) == maxCapacidad)
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("👥 CAPACIDAD MÁXIMA DEL HOTEL\n");
        sb.append(LINEA).append("\n");
        sb.append(String.format("   Máximo de personas por habitación: %d\n", maxCapacidad));
        sb.append("   Habitaciones con esta capacidad:\n");
        for (Habitacion h : conMaxCapacidad) {
            sb.append(String.format("      • Hab.%s | %s\n",
                    h.getNumero(),
                    h.getTipoHabitacion() != null ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "SIMPLE"));
        }
        return sb.toString();
    }

    private String contextoTarifas() {
        List<Habitacion> todas = habitacionDAO.listarTodas();
        Map<String, Double> tarifasPorTipo = new LinkedHashMap<>();

        for (Habitacion h : todas) {
            String tipo = h.getTipoHabitacion() != null ? h.getTipoHabitacion().obtenerEtiquetaTipo() : "SIMPLE";
            double precio = h.calcularPrecioFinal();
            if (!tarifasPorTipo.containsKey(tipo) || precio < tarifasPorTipo.get(tipo)) {
                tarifasPorTipo.put(tipo, precio);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("💰 TARIFAS POR TIPO DE HABITACIÓN\n");
        sb.append(LINEA).append("\n");

        for (Map.Entry<String, Double> entry : tarifasPorTipo.entrySet()) {
            sb.append(String.format("   • %s: %s/noche\n", entry.getKey(), fmtMoneda(entry.getValue())));
        }
        return sb.toString();
    }

    // ==================== CONSULTAS DE CLIENTES ====================
    private String contextoBuscarCliente(String mensaje) {
        String termino = mensaje.replaceAll("(?i)buscar cliente|buscar huesped|cliente llamado|datos del cliente|informacion del cliente", "").trim();
        if (termino.isEmpty()) {
            return "⚠️ Escribe el nombre o cédula del cliente.";
        }

        String norm = normalizar(termino);
        List<Cliente> encontrados = clienteDAO.listarTodos().stream()
                .filter(c -> normalizar(c.obtenerNombreCompleto()).contains(norm)
                || (c.getDocumento() != null && c.getDocumento().contains(termino)))
                .collect(Collectors.toList());

        if (encontrados.isEmpty()) {
            return "❌ No se encontraron clientes con: '" + termino + "'";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("👤 CLIENTES ENCONTRADOS (%d)\n", encontrados.size()));
        sb.append(LINEA).append("\n");
        for (Cliente c : encontrados) {
            sb.append(String.format("   • %s%s\n", c.obtenerNombreCompleto(), c.isEsVip() ? " [VIP]" : ""));
            sb.append(String.format("     Documento: %s\n", c.getDocumento() != null ? c.getDocumento() : "—"));
            sb.append(String.format("     Teléfono: %s\n", c.getTelefono() != null ? c.getTelefono() : "—"));
            sb.append(String.format("     Email: %s\n", c.getEmail() != null ? c.getEmail() : "—"));
            sb.append(String.format("     Ciudad: %s\n", c.getCiudadOrigen() != null ? c.getCiudadOrigen() : "—"));
        }
        return sb.toString();
    }

    private String contextoClientesVip() {
        List<Cliente> vip = clienteDAO.listarTodos().stream()
                .filter(Cliente::isEsVip)
                .collect(Collectors.toList());

        if (vip.isEmpty()) {
            return "👤 No hay clientes VIP registrados.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("⭐ CLIENTES VIP (%d)\n", vip.size()));
        sb.append(LINEA).append("\n");
        for (Cliente c : vip) {
            sb.append(String.format("   • %s\n", c.obtenerNombreCompleto()));
            sb.append(String.format("     Documento: %s | Tel: %s\n",
                    c.getDocumento() != null ? c.getDocumento() : "—",
                    c.getTelefono() != null ? c.getTelefono() : "—"));
        }
        return sb.toString();
    }

    private String contextoClientesFrecuentes() {
        List<Reserva> todasReservas = reservaDAO.listarTodas();
        Map<Integer, Long> conteoPorCliente = todasReservas.stream()
                .filter(r -> r.getCliente() != null)
                .collect(Collectors.groupingBy(r -> r.getCliente().getId(), Collectors.counting()));

        List<Map.Entry<Integer, Long>> topClientes = conteoPorCliente.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        if (topClientes.isEmpty()) {
            return "👤 No hay clientes con reservas registradas.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🏆 CLIENTES MÁS FRECUENTES (Top 5)\n");
        sb.append(LINEA).append("\n");

        for (Map.Entry<Integer, Long> entry : topClientes) {
            Optional<Cliente> cliente = clienteDAO.buscarPorId(entry.getKey());
            if (cliente.isPresent()) {
                double totalGastado = todasReservas.stream()
                        .filter(r -> r.getCliente() != null && r.getCliente().getId() == entry.getKey())
                        .mapToDouble(Reserva::getPrecioTotal)
                        .sum();
                sb.append(String.format("   • %s\n", cliente.get().obtenerNombreCompleto()));
                sb.append(String.format("     Reservas: %d | Total gastado: %s\n", entry.getValue(), fmtMoneda(totalGastado)));
            }
        }
        return sb.toString();
    }

    private String contextoClienteMasFiel() {
        List<Reserva> todasReservas = reservaDAO.listarTodas();

        Map<Integer, Long> totalNochesPorCliente = todasReservas.stream()
                .filter(r -> r.getCliente() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getCliente().getId(),
                        Collectors.summingLong(r -> r.getFechaEntrada().until(r.getFechaSalida()).getDays())));

        Optional<Map.Entry<Integer, Long>> max = totalNochesPorCliente.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (max.isEmpty()) {
            return "👤 No hay datos suficientes para determinar el cliente más fiel.";
        }

        Optional<Cliente> cliente = clienteDAO.buscarPorId(max.get().getKey());
        if (cliente.isEmpty()) {
            return "👤 No se encontró el cliente.";
        }

        double totalGastado = todasReservas.stream()
                .filter(r -> r.getCliente() != null && r.getCliente().getId() == max.get().getKey())
                .mapToDouble(Reserva::getPrecioTotal)
                .sum();

        return String.format("🏆 CLIENTE MÁS FIEL\n" + LINEA + "\n"
                + "   • %s%s\n"
                + "   • Total noches: %d\n"
                + "   • Total gastado: %s\n"
                + "   • Reservas: %d",
                cliente.get().obtenerNombreCompleto(),
                cliente.get().isEsVip() ? " [VIP]" : "",
                max.get().getValue(),
                fmtMoneda(totalGastado),
                todasReservas.stream().filter(r -> r.getCliente() != null && r.getCliente().getId() == max.get().getKey()).count());
    }

    private String contextoClientesRegistrados() {
        List<Cliente> clientes = clienteDAO.listarTodos();
        long vip = clientes.stream().filter(Cliente::isEsVip).count();

        return String.format("👤 CLIENTES REGISTRADOS\n" + LINEA + "\n"
                + "   • Total clientes: %d\n"
                + "   • Clientes VIP: %d\n"
                + "   • Clientes regulares: %d",
                clientes.size(), vip, clientes.size() - vip);
    }

    private String contextoClientePorDocumento(String mensaje) {
        Matcher m = PATRON_DOCUMENTO.matcher(mensaje);
        String documento = "";
        if (m.find()) {
            documento = m.group(1);
        } else {
            String termino = mensaje.replaceAll("(?i)cliente por documento|buscar cliente por documento|cedula del cliente", "").trim();
            if (!termino.isEmpty()) {
                documento = termino;
            }
        }

        if (documento.isEmpty()) {
            return "⚠️ Escribe el número de documento. Ej: 'cliente por documento 12345678'";
        }

        final String docBuscar = documento;

        Optional<Cliente> cliente = clienteDAO.listarTodos().stream()
                .filter(c -> docBuscar != null && docBuscar.equals(c.getDocumento()))
                .findFirst();

        if (cliente.isEmpty()) {
            return "❌ No se encontró un cliente con documento: " + documento;
        }

        Cliente c = cliente.get();
        List<Reserva> reservas = reservaDAO.listarTodas().stream()
                .filter(r -> r.getCliente() != null && r.getCliente().getId() == c.getId())
                .collect(Collectors.toList());

        double totalGastado = reservas.stream().mapToDouble(Reserva::getPrecioTotal).sum();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("👤 CLIENTE POR DOCUMENTO\n" + LINEA + "\n"));
        sb.append(String.format("   • Nombre: %s%s\n", c.obtenerNombreCompleto(), c.isEsVip() ? " [VIP]" : ""));
        sb.append(String.format("   • Documento: %s\n", c.getDocumento()));
        sb.append(String.format("   • Teléfono: %s\n", c.getTelefono() != null ? c.getTelefono() : "—"));
        sb.append(String.format("   • Email: %s\n", c.getEmail() != null ? c.getEmail() : "—"));
        sb.append(String.format("   • Ciudad: %s\n", c.getCiudadOrigen() != null ? c.getCiudadOrigen() : "—"));
        sb.append(String.format("   • Total reservas: %d | Total gastado: %s", reservas.size(), fmtMoneda(totalGastado)));
        return sb.toString();
    }

    private String contextoClientePorTelefono(String mensaje) {
        Matcher m = PATRON_TELEFONO.matcher(mensaje);
        String telefono = "";
        if (m.find()) {
            telefono = m.group(1);
        } else {
            String termino = mensaje.replaceAll("(?i)cliente por telefono|buscar cliente por telefono|numero de telefono cliente", "").trim();
            if (!termino.isEmpty()) {
                telefono = termino;
            }
        }

        if (telefono.isEmpty()) {
            return "⚠️ Escribe el número de teléfono. Ej: 'cliente por telefono 3001234567'";
        }

        final String telBuscar = telefono;

        Optional<Cliente> cliente = clienteDAO.listarTodos().stream()
                .filter(c -> telBuscar != null && telBuscar.equals(c.getTelefono()))
                .findFirst();

        if (cliente.isEmpty()) {
            return "❌ No se encontró un cliente con teléfono: " + telefono;
        }

        Cliente c = cliente.get();
        return String.format("👤 CLIENTE POR TELÉFONO\n" + LINEA + "\n"
                + "   • Nombre: %s%s\n"
                + "   • Documento: %s\n"
                + "   • Teléfono: %s\n"
                + "   • Email: %s",
                c.obtenerNombreCompleto(), c.isEsVip() ? " [VIP]" : "",
                c.getDocumento() != null ? c.getDocumento() : "—",
                c.getTelefono(),
                c.getEmail() != null ? c.getEmail() : "—");
    }

    private String contextoClientesPorNacionalidad() {
        List<Cliente> clientes = clienteDAO.listarTodos();
        Map<String, Long> porNacionalidad = clientes.stream()
                .filter(c -> c.getNacionalidad() != null && !c.getNacionalidad().isEmpty())
                .collect(Collectors.groupingBy(Cliente::getNacionalidad, Collectors.counting()));

        if (porNacionalidad.isEmpty()) {
            return "🌎 No hay clientes con nacionalidad registrada.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🌎 CLIENTES POR NACIONALIDAD\n");
        sb.append(LINEA).append("\n");

        porNacionalidad.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    sb.append(String.format("   • %s: %d clientes\n", entry.getKey(), entry.getValue()));
                });
        return sb.toString();
    }

    // ==================== MÉTODOS EXISTENTES (resumidos) ====================
    private String obtenerResumenGeneral() {
        try {
            LocalDate hoy = LocalDate.now();
            List<Habitacion> habs = habitacionDAO.listarTodas();
            long disponibles = habs.stream().filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.DISPONIBLE).count();
            long ocupadas = habs.stream().filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.OCUPADA).count();
            long reservadas = habs.stream().filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.RESERVADA).count();
            long mantenimiento = habs.stream().filter(h -> h.getEstado() == Habitacion.EstadoHabitacion.MANTENIMIENTO).count();
            List<Reserva> checkinsHoy = reservaBusqueda.buscarPorRangoFechas(hoy, hoy);
            double ingresosHoy = facturaDAO.listarTodas().stream().filter(f -> hoy.equals(f.getFechaEmision())).mapToDouble(Factura::getTotal).sum();
            long empleados = empleadoDAO.listarTodos().size();
            double pctOcupacion = habs.isEmpty() ? 0 : (ocupadas * 100.0 / habs.size());

            return String.format(
                    "📊 RESUMEN HOTEL NATIVO - %s\n"
                    + LINEA + "\n"
                    + "🏨 HABITACIONES\n"
                    + "   Total: %d   |   Disp: %d   |   Ocup: %d   |   Reserv: %d   |   Mant: %d\n"
                    + "   Ocupación: %.1f%%\n"
                    + LINEA + "\n"
                    + "📅 MOVIMIENTO\n"
                    + "   Check-ins hoy: %d   |   Ingresos hoy: %s\n"
                    + LINEA + "\n"
                    + "👥 PERSONAL\n"
                    + "   Empleados activos: %d   |   Mantenimientos pendientes: %d\n"
                    + LINEA,
                    fmtFecha(hoy), habs.size(), disponibles, ocupadas, reservadas, mantenimiento,
                    pctOcupacion, checkinsHoy.size(), fmtMoneda(ingresosHoy), empleados, mantenimiento);
        } catch (Exception e) {
            return "❌ Error: " + e.getMessage();
        }
    }

    private String contextoCheckinsHoy() {
        LocalDate hoy = LocalDate.now();
        List<Reserva> reservas = reservaBusqueda.buscarPorRangoFechas(hoy, hoy);
        if (reservas.isEmpty()) {
            return "📅 No hay check-ins programados para hoy " + fmtFecha(hoy);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📅 CHECK-INS HOY (%s)\n", fmtFecha(hoy)));
        sb.append(LINEA).append("\n");
        for (Reserva r : reservas) {
            sb.append(String.format("   • Reserva #%d | %s | Hab.%s | %d noches\n",
                    r.getId(),
                    r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—",
                    r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—",
                    r.getFechaEntrada().until(r.getFechaSalida()).getDays()));
        }
        sb.append(LINEA).append("\n");
        sb.append(String.format("Total: %d llegada(s)", reservas.size()));
        return sb.toString();
    }

    private String contextoCheckoutsHoy() {
        LocalDate hoy = LocalDate.now();
        List<Reserva> salidas = reservaDAO.listarTodas().stream()
                .filter(r -> hoy.equals(r.getFechaSalida()))
                .collect(Collectors.toList());
        if (salidas.isEmpty()) {
            return "📅 No hay check-outs programados para hoy " + fmtFecha(hoy);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📅 CHECK-OUTS HOY (%s)\n", fmtFecha(hoy)));
        sb.append(LINEA).append("\n");
        for (Reserva r : salidas) {
            sb.append(String.format("   • Reserva #%d | %s | Hab.%s | %s\n",
                    r.getId(),
                    r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—",
                    r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—",
                    fmtMoneda(r.getPrecioTotal())));
        }
        sb.append(LINEA).append("\n");
        sb.append(String.format("Total: %d salida(s)", salidas.size()));
        return sb.toString();
    }

    private String contextoReservasActivas() {
        List<Reserva> pendientes = reservaBusqueda.buscarPorEstado("PENDIENTE");
        List<Reserva> confirmadas = reservaBusqueda.buscarPorEstado("CONFIRMADA");
        List<Reserva> enProceso = reservaBusqueda.buscarPorEstado("EN_PROCESO");
        int total = pendientes.size() + confirmadas.size() + enProceso.size();
        if (total == 0) {
            return "📋 No hay reservas activas en este momento.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📋 RESERVAS ACTIVAS (%d)\n", total));
        sb.append(LINEA).append("\n");
        sb.append(String.format("   En proceso: %d   |   Confirmadas: %d   |   Pendientes: %d\n\n",
                enProceso.size(), confirmadas.size(), pendientes.size()));

        for (Reserva r : enProceso) {
            sb.append(String.format("   • #%d | %s | Hab.%s | %s→%s | [En proceso]\n",
                    r.getId(),
                    r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—",
                    r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—",
                    fmtFecha(r.getFechaEntrada()), fmtFecha(r.getFechaSalida())));
        }
        for (Reserva r : confirmadas) {
            sb.append(String.format("   • #%d | %s | Hab.%s | %s→%s | [Confirmada]\n",
                    r.getId(),
                    r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—",
                    r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—",
                    fmtFecha(r.getFechaEntrada()), fmtFecha(r.getFechaSalida())));
        }
        for (Reserva r : pendientes) {
            sb.append(String.format("   • #%d | %s | Hab.%s | %s→%s | [Pendiente]\n",
                    r.getId(),
                    r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—",
                    r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—",
                    fmtFecha(r.getFechaEntrada()), fmtFecha(r.getFechaSalida())));
        }
        return sb.toString();
    }

    private String contextoEstadoReserva(String mensaje) {
        int id = extraerIdDesdePatron(PATRON_ID_RESERVA, mensaje);
        if (id < 0) {
            id = extraerPrimerNumero(mensaje);
        }
        if (id < 0) {
            return "⚠️ No se encontró un número de reserva.";
        }

        Optional<Reserva> resultado = reservaDAO.buscarPorId(id);
        if (resultado.isEmpty()) {
            return "❌ No existe la reserva #" + id;
        }

        Reserva r = resultado.get();
        boolean esVip = r.getCliente() != null && r.getCliente().isEsVip();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📋 RESERVA #%d\n", r.getId()));
        sb.append(LINEA).append("\n");
        sb.append(String.format("   Estado: %s\n", traducirEstadoReserva(r.getEstado())));
        sb.append(String.format("   Cliente: %s%s\n",
                r.getCliente() != null ? r.getCliente().obtenerNombreCompleto() : "—",
                esVip ? " [VIP]" : ""));
        sb.append(String.format("   Documento: %s\n", r.getCliente() != null ? r.getCliente().getDocumento() : "—"));
        sb.append(String.format("   Teléfono: %s\n", r.getCliente() != null && r.getCliente().getTelefono() != null ? r.getCliente().getTelefono() : "—"));
        sb.append(String.format("   Habitación: %s\n", r.getHabitacion() != null ? r.getHabitacion().getNumero() : "—"));
        sb.append(String.format("   Tipo: %s\n", r.getHabitacion() != null && r.getHabitacion().getTipoHabitacion() != null
                ? r.getHabitacion().getTipoHabitacion().obtenerEtiquetaTipo() : "—"));
        sb.append(String.format("   Entrada: %s\n", fmtFecha(r.getFechaEntrada())));
        sb.append(String.format("   Salida: %s\n", fmtFecha(r.getFechaSalida())));
        sb.append(String.format("   Noches: %d\n", r.getFechaEntrada().until(r.getFechaSalida()).getDays()));
        sb.append(String.format("   Total: %s\n", fmtMoneda(r.getPrecioTotal())));

        try {
            Optional<Factura> f = facturaDAO.buscarPorReserva(id);
            if (f.isPresent()) {
                sb.append(String.format("   Factura #%d | Estado: %s | Método: %s",
                        f.get().getId(),
                        traducirEstadoPago(f.get().getEstadoPago()),
                        f.get().getMetodoPago() != null ? f.get().getMetodoPago().name() : "—"));
            } else {
                sb.append("   Sin factura generada aún.");
            }
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    private String contextoFacturaPorId(String mensaje) {
        int id = extraerIdDesdePatron(PATRON_ID_FACTURA, mensaje);
        if (id < 0) {
            id = extraerPrimerNumero(mensaje);
        }
        if (id < 0) {
            return "⚠️ No se encontró un número de factura.";
        }

        Optional<Factura> resultado = facturaDAO.buscarPorId(id);
        if (resultado.isEmpty()) {
            return "❌ No existe la factura #" + id;
        }

        Factura f = resultado.get();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("💰 FACTURA #%d\n", f.getId()));
        sb.append(LINEA).append("\n");
        sb.append(String.format("   Cliente: %s\n", f.getCliente() != null ? f.getCliente().obtenerNombreCompleto() : "—"));
        sb.append(String.format("   Reserva: #%d\n", f.getReserva() != null ? f.getReserva().getId() : 0));
        sb.append(String.format("   Fecha: %s\n", f.getFechaEmision()));
        sb.append(String.format("   Subtotal: %s\n", fmtMoneda(f.getSubtotal())));
        sb.append(String.format("   IVA: %s\n", fmtMoneda(f.getImpuestos())));
        sb.append(String.format("   TOTAL: %s\n", fmtMoneda(f.getTotal())));
        sb.append(String.format("   Estado: %s\n", traducirEstadoPago(f.getEstadoPago())));
        sb.append(String.format("   Método: %s", f.getMetodoPago() != null ? f.getMetodoPago().name() : "—"));
        return sb.toString();
    }

    private String contextoFacturasPendientes() {
        List<Factura> pendientes = facturaDAO.listarTodas().stream()
                .filter(f -> f.getEstadoPago() == Factura.EstadoPago.PENDIENTE)
                .collect(Collectors.toList());
        if (pendientes.isEmpty()) {
            return "✅ No hay facturas pendientes de pago.";
        }

        double totalPendiente = pendientes.stream().mapToDouble(Factura::getTotal).sum();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("💰 FACTURAS PENDIENTES (%d)\n", pendientes.size()));
        sb.append(LINEA).append("\n");
        for (Factura f : pendientes) {
            sb.append(String.format("   • Fac.#%d | %s | %s\n",
                    f.getId(),
                    f.getCliente() != null ? truncar(f.getCliente().obtenerNombreCompleto(), 30) : "—",
                    fmtMoneda(f.getTotal())));
        }
        sb.append(LINEA).append("\n");
        sb.append(String.format("Total por cobrar: %s", fmtMoneda(totalPendiente)));
        return sb.toString();
    }

    private String contextoIngresosHoy() {
        LocalDate hoy = LocalDate.now();
        List<Factura> facturasHoy = facturaDAO.listarTodas().stream()
                .filter(f -> hoy.equals(f.getFechaEmision()))
                .collect(Collectors.toList());

        if (facturasHoy.isEmpty()) {
            return "💰 No se han emitido facturas hoy " + fmtFecha(hoy);
        }

        double total = facturasHoy.stream().mapToDouble(Factura::getTotal).sum();
        double efectivo = facturasHoy.stream().filter(f -> f.getMetodoPago() == Factura.MetodoPago.EFECTIVO).mapToDouble(Factura::getTotal).sum();
        double tarjeta = facturasHoy.stream().filter(f -> f.getMetodoPago() == Factura.MetodoPago.TARJETA_CREDITO || f.getMetodoPago() == Factura.MetodoPago.TARJETA_DEBITO).mapToDouble(Factura::getTotal).sum();
        double transferencia = facturasHoy.stream().filter(f -> f.getMetodoPago() == Factura.MetodoPago.TRANSFERENCIA).mapToDouble(Factura::getTotal).sum();

        return String.format(
                "💰 INGRESOS HOY (%s)\n" + LINEA + "\n"
                + "   Facturas emitidas: %d\n"
                + "   Total recaudado: %s\n"
                + "     • Efectivo: %s\n"
                + "     • Tarjeta: %s\n"
                + "     • Transferencia: %s\n" + LINEA,
                fmtFecha(hoy), facturasHoy.size(), fmtMoneda(total),
                fmtMoneda(efectivo), fmtMoneda(tarjeta), fmtMoneda(transferencia));
    }

    private String contextoResumenFacturacion() {
        List<Factura> todas = facturaDAO.listarTodas();
        if (todas.isEmpty()) {
            return "💰 No hay facturas registradas.";
        }
        long pagadas = todas.stream().filter(f -> f.getEstadoPago() == Factura.EstadoPago.PAGADA).count();
        long pendientes = todas.stream().filter(f -> f.getEstadoPago() == Factura.EstadoPago.PENDIENTE).count();
        double totalFacturado = todas.stream().mapToDouble(Factura::getTotal).sum();
        double totalCobrado = todas.stream().filter(f -> f.getEstadoPago() == Factura.EstadoPago.PAGADA).mapToDouble(Factura::getTotal).sum();
        return String.format(
                "📊 RESUMEN FACTURACIÓN\n" + LINEA + "\n"
                + "   Total facturas: %d (Pagadas: %d | Pendientes: %d)\n"
                + "   Total facturado: %s\n"
                + "   Total cobrado: %s\n"
                + "   Por cobrar: %s\n" + LINEA,
                todas.size(), pagadas, pendientes, fmtMoneda(totalFacturado), fmtMoneda(totalCobrado), fmtMoneda(totalFacturado - totalCobrado));
    }

    private String contextoReservasHoy() {
        LocalDate hoy = LocalDate.now();
        List<Reserva> checkinsHoy = reservaBusqueda.buscarPorRangoFechas(hoy, hoy);
        List<Reserva> checkoutsHoy = reservaDAO.listarTodas().stream()
                .filter(r -> hoy.equals(r.getFechaSalida()))
                .collect(Collectors.toList());
        return String.format(
                "📅 MOVIMIENTO DEL DÍA (%s)\n" + LINEA + "\n"
                + "   Check-ins: %d\n"
                + "   Check-outs: %d\n" + LINEA,
                fmtFecha(hoy), checkinsHoy.size(), checkoutsHoy.size());
    }

    private String contextoEmpleadosActivos() {
        List<Empleado> empleados = empleadoDAO.listarTodos();
        if (empleados.isEmpty()) {
            return "👥 No hay empleados registrados.";
        }

        Map<String, List<Empleado>> porCargo = empleados.stream()
                .collect(Collectors.groupingBy(e -> e.getCargo() != null ? e.getCargo().getNombreCargo() : "Sin cargo"));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("👥 PERSONAL DEL HOTEL (%d empleados)\n", empleados.size()));
        sb.append(LINEA).append("\n");

        for (Map.Entry<String, List<Empleado>> entry : porCargo.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
            sb.append(String.format("\n📌 %s (%d):\n", entry.getKey().toUpperCase(), entry.getValue().size()));
            for (Empleado e : entry.getValue()) {
                sb.append(String.format("   • %s | Tel: %s\n", e.obtenerNombreCompleto(),
                        e.getTelefono() != null ? e.getTelefono() : "—"));
                if (e.getTipoContrato() != null) {
                    sb.append(String.format("     Contrato: %s\n", e.getTipoContrato()));
                }
            }
        }
        return sb.toString();
    }

    private String contextoEmpleadosPorCargo(String mensaje) {
        List<Cargo> cargos = empleadoDAO.listarCargos();
        String norm = normalizar(mensaje);
        Optional<Cargo> cargoEncontrado = cargos.stream()
                .filter(c -> normalizar(c.getNombreCargo()).contains(norm) || norm.contains(normalizar(c.getNombreCargo())))
                .findFirst();
        if (cargoEncontrado.isEmpty()) {
            return contextoEmpleadosActivos();
        }
        List<Empleado> empleados = empleadoDAO.buscarPorCargo(cargoEncontrado.get().getId());
        if (empleados.isEmpty()) {
            return "👥 No hay empleados con el cargo: " + cargoEncontrado.get().getNombreCargo();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("👥 EMPLEADOS - %s (%d)\n", cargoEncontrado.get().getNombreCargo().toUpperCase(), empleados.size()));
        sb.append(LINEA).append("\n");
        for (Empleado e : empleados) {
            sb.append(String.format("   • %s | Tel: %s\n", e.obtenerNombreCompleto(), e.getTelefono() != null ? e.getTelefono() : "—"));
        }
        return sb.toString();
    }

    private String contextoBuscarEmpleado(String mensaje) {
        String termino = mensaje.replaceAll("(?i)buscar empleado|empleado llamado|datos del empleado|informacion del empleado", "").trim();
        if (termino.isEmpty()) {
            return "⚠️ Escribe el nombre del empleado.";
        }
        String norm = normalizar(termino);
        List<Empleado> encontrados = empleadoDAO.listarTodos().stream()
                .filter(e -> normalizar(e.obtenerNombreCompleto()).contains(norm))
                .collect(Collectors.toList());
        if (encontrados.isEmpty()) {
            return "❌ No se encontró ningún empleado con: '" + termino + "'";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("👥 EMPLEADOS ENCONTRADOS (%d)\n", encontrados.size()));
        sb.append(LINEA).append("\n");
        for (Empleado e : encontrados) {
            sb.append(String.format("   • %s\n", e.obtenerNombreCompleto()));
            sb.append(String.format("     Cargo: %s | Tel: %s\n", e.getCargo() != null ? e.getCargo().getNombreCargo() : "—", e.getTelefono() != null ? e.getTelefono() : "—"));
        }
        return sb.toString();
    }

    private String contextoTurnosHoy() {
        List<Empleado> empleados = empleadoDAO.listarTodos();
        if (empleados.isEmpty()) {
            return "👥 No hay empleados registrados.";
        }
        LocalDate hoy = LocalDate.now();
        List<Empleado> activos = empleados.stream()
                .filter(e -> e.getFechaFinContrato() == null || !e.getFechaFinContrato().isBefore(hoy))
                .collect(Collectors.toList());
        Map<String, List<Empleado>> porCargo = activos.stream()
                .collect(Collectors.groupingBy(e -> e.getCargo() != null ? e.getCargo().getNombreCargo() : "Sin cargo"));
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("👥 PERSONAL DISPONIBLE HOY (%s)\n", fmtFecha(hoy)));
        sb.append(LINEA).append("\n");
        for (Map.Entry<String, List<Empleado>> entry : porCargo.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
            sb.append(String.format("\n📌 %s (%d):\n", entry.getKey().toUpperCase(), entry.getValue().size()));
            for (Empleado e : entry.getValue()) {
                sb.append(String.format("   • %s | Tel: %s\n", e.obtenerNombreCompleto(), e.getTelefono() != null ? e.getTelefono() : "—"));
            }
        }
        return sb.toString();
    }

    private String contextoCargosDisponibles() {
        List<Cargo> cargos = empleadoDAO.listarCargos();
        if (cargos.isEmpty()) {
            return "📋 No hay cargos registrados.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📋 CARGOS DEL HOTEL (%d)\n", cargos.size()));
        sb.append(LINEA).append("\n");
        for (Cargo c : cargos) {
            int total = empleadoDAO.buscarPorCargo(c.getId()).size();
            sb.append(String.format("   • %s (%d empleados)\n", c.getNombreCargo(), total));
        }
        return sb.toString();
    }

    private String contextoMantenimientoPendiente() {
        List<Mantenimiento> pendientes = mantenimientoDAO.listarPendientes();
        if (pendientes.isEmpty()) {
            return "✅ No hay mantenimientos pendientes.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🔧 MANTENIMIENTOS PENDIENTES (%d)\n", pendientes.size()));
        sb.append(LINEA).append("\n");
        for (Mantenimiento m : pendientes) {
            sb.append(String.format("   • Mnt.#%d | Hab.%s | %s | %s\n",
                    m.getId(),
                    m.getHabitacion() != null ? m.getHabitacion().getNumero() : "—",
                    traducirTipoMant(m.getTipo()),
                    traducirEstadoMant(m.getEstado())));
        }
        return sb.toString();
    }

    private String contextoMantenimientoPorHabitacion(String mensaje) {
        String numero = extraerNumeroHabitacion(mensaje);
        if (numero == null) {
            return "⚠️ Indica el número de habitación. Ej: 'mantenimiento habitación 101'";
        }
        List<Mantenimiento> lista = mantenimientoDAO.listarPorHabitacion(numero);
        if (lista.isEmpty()) {
            return "ℹ️ La habitación " + numero + " no tiene registros de mantenimiento.";
        }
        long completados = lista.stream().filter(Mantenimiento::estaCompletado).count();
        double costoTotal = lista.stream().mapToDouble(Mantenimiento::getCosto).sum();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🔧 MANTENIMIENTOS - Habitación %s (%d registros)\n", numero, lista.size()));
        sb.append(LINEA).append("\n");
        sb.append(String.format("   Completados: %d | Costo total: %s\n\n", completados, fmtMoneda(costoTotal)));
        for (Mantenimiento m : lista) {
            sb.append(String.format("   • Mnt.#%d | %s | %s | %s\n",
                    m.getId(),
                    traducirTipoMant(m.getTipo()),
                    m.getFechaSolicitud(),
                    traducirEstadoMant(m.getEstado())));
        }
        return sb.toString();
    }

    private String contextoTodosMantenimientos() {
        List<Mantenimiento> todos = mantenimientoDAO.listarTodos();
        if (todos.isEmpty()) {
            return "ℹ️ No hay registros de mantenimiento.";
        }
        long completados = todos.stream().filter(Mantenimiento::estaCompletado).count();
        long pendientes = todos.stream().filter(m -> !m.estaCompletado() && m.getEstado() != Mantenimiento.EstadoMantenimiento.CANCELADO).count();
        double costoTotal = todos.stream().filter(Mantenimiento::estaCompletado).mapToDouble(Mantenimiento::getCosto).sum();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🔧 HISTORIAL DE MANTENIMIENTOS (%d total)\n", todos.size()));
        sb.append(LINEA).append("\n");
        sb.append(String.format("   Completados: %d | Pendientes: %d | Costo total: %s\n\n", completados, pendientes, fmtMoneda(costoTotal)));
        List<Mantenimiento> ultimos = todos.subList(0, Math.min(10, todos.size()));
        sb.append("Últimos registros:\n");
        for (Mantenimiento m : ultimos) {
            sb.append(String.format("   • Mnt.#%d | Hab.%s | %s | %s\n",
                    m.getId(),
                    m.getHabitacion() != null ? m.getHabitacion().getNumero() : "—",
                    traducirTipoMant(m.getTipo()),
                    m.getFechaSolicitud()));
        }
        if (todos.size() > 10) {
            sb.append(String.format("\n... y %d registros más", todos.size() - 10));
        }
        return sb.toString();
    }

    private String contextoResumenCostoMantenimiento() {
        List<Mantenimiento> todos = mantenimientoDAO.listarTodos();
        if (todos.isEmpty()) {
            return "ℹ️ No hay registros de mantenimiento.";
        }
        double costoPreventivo = todos.stream().filter(m -> m.getTipo() == Mantenimiento.TipoMantenimiento.PREVENTIVO && m.estaCompletado()).mapToDouble(Mantenimiento::getCosto).sum();
        double costoCorrectivo = todos.stream().filter(m -> m.getTipo() == Mantenimiento.TipoMantenimiento.CORRECTIVO && m.estaCompletado()).mapToDouble(Mantenimiento::getCosto).sum();
        double costoEmergencia = todos.stream().filter(m -> m.getTipo() == Mantenimiento.TipoMantenimiento.EMERGENCIA && m.estaCompletado()).mapToDouble(Mantenimiento::getCosto).sum();
        double costoTotal = costoPreventivo + costoCorrectivo + costoEmergencia;
        return String.format(
                "📊 COSTOS DE MANTENIMIENTO\n" + LINEA + "\n"
                + "   Total ejecutado: %s\n"
                + "     • Preventivo: %s\n"
                + "     • Correctivo: %s\n"
                + "     • Emergencia: %s\n" + LINEA,
                fmtMoneda(costoTotal), fmtMoneda(costoPreventivo),
                fmtMoneda(costoCorrectivo), fmtMoneda(costoEmergencia));
    }

    // ==================== MÉTODOS AUXILIARES ====================
    private String contextoFacturaPorReserva(String mensaje) {
        int id = extraerIdDesdePatron(PATRON_ID_RESERVA, mensaje);
        if (id < 0) {
            id = extraerPrimerNumero(mensaje);
        }
        if (id < 0) {
            return "⚠️ Indica el número de reserva.";
        }
        Optional<Factura> resultado = facturaDAO.buscarPorReserva(id);
        if (resultado.isEmpty()) {
            return "ℹ️ La reserva #" + id + " no tiene factura generada.";
        }
        return contextoFacturaPorId("factura #" + resultado.get().getId());
    }

    private String contextoFacturasCliente(String mensaje) {
        int idCliente = extraerPrimerNumero(mensaje);
        String nombreCliente = "";
        List<Factura> facturas;

        if (idCliente > 0) {
            facturas = facturaDAO.listarPorCliente(idCliente);
        } else {
            String termino = mensaje.replaceAll("(?i)facturas del cliente|historial de pagos|pagos del cliente", "").trim();
            if (termino.isEmpty()) {
                return "⚠️ Indica el nombre o ID del cliente.";
            }
            String norm = normalizar(termino);
            Optional<Cliente> cliente = clienteDAO.listarTodos().stream()
                    .filter(c -> normalizar(c.obtenerNombreCompleto()).contains(norm))
                    .findFirst();
            if (cliente.isEmpty()) {
                return "❌ No se encontró el cliente: '" + termino + "'";
            }
            idCliente = cliente.get().getId();
            nombreCliente = cliente.get().obtenerNombreCompleto();
            facturas = facturaDAO.listarPorCliente(idCliente);
        }

        if (facturas.isEmpty()) {
            return "ℹ️ El cliente no tiene facturas registradas.";
        }
        double totalPagado = facturas.stream().filter(f -> f.getEstadoPago() == Factura.EstadoPago.PAGADA).mapToDouble(Factura::getTotal).sum();
        double totalPendiente = facturas.stream().filter(f -> f.getEstadoPago() == Factura.EstadoPago.PENDIENTE).mapToDouble(Factura::getTotal).sum();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("💰 FACTURAS DE %s\n", nombreCliente.isEmpty() ? "Cliente #" + idCliente : nombreCliente));
        sb.append(LINEA).append("\n");
        for (Factura f : facturas) {
            sb.append(String.format("   • Fac.#%d | %s | %s\n", f.getId(), f.getFechaEmision(), fmtMoneda(f.getTotal())));
        }
        sb.append(LINEA).append("\n");
        sb.append(String.format("Pagado: %s | Pendiente: %s", fmtMoneda(totalPagado), fmtMoneda(totalPendiente)));
        return sb.toString();
    }

    // ==================== TRADUCCIONES ====================
    private String traducirEstadoReserva(Reserva.EstadoReserva estado) {
        if (estado == null) {
            return "—";
        }
        switch (estado) {
            case PENDIENTE:
                return "Pendiente";
            case CONFIRMADA:
                return "Confirmada";
            case EN_PROCESO:
                return "En proceso";
            case COMPLETADA:
                return "Completada";
            case CANCELADA:
                return "Cancelada";
            default:
                return estado.name();
        }
    }

    private String traducirEstadoPago(Factura.EstadoPago estado) {
        if (estado == null) {
            return "—";
        }
        switch (estado) {
            case PAGADA:
                return "Pagada";
            case PENDIENTE:
                return "Pendiente";
            case ANULADA:
                return "Anulada";
            default:
                return estado.name();
        }
    }

    private String traducirTipoMant(Mantenimiento.TipoMantenimiento tipo) {
        if (tipo == null) {
            return "—";
        }
        switch (tipo) {
            case PREVENTIVO:
                return "Preventivo";
            case CORRECTIVO:
                return "Correctivo";
            case EMERGENCIA:
                return "Emergencia";
            default:
                return tipo.name();
        }
    }

    private String traducirEstadoMant(Mantenimiento.EstadoMantenimiento estado) {
        if (estado == null) {
            return "—";
        }
        switch (estado) {
            case SOLICITADO:
                return "Solicitado";
            case EN_PROCESO:
                return "En proceso";
            case COMPLETADO:
                return "Completado";
            case CANCELADO:
                return "Cancelado";
            default:
                return estado.name();
        }
    }

    // ==================== MÉTODOS DE EXTRACCIÓN ====================
    private String extraerNumeroHabitacion(String mensaje) {
        if (mensaje == null) {
            return null;
        }
        String norm = mensaje.toUpperCase();
        Matcher m1 = Pattern.compile("HAB[\\s.-]*(\\w+)").matcher(norm);
        if (m1.find()) {
            return m1.group(1).replace("-", "");
        }
        Matcher m2 = Pattern.compile("SUITE[\\s]*(\\d+)").matcher(norm);
        if (m2.find()) {
            return m2.group(1);
        }
        Matcher m3 = Pattern.compile("\\b(\\d{3})\\b").matcher(mensaje);
        if (m3.find()) {
            return m3.group(1);
        }
        return null;
    }

    private String truncar(String texto, int max) {
        if (texto == null) {
            return "—";
        }
        return texto.length() <= max ? texto : texto.substring(0, max - 3) + "...";
    }

    private List<LocalDate> extraerFechas(String texto) {
        List<LocalDate> fechas = new ArrayList<>();
        Matcher m = PATRON_FECHA_ISO.matcher(texto);
        while (m.find()) {
            try {
                fechas.add(LocalDate.parse(m.group()));
            } catch (DateTimeParseException ignored) {
            }
        }
        if (fechas.size() < 2) {
            Matcher m2 = PATRON_FECHA_DMY.matcher(texto);
            while (m2.find()) {
                try {
                    String[] p = m2.group().split("/");
                    fechas.add(LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0])));
                } catch (Exception ignored) {
                }
            }
        }
        return fechas;
    }

    private int contarFechas(String texto) {
        int n = 0;
        Matcher m = PATRON_FECHA_ISO.matcher(texto);
        while (m.find()) {
            n++;
        }
        if (n < 2) {
            Matcher m2 = PATRON_FECHA_DMY.matcher(texto);
            while (m2.find()) {
                n++;
            }
        }
        return n;
    }

    private int extraerIdDesdePatron(Pattern patron, String texto) {
        Matcher m = patron.matcher(normalizar(texto));
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    private int extraerPrimerNumero(String texto) {
        Matcher m = PATRON_NUMERO.matcher(texto);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    private int extraerNumeroPersonas(String texto, int porDefecto) {
        Matcher m = PATRON_PERSONAS.matcher(texto);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return porDefecto;
    }

    // ==================== TEXTOS ESTÁTICOS ====================
    private String textoAyuda() {
        return "📚 COMANDOS DISPONIBLES\n"
                + LINEA + "\n"
                + "📋 RESERVAS:\n"
                + "   • check-in hoy / checkout hoy\n"
                + "   • reservas activas / reserva #ID\n"
                + "   • reservas confirmadas / pendientes / completadas / canceladas\n"
                + "   • reservas del mes / reservas mañana / proxima semana\n"
                + "   • reservas por cliente NOMBRE / por habitacion 101\n"
                + "   • reserva mas cara / reserva mas larga\n"
                + "\n"
                + "🏨 HABITACIONES:\n"
                + "   • estado habitaciones\n"
                + "   • habitaciones libres hoy\n"
                + "   • habitaciones por tipo\n"
                + "   • habitaciones en mantenimiento / reservadas\n"
                + "   • precio habitacion 101\n"
                + "   • tipos de habitacion / tarifas\n"
                + "   • capacidad maxima\n"
                + "\n"
                + "👤 CLIENTES:\n"
                + "   • buscar cliente NOMBRE\n"
                + "   • clientes vip / clientes frecuentes\n"
                + "   • cliente mas fiel\n"
                + "   • clientes registrados\n"
                + "   • cliente por documento 12345678\n"
                + "   • cliente por telefono 3001234567\n"
                + "   • clientes por nacionalidad\n"
                + "\n"
                + "💰 FACTURACIÓN:\n"
                + "   • factura #ID / facturas pendientes\n"
                + "   • ingresos hoy / resumen facturación\n"
                + "\n"
                + "👥 EMPLEADOS:\n"
                + "   • empleados activos / quien esta de turno\n"
                + "   • buscar empleado NOMBRE / cargos disponibles\n"
                + "\n"
                + "🔧 MANTENIMIENTO:\n"
                + "   • mantenimientos pendientes\n"
                + "   • mantenimiento habitacion 101\n"
                + "   • costo mantenimiento\n"
                + "\n"
                + "📊 GENERAL:\n"
                + "   • como estamos hoy / ayuda\n"
                + LINEA;
    }

    private String mensajeBienvenidaEstatico() {
        LocalDate hoy = LocalDate.now();
        String ej1 = hoy.plusDays(3).toString();
        String ej2 = hoy.plusDays(6).toString();
        return "🤖 ¡Hola! Soy Nova, asistente del Hotel Nativo\n"
                + LINEA + "\n"
                + "📋 EJEMPLOS DE CONSULTAS:\n"
                + "   • estado habitaciones\n"
                + "   • disponibilidad " + ej1 + " " + ej2 + "\n"
                + "   • reserva #123\n"
                + "   • reservas confirmadas\n"
                + "   • reservas por cliente Pedro\n"
                + "   • factura #1\n"
                + "   • empleados activos\n"
                + "   • mantenimientos pendientes\n"
                + "\n"
                + "Escribe 'ayuda' para ver todos los comandos disponibles.\n"
                + LINEA;
    }

    private String respuestaNoEntendida() {
        return "❓ No entendí tu consulta. Escribe 'ayuda' para ver los comandos disponibles.";
    }
}

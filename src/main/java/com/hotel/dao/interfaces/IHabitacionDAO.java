package com.hotel.dao.interfaces;

import com.hotel.dto.BusquedaDisponibilidadDTO;
import com.hotel.model.Habitacion;
import java.util.List;
import java.util.Optional;

/**
 * Contrato para operaciones de Habitacion en base de datos.
 * GRASP: Bajo Acoplamiento - Service no conoce detalles de JDBC
 */
public interface IHabitacionDAO {

    Habitacion insertar(Habitacion habitacion);

    boolean actualizar(Habitacion habitacion);

    /** @param numero PK de HABITACION en v3, ej. "101", "202" */
    boolean eliminar(String numero);

    /** @param numero PK de HABITACION en v3, ej. "101", "202" */
    Optional<Habitacion> buscarPorNumero(String numero);

    List<Habitacion> listarTodas();

    /**
     * Retorna habitaciones disponibles para el rango de fechas dado.
     * Verifica contra reservas activas del periodo.
     */
    List<Habitacion> buscarDisponibles(BusquedaDisponibilidadDTO criterios);

    /**
     * Actualiza únicamente el estado de la habitación.
     * @param numero     PK de HABITACION, ej. "101"
     * @param nuevoEstado estado destino (debe ser FK válida en ESTADO_HABITACION)
     */
    boolean actualizarEstado(String numero, String nuevoEstado);
}
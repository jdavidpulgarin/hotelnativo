
package com.hotel.dao.interfaces;

/**
 *
 * @author rober
 */
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

    boolean eliminar(int idHabitacion);

    Optional<Habitacion> buscarPorId(int idHabitacion);

    Optional<Habitacion> buscarPorNumero(String numero);

    List<Habitacion> listarTodas();

    /**
     * Retorna habitaciones disponibles para el rango de fechas dado.
     * Query compleja que verifica contra reservas activas del periodo.
     *
     * @param criterios objeto con fechas y filtros opcionales
     * @return habitaciones sin reservas en el periodo solicitado
     */
    List<Habitacion> buscarDisponibles(BusquedaDisponibilidadDTO criterios);

    /**
     * Actualiza únicamente el estado de la habitación.
     *
     * @param idHabitacion  identificador de la habitación
     * @param nuevoEstado   estado destino
     * @return true si la actualización fue exitosa
     */
    boolean actualizarEstado(int idHabitacion, String nuevoEstado);
}

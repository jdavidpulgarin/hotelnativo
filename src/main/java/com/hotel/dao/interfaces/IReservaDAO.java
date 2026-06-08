
package com.hotel.dao.interfaces;

/**
 *
 * @author rober
 */
import com.hotel.model.Reserva;
import java.util.List;
import java.util.Optional;

/**
 * Contrato CRUD para operaciones básicas de Reserva.
 * SOLID: I - Interfaz segregada (CRUD separado de búsquedas)
 * GRASP: Indirección - capa de abstracción entre Service y BD
 * GRASP: Bajo Acoplamiento - Service no conoce la implementación JDBC
 */
public interface IReservaDAO {

    Reserva insertar(Reserva reserva);

    boolean actualizar(Reserva reserva);

    boolean eliminar(int idReserva);

    Optional<Reserva> buscarPorId(int idReserva);

    List<Reserva> listarTodas();
}
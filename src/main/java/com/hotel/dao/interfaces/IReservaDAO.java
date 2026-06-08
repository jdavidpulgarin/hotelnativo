package com.hotel.dao.interfaces;

import com.hotel.model.Reserva;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrato CRUD para operaciones básicas de Reserva.
 * Las operaciones de negocio (crear, cancelar) se delegan a PKG_HOTEL (Oracle).
 * SOLID: I - Interfaz segregada (CRUD separado de búsquedas)
 * GRASP: Indirección - capa de abstracción entre Service y BD
 * GRASP: Bajo Acoplamiento - Service no conoce la implementación JDBC
 */
public interface IReservaDAO {

    /** Llama PKG_HOTEL.crear_reserva — Oracle valida, calcula precio y hace COMMIT. */
    Reserva insertar(Reserva reserva);

    boolean actualizar(Reserva reserva);

    boolean eliminar(int idReserva);

    Optional<Reserva> buscarPorId(int idReserva);

    List<Reserva> listarTodas();

    /** Llama PKG_HOTEL.cancelar — Oracle libera la habitación si estaba RESERVADA/OCUPADA. */
    void cancelar(String idReserva, String motivo);

    /** Llama PKG_HOTEL.disponible — retorna true si la habitación está libre en esas fechas. */
    boolean verificarDisponibilidad(String idHabitacion, LocalDate entrada, LocalDate salida);

    /** Llama PKG_HOTEL.precio_reserva — retorna el precio calculado por Oracle (con descuento VIP). */
    double calcularPrecio(String idHabitacion, LocalDate entrada, LocalDate salida, boolean esVip);
}

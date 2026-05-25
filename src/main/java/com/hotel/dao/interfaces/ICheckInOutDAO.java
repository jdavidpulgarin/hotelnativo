
package com.hotel.dao.interfaces;

/**
 *
 * @author rober
 */
import com.hotel.model.CheckInOut;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrato para operaciones de CheckInOut en base de datos.
 *
 * MEJORA 5: añadido buscarCheckinsActivosPendientesCheckout para checkout automático.
 */
public interface ICheckInOutDAO {

    CheckInOut insertar(CheckInOut checkInOut);

    boolean actualizar(CheckInOut checkInOut);

    Optional<CheckInOut> buscarPorId(int id);

    Optional<CheckInOut> buscarCheckinActivoPorReserva(int idReserva);

    List<CheckInOut> listarTodos();

    /**
     * Devuelve todos los check-ins activos (sin checkout) cuya reserva
     * tiene fecha de salida menor o igual a {@code fecha}.
     * Usado por el proceso de checkout automático a las 11:00 AM.
     */
    List<CheckInOut> buscarCheckinsActivosPendientesCheckout(LocalDate fecha);
}
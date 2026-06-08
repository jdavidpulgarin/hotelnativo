package com.hotel.dao.interfaces;

import com.hotel.model.Mantenimiento;
import java.util.List;
import java.util.Optional;

/**
 * Contrato para operaciones de Mantenimiento en base de datos.
 */
public interface IMantenimientoDAO {

    Mantenimiento insertar(Mantenimiento mantenimiento);

    boolean actualizar(Mantenimiento mantenimiento);

    Optional<Mantenimiento> buscarPorId(int id);

    List<Mantenimiento> listarPorHabitacion(String numero);

    List<Mantenimiento> listarPendientes();

    List<Mantenimiento> listarTodos();
}

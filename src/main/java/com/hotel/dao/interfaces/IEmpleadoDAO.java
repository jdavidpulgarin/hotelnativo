
package com.hotel.dao.interfaces;

/**
 *
 * @author rober
 */
import com.hotel.model.Cargo;
import com.hotel.model.Empleado;
import java.util.List;
import java.util.Optional;

/**
 * Contrato para operaciones de Empleado en base de datos.
 */
public interface IEmpleadoDAO {

    Empleado insertar(Empleado empleado);

    boolean actualizar(Empleado empleado);

    boolean eliminar(int idEmpleado);

    Optional<Empleado> buscarPorId(int idEmpleado);

    List<Empleado> listarTodos();

    List<Empleado> buscarPorCargo(int idCargo);

    List<Cargo> listarCargos();

    /**
     * Actualiza únicamente el hash BCrypt de la contraseña del empleado en la BD.
     * Se llama tras cada cambio o reseteo de contraseña para garantizar persistencia.
     */
    boolean actualizarPasswordHash(int idEmpleado, String passwordHash);

    /**
     * Persiste el flag debe_cambiar_password en BD.
     * false = el empleado ya cambió su contraseña y no debe ser forzado de nuevo.
     */
    boolean actualizarDebeCambiarPassword(int idEmpleado, boolean debe);
}

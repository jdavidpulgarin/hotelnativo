
package com.hotel.dao.interfaces;

/**
 *
 * @author rober
 */
import com.hotel.model.Cliente;
import java.util.List;
import java.util.Optional;

/**
 * Contrato base para operaciones CRUD de Cliente.
 * SOLID: I - Interfaz segregada: CRUD básico separado de búsquedas (IClienteBusqueda)
 * SOLID: D - Service depende de esta abstracción, no de la implementación JDBC
 * GRASP: Indirección - desacopla Service del DAO concreto
 */
public interface IClienteDAO {

    /**
     * Persiste un nuevo cliente en la base de datos.
     *
     * @param cliente entidad a insertar
     * @return cliente con ID generado por la BD
     */
    Cliente insertar(Cliente cliente);

    /**
     * Actualiza los datos de un cliente existente.
     *
     * @param cliente entidad con datos actualizados
     * @return true si la actualización afectó al menos un registro
     */
    boolean actualizar(Cliente cliente);

    /**
     * Elimina un cliente por su identificador.
     *
     * @param idCliente identificador del cliente
     * @return true si se eliminó correctamente
     */
    boolean eliminar(int idCliente);

    /**
     * Busca un cliente por su identificador único.
     *
     * @param idCliente identificador del cliente
     * @return Optional con el cliente si existe, vacío si no
     */
    Optional<Cliente> buscarPorId(int idCliente);

    /**
     * Retorna todos los clientes registrados.
     *
     * @return lista de clientes, vacía si no hay registros
     */
    List<Cliente> listarTodos();

    /**
     * Elimina en cascada toda la información histórica del cliente:
     * check-ins → facturas → reservas. No elimina al cliente en sí.
     * Llamar antes de eliminar al cliente para evitar violaciones de FK.
     *
     * @param idCliente ID numérico del cliente
     */
    void purgarHistorialCliente(int idCliente);
}
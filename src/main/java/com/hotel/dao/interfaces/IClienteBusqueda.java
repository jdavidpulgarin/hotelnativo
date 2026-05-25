
package com.hotel.dao.interfaces;

/**
 *
 * @author rober
 */
import com.hotel.model.Cliente;
import java.util.List;
import java.util.Optional;

/**
 * Contrato para operaciones de búsqueda avanzada de clientes.
 * SOLID: I - Segregación de interfaz: separa consultas de operaciones CRUD
 * Solo los componentes que necesiten búsquedas implementarán esta interfaz.
 */
public interface IClienteBusqueda {

    /**
     * Busca clientes por número de documento de identidad.
     *
     * @param documento número de documento
     * @return Optional con el cliente si existe
     */
    Optional<Cliente> buscarPorDocumento(String documento);

    /**
     * Busca clientes cuyo nombre o apellido contenga el texto dado.
     *
     * @param textoBusqueda texto parcial a buscar
     * @return lista de clientes que coinciden
     */
    List<Cliente> buscarPorNombre(String textoBusqueda);

    /**
     * Busca clientes por correo electrónico.
     *
     * @param email correo del cliente
     * @return Optional con el cliente si existe
     */
    Optional<Cliente> buscarPorEmail(String email);

    /**
     * Retorna todos los clientes VIP del hotel.
     *
     * @return lista de clientes marcados como VIP
     */
    List<Cliente> listarClientesVip();
}

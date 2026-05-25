
package com.hotel.dao.interfaces;

/**
 *
 * @author rober
 */
import com.hotel.model.Factura;
import java.util.List;
import java.util.Optional;

/**
 * Contrato para operaciones de Factura en base de datos.
 */
public interface IFacturaDAO {

    Factura insertar(Factura factura);

    boolean actualizar(Factura factura);

    Optional<Factura> buscarPorId(int idFactura);

    Optional<Factura> buscarPorReserva(int idReserva);

    List<Factura> listarPorCliente(int idCliente);

    List<Factura> listarTodas();
}
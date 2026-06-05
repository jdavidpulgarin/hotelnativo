package com.hotel.service;

import com.hotel.dao.interfaces.IClienteDAO;
import com.hotel.dao.interfaces.IClienteBusqueda;
import com.hotel.dao.interfaces.IReservaBusqueda;
import com.hotel.dto.ClienteDTO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.exception.ExcepcionValidacion;
import com.hotel.model.Cliente;
import com.hotel.model.Reserva;
import com.hotel.util.ValidadorEntradas;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Lógica de negocio para gestión de clientes.
 *
 * GRASP: Alta Cohesión - solo maneja operaciones de clientes. GRASP:
 * Controlador - coordina los casos de uso relacionados con clientes. SOLID: S -
 * responsabilidad única: lógica de negocio de clientes. SOLID: D - depende de
 * interfaces IClienteDAO / IClienteBusqueda, no de implementaciones.
 */
public class ClienteService {

    // GRASP: Bajo Acoplamiento - depende de interfaces, no de implementaciones JDBC
    private final IClienteDAO clienteDAO;
    private final IClienteBusqueda clienteBusqueda;
    private final IReservaBusqueda reservaBusqueda;

    public ClienteService(IClienteDAO clienteDAO, IClienteBusqueda clienteBusqueda,
            IReservaBusqueda reservaBusqueda) {
        this.clienteDAO = clienteDAO;
        this.clienteBusqueda = clienteBusqueda;
        this.reservaBusqueda = reservaBusqueda;
    }

    /**
     * Registra un nuevo cliente validando datos de entrada. GRASP: Creador -
     * ClienteService crea instancias de Cliente.
     *
     * @param dto datos del cliente a registrar
     * @return cliente creado con ID asignado
     * @throws ExcepcionNegocio si el email o documento ya están registrados
     * @throws ExcepcionValidacion si algún campo tiene formato inválido
     */
    public Cliente registrarCliente(ClienteDTO dto) throws ExcepcionNegocio {
        validarDatosCliente(dto);
        verificarEmailNoRegistrado(dto.getEmail());

        // GRASP: Creador - esta clase tiene los datos para crear Cliente
        Cliente nuevoCliente = construirClienteDesdeDTO(dto);
        return clienteDAO.insertar(nuevoCliente);
    }

    /**
     * Actualiza datos de un cliente existente.
     *
     * @param idCliente identificador del cliente
     * @param dto nuevos datos
     * @return cliente actualizado
     * @throws ExcepcionNegocio si el cliente no existe
     */
    public Cliente actualizarCliente(int idCliente, ClienteDTO dto) throws ExcepcionNegocio {
        validarDatosCliente(dto);
        Cliente clienteExistente = obtenerClienteOLanzarError(idCliente);

        actualizarCamposCliente(clienteExistente, dto);
        clienteDAO.actualizar(clienteExistente);
        return clienteExistente;
    }

    /**
     * Elimina un cliente por ID.
     *
     * @throws ExcepcionNegocio si el cliente no existe
     */
    public void eliminarCliente(int idCliente) throws ExcepcionNegocio {
        ValidadorEntradas.validarIdPositivo(idCliente, "cliente");
        obtenerClienteOLanzarError(idCliente);

        List<Reserva> activas = reservaBusqueda.buscarReservasActivasPorCliente(idCliente);
        if (!activas.isEmpty()) {
            throw new ExcepcionNegocio("CLIENTE_CON_RESERVAS_ACTIVAS",
                    "El cliente tiene " + activas.size() + " reserva(s) activa(s) "
                    + "(PENDIENTE/CONFIRMADA/EN PROCESO). Cancélalas primero.");
        }

        try {
            clienteDAO.purgarHistorialCliente(idCliente);
            clienteDAO.eliminar(idCliente);
        } catch (ExcepcionBaseDatos e) {
            throw new ExcepcionNegocio("ERROR_BD", "Error al eliminar cliente: " + e.getMessage());
        }
    }

    public Optional<Cliente> buscarPorId(int idCliente) throws ExcepcionValidacion {
        ValidadorEntradas.validarIdPositivo(idCliente, "cliente");
        return clienteDAO.buscarPorId(idCliente);
    }

    public Optional<Cliente> buscarPorDocumento(String documento) throws ExcepcionValidacion {
        ValidadorEntradas.validarCampoRequerido(documento, "documento");
        return clienteBusqueda.buscarPorDocumento(documento);
    }

    public List<Cliente> buscarPorNombre(String textoBusqueda) throws ExcepcionValidacion {
        ValidadorEntradas.validarCampoRequerido(textoBusqueda, "textoBusqueda");
        return clienteBusqueda.buscarPorNombre(textoBusqueda);
    }

    public List<Cliente> listarTodosLosClientes() {
        return clienteDAO.listarTodos();
    }

    public List<Cliente> listarClientesVip() {
        return clienteBusqueda.listarClientesVip();
    }

    /**
     * Marca a un cliente como VIP.
     *
     * @throws ExcepcionNegocio si el cliente no existe
     */
    public void marcarClienteComoVip(int idCliente) throws ExcepcionNegocio {
        Cliente cliente = obtenerClienteOLanzarError(idCliente);
        cliente.setEsVip(true);
        clienteDAO.actualizar(cliente);
    }

    // ── métodos privados de apoyo ──────────────────────────────────────────────
    private void validarDatosCliente(ClienteDTO dto) throws ExcepcionValidacion {
        ValidadorEntradas.validarCampoRequerido(dto.getCedula(), "cédula");
        ValidadorEntradas.validarSoloNumeros(dto.getCedula(), "cédula");
        ValidadorEntradas.validarLargoNombre(dto.getNombre(), "nombre");
        ValidadorEntradas.validarLargoNombre(dto.getApellido(), "apellido");
        ValidadorEntradas.validarFormatoEmail(dto.getEmail());
        ValidadorEntradas.validarFormatoTelefono(dto.getTelefono());
        // nacionalidad almacena el id_pais (ej. "PAI01") — validar que no sea nulo
        if (dto.getNacionalidad() == null || dto.getNacionalidad().isBlank()) {
            throw new com.hotel.exception.ExcepcionValidacion("nacionalidad", "Selecciona la nacionalidad (país).");
        }
    }

    private void verificarEmailNoRegistrado(String email) throws ExcepcionNegocio {
        boolean emailYaRegistrado = clienteBusqueda.buscarPorEmail(email).isPresent();
        if (emailYaRegistrado) {
            throw new ExcepcionNegocio("EMAIL_DUPLICADO",
                    "El email '" + email + "' ya está registrado en el sistema.");
        }
    }

    private Cliente obtenerClienteOLanzarError(int idCliente) throws ExcepcionNegocio {
        return clienteDAO.buscarPorId(idCliente)
                .orElseThrow(() -> new ExcepcionNegocio("CLIENTE_NOT_FOUND",
                "No se encontró el cliente con ID: " + idCliente));
    }

    private Cliente construirClienteDesdeDTO(ClienteDTO dto) {
        // dto.getNacionalidad() = id_pais ("PAI01"), dto.getCiudadOrigen() = id_ciudad ("CIU79")
        Cliente c = new Cliente(0, dto.getNombre(), dto.getApellido(), dto.getEmail(),
                dto.getTelefono(), dto.getCedula(), dto.getNacionalidad(), LocalDate.now());
        c.setDocumento(dto.getCedula());
        c.setSegundoNombre(dto.getSegundoNombre());
        c.setApellido2(dto.getApellido2());
        c.setIdPais(dto.getNacionalidad());
        c.setIdCiudad(dto.getCiudadOrigen());
        c.setCiudadOrigen(dto.getCiudadOrigen());
        return c;
    }

    private void actualizarCamposCliente(Cliente cliente, ClienteDTO dto) {
        cliente.setNombre(dto.getNombre());
        cliente.setSegundoNombre(dto.getSegundoNombre());
        cliente.setApellido(dto.getApellido());
        cliente.setApellido2(dto.getApellido2());
        cliente.setEmail(dto.getEmail());
        cliente.setTelefono(dto.getTelefono());
        // dto almacena los IDs reales ("PAI01", "CIU79")
        cliente.setIdPais(dto.getNacionalidad());
        cliente.setIdCiudad(dto.getCiudadOrigen());
        cliente.setNacionalidad(dto.getNacionalidad());
        cliente.setCiudadOrigen(dto.getCiudadOrigen());
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
 *
 * @author Pulgarin
 */
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

    // ── métodos privados de apoyo ──────────────────────────────────────────────
    private void validarDatosCliente(ClienteDTO dto) throws ExcepcionValidacion {
        ValidadorEntradas.validarCampoRequerido(dto.getCedula(), "cédula");
        ValidadorEntradas.validarSoloNumeros(dto.getCedula(), "cédula");
        ValidadorEntradas.validarLargoNombre(dto.getNombre(), "nombre");
        ValidadorEntradas.validarLargoNombre(dto.getApellido(), "apellido");
        ValidadorEntradas.validarFormatoEmail(dto.getEmail());
        ValidadorEntradas.validarFormatoTelefono(dto.getTelefono());
        ValidadorEntradas.validarCampoRequerido(dto.getNacionalidad(), "nacionalidad");
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
        Cliente c = new Cliente(0, dto.getNombre(), dto.getApellido(), dto.getEmail(),
                dto.getTelefono(), dto.getCedula(), dto.getNacionalidad(), LocalDate.now());
        c.setDocumento(dto.getCedula());
        c.setSegundoNombre(dto.getSegundoNombre());
        c.setApellido2(dto.getApellido2());
        c.setCiudadOrigen(dto.getCiudadOrigen());
        return c;
    }

}

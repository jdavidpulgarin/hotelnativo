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
}

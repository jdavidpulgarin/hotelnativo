/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.service;

import com.hotel.dao.interfaces.IEmpleadoDAO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.exception.ExcepcionValidacion;
import com.hotel.model.Cargo;
import com.hotel.model.Empleado;
import com.hotel.util.ValidadorEntradas;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Pulgarin
 */
/**
 * Lógica de negocio para gestión de empleados del hotel.
 *
 * NUEVO: Esta clase faltaba completamente en el proyecto. Sin ella,
 * EmpleadoDAOImpl no tenía capa de servicio que la usara, y la vista de
 * empleados (EmpleadoView) no tenía a qué conectarse.
 *
 * Responsabilidades: - CRUD completo de empleados con validaciones. -
 * Integración con AuthService para registrar credenciales al crear. - Búsqueda
 * por cargo.
 *
 * GRASP: Alta Cohesión – solo maneja operaciones de empleados. GRASP:
 * Controlador – coordina los casos de uso de empleados. SOLID: S –
 * responsabilidad única: lógica de negocio de empleados. SOLID: D – depende de
 * IEmpleadoDAO, no de la implementación concreta.
 */
public class EmpleadoService {

    private final IEmpleadoDAO empleadoDAO;
    private final AuthService authService;
    
    
}

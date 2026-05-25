/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.service;

import com.hotel.dao.interfaces.*;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.*;
import com.hotel.util.ValidadorEntradas;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author Pulgarin
 */
/**
 * Lógica de negocio para check-in y check-out.
 *
 * MEJORA 5: añadido procesarCheckoutsAutomaticos() — recorre todos los
 * check-ins activos cuya reserva venció y ejecuta el checkout registrando la
 * nota "Checkout automático 11:00 AM".
 *
 * GRASP: Alta Cohesión — responsabilidad única: gestión de entrada y salida.
 * GRASP: Controlador — coordina el caso de uso "Realizar Check-in".
 */
public class CheckInOutService {

    private final ICheckInOutDAO checkInOutDAO;
    private final IReservaDAO reservaDAO;
    private final IHabitacionDAO habitacionDAO;
    private final IEmpleadoDAO empleadoDAO;

    public CheckInOutService(ICheckInOutDAO checkInOutDAO,
            IReservaDAO reservaDAO,
            IHabitacionDAO habitacionDAO,
            IEmpleadoDAO empleadoDAO) {
        this.checkInOutDAO = checkInOutDAO;
        this.reservaDAO = reservaDAO;
        this.habitacionDAO = habitacionDAO;
        this.empleadoDAO = empleadoDAO;
    }

}

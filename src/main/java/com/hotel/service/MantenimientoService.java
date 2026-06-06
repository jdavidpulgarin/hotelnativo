package com.hotel.service;

import com.hotel.dao.interfaces.*;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.exception.ExcepcionValidacion;
import com.hotel.model.*;
import com.hotel.util.ValidadorEntradas;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class MantenimientoService {

    private final IMantenimientoDAO mantenimientoDAO;
    private final IHabitacionDAO habitacionDAO;
    private final IEmpleadoDAO empleadoDAO;
    private final IReservaBusqueda reservaDAO;
    private final EmailService emailService;

    public MantenimientoService(IMantenimientoDAO mantenimientoDAO,
            IHabitacionDAO habitacionDAO,
            IEmpleadoDAO empleadoDAO,
            IReservaBusqueda reservaDAO,
            EmailService emailService) {
        this.mantenimientoDAO = mantenimientoDAO;
        this.habitacionDAO = habitacionDAO;
        this.empleadoDAO = empleadoDAO;
        this.reservaDAO = reservaDAO;
        this.emailService = emailService;
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.service;

import com.hotel.dao.interfaces.IFacturaDAO;
import com.hotel.dao.interfaces.IReservaDAO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.*;
import com.hotel.util.Constantes;
import com.hotel.util.ValidadorEntradas;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Pulgarin
 */
public class FacturaService {

    private final IFacturaDAO facturaDAO;
    private final IReservaDAO reservaDAO;

    public FacturaService(IFacturaDAO facturaDAO, IReservaDAO reservaDAO) {
        this.facturaDAO = facturaDAO;
        this.reservaDAO = reservaDAO;
    }
}

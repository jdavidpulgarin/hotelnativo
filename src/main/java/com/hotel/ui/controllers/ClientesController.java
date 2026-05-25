/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.controllers;

import com.hotel.AppContext;
import com.hotel.dto.ClienteDTO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.Cliente;
import com.hotel.ui.components.NotificationUtil;
import com.hotel.ui.components.ValidacionCampo;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Controlador del módulo de Clientes.
 * Carga datos en hilo secundario, muestra tabla con búsqueda en tiempo real,
 * y abre formularios modales para CRUD.
 */
public class ClientesController {

    @FXML private TableView<Cliente>          tabla;
    @FXML private TableColumn<Cliente,String> colId, colNombre, colApellido;
    @FXML private TableColumn<Cliente,String> colEmail, colTelefono, colDocumento;
    @FXML private TableColumn<Cliente,String> colNacionalidad, colVip, colRegistro;
    @FXML private TextField                   searchField;
    @FXML private Label                       lblTotal, lblVip;
    @FXML private ProgressBar                 progressBar;

    private final AppContext ctx = AppContext.getInstance();
    private ObservableList<Cliente> datos = FXCollections.observableArrayList();
    private FilteredList<Cliente>   datosFiltrados;
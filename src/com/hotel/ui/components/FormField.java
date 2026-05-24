/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.components;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Campo de formulario reutilizable con etiqueta, input y mensaje de error inline.
 *
 * Muestra un Label de título, un TextField y un Label de error (oculto por
 * defecto). El borde del input cambia visualmente según el estado de validación.
 *
 * Uso:
 * <pre>
 *   FormField campo = new FormField("NOMBRE", "Ingresa el nombre completo");
 *   campo.mostrarError("El nombre es obligatorio.");
 *   campo.limpiarError();
 *   String valor = campo.getValue();
 * </pre>
 *
 * GRASP: Fabricación Pura – componente de UI sin dominio propio.
 * SOLID: S – responsabilidad única: presentar un campo con validación visual.
 */
public class FormField extends VBox {

    private final TextField input;
    private final Label     lblError;
}
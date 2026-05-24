/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

/**
 * Diálogo de confirmación de acción destructiva (ej: eliminar registro).
 *
 * Muestra un icono de advertencia "⚠", un título, un mensaje descriptivo
 * y dos botones: "Cancelar" y "Sí, eliminar". Usa {@link ModalUtil} para
 * la presentación y las animaciones.
 *
 * Uso típico desde un controlador:
 * <pre>
 *   ConfirmDialog.mostrar(contentArea, "Eliminar cliente",
 *       "¿Estás seguro de que deseas eliminar a Carlos Ramírez?",
 *       () -> clienteService.eliminar(id));
 * </pre>
 *
 * GRASP: Fabricación Pura – componente de UI sin dominio propio.
 * SOLID: S – responsabilidad única: confirmar acciones destructivas.
 */
public final class ConfirmDialog {

    private ConfirmDialog() {}
}
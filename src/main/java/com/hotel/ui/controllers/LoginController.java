/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.controllers;

import com.hotel.AppContext;
import com.hotel.exception.ExcepcionValidacion;
import com.hotel.model.Empleado;
import com.hotel.service.AuthService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Controlador de la pantalla de Login.
 * Ejecuta la autenticación en hilo secundario para no bloquear la UI.
 *
 * Maneja el flujo de cambio de contraseña obligatorio: cuando AuthService lanza
 * CAMBIO_PASSWORD_REQUERIDO, muestra un diálogo inline antes de continuar.
 */
public class LoginController {

    @FXML private TextField         emailField;
    @FXML private PasswordField     passwordField;
    @FXML private Label             errorLabel;
    @FXML private Button            btnLogin;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private VBox              formCard;
    @FXML private HBox              emailWrapper;
    @FXML private HBox              passwordWrapper;

    private final AppContext ctx = AppContext.getInstance();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);

        emailField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleLogin());

        // Focus glow — clase "focused" en el HBox wrapper activa CSS verde esmeralda
        configurarFocusWrapper(emailField,    emailWrapper);
        configurarFocusWrapper(passwordField, passwordWrapper);

        // Botón login — hover scale 1.03x (EASE_OUT 180ms)
        ScaleTransition btnHIn  = new ScaleTransition(Duration.millis(180), btnLogin);
        ScaleTransition btnHOut = new ScaleTransition(Duration.millis(180), btnLogin);
        btnHIn.setToX(1.03);  btnHIn.setToY(1.03);
        btnHOut.setToX(1.0);  btnHOut.setToY(1.0);
        btnHIn.setInterpolator(Interpolator.EASE_OUT);
        btnHOut.setInterpolator(Interpolator.EASE_OUT);
        btnLogin.setOnMouseEntered(e -> { btnHOut.stop(); btnHIn.playFromStart(); });
        btnLogin.setOnMouseExited(e  -> { btnHIn.stop();  btnHOut.playFromStart(); });

        // Botón login — pressed scale 0.98x (80ms) / release 1.0x (140ms)
        btnLogin.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(80), btnLogin);
            st.setToX(0.98); st.setToY(0.98);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });
        btnLogin.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(140), btnLogin);
            st.setToX(1.0); st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });

        // Animación de entrada: card FadeIn + Scale 0.94→1.0
        Platform.runLater(() -> {
            if (formCard != null) {
                formCard.setOpacity(0);
                formCard.setScaleX(0.94);
                formCard.setScaleY(0.94);
                FadeTransition ft = new FadeTransition(Duration.millis(500), formCard);
                ft.setFromValue(0); ft.setToValue(1);
                ScaleTransition st = new ScaleTransition(Duration.millis(500), formCard);
                st.setFromX(0.94); st.setToX(1.0);
                st.setFromY(0.94); st.setToY(1.0);
                st.setInterpolator(Interpolator.EASE_OUT);
                new ParallelTransition(ft, st).play();
            }
        });
    }

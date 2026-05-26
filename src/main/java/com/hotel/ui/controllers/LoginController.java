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
private void configurarFocusWrapper(javafx.scene.control.Control campo, HBox wrapper) {
        if (wrapper == null) return;
        campo.focusedProperty().addListener((obs, old, isFocused) -> {
            if (isFocused) {
                if (!wrapper.getStyleClass().contains("focused"))
                    wrapper.getStyleClass().add("focused");
            } else {
                wrapper.getStyleClass().remove("focused");
            }
        });
    }

    @FXML
    public void handleLogin() {
        String email = emailField.getText().trim();
        String pass  = passwordField.getText().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            mostrarError("Por favor ingresa email y contraseña.");
            sacudirCampo(email.isEmpty() ? emailField : passwordField);
            return;
        }

        setLoading(true);
        errorLabel.setVisible(false);

        Thread tLogin = new Thread(() -> {
            try {
                ctx.getCredencialesCargadas().get(15, TimeUnit.SECONDS);

                String token = ctx.getAuthService().login(email, pass);
                Empleado emp = ctx.getAuthService().obtenerEmpleadoActual(token)
                                  .orElseThrow(() -> new RuntimeException(
                                      "Sesión creada pero no encontrada. Intenta de nuevo."));

                ctx.setTokenSesion(token);
                ctx.setEmpleadoActual(emp);

                Platform.runLater(() -> {
                    setLoading(false);
                    NavigatorUtil.irAlDashboard();
                });

            } catch (TimeoutException e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    mostrarError("No se pudo conectar a la base de datos. Verifica la conexión.");
                });
            } catch (AuthService.AuthException ex) {
                Platform.runLater(() -> {
                    setLoading(false);
                    if ("CAMBIO_PASSWORD_REQUERIDO".equals(ex.getCodigo())) {
                        mostrarDialogoCambioPassword(ex.getPreAuthToken());
                    } else {
                        mostrarError(ex.getMessage());
                        sacudirCampo(passwordField);
                        passwordField.clear();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    mostrarError("Error inesperado: " + e.getMessage());
                });
            }
        }, "hilo-login");
        tLogin.setDaemon(true);
        tLogin.start();
    }
    @FXML
    public void handleClose() {
        Platform.exit();
        System.exit(0);
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private void mostrarError(String mensaje) {
        errorLabel.setStyle("-fx-text-fill:#dc2626; -fx-font-size:12px;");
        errorLabel.setText(mensaje);
        errorLabel.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), errorLabel);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }

    private void mostrarExito(String mensaje) {
        errorLabel.setStyle("-fx-text-fill:#16a34a; -fx-font-size:12px;");
        errorLabel.setText(mensaje);
        errorLabel.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), errorLabel);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }

    private void sacudirCampo(Control campo) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), campo);
        tt.setFromX(0); tt.setToX(8);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> campo.setTranslateX(0));
        tt.play();
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        btnLogin.setDisable(loading);
        btnLogin.setText(loading ? "Verificando..." : "INICIAR SESIÓN");
    }
    @FXML
    public void handleClose() {
        Platform.exit();
        System.exit(0);
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private void mostrarError(String mensaje) {
        errorLabel.setStyle("-fx-text-fill:#dc2626; -fx-font-size:12px;");
        errorLabel.setText(mensaje);
        errorLabel.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), errorLabel);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }

    private void mostrarExito(String mensaje) {
        errorLabel.setStyle("-fx-text-fill:#16a34a; -fx-font-size:12px;");
        errorLabel.setText(mensaje);
        errorLabel.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), errorLabel);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }

    private void sacudirCampo(Control campo) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), campo);
        tt.setFromX(0); tt.setToX(8);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> campo.setTranslateX(0));
        tt.play();
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        btnLogin.setDisable(loading);
        btnLogin.setText(loading ? "Verificando..." : "INICIAR SESIÓN");
    }
     // ── Diálogo de cambio de contraseña obligatorio ───────────────────────────

    /**
     * Muestra una ventana modal para que el empleado cambie su contraseña
     * antes de continuar. Se activa únicamente cuando AuthService emite
     * el código CAMBIO_PASSWORD_REQUERIDO en el primer login.
     */
    private void mostrarDialogoCambioPassword(String preAuthToken) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initOwner(btnLogin.getScene().getWindow());
        dialog.setTitle("Cambiar contraseña");

        // ── Card central ──────────────────────────────────────────────────────
        VBox card = new VBox(14);
        card.setMaxWidth(400);
        card.setPadding(new Insets(28, 32, 28, 32));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 28, 0, 0, 8);");

        Label titulo = new Label("Cambiar contraseña");
        titulo.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#1e293b;");

        Label subtitulo = new Label(
            "Por seguridad debes crear una nueva contraseña antes de continuar.");
        subtitulo.setStyle("-fx-font-size:13px; -fx-text-fill:#64748b;");
        subtitulo.setWrapText(true);

        // Campos
        Label lblActual = etiqueta("CONTRASEÑA ACTUAL");
        PasswordField pfActual = campoPassword("Tu contraseña actual");

        Label lblNueva = etiqueta("NUEVA CONTRASEÑA");
        PasswordField pfNueva = campoPassword("Mínimo 8 caracteres, mayúscula, número y símbolo");

        Label lblConfirmar = etiqueta("CONFIRMAR NUEVA CONTRASEÑA");
        PasswordField pfConfirmar = campoPassword("Repite la nueva contraseña");

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill:#dc2626; -fx-font-size:12px;");
        lblError.setVisible(false);
        lblError.setWrapText(true);
        lblError.setMaxWidth(336);

        Button btnCambiar = new Button("Cambiar contraseña");
        btnCambiar.setMaxWidth(Double.MAX_VALUE);
        btnCambiar.setStyle(
            "-fx-background-color: linear-gradient(to bottom,#1e4575,#1a3a5c);" +
            "-fx-text-fill:white; -fx-font-size:14px; -fx-font-weight:bold;" +
            "-fx-background-radius:10px; -fx-pref-height:46px; -fx-cursor:hand;");
          btnCambiar.setOnAction(e -> {
            String actual    = pfActual.getText();
            String nueva     = pfNueva.getText();
            String confirmar = pfConfirmar.getText();

            if (actual.isBlank() || nueva.isBlank() || confirmar.isBlank()) {
                mostrarErrorDialog(lblError, "Todos los campos son obligatorios.");
                return;
            }
            if (!nueva.equals(confirmar)) {
                mostrarErrorDialog(lblError, "Las contraseñas nuevas no coinciden.");
                pfConfirmar.clear();
                return;
            }

            btnCambiar.setDisable(true);

            // Obtener email antes de que cambiarPassword consuma el token
            String emailNorm = ctx.getAuthService().obtenerEmailDePreAuthToken(preAuthToken);

            new Thread(() -> {
                try {
                    ctx.getAuthService().cambiarPassword(preAuthToken, actual, nueva);

                    // Buscar empleado en el mapa en memoria de AuthService — O(1), sin query extra
                    String nuevoHash = ctx.getAuthService().generarHash(nueva);
                    if (emailNorm != null) {
                        ctx.getAuthService().obtenerEmpleadoPorEmail(emailNorm).ifPresent(emp -> {
                            try {
                                ctx.getEmpleadoService().persistirHashEnBD(emp.getId(), nuevoHash);
                                ctx.getEmpleadoService().actualizarDebeCambiarPassword(emp.getId(), false);
                                emp.setDebeCambiarContrasena(false);
                                ctx.getAuthService().registrarCredencialesConHash(emp, nuevoHash);
                                System.out.println("[LOGIN] Hash y flag persistidos en BD para: " + emailNorm);
                            } catch (Exception ex) {
                                System.err.println("[LOGIN] Error persistiendo datos: " + ex.getMessage());
                            }
                        });
                    }

                    Platform.runLater(() -> {
                        dialog.close();
                        mostrarExito("Contraseña cambiada. Inicia sesión con tu nueva contraseña.");
                        passwordField.clear();
                    });
                } catch (AuthService.AuthException | ExcepcionValidacion ex) {
                    Platform.runLater(() -> {
                        btnCambiar.setDisable(false);
                        mostrarErrorDialog(lblError, ex.getMessage());
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        btnCambiar.setDisable(false);
                        mostrarErrorDialog(lblError, "Error inesperado: " + ex.getMessage());
                    });
                }
            }, "hilo-cambio-password").start();
        });

        card.getChildren().addAll(
            titulo, subtitulo,
            new Separator(),
            lblActual, pfActual,
            lblNueva, pfNueva,
            lblConfirmar, pfConfirmar,
            lblError,
            btnCambiar
        );

        // ── Overlay semitransparente ──────────────────────────────────────────
        StackPane overlay = new StackPane(card);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.55);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setPadding(new Insets(40));

        Scene scene = new Scene(overlay);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(
            getClass().getResource("/com/hotel/ui/styles/main.css").toExternalForm());

        dialog.setScene(scene);

        // ── Animación de entrada ──────────────────────────────────────────────
        card.setOpacity(0);
        card.setScaleX(0.88);
        card.setScaleY(0.88);
        dialog.show();

        FadeTransition fadein  = new FadeTransition(Duration.millis(200), card);
        fadein.setFromValue(0); fadein.setToValue(1);
        ScaleTransition scalein = new ScaleTransition(Duration.millis(200), card);
        scalein.setFromX(0.88); scalein.setToX(1.0);
        scalein.setFromY(0.88); scalein.setToY(1.0);
        new ParallelTransition(fadein, scalein).play();
    }
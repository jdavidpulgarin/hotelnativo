package com.hotel.ui.controllers;

import com.hotel.HotelApp;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Utilidad de navegación entre pantallas.
 * Centraliza la carga de FXML y aplica transiciones de fade para
 * una experiencia fluida sin saltos bruscos.
 */
public class NavigatorUtil {

    private static final String FXML_BASE = "/com/hotel/ui/fxml/";
    private static final String CSS_PATH  =
            NavigatorUtil.class.getResource("/com/hotel/ui/styles/main.css").toExternalForm();

    /**
     * Navega al dashboard principal reemplazando la escena actual.
     */
    public static void irAlDashboard() {
        cargarEscena("Dashboard.fxml", 1280, 800, false, true);
    }

    /**
     * Navega al login (al cerrar sesión).
     */
    public static void irAlLogin() {
        cargarEscena("Login.fxml", 1000, 650, false, false);
    }

    /**
     * Abre una ventana modal (formularios de alta/edición).
     *
     * @param fxml   nombre del archivo FXML
     * @param titulo título de la ventana
     * @param ancho  ancho en px
     * @param alto   alto en px
     * @return el controlador cargado (para pasarle datos)
     */
    public static Object abrirModal(String fxml, String titulo, int ancho, int alto) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    NavigatorUtil.class.getResource(FXML_BASE + fxml));
            Parent root = loader.load();

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initOwner(HotelApp.getPrimaryStage());
            modal.initStyle(StageStyle.UNDECORATED);
            modal.setTitle(titulo);

            Scene scene = new Scene(root, ancho, alto);
            scene.getStylesheets().add(CSS_PATH);
            modal.setScene(scene);
            modal.centerOnScreen();
            modal.showAndWait();

            return loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Privado ───────────────────────────────────────────────────────────────

    private static void cargarEscena(String fxml, int ancho, int alto,
                                      boolean undecorated, boolean maximizar) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    NavigatorUtil.class.getResource(FXML_BASE + fxml));
            Parent root = loader.load();

            Scene scene = new Scene(root, ancho, alto);
            scene.getStylesheets().add(CSS_PATH);

            // Fade out → cambio de escena → fade in
            Stage stage = HotelApp.getPrimaryStage();
            FadeTransition fadeOut = new FadeTransition(Duration.millis(180), stage.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                stage.setScene(scene);
                if (maximizar) {
                    stage.setMaximized(true);
                } else {
                    stage.setMaximized(false);
                    stage.setWidth(ancho);
                    stage.setHeight(alto);
                    stage.centerOnScreen();
                }
                FadeTransition fadeIn = new FadeTransition(Duration.millis(250), root);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

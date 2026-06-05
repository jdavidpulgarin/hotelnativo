package com.hotel;

import com.hotel.util.ManejadorExcepciones;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Punto de entrada de la aplicación JavaFX.
 * Carga la pantalla de login y configura el stage principal.
 */
public class HotelApp extends Application {

    private static Stage primaryStage;

    public static Stage getPrimaryStage() { return primaryStage; }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Manejador global de excepciones no capturadas
        ManejadorExcepciones handler = new ManejadorExcepciones();
        Thread.setDefaultUncaughtExceptionHandler(handler);
        Thread.currentThread().setUncaughtExceptionHandler(handler);

        // Ventana sin bordes nativos para diseño moderno
        stage.initStyle(StageStyle.UNDECORATED);

        // Cargar pantalla de login
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/hotel/ui/fxml/Login.fxml"));
        Scene scene = new Scene(loader.load(), 1000, 650);
        scene.getStylesheets().add(
                getClass().getResource("/com/hotel/ui/styles/main.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Hotel Nativo — Sistema de Gestión");
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setFullScreenExitHint("");

        // Inicia en ventana normal centrada (se puede mover y redimensionar)
        stage.setWidth(1280);
        stage.setHeight(780);
        stage.centerOnScreen();

        stage.show();
    }

    @Override
    public void stop() {
        // Cerrar pool de conexiones al salir
        try {
            com.hotel.util.ConexionBaseDatos.obtenerInstancia().cerrarPool();
        } catch (Exception ignored) {}
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package com.hotel;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class HotelApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/hotel/ui/views/login.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1400, 800);
        scene.getStylesheets().add(
                getClass().getResource("/com/hotel/ui/css/styles.css").toExternalForm());

        primaryStage.setTitle("Hotel Nativo — Sistema de Gestión");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package com.hotel.ui.components;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

/**
 * Utilidad para crear modales custom sobre un contenedor StackPane existente.
 *
 * El modal se compone de:
 *   - Overlay semitransparente rgba(0,0,0,0.5) que cubre toda la pantalla.
 *   - Card blanca centrada (max-width 520px, border-radius 16px, sombra).
 *   - Header con título y botón "✕".
 *   - Body con ScrollPane para el contenido.
 *
 * Animaciones: escala 0.9→1 + fade-in 200ms al abrir; fade-out 150ms al cerrar.
 *
 * GRASP: Fabricación Pura – utilidad de UI sin dominio propio.
 * SOLID: S – responsabilidad única: construir y animar modales.
 */
public final class ModalUtil {

    private ModalUtil() {}

    /**
     * Crea un modal y lo devuelve como StackPane listo para agregar al contenedor padre.
     * El llamador es responsable de añadirlo y de eliminarlo mediante {@code onCerrar}.
     *
     * @param contenido  nodo que aparece en el cuerpo del modal
     * @param titulo     texto del encabezado
     * @param onCerrar   acción ejecutada tras la animación de cierre (ej: remover del padre)
     * @return           StackPane overlay con el modal embebido
     */
    public static StackPane crearModal(Node contenido, String titulo, Runnable onCerrar) {

        // ── Header ────────────────────────────────────────────────────────────
        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle(
            "-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1e293b;");

        Button btnCerrar = new Button("✕");
        btnCerrar.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#64748b;" +
            "-fx-font-size:16px; -fx-cursor:hand; -fx-padding:2px 6px;");
        btnCerrar.setOnMouseEntered(e ->
            btnCerrar.setStyle(
                "-fx-background-color:#fee2e2; -fx-text-fill:#b91c1c;" +
                "-fx-font-size:16px; -fx-cursor:hand; -fx-padding:2px 6px;" +
                "-fx-background-radius:6px;"));
        btnCerrar.setOnMouseExited(e ->
            btnCerrar.setStyle(
                "-fx-background-color:transparent; -fx-text-fill:#64748b;" +
                "-fx-font-size:16px; -fx-cursor:hand; -fx-padding:2px 6px;"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(lblTitulo, spacer, btnCerrar);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 12, 0));

        // ── Body con scroll ────────────────────────────────────────────────
        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setStyle(
            "-fx-background-color:transparent; -fx-border-color:transparent;");
        scroll.setMaxHeight(480);

        // ── Card ───────────────────────────────────────────────────────────
        VBox card = new VBox(header, new Separator(), scroll);
        card.setPadding(new Insets(24));
        card.setMaxWidth(520);
        card.setStyle(
            "-fx-background-color:white;" +
            "-fx-background-radius:16px;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.30),24,0,0,6);");

        // ── Overlay ────────────────────────────────────────────────────────
        StackPane overlay = new StackPane(card);
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color:rgba(0,0,0,0.50);");
        overlay.setPadding(new Insets(32));

        // Cerrar al hacer click en el overlay (pero no en la card)
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) cerrarModal(overlay, onCerrar);
        });

        btnCerrar.setOnAction(e -> cerrarModal(overlay, onCerrar));

        // ── Animación de entrada ───────────────────────────────────────────
        overlay.setOpacity(0);
        card.setScaleX(0.9);
        card.setScaleY(0.9);

        FadeTransition fadeIn   = new FadeTransition(Duration.millis(200), overlay);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), card);
        scaleIn.setFromX(0.9); scaleIn.setToX(1.0);
        scaleIn.setFromY(0.9); scaleIn.setToY(1.0);
        new ParallelTransition(fadeIn, scaleIn).play();

        return overlay;
    }

    /** Animación de salida (fade-out 150ms) y luego ejecuta {@code onCerrar}. */
    static void cerrarModal(StackPane overlay, Runnable onCerrar) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), overlay);
        fadeOut.setFromValue(overlay.getOpacity());
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> onCerrar.run());
        fadeOut.play();
    }
}

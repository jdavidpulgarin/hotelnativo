/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.ui.controllers;

import com.hotel.AppContext;
import com.hotel.dto.ClienteDTO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.*;
import com.hotel.ui.components.NotificationUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class CheckInOutController {

    @FXML private TableView<CheckInOut>          tabla;
    @FXML private TableColumn<CheckInOut,String> colId, colCliente, colHab;
    @FXML private TableColumn<CheckInOut,String> colEntrada, colSalida, colEmpleado;
    @FXML private TableColumn<CheckInOut,String> colHoraCheckin, colHoraCheckout, colEstado;
    @FXML private TextField                      searchField;
    @FXML private HBox                           statsRow;
    @FXML private ProgressBar                    progressBar;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppContext ctx = AppContext.getInstance();
    private ObservableList<CheckInOut> datos    = FXCollections.observableArrayList();
    private FilteredList<CheckInOut>   filtradas;

    @FXML
    public void initialize() {
        configurarColumnas();
        filtradas = new FilteredList<>(datos, p -> true);
        tabla.setItems(filtradas);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cargarDatos();
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(
                "#" + c.getValue().getId()));
        colCliente.setCellValueFactory(c -> {
            Reserva r = c.getValue().getReserva();
            return new SimpleStringProperty(
                    r != null && r.getCliente() != null
                            ? r.getCliente().obtenerNombreCompleto() : "—");
        });
        colHab.setCellValueFactory(c -> {
            Reserva r = c.getValue().getReserva();
            return new SimpleStringProperty(
                    r != null && r.getHabitacion() != null
                            ? r.getHabitacion().getNumero() : "—");
        });
        colEntrada.setCellValueFactory(c -> {
            Reserva r = c.getValue().getReserva();
            return new SimpleStringProperty(
                    r != null && r.getFechaEntrada() != null
                            ? r.getFechaEntrada().toString() : "—");
        });
        colSalida.setCellValueFactory(c -> {
            Reserva r = c.getValue().getReserva();
            return new SimpleStringProperty(
                    r != null && r.getFechaSalida() != null
                            ? r.getFechaSalida().toString() : "—");
        });
        colEmpleado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEmpleadoResponsable() != null
                        ? c.getValue().getEmpleadoResponsable().obtenerNombreCompleto() : "—"));
        colHoraCheckin.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaHoraCheckin() != null
                        ? c.getValue().getFechaHoraCheckin().format(FMT) : "—"));
        colHoraCheckout.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaHoraCheckout() != null
                        ? c.getValue().getFechaHoraCheckout().format(FMT) : "—"));

        colEstado.setCellValueFactory(c -> {
            Reserva r = c.getValue().getReserva();
            return new SimpleStringProperty(
                    r != null && r.getEstado() != null ? r.getEstado().name() : "—");
        });
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                badge.setStyle(getBadgeEstilo(item));
                setGraphic(badge); setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        tabla.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(CheckInOut ci, boolean empty) {
                super.updateItem(ci, empty);
                if (ci == null || empty) { setStyle(""); return; }
                setStyle(ci.haRealizadoCheckout()
                        ? "-fx-background-color:#f0fdf4;"
                        : "-fx-background-color:#fffbeb;");
            }
        });
    }

    @FXML
    public void cargarDatos() {
        progressBar.setVisible(true);
        new Thread(() -> {
            try {
                List<CheckInOut> lista = ctx.getCheckInOutService().listarTodos();
                Platform.runLater(() -> {
                    datos.setAll(lista);
                    actualizarStats(lista);
                    progressBar.setVisible(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    NotificationUtil.error("Error cargando check-ins: " + e.getMessage());
                    progressBar.setVisible(false);
                });
            }
        }).start();
    }

    @FXML
    public void filtrar() {
        String texto = searchField.getText().toLowerCase().trim();
        filtradas.setPredicate(ci -> {
            if (texto.isEmpty()) return true;
            Reserva r = ci.getReserva();
            return String.valueOf(ci.getId()).contains(texto)
                || (r != null && r.getCliente() != null
                        && r.getCliente().obtenerNombreCompleto().toLowerCase().contains(texto))
                || (r != null && r.getHabitacion() != null
                        && r.getHabitacion().getNumero().toLowerCase().contains(texto))
                || (ci.getEmpleadoResponsable() != null
                        && ci.getEmpleadoResponsable().obtenerNombreCompleto()
                                .toLowerCase().contains(texto));
        });
    }

    @FXML
    public void realizarCheckin() { mostrarFormularioCheckin(); }

    @FXML
    public void realizarCheckout() {
        CheckInOut sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) {
            NotificationUtil.advertencia("Selecciona un check-in activo para hacer check-out.");
            return;
        }
        if (sel.haRealizadoCheckout()) {
            NotificationUtil.advertencia("Este registro ya tiene check-out registrado.");
            return;
        }
        mostrarDialogCheckout(sel);
    }

    @FXML
    public void checkoutsAutomaticos() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Checkouts Automáticos 11AM");
        confirm.setHeaderText("¿Procesar checkouts automáticos?");
        confirm.setContentText(
                "Se procesarán todos los check-ins activos cuya fecha de salida ya venció.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    int procesados = ctx.getCheckInOutService().procesarCheckoutsAutomaticos();
                    Platform.runLater(() -> {
                        NotificationUtil.exito(
                                "Checkouts automáticos: " + procesados + " procesado(s).");
                        cargarDatos();
                    });
                }).start();
            }
        });
    }

    // ── Formulario Check-in ───────────────────────────────────────────────────

    private void mostrarFormularioCheckin() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Realizar Check-in");

        // ── Sección búsqueda por cédula ────────────────────────────────────────
        GridPane gridBusqueda = new GridPane();
        gridBusqueda.setHgap(12); gridBusqueda.setVgap(10);
        TextField fCedulaBuscar = tf("Número de cédula");
        fCedulaBuscar.setPrefWidth(220);
        Button btnBuscar = new Button("🔍 Buscar");
        btnBuscar.setStyle("-fx-background-color:#2563a8; -fx-text-fill:white;" +
                           "-fx-background-radius:8px; -fx-font-size:12px; -fx-padding:7px 14px;");
        gridBusqueda.add(lab("Cédula:"), 0, 0);
        gridBusqueda.add(fCedulaBuscar, 1, 0);
        gridBusqueda.add(btnBuscar, 2, 0);

        Label lblClienteInfo = new Label("Ingresa la cédula y haz clic en Buscar.");
        lblClienteInfo.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b;");

        // ── Sección datos cliente ───────────────────────────────────────────────
        GridPane gridCliente = new GridPane();
        gridCliente.setHgap(12); gridCliente.setVgap(10);
        TextField fCedula        = tf("Cédula *");
        TextField fNombre        = tf("Primer nombre *");
        TextField fSegundoNombre = tf("Segundo nombre");
        TextField fApellido      = tf("Primer apellido *");
        TextField fApellido2     = tf("Segundo apellido");
        TextField fEmail         = tf("Email *");
        TextField fTelefono      = tf("Teléfono *");
        TextField fNacionalidad  = tf("Nacionalidad *");
        TextField fCiudad        = tf("Ciudad origen");
        setClienteFieldsDisabled(true, fCedula, fNombre, fSegundoNombre, fApellido, fApellido2,
                                       fEmail, fTelefono, fNacionalidad, fCiudad);
        gridCliente.addRow(0, lab("Cédula *:"),          fCedula);
        GridPane.setColumnSpan(fCedula, 3);
        gridCliente.addRow(1, lab("Primer nombre *:"),  fNombre,      lab("Segundo nombre:"),  fSegundoNombre);
        gridCliente.addRow(2, lab("Primer apellido *:"), fApellido,   lab("Segundo apellido:"), fApellido2);
        gridCliente.addRow(3, lab("Email *:"),            fEmail,      lab("Teléfono *:"),      fTelefono);
        gridCliente.addRow(4, lab("Nacionalidad *:"),     fNacionalidad, lab("Ciudad origen:"), fCiudad);

        // ── Sección datos reserva ───────────────────────────────────────────────
        GridPane gridReserva = new GridPane();
        gridReserva.setHgap(12); gridReserva.setVgap(10);
        TextField fIdReserva  = tf("ID de la reserva (CONFIRMADA)");
        TextField fIdEmpleado = tf("ID del empleado que atiende");
        TextArea  fObs        = new TextArea();
        fObs.setPromptText("Observaciones (opcional)");
        fObs.setPrefRowCount(2);
        fObs.setStyle("-fx-background-color:#f8fafc; -fx-border-color:#e2e8f0;" +
                      "-fx-border-width:1.5px; -fx-border-radius:8px;");
        Label lblHoraIngreso = new Label("⏱ " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        lblHoraIngreso.setStyle("-fx-font-size:11px; -fx-text-fill:#475569;");
        gridReserva.addRow(0, lab("ID Reserva:"),    fIdReserva,    lab("ID Empleado:"),  fIdEmpleado);
        gridReserva.addRow(1, lab("Hora ingreso:"),  lblHoraIngreso);
        gridReserva.addRow(2, lab("Observaciones:"), fObs);
        GridPane.setColumnSpan(fObs, 3);

        final int[] clienteIdRef = {-1};

        btnBuscar.setOnAction(e -> {
            String cedulaBusq = fCedulaBuscar.getText().trim();
            if (cedulaBusq.isEmpty()) {
                lblClienteInfo.setText("Ingresa la cédula.");
                lblClienteInfo.setStyle("-fx-font-size:11px; -fx-text-fill:#b45309;");
                return;
            }
            try {
                Optional<Cliente> encontrado = ctx.getClienteService()
                        .listarTodosLosClientes().stream()
                        .filter(c -> cedulaBusq.equals(c.getDocumento())
                                  || cedulaBusq.equals(String.valueOf(c.getId())))
                        .findFirst();
                if (encontrado.isPresent()) {
                    Cliente c = encontrado.get();
                    clienteIdRef[0] = c.getId();
                    String cedStr = c.getDocumento() != null ? c.getDocumento() : String.valueOf(c.getId());
                    fCedula.setText(cedStr);
                    fNombre.setText(c.getNombre());
                    fSegundoNombre.setText(c.getSegundoNombre() != null ? c.getSegundoNombre() : "");
                    fApellido.setText(c.getApellido());
                    fApellido2.setText(c.getApellido2() != null ? c.getApellido2() : "");
                    fEmail.setText(c.getEmail());
                    fTelefono.setText(c.getTelefono() != null ? c.getTelefono() : "");
                    fNacionalidad.setText(c.getNacionalidad() != null ? c.getNacionalidad() : "");
                    fCiudad.setText(c.getCiudadOrigen() != null ? c.getCiudadOrigen() : "");
                    setClienteFieldsDisabled(true, fCedula, fNombre, fSegundoNombre, fApellido, fApellido2,
                                                   fEmail, fTelefono, fNacionalidad, fCiudad);
                    lblClienteInfo.setText("✓ Cliente encontrado: " + c.obtenerNombreCompleto()
                            + "  (Cédula: " + cedStr + ")");
                    lblClienteInfo.setStyle("-fx-font-size:11px; -fx-text-fill:#16a34a;");
                } else {
                    clienteIdRef[0] = -1;
                    fCedula.setText(cedulaBusq);
                    fNombre.clear(); fSegundoNombre.clear();
                    fApellido.clear(); fApellido2.clear();
                    fEmail.clear(); fTelefono.clear(); fNacionalidad.clear(); fCiudad.clear();
                    setClienteFieldsDisabled(false, fCedula, fNombre, fSegundoNombre, fApellido, fApellido2,
                                                    fEmail, fTelefono, fNacionalidad, fCiudad);
                    fCedula.setEditable(false);
                    lblClienteInfo.setText("⚠ Cliente no encontrado. Completa los datos para registrarlo.");
                    lblClienteInfo.setStyle("-fx-font-size:11px; -fx-text-fill:#b45309;");
                }
            } catch (Exception ex) {
                NotificationUtil.error("Error buscando cliente: " + ex.getMessage());
            }
        });

        Label header = new Label("🔑 Realizar Check-in");
        header.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;");
        Label errLabel = new Label("");
        errLabel.setStyle("-fx-text-fill:#dc2626; -fx-font-size:12px;");
        errLabel.setWrapText(true);

        VBox content = new VBox(12);
        content.setPadding(new Insets(12, 8, 4, 8));
        content.setPrefWidth(560);
        content.getChildren().addAll(header, new Separator(),
                seccion("Búsqueda de cliente"), gridBusqueda, lblClienteInfo, gridCliente,
                new Separator(), seccion("Datos de la reserva"), gridReserva, errLabel);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(520);
        scroll.setStyle("-fx-background-color:white; -fx-border-color:transparent;");

        ButtonType btnCheckin = new ButtonType("Registrar Check-in", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().setPrefWidth(620);
        dialog.getDialogPane().getButtonTypes().addAll(btnCheckin, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color:white; -fx-background-radius:12px;");

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(btnCheckin);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            errLabel.setText("");
            try {
                int idReserva  = Integer.parseInt(fIdReserva.getText().trim());
                int idEmpleado = Integer.parseInt(fIdEmpleado.getText().trim());
                if (clienteIdRef[0] < 0) {
                    String sn    = fSegundoNombre.getText().trim();
                    String a2    = fApellido2.getText().trim();
                    String ciudad = fCiudad.getText().trim();
                    ClienteDTO dto = new ClienteDTO(
                            fCedula.getText().trim(),
                            fNombre.getText().trim(),
                            sn.isEmpty() ? null : sn,
                            fApellido.getText().trim(),
                            a2.isEmpty() ? null : a2,
                            fEmail.getText().trim(),
                            fTelefono.getText().trim(),
                            fNacionalidad.getText().trim(),
                            ciudad.isEmpty() ? null : ciudad);
                    ctx.getClienteService().registrarCliente(dto);
                    NotificationUtil.info("Nuevo cliente registrado.");
                }
                ctx.getCheckInOutService().realizarCheckin(
                        idReserva, idEmpleado, fObs.getText().trim());
                NotificationUtil.exito(
                        "Check-in registrado correctamente (Reserva #" + idReserva + ").");
                cargarDatos();
            } catch (NumberFormatException e) {
                errLabel.setText("ID de reserva e ID de empleado deben ser números enteros.");
                event.consume();
            } catch (ExcepcionNegocio e) {
                errLabel.setText("Error: " + e.getMessage());
                event.consume();
            } catch (Exception e) {
                e.printStackTrace();
                errLabel.setText("Error: " + e.getMessage());
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> btn);
        dialog.showAndWait();
    }

    // ── Diálogo Check-out ─────────────────────────────────────────────────────

    private void mostrarDialogCheckout(CheckInOut sel) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Realizar Check-out");

        TextArea fObs = new TextArea();
        fObs.setPromptText("Observaciones del check-out (opcional)");
        fObs.setPrefRowCount(3);
        fObs.setStyle("-fx-background-color:#f8fafc; -fx-border-color:#e2e8f0;" +
                      "-fx-border-width:1.5px; -fx-border-radius:8px;");

        Reserva r = sel.getReserva();
        Label infoCliente = new Label(
                "Cliente: " + (r != null && r.getCliente() != null
                        ? r.getCliente().obtenerNombreCompleto() : "—")
                + "   |   Hab: " + (r != null && r.getHabitacion() != null
                        ? r.getHabitacion().getNumero() : "—")
                + "   |   Check-in: " + (sel.getFechaHoraCheckin() != null
                        ? sel.getFechaHoraCheckin().format(FMT) : "—"));
        infoCliente.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        Label errLabel = new Label("");
        errLabel.setStyle("-fx-text-fill:#dc2626; -fx-font-size:12px;");
        errLabel.setWrapText(true);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(24, 24);
        spinner.setVisible(false);

        Label header = new Label("🔓 Check-out — Registro #" + sel.getId());
        header.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;");
        VBox content = new VBox(12, header, infoCliente, new Separator(),
                                lab("Observaciones:"), fObs, spinner, errLabel);
        content.setPadding(new Insets(20, 24, 12, 24));
        content.setPrefWidth(420);

        ButtonType btnConfirmar = new ButtonType("Confirmar Check-out", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color:white; -fx-background-radius:12px;");

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(btnConfirmar);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            if (r == null) {
                errLabel.setText("No se puede hacer checkout: reserva no encontrada.");
                return;
            }
            okBtn.setDisable(true);
            spinner.setVisible(true);
            errLabel.setText("");
            String obs = fObs.getText().trim();
            int idReserva = r.getId();

            new Thread(() -> {
                try {
                    ctx.getCheckInOutService().realizarCheckout(idReserva, obs);
                    Platform.runLater(() -> {
                        NotificationUtil.exito("Check-out registrado (Reserva #" + idReserva + ").");
                        cargarDatos();
                        dialog.close();
                    });
                } catch (ExcepcionNegocio e) {
                    Platform.runLater(() -> {
                        errLabel.setText("Error: " + e.getMessage());
                        okBtn.setDisable(false);
                        spinner.setVisible(false);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        errLabel.setText("Error inesperado: " + e.getMessage());
                        okBtn.setDisable(false);
                        spinner.setVisible(false);
                    });
                }
            }).start();
        });

        dialog.setResultConverter(btn -> btn);
        dialog.showAndWait();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void actualizarStats(List<CheckInOut> lista) {
        LocalDate hoy = LocalDate.now();
        long activos = lista.stream().filter(ci -> !ci.haRealizadoCheckout()).count();
        // BUG 6 FIX: filtrar solo checkouts registrados hoy, no el total histórico
        long checkoutsHoy = lista.stream()
                .filter(ci -> ci.getFechaHoraCheckout() != null
                        && ci.getFechaHoraCheckout().toLocalDate().equals(hoy))
                .count();
        statsRow.getChildren().clear();
        statsRow.getChildren().addAll(
            chip("🔑 Check-ins activos: " + activos,   "#ede9fe", "#6d28d9"),
            chip("🔓 Check-outs hoy: " + checkoutsHoy, "#dcfce7", "#15803d")
        );
    }

    private void setClienteFieldsDisabled(boolean disabled,
            TextField cedula, TextField nombre, TextField segundoNombre,
            TextField apellido, TextField apellido2,
            TextField email, TextField telefono, TextField nacionalidad, TextField ciudad) {
        cedula.setDisable(disabled);
        nombre.setDisable(disabled);
        segundoNombre.setDisable(disabled);
        apellido.setDisable(disabled);
        apellido2.setDisable(disabled);
        email.setDisable(disabled);
        telefono.setDisable(disabled);
        nacionalidad.setDisable(disabled);
        ciudad.setDisable(disabled);
    }

    private String getBadgeEstilo(String estado) {
        switch (estado) {
            case "EN_PROCESO": return "-fx-background-color:#ede9fe; -fx-text-fill:#6d28d9;" +
                    "-fx-background-radius:20px; -fx-padding:3px 10px; -fx-font-size:10px; -fx-font-weight:bold;";
            case "COMPLETADA": return "-fx-background-color:#dbeafe; -fx-text-fill:#1d4ed8;" +
                    "-fx-background-radius:20px; -fx-padding:3px 10px; -fx-font-size:10px; -fx-font-weight:bold;";
            default:           return "-fx-background-color:#f1f5f9; -fx-text-fill:#475569;" +
                    "-fx-background-radius:20px; -fx-padding:3px 10px; -fx-font-size:10px;";
        }
    }

    private Label chip(String t, String bg, String fg) {
        Label l = new Label(t);
        l.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + ";" +
                "-fx-background-radius:20px; -fx-padding:4px 12px; -fx-font-size:12px;");
        return l;
    }

    private TextField tf(String placeholder) {
        TextField f = new TextField();
        f.setPromptText(placeholder);
        f.setStyle("-fx-background-color:#f8fafc; -fx-border-color:#e2e8f0;" +
                "-fx-border-width:1.5px; -fx-border-radius:8px;" +
                "-fx-background-radius:8px; -fx-padding:8px 12px;");
        f.setPrefWidth(160);
        return f;
    }

    private Label lab(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#374151;");
        return l;
    }

    private Label seccion(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;" +
                "-fx-padding:4px 0 2px 0;");
        return l;
    }
}

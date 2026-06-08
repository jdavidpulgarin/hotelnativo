package com.hotel.ui.controllers;

import com.hotel.AppContext;
import com.hotel.dao.impl.UbicacionDAO;
import com.hotel.dto.ClienteDTO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.model.Cliente;
import com.hotel.ui.components.NotificationUtil;
import com.hotel.ui.components.ValidacionCampo;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Controlador del módulo de Clientes.
 * Tabla con búsqueda en tiempo real, filas VIP resaltadas, email azul clickeable
 * y formulario CRUD con combos de ubicación en cascada (diseño moderno).
 */
public class ClientesController {

    // ── FXML — coinciden exactamente con fx:id en Clientes.fxml ──────────────
    @FXML private TableView<Cliente>          tabla;
    @FXML private TableColumn<Cliente,String> colDocumento, colNombre, colApellido;
    @FXML private TableColumn<Cliente,String> colEmail, colTelefono, colCiudad;
    @FXML private TableColumn<Cliente,String> colNacionalidad, colVip, colRegistro;
    @FXML private TextField                   searchField;
    @FXML private HBox                        statsRow;
    @FXML private ProgressBar                 progressBar;
    @FXML private Label                       lblFecha;

    private static final DateTimeFormatter FECHA_FORMATTER = 
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("es-CO"));
    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-CO");

    private final AppContext             ctx           = AppContext.getInstance();
    private final ObservableList<Cliente> datos        = FXCollections.observableArrayList();
    private FilteredList<Cliente>         datosFiltrados;

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        configurarFecha();
        configurarColumnas();
        datosFiltrados = new FilteredList<>(datos, p -> true);
        tabla.setItems(datosFiltrados);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cargarDatos();
    }

    private void configurarFecha() {
        if (lblFecha == null) return;
        LocalDate hoy = LocalDate.now();
        String fechaStr = hoy.getDayOfWeek().getDisplayName(TextStyle.FULL, LOCALE_ES)
                + " " + hoy.getDayOfMonth()
                + " de " + hoy.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES);
        lblFecha.setText("📅  " + fechaStr.substring(0,1).toUpperCase() + fechaStr.substring(1));
    }

    // ── Configuración de columnas ─────────────────────────────────────────────

    private void configurarColumnas() {
        // Cédula / Documento
        colDocumento.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDocumento() != null
                        ? c.getValue().getDocumento()
                        : String.valueOf(c.getValue().getId())));

        // Nombre — bold
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNombre() != null ? c.getValue().getNombre() : "—"));
        colNombre.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); }
                else { setText(v); setStyle("-fx-font-weight:600; -fx-text-fill:#111827;"); }
            }
        });

        // Apellido
        colApellido.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getApellido() != null ? c.getValue().getApellido() : "—"));

        // Email — texto azul #3b82f6
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEmail() != null ? c.getValue().getEmail() : "—"));
        colEmail.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null || "—".equals(v)) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle("-fx-text-fill:#3b82f6; -fx-cursor:hand;");
            }
        });

        // Teléfono
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTelefono() != null ? c.getValue().getTelefono() : "—"));

        // Ciudad origen
        colCiudad.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCiudadOrigen() != null ? c.getValue().getCiudadOrigen() : "—"));

        // Nacionalidad
        colNacionalidad.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNacionalidad() != null ? c.getValue().getNacionalidad() : "—"));

        // VIP — estrella ★ dorada o vacío
        colVip.setMinWidth(65);
        colVip.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isEsVip() ? "VIP" : ""));
        colVip.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null || v.isEmpty()) {
                    setGraphic(null); setText(null); setStyle(""); return;
                }
                Label star = new Label("★");
                star.setStyle("-fx-text-fill:#eab308; -fx-font-size:16px;");
                setGraphic(star);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        // Registro
        colRegistro.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaRegistro() != null
                        ? c.getValue().getFechaRegistro().format(FECHA_FORMATTER) : "—"));

        tabla.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Cliente item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("");
            }
        });
    }

    // ── Carga de datos ────────────────────────────────────────────────────────

    @FXML
    public void cargarDatos() {
        progressBar.setVisible(true);
        new Thread(() -> {
            try {
                List<Cliente> lista = ctx.getClienteService().listarTodosLosClientes();
                Platform.runLater(() -> {
                    datos.setAll(lista);
                    actualizarStats(lista);
                    progressBar.setVisible(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    NotificationUtil.error("Error cargando clientes: " + e.getMessage());
                    progressBar.setVisible(false);
                });
            }
        }, "carga-clientes").start();
    }

    private void actualizarStats(List<Cliente> lista) {
        long vipCount = lista.stream().filter(Cliente::isEsVip).count();
        statsRow.getChildren().clear();
        statsRow.getChildren().addAll(
            chip("Total: " + lista.size(),  "#fca5a5", "#991b1b"),
            chip("VIP: " + vipCount,        "#fcd34d", "#92400e",
                 "-fx-background-color:#fffbeb;")
        );
    }

    private Label chip(String texto, String borderColor, String textColor) {
        return chip(texto, borderColor, textColor, "white");
    }

    private Label chip(String texto, String borderColor, String textColor, String bgColor) {
        Label l = new Label(texto);
        l.setStyle(
            "-fx-background-color:" + bgColor + ";" +
            "-fx-border-color:" + borderColor + ";" +
            "-fx-border-width:1.5;" +
            "-fx-border-radius:999;" +
            "-fx-background-radius:999;" +
            "-fx-padding:5 14;" +
            "-fx-font-size:12px;" +
            "-fx-font-weight:600;" +
            "-fx-text-fill:" + textColor + ";");
        return l;
    }

    // ── Búsqueda ──────────────────────────────────────────────────────────────

    @FXML
    public void filtrar() {
        String q = searchField.getText().toLowerCase().trim();
        datosFiltrados.setPredicate(c -> {
            if (q.isEmpty()) return true;
            return (c.getDocumento()    != null && c.getDocumento().contains(q))
                || (c.getNombre()       != null && c.getNombre().toLowerCase().contains(q))
                || (c.getApellido()     != null && c.getApellido().toLowerCase().contains(q))
                || (c.getEmail()        != null && c.getEmail().toLowerCase().contains(q))
                || (c.getCiudadOrigen() != null && c.getCiudadOrigen().toLowerCase().contains(q))
                || (c.getTelefono()     != null && c.getTelefono().contains(q));
        });
    }

    // ── Acciones FXML (coinciden con onAction en el FXML) ────────────────────

    @FXML public void nuevoCliente()   { mostrarFormularioModerno(null); }
    @FXML public void editarCliente()  { abrirEdicion(); }
    @FXML public void eliminarCliente(){ confirmarEliminacion(); }
    @FXML public void toggleVip()      { marcarVip(); }

    @FXML
    public void handleDobleClick(MouseEvent e) {
        if (e.getClickCount() == 2) abrirEdicion();
    }

    // ── Lógica de negocio ─────────────────────────────────────────────────────

    private void abrirEdicion() {
        Cliente sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { NotificationUtil.advertencia("Selecciona un cliente para editar."); return; }
        mostrarFormularioModerno(sel);
    }

    private void confirmarEliminacion() {
        Cliente sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { NotificationUtil.advertencia("Selecciona un cliente."); return; }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmar eliminación");
        a.setHeaderText("¿Eliminar a " + sel.obtenerNombreCompleto() + "?");
        a.setContentText("Se eliminarán también sus reservas completadas/canceladas,\n" +
                "facturas e historial. Esta acción no se puede deshacer.");
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    ctx.getClienteService().eliminarCliente(sel.getId());
                    NotificationUtil.exito("Cliente eliminado correctamente.");
                    cargarDatos();
                } catch (ExcepcionNegocio ex) {
                    NotificationUtil.error(ex.getMessage());
                } catch (Exception ex) {
                    NotificationUtil.error("No se pudo eliminar: " + ex.getMessage());
                }
            }
        });
    }

    private void marcarVip() {
        Cliente sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { NotificationUtil.advertencia("Selecciona un cliente."); return; }
        try {
            ctx.getClienteService().marcarClienteComoVip(sel.getId());
            NotificationUtil.exito(sel.obtenerNombreCompleto()
                    + (sel.isEsVip() ? " desmarcado como VIP." : " marcado como VIP ★"));
            cargarDatos();
        } catch (ExcepcionNegocio ex) {
            NotificationUtil.error(ex.getMessage());
        }
    }

    // ─── NUEVO FORMULARIO MODERNO (ESTILO DASHBOARD) ───────────────────────────

    private void mostrarFormularioModerno(Cliente clienteEditar) {
        UbicacionDAO ubicacionDAO = new UbicacionDAO();
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Sombra paralela sutil
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.25));
        shadow.setRadius(24);
        shadow.setOffsetY(8);
        dialog.getDialogPane().setEffect(shadow);
        
        VBox mainContainer = new VBox(0);
        mainContainer.setStyle("-fx-background-color: white; -fx-background-radius: 16px;");
        
        // ─── CABECERA (Fondo gris oscuro #1a1a2e) ─────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 16px 16px 0 0;");
        
        // Icono cuadrado con fondo azul real
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(48, 48);
        iconBox.setMaxSize(48, 48);
        iconBox.setStyle("-fx-background-color: #1e3a5f; -fx-background-radius: 12px;");
        Label iconLabel = new Label(clienteEditar == null ? "➕" : "✏️");
        iconLabel.setStyle("-fx-font-size: 22px;");
        iconBox.getChildren().add(iconLabel);
        
        // Textos del header
        VBox headerTexts = new VBox(4);
        Label titleLabel = new Label(clienteEditar == null ? "Registrar nuevo cliente" : "Editar cliente");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // Badge con información
        HBox totalBadge = new HBox(6);
        totalBadge.setAlignment(Pos.CENTER);
        totalBadge.setPadding(new Insets(4, 10, 4, 10));
        totalBadge.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 20px;");
        Label dotIndicator = new Label("●");
        dotIndicator.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 8px;");
        Label totalLabel = new Label(clienteEditar == null ? "COMPLETE LOS DATOS" : "MODIFIQUE LOS DATOS NECESARIOS");
        totalLabel.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 11px; -fx-font-weight: 600;");
        totalBadge.getChildren().addAll(dotIndicator, totalLabel);
        headerTexts.getChildren().addAll(titleLabel, totalBadge);
        
        // Botón de cierre (X)
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #a0a0c0; " +
                         "-fx-font-size: 14px; -fx-background-radius: 20px; -fx-cursor: hand; " +
                         "-fx-padding: 6px 10px;");
        closeBtn.setOnAction(e -> dialog.close());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(iconBox, headerTexts, spacer, closeBtn);
        
        // ─── CUERPO DEL FORMULARIO ───────────────────────────────────────────
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        
        VBox body = new VBox(20);
        body.setPadding(new Insets(24));
        
        // ─── SECCIÓN: INFORMACIÓN PERSONAL ────────────────────────────────────
        VBox personalSection = new VBox(12);
        Label personalTitle = new Label("👤 INFORMACIÓN PERSONAL");
        personalTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        
        GridPane gridPersonal = new GridPane();
        gridPersonal.setHgap(16);
        gridPersonal.setVgap(14);
        gridPersonal.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-padding: 16px;");
        
        // Campos personales
        TextField fCedula = crearCampoModerno("Cédula *");
        TextField fNombre = crearCampoModerno("Primer nombre *");
        TextField fSegundoNombre = crearCampoModerno("Segundo nombre");
        TextField fApellido = crearCampoModerno("Primer apellido *");
        TextField fApellido2 = crearCampoModerno("Segundo apellido");
        
        // Labels de error
        Label errCedula = new Label(), errNombre = new Label(), errApe = new Label();
        ValidacionCampo.aplicarSoloNumeros(fCedula, errCedula);
        ValidacionCampo.aplicarMaxLength(fCedula, 15);
        ValidacionCampo.aplicarSoloLetras(fNombre, errNombre);
        ValidacionCampo.aplicarSoloLetras(fApellido, errApe);
        
        gridPersonal.addRow(0, crearLabelCampo("CÉDULA *:"), new VBox(2, fCedula, errCedula));
        gridPersonal.addRow(1, crearLabelCampo("PRIMER NOMBRE *:"), new VBox(2, fNombre, errNombre), 
                              crearLabelCampo("SEGUNDO NOMBRE:"), fSegundoNombre);
        gridPersonal.addRow(2, crearLabelCampo("PRIMER APELLIDO *:"), new VBox(2, fApellido, errApe), 
                              crearLabelCampo("SEGUNDO APELLIDO:"), fApellido2);
        
        personalSection.getChildren().addAll(personalTitle, gridPersonal);
        
        // ─── SECCIÓN: INFORMACIÓN DE CONTACTO ─────────────────────────────────
        VBox contactoSection = new VBox(12);
        Label contactoTitulo = new Label("📞 INFORMACIÓN DE CONTACTO");
        contactoTitulo.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        
        GridPane gridContacto = new GridPane();
        gridContacto.setHgap(16);
        gridContacto.setVgap(14);
        gridContacto.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-padding: 16px;");
        
        TextField fEmail = crearCampoModerno("Email *");
        TextField fTelefono = crearCampoModerno("Teléfono *");
        
        Label errEmail = new Label(), errTel = new Label();
        ValidacionCampo.aplicarEmail(fEmail, errEmail);
        ValidacionCampo.aplicarTelefono(fTelefono, errTel);
        
        gridContacto.addRow(0, crearLabelCampo("EMAIL *:"), new VBox(2, fEmail, errEmail),
                              crearLabelCampo("TELÉFONO *:"), new VBox(2, fTelefono, errTel));
        
        contactoSection.getChildren().addAll(contactoTitulo, gridContacto);
        
        // ─── SECCIÓN: UBICACIÓN ───────────────────────────────────────────────
        VBox ubicacionSection = new VBox(12);
        Label ubicacionTitulo = new Label("📍 UBICACIÓN");
        ubicacionTitulo.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        
        GridPane gridUbicacion = new GridPane();
        gridUbicacion.setHgap(16);
        gridUbicacion.setVgap(14);
        gridUbicacion.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-padding: 16px;");
        
        // Combos en cascada
        ComboBox<String[]> cmbPais = crearComboUbicacionModerno("Seleccione país *");
        ComboBox<String[]> cmbDepto = crearComboUbicacionModerno("Seleccione departamento *");
        ComboBox<String[]> cmbCiudad = crearComboUbicacionModerno("Seleccione ciudad *");
        cmbDepto.setDisable(true);
        cmbCiudad.setDisable(true);
        
        try { 
            cmbPais.setItems(FXCollections.observableArrayList(ubicacionDAO.listarPaises())); 
        } catch (Exception ex) { 
            NotificationUtil.error("No se cargaron países: " + ex.getMessage()); 
        }
        
        cmbPais.setOnAction(e -> {
            cmbDepto.getItems().clear(); 
            cmbCiudad.getItems().clear();
            cmbDepto.setValue(null); 
            cmbCiudad.setValue(null);
            String[] p = cmbPais.getValue();
            if (p != null) {
                try { 
                    cmbDepto.setItems(FXCollections.observableArrayList(ubicacionDAO.listarDepartamentos(p[0]))); 
                    cmbDepto.setDisable(false); 
                    cmbCiudad.setDisable(true); 
                } catch (Exception ex) { 
                    NotificationUtil.error("Error cargando departamentos: " + ex.getMessage()); 
                }
            } else { 
                cmbDepto.setDisable(true); 
                cmbCiudad.setDisable(true); 
            }
        });
        
        cmbDepto.setOnAction(e -> {
            cmbCiudad.getItems().clear(); 
            cmbCiudad.setValue(null);
            String[] d = cmbDepto.getValue();
            if (d != null) {
                try { 
                    cmbCiudad.setItems(FXCollections.observableArrayList(ubicacionDAO.listarCiudades(d[0]))); 
                    cmbCiudad.setDisable(false); 
                } catch (Exception ex) { 
                    NotificationUtil.error("Error cargando ciudades: " + ex.getMessage()); 
                }
            } else { 
                cmbCiudad.setDisable(true); 
            }
        });
        
        gridUbicacion.addRow(0, crearLabelCampo("PAÍS / NACIONALIDAD *:"), cmbPais,
                              crearLabelCampo("DEPARTAMENTO *:"), cmbDepto);
        gridUbicacion.addRow(1, crearLabelCampo("CIUDAD *:"), cmbCiudad);
        
        ubicacionSection.getChildren().addAll(ubicacionTitulo, gridUbicacion);
        
        // ─── PRE-POBLAR EN EDICIÓN ────────────────────────────────────────────
        if (clienteEditar != null) {
            fCedula.setText(nvl(clienteEditar.getDocumento() != null
                    ? clienteEditar.getDocumento() : String.valueOf(clienteEditar.getId())));
            fCedula.setEditable(false);
            fNombre.setText(nvl(clienteEditar.getNombre()));
            fSegundoNombre.setText(nvl(clienteEditar.getSegundoNombre()));
            fApellido.setText(nvl(clienteEditar.getApellido()));
            fApellido2.setText(nvl(clienteEditar.getApellido2()));
            fEmail.setText(nvl(clienteEditar.getEmail()));
            fTelefono.setText(nvl(clienteEditar.getTelefono()));

            String idPais = clienteEditar.getIdPais();
            String idCiudad = clienteEditar.getIdCiudad();
            if (idPais != null && !idPais.isBlank()) {
                seleccionarEnCombo(cmbPais, idPais);
                try {
                    String idDepto = idCiudad != null
                            ? ubicacionDAO.buscarIdDepartamentoDeCiudad(idCiudad) : null;
                    cmbDepto.setItems(FXCollections.observableArrayList(
                            ubicacionDAO.listarDepartamentos(idPais)));
                    cmbDepto.setDisable(false);
                    if (idDepto != null) {
                        seleccionarEnCombo(cmbDepto, idDepto);
                        cmbCiudad.setItems(FXCollections.observableArrayList(
                                ubicacionDAO.listarCiudades(idDepto)));
                        cmbCiudad.setDisable(false);
                        if (idCiudad != null) seleccionarEnCombo(cmbCiudad, idCiudad);
                    }
                } catch (Exception ex) {
                    NotificationUtil.advertencia("No se pudo precargar la ubicación: " + ex.getMessage());
                }
            }
        }
        
        body.getChildren().addAll(personalSection, contactoSection, ubicacionSection);
        
        // ─── LABEL DE ERROR GLOBAL ───────────────────────────────────────────
        Label errGlobal = new Label();
        errGlobal.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        errGlobal.setWrapText(true);
        errGlobal.setVisible(false);
        body.getChildren().add(errGlobal);
        
        scrollPane.setContent(body);
        
        // ─── FOOTER CON BOTONES ───────────────────────────────────────────────
        VBox footer = new VBox();
        footer.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16px 16px;");
        
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #e2e8f0;");
        
        HBox footerContent = new HBox(16);
        footerContent.setAlignment(Pos.CENTER_RIGHT);
        footerContent.setPadding(new Insets(16, 24, 20, 24));
        
        Button cancelarBtn = new Button("Cancelar");
        cancelarBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; " +
                            "-fx-background-radius: 8px; -fx-padding: 10px 24px; " +
                            "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        cancelarBtn.setOnAction(e -> dialog.close());
        
        Button guardarBtn = new Button(clienteEditar == null ? "Registrar Cliente" : "Actualizar Cliente");
        guardarBtn.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: white; " +
                            "-fx-background-radius: 8px; -fx-padding: 10px 24px; " +
                            "-fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
        
        footerContent.getChildren().addAll(cancelarBtn, guardarBtn);
        footer.getChildren().addAll(divider, footerContent);
        
        mainContainer.getChildren().addAll(header, scrollPane, footer);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().setPrefSize(750, 700);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        
        // ─── ACCIÓN DEL BOTÓN GUARDAR ─────────────────────────────────────────
        guardarBtn.setOnAction(e -> {
            errGlobal.setVisible(false);
            errGlobal.setText("");
            
            // Validaciones
            if (ValidacionCampo.tieneError(errCedula) || ValidacionCampo.tieneError(errNombre)
                    || ValidacionCampo.tieneError(errApe)
                    || ValidacionCampo.tieneError(errEmail) || ValidacionCampo.tieneError(errTel)) {
                errGlobal.setText("Corrige los campos marcados.");
                errGlobal.setVisible(true);
                return;
            }
            if (fCedula.getText().trim().isEmpty())   { errGlobal.setText("La cédula es obligatoria."); errGlobal.setVisible(true); return; }
            if (fNombre.getText().trim().isEmpty())    { errGlobal.setText("El nombre es obligatorio."); errGlobal.setVisible(true); return; }
            if (fApellido.getText().trim().isEmpty())  { errGlobal.setText("El apellido es obligatorio."); errGlobal.setVisible(true); return; }
            if (fEmail.getText().trim().isEmpty())     { errGlobal.setText("El email es obligatorio."); errGlobal.setVisible(true); return; }
            if (fTelefono.getText().trim().isEmpty())  { errGlobal.setText("El teléfono es obligatorio."); errGlobal.setVisible(true); return; }
            if (cmbPais.getValue() == null)            { errGlobal.setText("Selecciona el país."); errGlobal.setVisible(true); return; }
            if (cmbCiudad.getValue() == null)          { errGlobal.setText("Selecciona la ciudad."); errGlobal.setVisible(true); return; }
            
            guardarBtn.setDisable(true);
            
            new Thread(() -> {
                try {
                    String sn = fSegundoNombre.getText().trim(), a2 = fApellido2.getText().trim();
                    ClienteDTO dto = new ClienteDTO(
                            fCedula.getText().trim(),
                            fNombre.getText().trim(),
                            sn.isEmpty() ? null : sn,
                            fApellido.getText().trim(),
                            a2.isEmpty() ? null : a2,
                            fEmail.getText().trim(),
                            fTelefono.getText().trim(),
                            cmbPais.getValue()[0],
                            cmbCiudad.getValue()[0]);

                    if (clienteEditar == null) {
                        ctx.getClienteService().registrarCliente(dto);
                        Platform.runLater(() -> NotificationUtil.exito("Cliente registrado correctamente."));
                    } else {
                        ctx.getClienteService().actualizarCliente(clienteEditar.getId(), dto);
                        Platform.runLater(() -> NotificationUtil.exito("Cliente actualizado correctamente."));
                    }
                    Platform.runLater(() -> {
                        cargarDatos();
                        dialog.close();
                    });
                } catch (ExcepcionNegocio ex) {
                    Platform.runLater(() -> {
                        errGlobal.setText("Error: " + ex.getMessage());
                        errGlobal.setVisible(true);
                        guardarBtn.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        errGlobal.setText("Error inesperado: " + ex.getMessage());
                        errGlobal.setVisible(true);
                        guardarBtn.setDisable(false);
                    });
                }
            }).start();
        });
        
        // Ocultar botón de cierre por defecto
        Button defaultCloseButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        defaultCloseButton.setVisible(false);
        defaultCloseButton.setManaged(false);
        
        // Animación de entrada
        dialog.setOnShown(ev -> {
            FadeTransition ft = new FadeTransition(Duration.millis(250), dialog.getDialogPane());
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        });
        
        dialog.showAndWait();
    }

    // ─── HELPERS DE UI MODERNOS ───────────────────────────────────────────────

    private TextField crearCampoModerno(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                   "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px; " +
                   "-fx-padding: 8px 12px; -fx-font-size: 13px;");
        f.setPrefWidth(200);
        return f;
    }
    
    private ComboBox<String[]> crearComboUbicacionModerno(String prompt) {
        ComboBox<String[]> c = new ComboBox<>();
        c.setPromptText(prompt);
        c.setPrefWidth(220);
        c.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                   "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px; " +
                   "-fx-padding: 6px 8px; -fx-font-size: 13px;");
        c.setConverter(new StringConverter<String[]>() {
            @Override public String toString(String[] item) { return item != null ? item[1] : ""; }
            @Override public String[] fromString(String s) { return null; }
        });
        return c;
    }
    
    private Label crearLabelCampo(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        return l;
    }
    
    private void seleccionarEnCombo(ComboBox<String[]> combo, String id) {
        if (id == null) return;
        combo.getItems().stream().filter(i -> id.equals(i[0])).findFirst().ifPresent(combo::setValue);
    }
    
    private String nvl(String v) { return v != null ? v : ""; }
}
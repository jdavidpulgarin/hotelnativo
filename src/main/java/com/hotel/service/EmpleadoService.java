/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hotel.service;

import com.hotel.dao.interfaces.IEmpleadoDAO;
import com.hotel.exception.ExcepcionNegocio;
import com.hotel.exception.ExcepcionValidacion;
import com.hotel.model.Cargo;
import com.hotel.model.Empleado;
import com.hotel.util.ValidadorEntradas;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Pulgarin
 */
/**
 * Lógica de negocio para gestión de empleados del hotel.
 *
 * NUEVO: Esta clase faltaba completamente en el proyecto. Sin ella,
 * EmpleadoDAOImpl no tenía capa de servicio que la usara, y la vista de
 * empleados (EmpleadoView) no tenía a qué conectarse.
 *
 * Responsabilidades: - CRUD completo de empleados con validaciones. -
 * Integración con AuthService para registrar credenciales al crear. - Búsqueda
 * por cargo.
 *
 * GRASP: Alta Cohesión – solo maneja operaciones de empleados. GRASP:
 * Controlador – coordina los casos de uso de empleados. SOLID: S –
 * responsabilidad única: lógica de negocio de empleados. SOLID: D – depende de
 * IEmpleadoDAO, no de la implementación concreta.
 */
public class EmpleadoService {

    private final IEmpleadoDAO empleadoDAO;
    private final AuthService authService;

    /**
     * @param empleadoDAO repositorio de empleados
     * @param authService servicio de autenticación (para registrar
     * credenciales)
     */
    public EmpleadoService(IEmpleadoDAO empleadoDAO, AuthService authService) {
        this.empleadoDAO = empleadoDAO;
        this.authService = authService;
    }

    /**
     * Registra un nuevo empleado y su contraseña inicial en el sistema de
     * autenticación.
     *
     * @param nombre nombre del empleado
     * @param apellido apellido
     * @param email correo (usado como login)
     * @param telefono teléfono
     * @param idCargo ID del cargo asignado
     * @param passwordInicial contraseña inicial (se almacena como hash SHA-256)
     * @return empleado creado con ID generado por la BD
     * @throws ExcepcionNegocio si el email ya está registrado
     * @throws ExcepcionValidacion si algún campo es inválido
     */
    public Empleado registrarEmpleado(String nombre, String segundoNombre,
            String apellido, String apellido2,
            String email, String telefono,
            int idCargo, String passwordInicial,
            double salario, String tipoContrato,
            String tipoPago, LocalDate fechaFinContrato)
            throws ExcepcionNegocio {
        ValidadorEntradas.validarLargoNombre(nombre, "nombre");
        ValidadorEntradas.validarLargoNombre(apellido, "apellido");
        ValidadorEntradas.validarFormatoEmail(email);
        ValidadorEntradas.validarFormatoTelefono(telefono);
        ValidadorEntradas.validarIdPositivo(idCargo, "cargo");
        ValidadorEntradas.validarCampoRequerido(passwordInicial, "password");

        if (authService.obtenerEmpleadoPorEmail(email).isPresent()) {
            throw new ExcepcionNegocio("EMAIL_DUPLICADO",
                    "Ya existe un empleado registrado con el email: " + email);
        }

        Cargo cargoRef = new Cargo();
        cargoRef.setId(idCargo);

        Empleado nuevoEmpleado = new Empleado(0, nombre, apellido, email,
                telefono, cargoRef, LocalDate.now());
        nuevoEmpleado.setSegundoNombre(segundoNombre);
        nuevoEmpleado.setApellido2(apellido2);
        nuevoEmpleado.setSalario(salario);
        nuevoEmpleado.setTipoContrato(tipoContrato);
        nuevoEmpleado.setTipoPago(tipoPago);
        nuevoEmpleado.setFechaFinContrato(fechaFinContrato);

        Empleado guardado = empleadoDAO.insertar(nuevoEmpleado);

        // Hashear y persistir contraseña en BD + registrar en memoria
        String hash = authService.generarHash(passwordInicial);
        authService.registrarCredencialesConHash(guardado, hash);
        empleadoDAO.actualizarPasswordHash(guardado.getId(), hash);
        guardado.setDebeCambiarContrasena(true);
        System.out.println("[EMPLEADO] Credenciales registradas para ID: " + guardado.getId());

        return guardado;
    }

    /**
     * Actualiza los datos de un empleado existente. No modifica contraseña
     * (usar resetearPassword para eso).
     *
     * @throws ExcepcionNegocio si el empleado no existe
     */
    public Empleado actualizarEmpleado(int idEmpleado, String nombre, String segundoNombre,
            String apellido, String apellido2,
            String email, String telefono, int idCargo,
            LocalDate fechaContratacion,
            double salario, String tipoContrato,
            String tipoPago, LocalDate fechaFinContrato)
            throws ExcepcionNegocio {
        ValidadorEntradas.validarIdPositivo(idEmpleado, "empleado");
        ValidadorEntradas.validarLargoNombre(nombre, "nombre");
        ValidadorEntradas.validarLargoNombre(apellido, "apellido");
        ValidadorEntradas.validarFormatoEmail(email);
        ValidadorEntradas.validarFormatoTelefono(telefono);
        ValidadorEntradas.validarIdPositivo(idCargo, "cargo");

        Empleado empleado = obtenerEmpleadoOLanzarError(idEmpleado);

        // Solo verificar duplicado si el email cambió
        if (!email.equalsIgnoreCase(empleado.getEmail())
                && authService.obtenerEmpleadoPorEmail(email).isPresent()) {
            throw new ExcepcionNegocio("EMAIL_DUPLICADO",
                    "Ya existe un empleado registrado con el email: " + email);
        }

        Cargo cargoRef = new Cargo();
        cargoRef.setId(idCargo);

        empleado.setNombre(nombre);
        empleado.setSegundoNombre(segundoNombre);
        empleado.setApellido(apellido);
        empleado.setApellido2(apellido2);
        empleado.setEmail(email);
        empleado.setTelefono(telefono);
        empleado.setCargo(cargoRef);
        if (fechaContratacion != null) {
            empleado.setFechaContratacion(fechaContratacion);
        }
        empleado.setSalario(salario);
        empleado.setTipoContrato(tipoContrato);
        empleado.setTipoPago(tipoPago);
        empleado.setFechaFinContrato(fechaFinContrato);

        empleadoDAO.actualizar(empleado);
        return empleado;
    }

    /**
     * Elimina un empleado del sistema.
     *
     * @throws ExcepcionNegocio si el empleado no existe
     */
    public void eliminarEmpleado(int idEmpleado) throws ExcepcionNegocio {
        ValidadorEntradas.validarIdPositivo(idEmpleado, "empleado");
        obtenerEmpleadoOLanzarError(idEmpleado);
        empleadoDAO.eliminar(idEmpleado);
    }

    /**
     * Resetea la contraseña de un empleado. Solo puede ejecutarlo un
     * administrador (verificar permiso en la vista).
     *
     * @throws ExcepcionNegocio si el empleado no existe
     */
    public void resetearPassword(int idEmpleado, String nuevaPassword) throws ExcepcionNegocio {
        ValidadorEntradas.validarIdPositivo(idEmpleado, "empleado");
        ValidadorEntradas.validarCampoRequerido(nuevaPassword, "nuevaPassword");

        Empleado empleado = obtenerEmpleadoOLanzarError(idEmpleado);

        // Generar hash una sola vez y usarlo tanto en memoria como en BD
        String hash = authService.generarHash(nuevaPassword);
        authService.registrarCredencialesConHash(empleado, hash);
        empleadoDAO.actualizarPasswordHash(idEmpleado, hash);

        System.out.println("[EMPLEADO] Contraseña reseteada y persistida para: " + empleado.getEmail());
    }

    /**
     * Persiste en BD un hash ya calculado (usado por LoginController tras
     * cambio de primer login).
     */
    public void persistirHashEnBD(int idEmpleado, String hash) throws ExcepcionNegocio {
        ValidadorEntradas.validarIdPositivo(idEmpleado, "empleado");
        empleadoDAO.actualizarPasswordHash(idEmpleado, hash);
    }

    /**
     * Persiste el flag debe_cambiar_password en BD. Llamar con {@code false}
     * tras un cambio de contraseña exitoso para no volver a pedirlo.
     */
    public void actualizarDebeCambiarPassword(int idEmpleado, boolean debe) throws ExcepcionNegocio {
        ValidadorEntradas.validarIdPositivo(idEmpleado, "empleado");
        empleadoDAO.actualizarDebeCambiarPassword(idEmpleado, debe);
    }

    public Optional<Empleado> buscarPorId(int idEmpleado) throws ExcepcionValidacion {
        ValidadorEntradas.validarIdPositivo(idEmpleado, "empleado");
        return empleadoDAO.buscarPorId(idEmpleado);
    }

    public List<Empleado> listarTodosLosEmpleados() {
        return empleadoDAO.listarTodos();
    }

    public List<Cargo> listarCargos() {
        return empleadoDAO.listarCargos();
    }

    public List<Empleado> buscarPorCargo(int idCargo) throws ExcepcionValidacion {
        ValidadorEntradas.validarIdPositivo(idCargo, "cargo");
        return empleadoDAO.buscarPorCargo(idCargo);
    }

    // ── Métodos privados ──────────────────────────────────────────────────────
    private Empleado obtenerEmpleadoOLanzarError(int idEmpleado) throws ExcepcionNegocio {
        return empleadoDAO.buscarPorId(idEmpleado)
                .orElseThrow(() -> new ExcepcionNegocio("EMPLEADO_NOT_FOUND",
                "No se encontró el empleado con ID: " + idEmpleado));
    }
}

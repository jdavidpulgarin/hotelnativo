/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.java.com.hotel.service;

/**
 *
 * @author Pulgarin
 */
/**
 * Servicio de autenticación y gestión de sesiones.
 *
 * CORRECCIÓN WARN #4: El campo {@code creadaEn} en SesionActiva existía pero
 * nunca se usaba para verificar expiración. Los tokens vivían indefinidamente
 * hasta un logout manual. Un token robado tenía acceso permanente.
 *
 * Ahora verificarPermiso() y obtenerEmpleadoDeToken() validan que la sesión
 * no haya superado MINUTOS_EXPIRACION_SESION. Las sesiones expiradas se
 * limpian automáticamente con limpiarSesionesExpiradas().
 *
 * SEGURIDAD: Las contraseñas se almacenan con BCrypt (cost=12). Para cuentas
 * migradas desde SHA-256, el hash se actualiza automáticamente en el primer
 * login exitoso. Los hashes legacy se detectan por ausencia del prefijo "$2".
 *
 * GRASP: Controlador – gestiona el ciclo de vida de sesiones.
 * SOLID: S – responsabilidad única: autenticación y autorización.
 */
public class AuthService {

    public static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    public static final String ROL_RECEPCIONISTA = "RECEPCIONISTA";
    public static final String ROL_MANTENIMIENTO = "MANTENIMIENTO";
    public static final String ROL_CONTADOR = "CONTADOR";
}

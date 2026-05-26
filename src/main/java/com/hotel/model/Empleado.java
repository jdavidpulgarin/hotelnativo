
package com.hotel.model;

/**
 *
 * @author rober
 */
import java.time.LocalDate;

/**
 * Entidad Empleado del hotel.
 * 
 *
 * El campo {@code debeCambiarContrasena} fuerza un cambio de credenciales
 * en el primer inicio de sesión o cuando un administrador lo resetea.
 */
public class Empleado extends Persona {

    private String    segundoNombre;
    private String    apellido2;
    private Cargo     cargo;
    private LocalDate fechaContratacion;

    
    private double    salario;

    
    private String    tipoContrato  = "INDEFINIDO";

   
    private String    tipoPago      = "MENSUAL";

  
    private LocalDate fechaFinContrato;

   
    private String  hashContrasena;

    
    private boolean debeCambiarContrasena = true;

    public Empleado() {}

    public Empleado(int id, String nombre, String apellido, String email,
                    String telefono, Cargo cargo, LocalDate fechaContratacion) {
        super(id, nombre, apellido, email, telefono);
        this.cargo              = cargo;
        this.fechaContratacion  = fechaContratacion;
    }
}
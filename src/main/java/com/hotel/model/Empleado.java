
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
     public String getSegundoNombre()                { return segundoNombre; }
    public void   setSegundoNombre(String v)        { this.segundoNombre = v; }

    public String getApellido2()                    { return apellido2; }
    public void   setApellido2(String v)            { this.apellido2 = v; }

    public Cargo getCargo() { return cargo; }
    public void  setCargo(Cargo cargo) { this.cargo = cargo; }

    public LocalDate getFechaContratacion() { return fechaContratacion; }
    public void      setFechaContratacion(LocalDate fechaContratacion) {
        this.fechaContratacion = fechaContratacion;
    }

    public double getSalario()              { return salario; }
    public void   setSalario(double v)      { this.salario = v; }

    public String getTipoContrato()         { return tipoContrato; }
    public void   setTipoContrato(String v) { this.tipoContrato = v != null ? v : "INDEFINIDO"; }

    public String getTipoPago()             { return tipoPago; }
    public void   setTipoPago(String v)     { this.tipoPago = v != null ? v : "MENSUAL"; }

    public LocalDate getFechaFinContrato()              { return fechaFinContrato; }
    public void      setFechaFinContrato(LocalDate v)   { this.fechaFinContrato = v; }

    public String getHashContrasena()                    { return hashContrasena; }
    public void   setHashContrasena(String hash)         { this.hashContrasena = hash; }

    public boolean isDebeCambiarContrasena()             { return debeCambiarContrasena; }
    public void    setDebeCambiarContrasena(boolean v)   { this.debeCambiarContrasena = v; }
    @Override
    public String toString() {
        return String.format("Empleado[id=%d, nombre=%s, cargo=%s]",
                getId(), obtenerNombreCompleto(),
                cargo != null ? cargo.getNombreCargo() : "Sin cargo");
    }
}
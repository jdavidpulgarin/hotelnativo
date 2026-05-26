
package com.hotel.model;

/**
 *
 * @author rober
 */
import java.time.LocalDate;


public class Cliente extends Persona {

    private String segundoNombre;
    private String apellido2;
    private String documento;
    private String nacionalidad;
    private LocalDate fechaRegistro;
    private boolean esVip;
    private String subtipoDocumento;
    private String ciudadOrigen;

    public Cliente() {}

    public Cliente(int id, String nombre, String apellido, String email,
                   String telefono, String documento, String nacionalidad,
                   LocalDate fechaRegistro) {
        super(id, nombre, apellido, email, telefono);
        this.documento        = documento;
        this.nacionalidad     = nacionalidad;
        this.fechaRegistro    = fechaRegistro;
        this.esVip            = false;
        this.subtipoDocumento = "COLOMBIANA";
        this.ciudadOrigen     = null;
    }

    public Cliente(int id, String nombre, String apellido, String email,
                   String telefono, String documento, String nacionalidad,
                   LocalDate fechaRegistro, String subtipoDocumento, String ciudadOrigen) {
        super(id, nombre, apellido, email, telefono);
        this.documento        = documento;
        this.nacionalidad     = nacionalidad;
        this.fechaRegistro    = fechaRegistro;
        this.esVip            = false;
        this.subtipoDocumento = (subtipoDocumento != null) ? subtipoDocumento : "COLOMBIANA";
        this.ciudadOrigen     = ciudadOrigen;
    }
}
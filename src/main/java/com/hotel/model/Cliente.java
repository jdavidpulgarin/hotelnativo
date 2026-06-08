
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
    private String nacionalidad;   // nombre del país para mostrar en pantalla (ej. "Colombia")
    private String idPais;         // clave FK: "PAI01" — usado para INSERT/UPDATE en BD
    private LocalDate fechaRegistro;
    private boolean esVip;
    private String subtipoDocumento;
    private String ciudadOrigen;   // nombre de la ciudad para mostrar en pantalla (ej. "Bogotá")
    private String idCiudad;       // clave FK: "CIU79" — usado para INSERT/UPDATE en BD

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

    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(String segundoNombre) { this.segundoNombre = segundoNombre; }

    public String getApellido2() { return apellido2; }
    public void setApellido2(String apellido2) { this.apellido2 = apellido2; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getNacionalidad() { return nacionalidad; }
    public void setNacionalidad(String nacionalidad) { this.nacionalidad = nacionalidad; }

    /** Clave FK de la tabla PAIS (ej. "PAI01"). Usar para INSERT/UPDATE en BD. */
    public String getIdPais() { return idPais; }
    public void setIdPais(String idPais) { this.idPais = idPais; }

    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public boolean isEsVip() { return esVip; }
    public void setEsVip(boolean esVip) { this.esVip = esVip; }

    public String getSubtipoDocumento() { return subtipoDocumento; }
    public void setSubtipoDocumento(String subtipoDocumento) {
        this.subtipoDocumento = (subtipoDocumento != null) ? subtipoDocumento : "COLOMBIANA";
    }

    public String getCiudadOrigen() { return ciudadOrigen; }
    public void setCiudadOrigen(String ciudadOrigen) { this.ciudadOrigen = ciudadOrigen; }

    /** Clave FK de la tabla CIUDAD (ej. "CIU79"). Usar para INSERT/UPDATE en BD. */
    public String getIdCiudad() { return idCiudad; }
    public void setIdCiudad(String idCiudad) { this.idCiudad = idCiudad; }

    @Override
    public String toString() {
        return String.format("Cliente[id=%d, nombre=%s %s, apellido=%s %s]",
                getId(), getNombre(), segundoNombre != null ? segundoNombre : "",
                getApellido(), apellido2 != null ? apellido2 : "");
    }
}
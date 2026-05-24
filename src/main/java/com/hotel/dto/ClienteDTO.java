/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.java.com.hotel.dto;

/**
 *
 * @author Pulgarin
 */
/**
 * DTO para crear o actualizar un cliente. Separa los datos de entrada del
 * modelo de dominio. SOLID: S - solo transporta datos, sin lógica.
 */
public class ClienteDTO {

    private String cedula;
    private String nombre;
    private String segundoNombre;
    private String apellido;
    private String apellido2;
    private String email;
    private String telefono;
    private String nacionalidad;
    private String ciudadOrigen;

    public ClienteDTO() {
    }

    public ClienteDTO(String cedula, String nombre, String segundoNombre, String apellido, String apellido2, String email, String telefono, String nacionalidad, String ciudadOrigen) {
        this.cedula = cedula;
        this.nombre = nombre;
        this.segundoNombre = segundoNombre;
        this.apellido = apellido;
        this.apellido2 = apellido2;
        this.email = email;
        this.telefono = telefono;
        this.nacionalidad = nacionalidad;
        this.ciudadOrigen = ciudadOrigen;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String v) {
        this.cedula = v;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String v) {
        this.nombre = v;
    }

    public String getSegundoNombre() {
        return segundoNombre;
    }

    public void setSegundoNombre(String v) {
        this.segundoNombre = v;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String v) {
        this.apellido = v;
    }

    public String getApellido2() {
        return apellido2;
    }

    public void setApellido2(String v) {
        this.apellido2 = v;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        this.email = v;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String v) {
        this.telefono = v;
    }

    public String getNacionalidad() {
        return nacionalidad;
    }

    public void setNacionalidad(String v) {
        this.nacionalidad = v;
    }

    public String getCiudadOrigen() {
        return ciudadOrigen;
    }

    public void setCiudadOrigen(String v) {
        this.ciudadOrigen = v;
    }

    /**
     * La cédula es el id_cliente en la base de datos.
     */
    public String getDocumento() {
        return cedula;
    }

    public String getSubtipoDocumento() {
        return null;
    }

}

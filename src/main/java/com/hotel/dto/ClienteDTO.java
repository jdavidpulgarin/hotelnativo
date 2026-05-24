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
 * DTO para crear o actualizar un cliente.
 * Separa los datos de entrada del modelo de dominio.
 * SOLID: S - solo transporta datos, sin lógica.
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
    
    
}

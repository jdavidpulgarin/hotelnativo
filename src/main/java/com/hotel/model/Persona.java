
package com.hotel.model;

/**
 *
 * @author rober
 */
package com.hotel.model;

/**
 * Clase abstracta base para entidades con identidad personal.
 * SOLID: L - Liskov Substitution Principle (subclases son sustituibles)
 * GRASP: Generalización para reducir duplicación en Cliente y Empleado
 */
public abstract class Persona {

    private int id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;

    protected Persona() {}

    protected Persona(int id, String nombre, String apellido, String email, String telefono) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
    }

    /** Retorna nombre completo como unidad semántica. */
    public String obtenerNombreCompleto() {
        return nombre + " " + apellido;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}

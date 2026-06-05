package com.hotel.util;

import com.hotel.exception.ExcepcionBaseDatos;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {

    private static final String URL  = "jdbc:oracle:thin:@localhost:1521/XEPDB1";
    private static final String USER = "HOTELNATIVO";
    private static final String PASS = "hotel123";

    private static Conexion instancia;
    private Connection conexion;

    private Conexion() {}

    public static synchronized Conexion obtenerInstancia() {
        if (instancia == null) instancia = new Conexion();
        return instancia;
    }

    public Connection obtenerConexion() {
        try {
            if (conexion == null || conexion.isClosed()) {
                conexion = DriverManager.getConnection(URL, USER, PASS);
            }
            return conexion;
        } catch (SQLException e) {
            throw new ExcepcionBaseDatos("No se pudo conectar a Oracle: " + e.getMessage(), e);
        }
    }
}

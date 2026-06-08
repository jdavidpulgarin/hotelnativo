package com.hotel.dao.impl;

import com.hotel.exception.ExcepcionBaseDatos;
import com.hotel.util.ConexionBaseDatos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


 
public abstract class BaseDAO {

    protected final ConexionBaseDatos conexionBD;

    protected BaseDAO() {
        this.conexionBD = ConexionBaseDatos.obtenerInstancia();
    }

   
    protected Connection obtener() {
        return conexionBD.obtenerConexion();
    }


    protected void liberar(Connection conn) {
        conexionBD.liberarConexion(conn);
    }

  
    @FunctionalInterface
    protected interface TransaccionCallback<T> {
      
        T ejecutar(Connection conn) throws Exception;
    }

 
    protected <T> T enTransaccion(TransaccionCallback<T> callback) {
        Connection conn = obtener();
        try {
            conn.setAutoCommit(false);
            T resultado = callback.ejecutar(conn);
            conn.commit();
            return resultado;
        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new ExcepcionBaseDatos("Error en transacción: " + e.getMessage(), e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            liberar(conn);
        }
    }

   
    
    protected int siguienteSeq(Connection conn, String secuencia) throws SQLException {
        try (PreparedStatement s = conn.prepareStatement(
                "SELECT " + secuencia + ".NEXTVAL FROM DUAL");
             java.sql.ResultSet rs = s.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

 
    protected static String fmt(String prefijo, int n) {
        return String.format("%s%02d", prefijo, n);
    }

   
    protected static String fmt3(String prefijo, int n) {
        return String.format("%s%03d", prefijo, n);
    }
}

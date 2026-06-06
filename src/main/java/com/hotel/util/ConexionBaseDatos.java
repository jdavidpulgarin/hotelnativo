package com.hotel.util;

import com.hotel.exception.ExcepcionBaseDatos;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Pool mínimo de conexiones a Oracle — inicialización LAZY.
 * Las conexiones se crean la primera vez que se necesitan, no al arrancar.
 * Esto evita bloquear el hilo de JavaFX durante el inicio de la aplicación.
 *
 * Las credenciales se cargan desde (en orden de prioridad):
 *   1. config/db.properties  (archivo externo, NO incluido en el JAR)
 *   2. Variables de entorno   HOTEL_DB_URL, HOTEL_DB_USER, HOTEL_DB_PASS
 * Si ninguna fuente está disponible se lanza ExcepcionBaseDatos.
 *
 * GRASP: Fabricación Pura – infraestructura, no existe en el dominio.
 * SOLID: S – responsabilidad única: gestionar el pool de conexiones.
 */

public class ConexionBaseDatos {

    private static final String DRIVER_ORACLE = "oracle.jdbc.driver.OracleDriver";

    private static final int TAMANO_POOL = 5;

    // Credenciales cargadas en la inicialización del Singleton
    private final String urlConexion;
    private final String usuarioBD;
    private final String contrasenaBD;

    private static volatile ConexionBaseDatos instanciaUnica;

    private final BlockingQueue<Connection> pool;

    /** Constructor: carga credenciales y el driver; las conexiones se crean bajo demanda. */
    private ConexionBaseDatos() {
        Properties props = cargarPropiedades();
        this.urlConexion  = props.getProperty("db.url");
        this.usuarioBD    = props.getProperty("db.user");
        this.contrasenaBD = props.getProperty("db.password");
        inicializarDriver();
        pool = new ArrayBlockingQueue<>(TAMANO_POOL);
    }
}

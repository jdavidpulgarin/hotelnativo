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
 * Pool mínimo de conexiones a Oracle — inicialización LAZY. Las conexiones se
 * crean la primera vez que se necesitan, no al arrancar. Esto evita bloquear el
 * hilo de JavaFX durante el inicio de la aplicación.
 *
 * Las credenciales se cargan desde (en orden de prioridad): 1.
 * config/db.properties (archivo externo, NO incluido en el JAR) 2. Variables de
 * entorno HOTEL_DB_URL, HOTEL_DB_USER, HOTEL_DB_PASS Si ninguna fuente está
 * disponible se lanza ExcepcionBaseDatos.
 *
 * GRASP: Fabricación Pura – infraestructura, no existe en el dominio. SOLID: S
 * – responsabilidad única: gestionar el pool de conexiones.
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

    /**
     * Constructor: carga credenciales y el driver; las conexiones se crean bajo
     * demanda.
     */
    private ConexionBaseDatos() {
        Properties props = cargarPropiedades();
        this.urlConexion = props.getProperty("db.url");
        this.usuarioBD = props.getProperty("db.user");
        this.contrasenaBD = props.getProperty("db.password");
        inicializarDriver();
        pool = new ArrayBlockingQueue<>(TAMANO_POOL);
    }

    public static ConexionBaseDatos obtenerInstancia() {
        if (instanciaUnica == null) {
            synchronized (ConexionBaseDatos.class) {
                if (instanciaUnica == null) {
                    instanciaUnica = new ConexionBaseDatos();
                }
            }
        }
        return instanciaUnica;
    }

    /**
     * Carga las propiedades desde config/db.properties. Prueba múltiples rutas
     * relativas para ser compatible con distintos directorios de trabajo
     * (Maven, VSCode F5, ejecución directa). Si el archivo no existe en ninguna
     * ruta, intenta leer variables de entorno. Si tampoco existen, lanza
     * ExcepcionBaseDatos con instrucciones claras.
     */
    private static Properties cargarPropiedades() {
        // Rutas candidatas en orden de prioridad
        Path[] candidatos = {
            Path.of("config", "db.properties"), // CWD = raíz del proyecto
            Path.of("hotel-system", "config", "db.properties"), // CWD = carpeta workspace padre
            Path.of("..", "config", "db.properties"), // CWD = dentro del proyecto
        };

        for (Path candidato : candidatos) {
            if (Files.exists(candidato)) {
                try (InputStream is = Files.newInputStream(candidato)) {
                    Properties props = new Properties();
                    props.load(is);
                    validarPropiedades(props, candidato.toString());
                    return props;
                } catch (IOException e) {
                    throw new ExcepcionBaseDatos(
                            "No se pudo leer " + candidato + ": " + e.getMessage(), e);
                }
            }
        }

        // Fallback: variables de entorno
        String url = System.getenv("HOTEL_DB_URL");
        String user = System.getenv("HOTEL_DB_USER");
        String pass = System.getenv("HOTEL_DB_PASS");

        if (url != null && user != null && pass != null) {
            Properties props = new Properties();
            props.setProperty("db.url", url);
            props.setProperty("db.user", user);
            props.setProperty("db.password", pass);
            return props;
        }

        throw new ExcepcionBaseDatos(
                "No se encontraron credenciales de BD. "
                + "Crea el archivo 'config/db.properties' con db.url, db.user y db.password, "
                + "o define las variables de entorno HOTEL_DB_URL, HOTEL_DB_USER y HOTEL_DB_PASS.");
    }

    private static void validarPropiedades(Properties props, String fuente) {
        String[] requeridas = {"db.url", "db.user", "db.password"};
        for (String clave : requeridas) {
            if (props.getProperty(clave) == null || props.getProperty(clave).isBlank()) {
                throw new ExcepcionBaseDatos(
                        "La propiedad '" + clave + "' es requerida en " + fuente + " pero está ausente o vacía.");
            }
        }
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.java.com.hotel.service;

import com.hotel.dao.interfaces.IAuditoriaDAO;
import com.hotel.model.Auditoria;
/**
 *
 * @author Pulgarin
 */
/**
 * Servicio de registro de eventos de auditoría.
 *
 * Este servicio nunca debe lanzar excepciones al caller: la auditoría es
 * complementaria y no debe interrumpir operaciones de negocio si falla.
 * Cualquier error se registra en la salida de error estándar.
 *
 * GRASP: Controlador – coordina la creación y persistencia de eventos.
 * SOLID: S – responsabilidad única: registrar acciones del sistema.
 * SOLID: D – depende de IAuditoriaDAO, no de la implementación concreta.
 */
public class AuditoriaService {

    private final IAuditoriaDAO auditoriaDAO;

    public AuditoriaService(IAuditoriaDAO auditoriaDAO) {
        this.auditoriaDAO = auditoriaDAO;
    }
    
        /**
     * Registra un evento de auditoría de forma silenciosa.
     * Si la inserción falla, el error se imprime en stderr pero
     * NO se propaga al llamador para no interrumpir operaciones de negocio.
     *
     * @param idEmpleado id del empleado que realizó la acción
     * @param accion     nombre de la acción (ej: "CREAR_RESERVA")
     * @param entidad    nombre de la entidad afectada (ej: "reservas")
     * @param idEntidad  id de la entidad afectada
     * @param detalles   información adicional del evento
     */
    public void registrar(int idEmpleado, String accion, String entidad,
                          int idEntidad, String detalles) {
        try {
            Auditoria registro = new Auditoria(idEmpleado, accion, entidad, idEntidad, detalles);
            auditoriaDAO.insertar(registro);
        } catch (Exception e) {
            System.err.println("[AUDITORIA] Error al registrar evento '"
                    + accion + "' sobre " + entidad + "#" + idEntidad
                    + ": " + e.getMessage());
        }
    }
}

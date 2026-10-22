package com.cooperativa.cuidado.web.asignacion;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.service.AsistenciaService;
import com.cooperativa.cuidado.exception.ReglaNegocioException;
import com.cooperativa.cuidado.socio.EstadoSocio;
import com.cooperativa.cuidado.socio.service.SocioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AsignacionService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AsistenciaService asistenciaService;
    private final SocioService socioService;

    public AsignacionService(AsistenciaService asistenciaService, SocioService socioService) {
        this.asistenciaService = asistenciaService;
        this.socioService = socioService;
    }

    @Transactional
    public Asistencia asignar(Asistencia asistencia) {
        Asistencia guardada = asistenciaService.registrar(asistencia);
        socioService.cambiarEstado(guardada.getSocio().getId(), EstadoSocio.ACTIVO_EN_SERVICIO);
        return guardada;
    }

    @Transactional
    public Asistencia modificar(Long id, Asistencia datos) {
        Asistencia guardada = asistenciaService.actualizar(id, datos);
        socioService.cambiarEstado(guardada.getSocio().getId(), EstadoSocio.ACTIVO_EN_SERVICIO);
        return guardada;
    }

    /** Crea un turno por cada fecha, todo o nada: si una fecha choca con otro turno del socio no se guarda ninguno. */
    @Transactional
    public int asignarSerie(Asistencia plantilla, List<LocalDate> fechas) {
        for (LocalDate fecha : fechas) {
            Asistencia turno = new Asistencia();
            turno.setSocio(plantilla.getSocio());
            turno.setCliente(plantilla.getCliente());
            turno.setServicioDomicilio(plantilla.getServicioDomicilio());
            turno.setFecha(fecha);
            turno.setHoraInicio(plantilla.getHoraInicio());
            turno.setHoraFin(plantilla.getHoraFin());
            try {
                asistenciaService.registrar(turno);
            } catch (ReglaNegocioException ex) {
                throw new ReglaNegocioException("No se pudo asignar el " + FORMATO_FECHA.format(fecha) + ": "
                        + ex.getMessage() + ". No se creó ningún turno de la serie.");
            }
        }
        socioService.cambiarEstado(plantilla.getSocio().getId(), EstadoSocio.ACTIVO_EN_SERVICIO);
        return fechas.size();
    }
}

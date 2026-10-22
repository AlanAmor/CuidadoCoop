package com.cooperativa.cuidado.servicio.service;

import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.servicio.ServicioDomicilioRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
public class ServicioDomicilioService {

    private final ServicioDomicilioRepository servicioRepository;
    private final AsistenciaRepository asistenciaRepository;

    public ServicioDomicilioService(
            ServicioDomicilioRepository servicioRepository,
            AsistenciaRepository asistenciaRepository) {
        this.servicioRepository = servicioRepository;
        this.asistenciaRepository = asistenciaRepository;
    }

    @Transactional(readOnly = true)
    public List<ServicioDomicilio> listar() {
        return servicioRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public ServicioDomicilio obtener(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el servicio con ID " + id));
    }

    @Transactional
    public ServicioDomicilio registrar(@Valid ServicioDomicilio datos) {
        datos.setId(null);
        return servicioRepository.save(datos);
    }

    /**
     * Cambiar el valor hora no modifica las asistencias ya cargadas: cada una conserva
     * el valor vigente cuando se registró.
     */
    @Transactional
    public ServicioDomicilio actualizar(Long id, @Valid ServicioDomicilio datos) {
        ServicioDomicilio servicio = obtener(id);
        servicio.setTipoServicio(datos.getTipoServicio());
        servicio.setValorHora(datos.getValorHora());
        servicio.setValorHoraSocio(datos.getValorHoraSocio());
        return servicioRepository.save(servicio);
    }

    @Transactional
    public void eliminar(Long id) {
        ServicioDomicilio servicio = obtener(id);
        asistenciaRepository.deleteAllByServicioDomicilioId(id);
        asistenciaRepository.flush();
        servicioRepository.delete(servicio);
    }
}

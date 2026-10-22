package com.cooperativa.cuidado.servicio.service;

import com.cooperativa.cuidado.exception.RecursoDuplicadoException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.exception.ReglaNegocioException;
import com.cooperativa.cuidado.servicio.ServicioDomicilioRepository;
import com.cooperativa.cuidado.servicio.TipoServicio;
import com.cooperativa.cuidado.servicio.TipoServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TipoServicioService {

    private static final int LARGO_MAXIMO = 60;

    private final TipoServicioRepository tipoRepository;
    private final ServicioDomicilioRepository servicioRepository;

    public TipoServicioService(
            TipoServicioRepository tipoRepository,
            ServicioDomicilioRepository servicioRepository) {
        this.tipoRepository = tipoRepository;
        this.servicioRepository = servicioRepository;
    }

    @Transactional(readOnly = true)
    public List<TipoServicio> listar() {
        return tipoRepository.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public TipoServicio obtener(Long id) {
        if (id == null) {
            throw new RecursoNoEncontradoException("Seleccioná un tipo de servicio.");
        }
        return tipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El tipo de servicio seleccionado ya no existe. Actualizá la página e intentá de nuevo."));
    }

    @Transactional(readOnly = true)
    public long cantidadDeServicios(Long id) {
        return servicioRepository.countByTipoServicioId(id);
    }

    @Transactional
    public TipoServicio crear(String nombre) {
        String limpio = normalizar(nombre);
        validarNoDuplicado(limpio, null);
        return tipoRepository.save(new TipoServicio(limpio));
    }

    @Transactional
    public TipoServicio renombrar(Long id, String nombre) {
        TipoServicio tipo = obtener(id);
        String limpio = normalizar(nombre);
        validarNoDuplicado(limpio, id);
        tipo.setNombre(limpio);
        return tipoRepository.save(tipo);
    }

    @Transactional
    public void eliminar(Long id) {
        TipoServicio tipo = obtener(id);
        long enUso = servicioRepository.countByTipoServicioId(id);
        if (enUso > 0) {
            throw new ReglaNegocioException("No se puede eliminar \"" + tipo.getNombre() + "\" porque lo usan "
                    + enUso + (enUso == 1 ? " servicio" : " servicios")
                    + ". Cambiá el tipo de esos servicios o eliminalos primero.");
        }
        tipoRepository.delete(tipo);
    }

    private String normalizar(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) {
            throw new ReglaNegocioException("Ingresá el nombre del tipo de servicio.");
        }
        if (limpio.length() > LARGO_MAXIMO) {
            throw new ReglaNegocioException("El nombre admite hasta " + LARGO_MAXIMO + " caracteres.");
        }
        return limpio;
    }

    private void validarNoDuplicado(String nombre, Long idActual) {
        tipoRepository.findByNombreIgnoreCase(nombre)
                .filter(existente -> !existente.getId().equals(idActual))
                .ifPresent(existente -> {
                    throw new RecursoDuplicadoException("Ya existe un tipo de servicio llamado \"" + existente.getNombre() + "\".");
                });
    }
}

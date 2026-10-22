package com.cooperativa.cuidado.socio.service;

import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.exception.RecursoDuplicadoException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.legajo.TipoLegajo;
import com.cooperativa.cuidado.legajo.service.LegajoService;
import com.cooperativa.cuidado.socio.EstadoSocio;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Validated
public class SocioService {

    private final SocioRepository socioRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final LegajoService legajoService;

    public SocioService(
            SocioRepository socioRepository,
            AsistenciaRepository asistenciaRepository,
            LegajoService legajoService) {
        this.socioRepository = socioRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.legajoService = legajoService;
    }

    @Transactional(readOnly = true)
    public List<Socio> listar() {
        return socioRepository.findAll(Sort.by("apellido", "nombre"));
    }

    @Transactional
    public void eliminar(Long id) {
        Socio socio = obtenerEntidad(id);
        asistenciaRepository.deleteAllBySocioId(id);
        asistenciaRepository.flush();
        legajoService.eliminarLegajo(TipoLegajo.SOCIO, id);
        socioRepository.delete(socio);
    }

    @Transactional
    public Socio registrar(@Valid Socio socio) {
        socio.setId(null);
        validarUnicidad(socio, null);
        return socioRepository.save(socio);
    }

    @Transactional
    public Socio actualizar(Long id, @Valid Socio datos) {
        Socio socio = obtenerEntidad(id);
        validarUnicidad(datos, id);

        socio.setNombre(datos.getNombre());
        socio.setApellido(datos.getApellido());
        socio.setDni(datos.getDni());
        socio.setDireccion(datos.getDireccion());
        socio.setTelefono(datos.getTelefono());
        socio.setCorreo(datos.getCorreo());
        socio.setCbu(datos.getCbu());
        socio.setEstado(datos.getEstado());
        socio.setEspecialidad(datos.getEspecialidad());
        return socioRepository.save(socio);
    }

    @Transactional
    public Socio cambiarEstado(Long id, EstadoSocio nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El estado nuevo es obligatorio");
        }
        Socio socio = obtenerEntidad(id);
        socio.setEstado(nuevoEstado);
        return socioRepository.save(socio);
    }

    @Transactional(readOnly = true)
    public Socio obtener(Long id) {
        return obtenerEntidad(id);
    }

    @Transactional(readOnly = true)
    public List<Socio> listarDisponiblesParaReemplazo() {
        return socioRepository.findAllByEstado(EstadoSocio.DISPONIBLE_REEMPLAZO);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumarHorasTrabajadas(Long socioId, LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("El rango de fechas es inválido");
        }
        if (!socioRepository.existsById(socioId)) {
            throw new RecursoNoEncontradoException("No existe el socio con ID " + socioId);
        }
        BigDecimal horas = socioRepository.sumarHorasTrabajadas(socioId, desde, hasta);
        return horas == null ? BigDecimal.ZERO : horas;
    }

    private Socio obtenerEntidad(Long id) {
        return socioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el socio con ID " + id));
    }

    private void validarUnicidad(Socio socio, Long idActual) {
        boolean dniDuplicado = idActual == null
                ? socioRepository.findByDni(socio.getDni()).isPresent()
                : socioRepository.existsByDniAndIdNot(socio.getDni(), idActual);
        if (dniDuplicado) {
            throw new RecursoDuplicadoException("Ya existe un socio con el DNI indicado");
        }

        boolean cbuDuplicado = idActual == null
                ? socioRepository.existsByCbu(socio.getCbu())
                : socioRepository.existsByCbuAndIdNot(socio.getCbu(), idActual);
        if (cbuDuplicado) {
            throw new RecursoDuplicadoException("Ya existe un socio con el CBU indicado");
        }
    }
}

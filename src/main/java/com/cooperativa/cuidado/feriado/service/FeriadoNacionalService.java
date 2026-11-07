package com.cooperativa.cuidado.feriado.service;

import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.exception.ReglaNegocioException;
import com.cooperativa.cuidado.feriado.FeriadoNacional;
import com.cooperativa.cuidado.feriado.FeriadoNacionalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FeriadoNacionalService {

    private final FeriadoNacionalRepository feriadoRepository;

    public FeriadoNacionalService(FeriadoNacionalRepository feriadoRepository) {
        this.feriadoRepository = feriadoRepository;
    }

    @Transactional(readOnly = true)
    public List<FeriadoNacional> listar() {
        return feriadoRepository.findAllByOrderByFechaAsc();
    }

    @Transactional(readOnly = true)
    public List<FeriadoNacional> listarPorAnio(int anio) {
        LocalDate desde = LocalDate.of(anio, 1, 1);
        LocalDate hasta = LocalDate.of(anio, 12, 31);
        return feriadoRepository.findAllByFechaBetweenOrderByFechaAsc(desde, hasta);
    }

    @Transactional(readOnly = true)
    public boolean esFeriado(LocalDate fecha) {
        return fecha != null && feriadoRepository.existsByFecha(fecha);
    }

    @Transactional(readOnly = true)
    public Optional<FeriadoNacional> buscarPorFecha(LocalDate fecha) {
        if (fecha == null) {
            return Optional.empty();
        }
        return feriadoRepository.findByFecha(fecha);
    }

    @Transactional
    public FeriadoNacional registrar(LocalDate fecha, String descripcion) {
        if (fecha == null) {
            throw new DatosInvalidosException("Seleccioná la fecha del feriado nacional.");
        }
        String descLimpia = descripcion == null ? "" : descripcion.trim();
        if (descLimpia.isEmpty()) {
            throw new DatosInvalidosException("Ingresá el nombre o motivo del feriado nacional.");
        }
        if (descLimpia.length() > 160) {
            throw new DatosInvalidosException("La descripción del feriado no puede superar los 160 caracteres.");
        }
        if (feriadoRepository.existsByFecha(fecha)) {
            throw new ReglaNegocioException("La fecha seleccionada ya está registrada como feriado nacional.");
        }
        return feriadoRepository.save(new FeriadoNacional(fecha, descLimpia));
    }

    @Transactional
    public void eliminar(Long id) {
        FeriadoNacional existente = feriadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El feriado nacional seleccionado ya no existe."));
        feriadoRepository.delete(existente);
    }

    /**
     * Carga los feriados nacionales inamovibles de Argentina para el año indicado,
     * omitiendo aquellos que ya estén registrados.
     */
    @Transactional
    public int cargarFeriadosFijosAnio(int anio) {
        if (anio < 2000 || anio > 2100) {
            throw new DatosInvalidosException("Indicá un año válido entre 2000 y 2100.");
        }
        Map<LocalDate, String> fijos = new LinkedHashMap<>();
        fijos.put(LocalDate.of(anio, 1, 1), "Año Nuevo");
        fijos.put(LocalDate.of(anio, 3, 24), "Día Nacional de la Memoria por la Verdad y la Justicia");
        fijos.put(LocalDate.of(anio, 4, 2), "Día del Veterano y de los Caídos en la Guerra de Malvinas");
        fijos.put(LocalDate.of(anio, 5, 1), "Día del Trabajador");
        fijos.put(LocalDate.of(anio, 5, 25), "Día de la Revolución de Mayo");
        fijos.put(LocalDate.of(anio, 6, 20), "Paso a la Inmortalidad del Gral. Manuel Belgrano");
        fijos.put(LocalDate.of(anio, 7, 9), "Día de la Independencia");
        fijos.put(LocalDate.of(anio, 12, 8), "Día de la Inmaculada Concepción de María");
        fijos.put(LocalDate.of(anio, 12, 25), "Navidad");

        int agregados = 0;
        for (Map.Entry<LocalDate, String> entry : fijos.entrySet()) {
            if (!feriadoRepository.existsByFecha(entry.getKey())) {
                feriadoRepository.save(new FeriadoNacional(entry.getKey(), entry.getValue()));
                agregados++;
            }
        }
        return agregados;
    }
}


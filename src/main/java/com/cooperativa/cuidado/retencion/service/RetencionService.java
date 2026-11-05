package com.cooperativa.cuidado.retencion.service;

import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.retencion.RetencionSocio;
import com.cooperativa.cuidado.retencion.RetencionSocioRepository;
import com.cooperativa.cuidado.retencion.TipoRetencion;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class RetencionService {

    private final RetencionSocioRepository retencionRepository;
    private final SocioRepository socioRepository;

    public RetencionService(RetencionSocioRepository retencionRepository, SocioRepository socioRepository) {
        this.retencionRepository = retencionRepository;
        this.socioRepository = socioRepository;
    }

    @Transactional
    public RetencionSocio registrar(
            Long socioId,
            String periodo,
            TipoRetencion tipo,
            String motivo,
            BigDecimal unidad,
            BigDecimal montoUnitario) {
        if (socioId == null) {
            throw new DatosInvalidosException("Seleccioná un socio para registrar la retención.");
        }
        String periodoNormalizado = validarPeriodo(periodo);
        Socio socio = socioRepository.findById(socioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el socio seleccionado."));

        TipoRetencion tipoEfectivo = tipo != null ? tipo : TipoRetencion.OTRAS_RETENCIONES;
        String motivoLimpio = motivo != null ? motivo.trim() : "";
        if (motivoLimpio.isEmpty()) {
            motivoLimpio = tipoEfectivo.getDetallePorDefecto();
        }
        if (motivoLimpio.length() > 250) {
            throw new DatosInvalidosException("El motivo de la retención no puede superar los 250 caracteres.");
        }

        BigDecimal unidadEfectiva = unidad == null ? BigDecimal.ONE : unidad.setScale(2, RoundingMode.HALF_UP);
        if (unidadEfectiva.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DatosInvalidosException("La unidad de la retención debe ser mayor a cero.");
        }
        if (montoUnitario == null || montoUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DatosInvalidosException("Ingresá un monto de retención mayor a cero.");
        }
        BigDecimal valorNormalizado = montoUnitario.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = unidadEfectiva.multiply(valorNormalizado).setScale(2, RoundingMode.HALF_UP);

        RetencionSocio retencion = new RetencionSocio();
        retencion.setSocio(socio);
        retencion.setPeriodo(periodoNormalizado);
        retencion.setTipo(tipoEfectivo);
        retencion.setMotivo(motivoLimpio);
        retencion.setUnidad(unidadEfectiva);
        retencion.setValorUnidad(valorNormalizado);
        retencion.setTotal(total);
        retencion.setFechaCreacion(LocalDate.now());
        return retencionRepository.save(retencion);
    }

    @Transactional(readOnly = true)
    public List<RetencionSocio> listar(Long socioId, String periodo) {
        String per = periodo != null && !periodo.isBlank() ? periodo.trim() : null;
        if (socioId != null && per != null) {
            return retencionRepository.findAllBySocioIdAndPeriodoOrderByIdAsc(socioId, per);
        }
        if (socioId != null) {
            return retencionRepository.findAllBySocioIdOrderByPeriodoDescIdDesc(socioId);
        }
        if (per != null) {
            return retencionRepository.findAllByPeriodoOrderByIdDesc(per);
        }
        return retencionRepository.findAllByOrderByPeriodoDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<RetencionSocio> listarPorSocioYPeriodo(Long socioId, String periodo) {
        if (socioId == null || periodo == null || periodo.isBlank()) {
            return List.of();
        }
        return retencionRepository.findAllBySocioIdAndPeriodoOrderByIdAsc(socioId, periodo.trim());
    }

    @Transactional
    public void eliminar(Long id) {
        RetencionSocio existente = retencionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La retención indicada no existe."));
        retencionRepository.delete(existente);
    }

    private String validarPeriodo(String periodo) {
        if (periodo == null || periodo.isBlank()) {
            throw new DatosInvalidosException("Seleccioná un período (AAAA-MM).");
        }
        try {
            return YearMonth.parse(periodo.trim()).toString();
        } catch (DateTimeParseException ex) {
            throw new DatosInvalidosException("El período debe tener el formato AAAA-MM.");
        }
    }
}


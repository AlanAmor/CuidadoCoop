package com.cooperativa.cuidado.liquidacion.service;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.liquidacion.dto.LiquidacionMensualSocio;
import com.cooperativa.cuidado.liquidacion.dto.ResumenDomicilioLiquidacion;
import com.cooperativa.cuidado.servicio.TipoServicio;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LiquidacionService {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);

    private final SocioRepository socioRepository;
    private final AsistenciaRepository asistenciaRepository;

    public LiquidacionService(SocioRepository socioRepository, AsistenciaRepository asistenciaRepository) {
        this.socioRepository = socioRepository;
        this.asistenciaRepository = asistenciaRepository;
    }

    @Transactional(readOnly = true)
    public LiquidacionMensualSocio calcularRetornoMensual(Long socioId, YearMonth periodo) {
        if (periodo == null) {
            throw new IllegalArgumentException("El período de liquidación es obligatorio");
        }
        Socio socio = socioRepository.findById(socioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el socio con ID " + socioId));

        List<Asistencia> asistencias = asistenciaRepository.findAllBySocioIdAndFechaBetween(
                socioId, periodo.atDay(1), periodo.atEndOfMonth());

        Map<String, AcumuladorDomicilio> porDomicilio = new LinkedHashMap<>();
        BigDecimal totalHoras = CERO;
        BigDecimal totalRetorno = CERO;

        for (Asistencia asistencia : asistencias) {
            BigDecimal horas = asistencia.getHorasTotales();
            BigDecimal valorHora = asistencia.getValorHoraPagoSocio();
            if (horas == null || valorHora == null) {
                throw new IllegalStateException(
                        "La asistencia " + asistencia.getId() + " no tiene horas o valor hora registrado");
            }

            BigDecimal importe = horas.multiply(valorHora).setScale(2, RoundingMode.HALF_UP);
            Long clienteId = asistencia.getCliente().getId();
            TipoServicio tipo = asistencia.getServicioDomicilio().getTipoServicio();
            AcumuladorDomicilio acumulador = porDomicilio.computeIfAbsent(
                    clienteId + "-" + tipo,
                    ignored -> new AcumuladorDomicilio(
                            clienteId,
                            asistencia.getCliente().getDomicilio(),
                            tipo));
            acumulador.agregar(horas, importe);
            totalHoras = totalHoras.add(horas);
            totalRetorno = totalRetorno.add(importe);
        }

        List<ResumenDomicilioLiquidacion> domicilios = porDomicilio.values().stream()
                .map(AcumuladorDomicilio::construirResumen)
                .toList();

        return new LiquidacionMensualSocio(
                socio.getId(),
                socio.getNombre() + " " + socio.getApellido(),
                periodo,
                totalHoras.setScale(2, RoundingMode.HALF_UP),
                totalRetorno.setScale(2, RoundingMode.HALF_UP),
                domicilios);
    }

    private static class AcumuladorDomicilio {

        private final Long servicioDomicilioId;
        private final String domicilio;
        private final TipoServicio tipoServicio;
        private BigDecimal horas = CERO;
        private BigDecimal importe = CERO;

        private AcumuladorDomicilio(
                Long servicioDomicilioId,
                String domicilio,
                TipoServicio tipoServicio) {
            this.servicioDomicilioId = servicioDomicilioId;
            this.domicilio = domicilio;
            this.tipoServicio = tipoServicio;
        }

        private void agregar(BigDecimal horas, BigDecimal importe) {
            this.horas = this.horas.add(horas);
            this.importe = this.importe.add(importe);
        }

        private ResumenDomicilioLiquidacion construirResumen() {
            return new ResumenDomicilioLiquidacion(
                    servicioDomicilioId,
                    domicilio,
                    tipoServicio,
                    horas.setScale(2, RoundingMode.HALF_UP),
                    importe.setScale(2, RoundingMode.HALF_UP));
        }
    }
}

package com.cooperativa.cuidado.recibo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PrevisualizacionReciboDto(
        Long proximoNumero,
        String proximoNumeroFormateado,
        Long socioId,
        String nombreApellidoSocio,
        String cuilSocio,
        String domicilioSocio,
        String localidadSocio,
        String cpSocio,
        String cbuSocio,
        String mesSeleccionado,
        LocalDate periodoDesde,
        LocalDate periodoHasta,
        BigDecimal horasCuidados,
        BigDecimal valorHoraCuidados,
        BigDecimal subtotalHoras,
        int unidadesSanatorio,
        BigDecimal valorSanatorio,
        BigDecimal subtotalSanatorio,
        int unidadesViaticos,
        BigDecimal valorViatico,
        BigDecimal subtotalViaticos,
        BigDecimal subtotalCuidados,
        List<RetencionPreviaDto> retencionesPeriodo) {

    public record RetencionPreviaDto(
            Long id,
            String tipo,
            String etiquetaConcepto,
            String motivo,
            BigDecimal unidad,
            BigDecimal valorUnidad,
            BigDecimal total) {
    }
}


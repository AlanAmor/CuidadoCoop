package com.cooperativa.cuidado.liquidacionfamilia.dto;

import java.math.BigDecimal;
import java.util.List;

public record PrevisualizacionFamiliaDto(
        Long clienteId,
        String nombrePaciente,
        String dniPaciente,
        String direccion,
        String ciudad,
        String localidad,
        String codigoPostal,
        String telefonoContacto,
        String periodo,
        String periodoEtiqueta,
        List<LineaCuidadorDto> cuidadores,
        BigDecimal subtotal) {

    public record LineaCuidadorDto(
            String nombreCuidador,
            BigDecimal horas,
            BigDecimal valorHoraCliente,
            BigDecimal totalLinea) {
    }
}


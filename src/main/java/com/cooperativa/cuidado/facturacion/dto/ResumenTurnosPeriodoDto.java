package com.cooperativa.cuidado.facturacion.dto;

import java.math.BigDecimal;
import java.util.List;

public class ResumenTurnosPeriodoDto {

    private Long clienteId;
    private String nombrePaciente;
    private String dniPaciente;
    private int cantidadTurnos;
    private BigDecimal totalHoras = BigDecimal.ZERO;
    private BigDecimal totalViaticos = BigDecimal.ZERO;
    private BigDecimal totalSanatorio = BigDecimal.ZERO;
    private BigDecimal totalServicios = BigDecimal.ZERO;
    private BigDecimal totalGeneral = BigDecimal.ZERO;

    private List<ItemLineaFacturaForm> lineasSugeridas;

    public ResumenTurnosPeriodoDto() {
    }

    public ResumenTurnosPeriodoDto(
            Long clienteId,
            String nombrePaciente,
            String dniPaciente,
            int cantidadTurnos,
            BigDecimal totalHoras,
            BigDecimal totalViaticos,
            BigDecimal totalSanatorio,
            BigDecimal totalServicios,
            BigDecimal totalGeneral,
            List<ItemLineaFacturaForm> lineasSugeridas) {
        this.clienteId = clienteId;
        this.nombrePaciente = nombrePaciente;
        this.dniPaciente = dniPaciente;
        this.cantidadTurnos = cantidadTurnos;
        this.totalHoras = totalHoras;
        this.totalViaticos = totalViaticos;
        this.totalSanatorio = totalSanatorio;
        this.totalServicios = totalServicios;
        this.totalGeneral = totalGeneral;
        this.lineasSugeridas = lineasSugeridas;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getNombrePaciente() {
        return nombrePaciente;
    }

    public void setNombrePaciente(String nombrePaciente) {
        this.nombrePaciente = nombrePaciente;
    }

    public String getDniPaciente() {
        return dniPaciente;
    }

    public void setDniPaciente(String dniPaciente) {
        this.dniPaciente = dniPaciente;
    }

    public int getCantidadTurnos() {
        return cantidadTurnos;
    }

    public void setCantidadTurnos(int cantidadTurnos) {
        this.cantidadTurnos = cantidadTurnos;
    }

    public BigDecimal getTotalHoras() {
        return totalHoras;
    }

    public void setTotalHoras(BigDecimal totalHoras) {
        this.totalHoras = totalHoras;
    }

    public BigDecimal getTotalViaticos() {
        return totalViaticos;
    }

    public void setTotalViaticos(BigDecimal totalViaticos) {
        this.totalViaticos = totalViaticos;
    }

    public BigDecimal getTotalSanatorio() {
        return totalSanatorio;
    }

    public void setTotalSanatorio(BigDecimal totalSanatorio) {
        this.totalSanatorio = totalSanatorio;
    }

    public BigDecimal getTotalServicios() {
        return totalServicios;
    }

    public void setTotalServicios(BigDecimal totalServicios) {
        this.totalServicios = totalServicios;
    }

    public BigDecimal getTotalGeneral() {
        return totalGeneral;
    }

    public void setTotalGeneral(BigDecimal totalGeneral) {
        this.totalGeneral = totalGeneral;
    }

    public List<ItemLineaFacturaForm> getLineasSugeridas() {
        return lineasSugeridas;
    }

    public void setLineasSugeridas(List<ItemLineaFacturaForm> lineasSugeridas) {
        this.lineasSugeridas = lineasSugeridas;
    }
}

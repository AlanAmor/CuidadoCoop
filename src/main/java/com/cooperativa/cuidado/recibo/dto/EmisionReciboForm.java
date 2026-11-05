package com.cooperativa.cuidado.recibo.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmisionReciboForm {

    private Long socioId;
    private String mesSeleccionado;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaEmision;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodoDesde;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodoHasta;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaPago;

    private String comprobanteTransferencia;

    private boolean incluirOtrosConceptos;
    private String otrosConcepto;
    private String otrosDetalle;
    private BigDecimal totalOtrosConceptos;

    private boolean incluirExcedenteAnual;
    private BigDecimal excedenteUnidad;
    private BigDecimal excedenteValorUnidad;
    private BigDecimal totalExcedente;

    private boolean incluirMonotributo;
    private String monotributoDetalle;
    private BigDecimal monotributoUnidad;
    private BigDecimal monotributoValorUnidad;

    private boolean incluirPrestamo;
    private String prestamoDetalle;
    private BigDecimal prestamoUnidad;
    private BigDecimal prestamoValorUnidad;

    private boolean incluirOtrasRetenciones;
    private List<String> otrasRetencionesDetalle = new ArrayList<>();
    private List<BigDecimal> otrasRetencionesUnidad = new ArrayList<>();
    private List<BigDecimal> otrasRetencionesValorUnidad = new ArrayList<>();

    public Long getSocioId() {
        return socioId;
    }

    public void setSocioId(Long socioId) {
        this.socioId = socioId;
    }

    public String getMesSeleccionado() {
        return mesSeleccionado;
    }

    public void setMesSeleccionado(String mesSeleccionado) {
        this.mesSeleccionado = mesSeleccionado;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public LocalDate getPeriodoDesde() {
        return periodoDesde;
    }

    public void setPeriodoDesde(LocalDate periodoDesde) {
        this.periodoDesde = periodoDesde;
    }

    public LocalDate getPeriodoHasta() {
        return periodoHasta;
    }

    public void setPeriodoHasta(LocalDate periodoHasta) {
        this.periodoHasta = periodoHasta;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getComprobanteTransferencia() {
        return comprobanteTransferencia;
    }

    public void setComprobanteTransferencia(String comprobanteTransferencia) {
        this.comprobanteTransferencia = comprobanteTransferencia;
    }

    public boolean isIncluirOtrosConceptos() {
        return incluirOtrosConceptos;
    }

    public void setIncluirOtrosConceptos(boolean incluirOtrosConceptos) {
        this.incluirOtrosConceptos = incluirOtrosConceptos;
    }

    public String getOtrosConcepto() {
        return otrosConcepto;
    }

    public void setOtrosConcepto(String otrosConcepto) {
        this.otrosConcepto = otrosConcepto;
    }

    public String getOtrosDetalle() {
        return otrosDetalle;
    }

    public void setOtrosDetalle(String otrosDetalle) {
        this.otrosDetalle = otrosDetalle;
    }

    public BigDecimal getTotalOtrosConceptos() {
        return totalOtrosConceptos;
    }

    public void setTotalOtrosConceptos(BigDecimal totalOtrosConceptos) {
        this.totalOtrosConceptos = totalOtrosConceptos;
    }

    public boolean isIncluirExcedenteAnual() {
        return incluirExcedenteAnual;
    }

    public void setIncluirExcedenteAnual(boolean incluirExcedenteAnual) {
        this.incluirExcedenteAnual = incluirExcedenteAnual;
    }

    public BigDecimal getExcedenteUnidad() {
        return excedenteUnidad;
    }

    public void setExcedenteUnidad(BigDecimal excedenteUnidad) {
        this.excedenteUnidad = excedenteUnidad;
    }

    public BigDecimal getExcedenteValorUnidad() {
        return excedenteValorUnidad;
    }

    public void setExcedenteValorUnidad(BigDecimal excedenteValorUnidad) {
        this.excedenteValorUnidad = excedenteValorUnidad;
    }

    public BigDecimal getTotalExcedente() {
        return totalExcedente;
    }

    public void setTotalExcedente(BigDecimal totalExcedente) {
        this.totalExcedente = totalExcedente;
    }

    public boolean isIncluirMonotributo() {
        return incluirMonotributo;
    }

    public void setIncluirMonotributo(boolean incluirMonotributo) {
        this.incluirMonotributo = incluirMonotributo;
    }

    public String getMonotributoDetalle() {
        return monotributoDetalle;
    }

    public void setMonotributoDetalle(String monotributoDetalle) {
        this.monotributoDetalle = monotributoDetalle;
    }

    public BigDecimal getMonotributoUnidad() {
        return monotributoUnidad;
    }

    public void setMonotributoUnidad(BigDecimal monotributoUnidad) {
        this.monotributoUnidad = monotributoUnidad;
    }

    public BigDecimal getMonotributoValorUnidad() {
        return monotributoValorUnidad;
    }

    public void setMonotributoValorUnidad(BigDecimal monotributoValorUnidad) {
        this.monotributoValorUnidad = monotributoValorUnidad;
    }

    public boolean isIncluirPrestamo() {
        return incluirPrestamo;
    }

    public void setIncluirPrestamo(boolean incluirPrestamo) {
        this.incluirPrestamo = incluirPrestamo;
    }

    public String getPrestamoDetalle() {
        return prestamoDetalle;
    }

    public void setPrestamoDetalle(String prestamoDetalle) {
        this.prestamoDetalle = prestamoDetalle;
    }

    public BigDecimal getPrestamoUnidad() {
        return prestamoUnidad;
    }

    public void setPrestamoUnidad(BigDecimal prestamoUnidad) {
        this.prestamoUnidad = prestamoUnidad;
    }

    public BigDecimal getPrestamoValorUnidad() {
        return prestamoValorUnidad;
    }

    public void setPrestamoValorUnidad(BigDecimal prestamoValorUnidad) {
        this.prestamoValorUnidad = prestamoValorUnidad;
    }

    public boolean isIncluirOtrasRetenciones() {
        return incluirOtrasRetenciones;
    }

    public void setIncluirOtrasRetenciones(boolean incluirOtrasRetenciones) {
        this.incluirOtrasRetenciones = incluirOtrasRetenciones;
    }

    public List<String> getOtrasRetencionesDetalle() {
        return otrasRetencionesDetalle;
    }

    public void setOtrasRetencionesDetalle(List<String> otrasRetencionesDetalle) {
        this.otrasRetencionesDetalle = otrasRetencionesDetalle == null ? new ArrayList<>() : otrasRetencionesDetalle;
    }

    public List<BigDecimal> getOtrasRetencionesUnidad() {
        return otrasRetencionesUnidad;
    }

    public void setOtrasRetencionesUnidad(List<BigDecimal> otrasRetencionesUnidad) {
        this.otrasRetencionesUnidad = otrasRetencionesUnidad == null ? new ArrayList<>() : otrasRetencionesUnidad;
    }

    public List<BigDecimal> getOtrasRetencionesValorUnidad() {
        return otrasRetencionesValorUnidad;
    }

    public void setOtrasRetencionesValorUnidad(List<BigDecimal> otrasRetencionesValorUnidad) {
        this.otrasRetencionesValorUnidad = otrasRetencionesValorUnidad == null ? new ArrayList<>() : otrasRetencionesValorUnidad;
    }
}


package com.cooperativa.cuidado.recibo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class ItemRetencionRecibo {

    @Column(nullable = false, length = 80)
    private String concepto;

    @Column(nullable = false, length = 250)
    private String detalle;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unidad = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valorUnidad = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    public ItemRetencionRecibo() {
    }

    public ItemRetencionRecibo(String concepto, String detalle, BigDecimal unidad, BigDecimal valorUnidad, BigDecimal total) {
        this.concepto = concepto;
        this.detalle = detalle;
        this.unidad = unidad;
        this.valorUnidad = valorUnidad;
        this.total = total;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public BigDecimal getUnidad() {
        return unidad;
    }

    public void setUnidad(BigDecimal unidad) {
        this.unidad = unidad;
    }

    public BigDecimal getValorUnidad() {
        return valorUnidad;
    }

    public void setValorUnidad(BigDecimal valorUnidad) {
        this.valorUnidad = valorUnidad;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}


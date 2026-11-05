package com.cooperativa.cuidado.liquidacionfamilia;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class LineaCuidadorFamilia {

    @Column(nullable = false, length = 150)
    private String nombreCuidador;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal horas = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valorHoraCliente = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalLinea = BigDecimal.ZERO;

    public LineaCuidadorFamilia() {
    }

    public LineaCuidadorFamilia(String nombreCuidador, BigDecimal horas, BigDecimal valorHoraCliente, BigDecimal totalLinea) {
        this.nombreCuidador = nombreCuidador;
        this.horas = horas;
        this.valorHoraCliente = valorHoraCliente;
        this.totalLinea = totalLinea;
    }

    public String getNombreCuidador() {
        return nombreCuidador;
    }

    public void setNombreCuidador(String nombreCuidador) {
        this.nombreCuidador = nombreCuidador;
    }

    public BigDecimal getHoras() {
        return horas;
    }

    public void setHoras(BigDecimal horas) {
        this.horas = horas;
    }

    public BigDecimal getValorHoraCliente() {
        return valorHoraCliente;
    }

    public void setValorHoraCliente(BigDecimal valorHoraCliente) {
        this.valorHoraCliente = valorHoraCliente;
    }

    public BigDecimal getTotalLinea() {
        return totalLinea;
    }

    public void setTotalLinea(BigDecimal totalLinea) {
        this.totalLinea = totalLinea;
    }
}


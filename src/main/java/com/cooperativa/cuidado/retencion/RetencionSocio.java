package com.cooperativa.cuidado.retencion;

import com.cooperativa.cuidado.socio.Socio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "retenciones_socio")
public class RetencionSocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "socio_id", nullable = false)
    private Socio socio;

    @Column(nullable = false, length = 7)
    private String periodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoRetencion tipo = TipoRetencion.OTRAS_RETENCIONES;

    @Column(nullable = false, length = 250)
    private String motivo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unidad = BigDecimal.ONE;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valorUnidad = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate fechaCreacion = LocalDate.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Socio getSocio() {
        return socio;
    }

    public void setSocio(Socio socio) {
        this.socio = socio;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public TipoRetencion getTipo() {
        return tipo;
    }

    public void setTipo(TipoRetencion tipo) {
        this.tipo = tipo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
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

    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}


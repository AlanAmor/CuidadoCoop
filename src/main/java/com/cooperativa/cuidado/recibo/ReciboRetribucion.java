package com.cooperativa.cuidado.recibo;

import com.cooperativa.cuidado.socio.Socio;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recibos_retribucion")
public class ReciboRetribucion {

    private static final DateTimeFormatter FORMATO_PERIODO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_recibo", nullable = false, unique = true)
    private Long numeroRecibo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "socio_id", nullable = false)
    private Socio socio;

    @Column(nullable = false, length = 180)
    private String nombreApellidoSocio;

    @Column(nullable = false, length = 30)
    private String cuilSocio;

    @Column(nullable = false, length = 200)
    private String domicilioSocio;

    @Column(nullable = false, length = 120)
    private String localidadSocio;

    @Column(nullable = false, length = 20)
    private String cpSocio;

    @Column(nullable = false, length = 40)
    private String cbuSocio;

    @Column(nullable = false)
    private LocalDate fechaEmision;

    @Column(nullable = false, length = 7)
    private String mesSeleccionado;

    @Column(nullable = false)
    private LocalDate periodoDesde;

    @Column(nullable = false)
    private LocalDate periodoHasta;

    @Column(nullable = false)
    private LocalDate fechaPago;

    @Column(nullable = false, length = 100)
    private String comprobanteTransferencia = "-";

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal horasCuidados = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valorHoraCuidados = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotalHoras = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer unidadesSanatorio = 0;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valorSanatorio = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotalSanatorio = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer unidadesViaticos = 0;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valorViatico = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotalViaticos = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotalCuidados = BigDecimal.ZERO;

    @Column(nullable = false, length = 120)
    private String otrosConcepto = "-";

    @Column(nullable = false, length = 250)
    private String otrosDetalle = "-";

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalOtrosConceptos = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalRetribucion = BigDecimal.ZERO;

    @Column(nullable = false, length = 120)
    private String excedenteConcepto = "Retiro de Excedente Anual";

    @Column(nullable = false, length = 250)
    private String excedenteDetalle = "Adelanto Excedente Anual por Asociado Cooperativo";

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal excedenteUnidad = BigDecimal.ONE;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal excedenteValorUnidad = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalExcedente = BigDecimal.ZERO;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recibo_retribucion_retenciones", joinColumns = @JoinColumn(name = "recibo_id"))
    @OrderColumn(name = "orden")
    private List<ItemRetencionRecibo> itemsRetencion = new ArrayList<>();

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalRetenciones = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal retribucionNeta = BigDecimal.ZERO;

    public String getNumeroFormateado() {
        return String.format("N° %05d", numeroRecibo != null ? numeroRecibo : 0L);
    }

    public String getNumeroCeros() {
        return String.format("%05d", numeroRecibo != null ? numeroRecibo : 0L);
    }

    public String getPeriodoTexto() {
        if (periodoDesde == null || periodoHasta == null) {
            return "-";
        }
        return FORMATO_PERIODO.format(periodoDesde) + " al " + FORMATO_PERIODO.format(periodoHasta);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNumeroRecibo() {
        return numeroRecibo;
    }

    public void setNumeroRecibo(Long numeroRecibo) {
        this.numeroRecibo = numeroRecibo;
    }

    public Socio getSocio() {
        return socio;
    }

    public void setSocio(Socio socio) {
        this.socio = socio;
    }

    public String getNombreApellidoSocio() {
        return nombreApellidoSocio;
    }

    public void setNombreApellidoSocio(String nombreApellidoSocio) {
        this.nombreApellidoSocio = nombreApellidoSocio;
    }

    public String getCuilSocio() {
        return cuilSocio;
    }

    public void setCuilSocio(String cuilSocio) {
        this.cuilSocio = cuilSocio;
    }

    public String getDomicilioSocio() {
        return domicilioSocio;
    }

    public void setDomicilioSocio(String domicilioSocio) {
        this.domicilioSocio = domicilioSocio;
    }

    public String getLocalidadSocio() {
        return localidadSocio;
    }

    public void setLocalidadSocio(String localidadSocio) {
        this.localidadSocio = localidadSocio;
    }

    public String getCpSocio() {
        return cpSocio;
    }

    public void setCpSocio(String cpSocio) {
        this.cpSocio = cpSocio;
    }

    public String getCbuSocio() {
        return cbuSocio;
    }

    public void setCbuSocio(String cbuSocio) {
        this.cbuSocio = cbuSocio;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getMesSeleccionado() {
        return mesSeleccionado;
    }

    public void setMesSeleccionado(String mesSeleccionado) {
        this.mesSeleccionado = mesSeleccionado;
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

    public BigDecimal getHorasCuidados() {
        return horasCuidados;
    }

    public void setHorasCuidados(BigDecimal horasCuidados) {
        this.horasCuidados = horasCuidados;
    }

    public BigDecimal getValorHoraCuidados() {
        return valorHoraCuidados;
    }

    public void setValorHoraCuidados(BigDecimal valorHoraCuidados) {
        this.valorHoraCuidados = valorHoraCuidados;
    }

    public BigDecimal getSubtotalHoras() {
        return subtotalHoras;
    }

    public void setSubtotalHoras(BigDecimal subtotalHoras) {
        this.subtotalHoras = subtotalHoras;
    }

    public Integer getUnidadesSanatorio() {
        return unidadesSanatorio;
    }

    public void setUnidadesSanatorio(Integer unidadesSanatorio) {
        this.unidadesSanatorio = unidadesSanatorio;
    }

    public BigDecimal getValorSanatorio() {
        return valorSanatorio;
    }

    public void setValorSanatorio(BigDecimal valorSanatorio) {
        this.valorSanatorio = valorSanatorio;
    }

    public BigDecimal getSubtotalSanatorio() {
        return subtotalSanatorio;
    }

    public void setSubtotalSanatorio(BigDecimal subtotalSanatorio) {
        this.subtotalSanatorio = subtotalSanatorio;
    }

    public Integer getUnidadesViaticos() {
        return unidadesViaticos;
    }

    public void setUnidadesViaticos(Integer unidadesViaticos) {
        this.unidadesViaticos = unidadesViaticos;
    }

    public BigDecimal getValorViatico() {
        return valorViatico;
    }

    public void setValorViatico(BigDecimal valorViatico) {
        this.valorViatico = valorViatico;
    }

    public BigDecimal getSubtotalViaticos() {
        return subtotalViaticos;
    }

    public void setSubtotalViaticos(BigDecimal subtotalViaticos) {
        this.subtotalViaticos = subtotalViaticos;
    }

    public BigDecimal getSubtotalCuidados() {
        return subtotalCuidados;
    }

    public void setSubtotalCuidados(BigDecimal subtotalCuidados) {
        this.subtotalCuidados = subtotalCuidados;
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

    public BigDecimal getTotalRetribucion() {
        return totalRetribucion;
    }

    public void setTotalRetribucion(BigDecimal totalRetribucion) {
        this.totalRetribucion = totalRetribucion;
    }

    public String getExcedenteConcepto() {
        return excedenteConcepto;
    }

    public void setExcedenteConcepto(String excedenteConcepto) {
        this.excedenteConcepto = excedenteConcepto;
    }

    public String getExcedenteDetalle() {
        return excedenteDetalle;
    }

    public void setExcedenteDetalle(String excedenteDetalle) {
        this.excedenteDetalle = excedenteDetalle;
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

    public List<ItemRetencionRecibo> getItemsRetencion() {
        return itemsRetencion;
    }

    public void setItemsRetencion(List<ItemRetencionRecibo> itemsRetencion) {
        this.itemsRetencion = itemsRetencion == null ? new ArrayList<>() : itemsRetencion;
    }

    public BigDecimal getTotalRetenciones() {
        return totalRetenciones;
    }

    public void setTotalRetenciones(BigDecimal totalRetenciones) {
        this.totalRetenciones = totalRetenciones;
    }

    public BigDecimal getRetribucionNeta() {
        return retribucionNeta;
    }

    public void setRetribucionNeta(BigDecimal retribucionNeta) {
        this.retribucionNeta = retribucionNeta;
    }
}


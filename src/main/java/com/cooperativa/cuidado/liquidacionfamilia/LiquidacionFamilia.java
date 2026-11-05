package com.cooperativa.cuidado.liquidacionfamilia;

import com.cooperativa.cuidado.cliente.Cliente;
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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "liquidaciones_familia")
public class LiquidacionFamilia {

    private static final String[] MESES_ES = {
            "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
            "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"
    };

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 150)
    private String nombrePaciente;

    @Column(nullable = false, length = 30)
    private String dniPaciente = "";

    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(nullable = false, length = 120)
    private String ciudad = "";

    @Column(nullable = false, length = 120)
    private String localidad = "";

    @Column(nullable = false, length = 20)
    private String codigoPostal = "";

    @Column(nullable = false, length = 40)
    private String telefonoContacto = "";

    @Column(nullable = false, length = 7)
    private String periodo;

    @Column(nullable = false)
    private LocalDate fechaEmision = LocalDate.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "liquidacion_familia_lineas", joinColumns = @JoinColumn(name = "liquidacion_id"))
    @OrderColumn(name = "orden")
    private List<LineaCuidadorFamilia> lineas = new ArrayList<>();

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal porcentajeIva = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal importeIva = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal importeImpuestos = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalMensual = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoATransferir = BigDecimal.ZERO;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal variacionIpcSalud = BigDecimal.ZERO;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal transportePublico = BigDecimal.ZERO;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal aumentoParitaria = BigDecimal.ZERO;

    public String getPeriodoEtiqueta() {
        if (periodo == null || periodo.isBlank()) {
            return "PERIODO -";
        }
        try {
            YearMonth ym = YearMonth.parse(periodo.trim());
            return "PERIODO " + MESES_ES[ym.getMonthValue() - 1] + " " + ym.getYear();
        } catch (Exception ex) {
            return "PERIODO " + periodo.toUpperCase();
        }
    }

    public String getLineaUbicacion() {
        List<String> partes = new ArrayList<>();
        if (ciudad != null && !ciudad.isBlank()) {
            partes.add(ciudad.trim());
        }
        if (localidad != null && !localidad.isBlank()) {
            partes.add(localidad.trim());
        }
        if (codigoPostal != null && !codigoPostal.isBlank()) {
            partes.add("CP " + codigoPostal.trim());
        }
        return partes.isEmpty() ? "-" : String.join(", ", partes);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
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

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getLocalidad() {
        return localidad;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }

    public String getTelefonoContacto() {
        return telefonoContacto;
    }

    public void setTelefonoContacto(String telefonoContacto) {
        this.telefonoContacto = telefonoContacto;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public List<LineaCuidadorFamilia> getLineas() {
        return lineas;
    }

    public void setLineas(List<LineaCuidadorFamilia> lineas) {
        this.lineas = lineas == null ? new ArrayList<>() : lineas;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getPorcentajeIva() {
        return porcentajeIva;
    }

    public void setPorcentajeIva(BigDecimal porcentajeIva) {
        this.porcentajeIva = porcentajeIva;
    }

    public BigDecimal getImporteIva() {
        return importeIva;
    }

    public void setImporteIva(BigDecimal importeIva) {
        this.importeIva = importeIva;
    }

    public BigDecimal getImporteImpuestos() {
        return importeImpuestos;
    }

    public void setImporteImpuestos(BigDecimal importeImpuestos) {
        this.importeImpuestos = importeImpuestos;
    }

    public BigDecimal getTotalMensual() {
        return totalMensual;
    }

    public void setTotalMensual(BigDecimal totalMensual) {
        this.totalMensual = totalMensual;
    }

    public BigDecimal getSaldoATransferir() {
        return saldoATransferir;
    }

    public void setSaldoATransferir(BigDecimal saldoATransferir) {
        this.saldoATransferir = saldoATransferir;
    }

    public BigDecimal getVariacionIpcSalud() {
        return variacionIpcSalud;
    }

    public void setVariacionIpcSalud(BigDecimal variacionIpcSalud) {
        this.variacionIpcSalud = variacionIpcSalud;
    }

    public BigDecimal getTransportePublico() {
        return transportePublico;
    }

    public void setTransportePublico(BigDecimal transportePublico) {
        this.transportePublico = transportePublico;
    }

    public BigDecimal getAumentoParitaria() {
        return aumentoParitaria;
    }

    public void setAumentoParitaria(BigDecimal aumentoParitaria) {
        this.aumentoParitaria = aumentoParitaria;
    }
}


package com.cooperativa.cuidado.liquidacionfamilia.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class EmisionLiquidacionFamiliaForm {

    private Long clienteId;
    private Integer mes;
    private Integer anio;
    private String periodo;

    private String nombrePaciente;
    private String dniPaciente;
    private String direccion;
    private String ciudad;
    private String localidad;
    private String codigoPostal;
    private String telefonoContacto;

    private List<String> cuidadorNombre = new ArrayList<>();
    private List<BigDecimal> cuidadorHoras = new ArrayList<>();
    private List<BigDecimal> cuidadorValorHora = new ArrayList<>();

    private BigDecimal porcentajeIva;
    private BigDecimal importeImpuestos;

    private BigDecimal variacionIpcSalud;
    private BigDecimal transportePublico;
    private BigDecimal aumentoParitaria;

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
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

    public List<String> getCuidadorNombre() {
        return cuidadorNombre;
    }

    public void setCuidadorNombre(List<String> cuidadorNombre) {
        this.cuidadorNombre = cuidadorNombre == null ? new ArrayList<>() : cuidadorNombre;
    }

    public List<BigDecimal> getCuidadorHoras() {
        return cuidadorHoras;
    }

    public void setCuidadorHoras(List<BigDecimal> cuidadorHoras) {
        this.cuidadorHoras = cuidadorHoras == null ? new ArrayList<>() : cuidadorHoras;
    }

    public List<BigDecimal> getCuidadorValorHora() {
        return cuidadorValorHora;
    }

    public void setCuidadorValorHora(List<BigDecimal> cuidadorValorHora) {
        this.cuidadorValorHora = cuidadorValorHora == null ? new ArrayList<>() : cuidadorValorHora;
    }

    public BigDecimal getPorcentajeIva() {
        return porcentajeIva;
    }

    public void setPorcentajeIva(BigDecimal porcentajeIva) {
        this.porcentajeIva = porcentajeIva;
    }

    public BigDecimal getImporteImpuestos() {
        return importeImpuestos;
    }

    public void setImporteImpuestos(BigDecimal importeImpuestos) {
        this.importeImpuestos = importeImpuestos;
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


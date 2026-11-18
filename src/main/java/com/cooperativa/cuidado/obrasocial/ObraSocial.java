package com.cooperativa.cuidado.obrasocial;

import com.cooperativa.cuidado.facturacion.CondicionIva;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(
        name = "obras_sociales",
        indexes = {
                @Index(name = "idx_obrasocial_nombre", columnList = "nombre"),
                @Index(name = "idx_obrasocial_cuit", columnList = "cuit")
        }
)
public class ObraSocial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la Obra Social / Prepaga no puede estar vacío")
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String nombre;

    @NotBlank(message = "El CUIT de la Obra Social es obligatorio")
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String cuit;

    @Size(max = 200)
    @Column(length = 200)
    private String razonSocial;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicion_iva", length = 30)
    private CondicionIva condicionIva = CondicionIva.IVA_EXENTO;

    @Size(max = 200)
    @Column(length = 200)
    private String domicilioFiscal;

    @Size(max = 60)
    @Column(length = 60)
    private String telefono;

    @Size(max = 120)
    @Column(length = 120)
    private String correo;

    @Size(max = 500)
    @Column(length = 500)
    private String observaciones;

    public ObraSocial() {
    }

    public ObraSocial(String nombre, String cuit, String razonSocial, CondicionIva condicionIva, String domicilioFiscal) {
        this.nombre = nombre;
        this.cuit = cuit;
        this.razonSocial = razonSocial;
        this.condicionIva = condicionIva;
        this.domicilioFiscal = domicilioFiscal;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCuit() {
        return cuit;
    }

    public void setCuit(String cuit) {
        this.cuit = cuit;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public CondicionIva getCondicionIva() {
        return condicionIva != null ? condicionIva : CondicionIva.IVA_EXENTO;
    }

    public void setCondicionIva(CondicionIva condicionIva) {
        this.condicionIva = condicionIva;
    }

    public String getDomicilioFiscal() {
        return domicilioFiscal;
    }

    public void setDomicilioFiscal(String domicilioFiscal) {
        this.domicilioFiscal = domicilioFiscal;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}

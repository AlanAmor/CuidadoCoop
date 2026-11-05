package com.cooperativa.cuidado.cliente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nombreFamiliar;

    @NotBlank
    @Size(min = 7, max = 20)
    @Column(nullable = false, length = 20)
    private String telefono;

    @Email
    @Size(max = 254)
    @Column(length = 254)
    private String correo;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nombrePaciente;

    @Size(max = 20)
    @Column(name = "dni_paciente", length = 20)
    private String dniPaciente;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String domicilio;

    @Size(max = 120)
    @Column(length = 120)
    private String ciudad;

    @Size(max = 120)
    @Column(length = 120)
    private String localidad;

    @Size(max = 20)
    @Column(name = "codigo_postal", length = 20)
    private String codigoPostal;

    @Size(max = 2000)
    @Column(length = 2000)
    private String observacionMedica;

    public Cliente() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreFamiliar() {
        return nombreFamiliar;
    }

    public void setNombreFamiliar(String nombreFamiliar) {
        this.nombreFamiliar = nombreFamiliar;
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

    public String getDomicilio() {
        return domicilio;
    }

    public void setDomicilio(String domicilio) {
        this.domicilio = domicilio;
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

    public String getObservacionMedica() {
        return observacionMedica;
    }

    public void setObservacionMedica(String observacionMedica) {
        this.observacionMedica = observacionMedica;
    }
}

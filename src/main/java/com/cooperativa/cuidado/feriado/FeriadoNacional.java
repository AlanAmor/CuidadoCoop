package com.cooperativa.cuidado.feriado;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Entity
@Table(name = "feriados_nacionales")
public class FeriadoNacional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Seleccioná la fecha del feriado nacional")
    @Column(nullable = false, unique = true)
    private LocalDate fecha;

    @NotBlank(message = "Ingresá el nombre o motivo del feriado nacional")
    @Size(max = 160, message = "La descripción del feriado no puede superar los 160 caracteres")
    @Column(nullable = false, length = 160)
    private String descripcion;

    public FeriadoNacional() {
    }

    public FeriadoNacional(LocalDate fecha, String descripcion) {
        this.fecha = fecha;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}


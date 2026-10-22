package com.cooperativa.cuidado.servicio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Categoría de servicio definida por la cooperativa (por ejemplo "Por horas" o "Kinesiología"). */
@Entity
@Table(name = "tipos_servicio", uniqueConstraints = @UniqueConstraint(name = "uk_tipo_servicio_nombre", columnNames = "nombre"))
public class TipoServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Ingresá el nombre del tipo de servicio")
    @Size(max = 60, message = "El nombre admite hasta 60 caracteres")
    @Column(nullable = false, length = 60)
    private String nombre;

    public TipoServicio() {
    }

    public TipoServicio(String nombre) {
        this.nombre = nombre;
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

    /** Se conserva para las vistas y documentos que muestran la etiqueta del tipo. */
    public String getEtiqueta() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}

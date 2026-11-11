package com.cooperativa.cuidado.cuenta;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "roles_usuario")
public class RolUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Ingresá el nombre del rol")
    @Size(max = 60)
    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    @Column(nullable = false)
    private boolean soloTurnosPropios = false;

    @Column(nullable = false)
    private boolean sistema = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rol_permisos", joinColumns = @JoinColumn(name = "rol_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permiso", nullable = false, length = 60, columnDefinition = "varchar(60)")
    private Set<Permiso> permisos = EnumSet.noneOf(Permiso.class);

    public RolUsuario() {
    }

    public RolUsuario(String nombre, boolean soloTurnosPropios, boolean sistema, Set<Permiso> permisos) {
        this.nombre = nombre;
        this.soloTurnosPropios = soloTurnosPropios;
        this.sistema = sistema;
        setPermisos(permisos);
    }

    public boolean tienePermiso(Permiso permiso) {
        if ("ADMIN".equalsIgnoreCase(nombre)) {
            return true;
        }
        return permisos != null && permisos.contains(permiso);
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

    public boolean isSoloTurnosPropios() {
        return soloTurnosPropios;
    }

    public void setSoloTurnosPropios(boolean soloTurnosPropios) {
        this.soloTurnosPropios = soloTurnosPropios;
    }

    public boolean isSistema() {
        return sistema;
    }

    public void setSistema(boolean sistema) {
        this.sistema = sistema;
    }

    public Set<Permiso> getPermisos() {
        return permisos;
    }

    public void setPermisos(Set<Permiso> nuevos) {
        Set<Permiso> copia = nuevos == null || nuevos.isEmpty()
                ? EnumSet.noneOf(Permiso.class)
                : EnumSet.copyOf(nuevos);
        if (permisos == null) {
            permisos = copia;
            return;
        }
        permisos.clear();
        permisos.addAll(copia);
    }
}


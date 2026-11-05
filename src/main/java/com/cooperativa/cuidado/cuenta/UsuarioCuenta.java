package com.cooperativa.cuidado.cuenta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "usuarios_cuenta")
public class UsuarioCuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "no puede estar vacío")
    @Size(max = 60, message = "no puede superar los 60 caracteres")
    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(nullable = false, length = 150)
    private String passwordHash;

    @NotBlank(message = "no puede estar vacío")
    @Size(max = 80, message = "no puede superar los 80 caracteres")
    @Column(nullable = false, length = 80)
    private String nombre;

    @NotBlank(message = "no puede estar vacío")
    @Size(max = 80, message = "no puede superar los 80 caracteres")
    @Column(nullable = false, length = 80)
    private String apellido;

    @NotBlank(message = "no puede estar vacío")
    @Pattern(regexp = "\\d{7,8}", message = "debe tener 7 u 8 dígitos")
    @Column(nullable = false, unique = true, length = 8)
    private String dni;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private RolUsuario rol;

    public UsuarioCuenta() {
    }

    public boolean tienePermiso(Permiso permiso) {
        return rol != null && rol.tienePermiso(permiso);
    }

    public boolean esAdmin() {
        return rol != null && "ADMIN".equalsIgnoreCase(rol.getNombre());
    }

    public boolean esSoloTurnosPropios() {
        return rol != null && rol.isSoloTurnosPropios() && !tienePermiso(Permiso.VER_CALENDARIO_GLOBAL);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }
}


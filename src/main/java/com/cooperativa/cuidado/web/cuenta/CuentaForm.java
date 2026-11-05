package com.cooperativa.cuidado.web.cuenta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CuentaForm {

    @NotBlank(message = "Ingresá un nombre de usuario")
    @Size(max = 60, message = "El usuario no puede superar los 60 caracteres")
    private String username;

    private String password;

    @NotBlank(message = "Ingresá el nombre")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    private String nombre;

    @NotBlank(message = "Ingresá el apellido")
    @Size(max = 80, message = "El apellido no puede superar los 80 caracteres")
    private String apellido;

    @NotBlank(message = "Ingresá el DNI")
    @Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener 7 u 8 dígitos")
    private String dni;

    @NotNull(message = "Seleccioná un rol")
    private Long rolId;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Long getRolId() {
        return rolId;
    }

    public void setRolId(Long rolId) {
        this.rolId = rolId;
    }
}


package com.cooperativa.cuidado.socio;

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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(
        name = "socios",
        indexes = {
                @Index(name = "idx_socios_apellido_nombre", columnList = "apellido, nombre"),
                @Index(name = "idx_socios_dni", columnList = "dni"),
                @Index(name = "idx_socios_especialidad", columnList = "especialidad"),
                @Index(name = "idx_socios_estado", columnList = "estado")
        }
)
public class Socio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String nombre;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String apellido;

    @NotBlank
    @Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener 7 u 8 dígitos")
    @Column(nullable = false, unique = true, length = 8)
    private String dni;

    @Size(max = 20, message = "El CUIL no puede superar los 20 caracteres")
    @Column(length = 20)
    private String cuil;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String direccion;

    @Size(max = 120, message = "La localidad no puede superar los 120 caracteres")
    @Column(length = 120)
    private String localidad;

    @Size(max = 15, message = "El código postal no puede superar los 15 caracteres")
    @Column(name = "codigo_postal", length = 15)
    private String codigoPostal;

    @NotBlank
    @Pattern(regexp = "[0-9+(). -]{7,20}", message = "El teléfono tiene un formato inválido")
    @Column(nullable = false, length = 20)
    private String telefono;

    @Email
    @Size(max = 254)
    @Column(length = 254)
    private String correo;

    @NotBlank
    @Pattern(regexp = "\\d{22}", message = "El CBU debe contener exactamente 22 dígitos")
    @Column(nullable = false, unique = true, length = 22)
    private String cbu;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSocio estado = EstadoSocio.DISPONIBLE_REEMPLAZO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Especialidad especialidad;

    @NotEmpty(message = "Seleccioná al menos un día de la semana en el que el socio pueda trabajar")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "socio_dias_disponibles", joinColumns = @JoinColumn(name = "socio_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "dia", nullable = false, length = 12)
    private Set<DayOfWeek> diasDisponibles = EnumSet.allOf(DayOfWeek.class);

    public Socio() {
    }

    public Set<DayOfWeek> getDiasDisponibles() {
        return diasDisponibles;
    }

    public void setDiasDisponibles(Set<DayOfWeek> dias) {
        Set<DayOfWeek> copia = dias == null || dias.isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class)
                : EnumSet.copyOf(dias);
        if (diasDisponibles == null) {
            diasDisponibles = copia;
            return;
        }
        diasDisponibles.clear();
        diasDisponibles.addAll(copia);
    }

    public boolean trabajaEl(DayOfWeek dia) {
        return diasDisponibles.contains(dia);
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

    public String getCuil() {
        return cuil;
    }

    public void setCuil(String cuil) {
        this.cuil = cuil;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
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

    public String getCbu() {
        return cbu;
    }

    public void setCbu(String cbu) {
        this.cbu = cbu;
    }

    public EstadoSocio getEstado() {
        return estado;
    }

    public void setEstado(EstadoSocio estado) {
        this.estado = estado;
    }

    public Especialidad getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(Especialidad especialidad) {
        this.especialidad = especialidad;
    }
}

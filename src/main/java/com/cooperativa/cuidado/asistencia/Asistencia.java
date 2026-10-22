package com.cooperativa.cuidado.asistencia;

import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.socio.Socio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "asistencias")
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "socio_id", nullable = false)
    private Socio socio;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_domicilio_id", nullable = false)
    private ServicioDomicilio servicioDomicilio;

    @NotNull
    @Column(nullable = false)
    private LocalDate fecha;

    @NotNull
    @Column(nullable = false)
    private LocalTime horaInicio;

    @NotNull
    @Column(nullable = false)
    private LocalTime horaFin;

    @NotNull
    @PositiveOrZero
    @DecimalMin("0.00")
    @Digits(integer = 3, fraction = 2)
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal horasTotales;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorHoraAplicado;

    @Digits(integer = 10, fraction = 2)
    @Column(precision = 12, scale = 2)
    private BigDecimal valorHoraSocioAplicado;

    public Asistencia() {
    }

    @PrePersist
    @PreUpdate
    public void calcularHorasTotales() {
        capturarValorHora();
        if (horaInicio == null || horaFin == null || horaInicio.equals(horaFin)) {
            horasTotales = null;
            return;
        }

        Duration duracion = Duration.between(horaInicio, horaFin);
        if (duracion.isNegative()) {
            duracion = duracion.plusDays(1);
        }
        horasTotales = BigDecimal.valueOf(duracion.toSeconds())
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
    }

    public void capturarValorHora() {
        if (servicioDomicilio == null) {
            return;
        }
        if (valorHoraAplicado == null) {
            valorHoraAplicado = servicioDomicilio.getValorHora();
        }
        if (valorHoraSocioAplicado == null) {
            valorHoraSocioAplicado = servicioDomicilio.getValorHoraSocio();
        }
    }

    /** Valor hora que se le paga al socio; en turnos anteriores a esta separación coincide con el cobrado. */
    public BigDecimal getValorHoraPagoSocio() {
        return valorHoraSocioAplicado != null ? valorHoraSocioAplicado : valorHoraAplicado;
    }

    public BigDecimal getValorHoraSocioAplicado() {
        return valorHoraSocioAplicado;
    }

    public void setValorHoraSocioAplicado(BigDecimal valorHoraSocioAplicado) {
        this.valorHoraSocioAplicado = valorHoraSocioAplicado;
    }

    @AssertTrue(message = "La hora de fin debe ser distinta de la hora de inicio")
    public boolean isRangoHorarioValido() {
        return horaInicio == null || horaFin == null || !horaInicio.equals(horaFin);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Socio getSocio() {
        return socio;
    }

    public void setSocio(Socio socio) {
        this.socio = socio;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public ServicioDomicilio getServicioDomicilio() {
        return servicioDomicilio;
    }

    public void setServicioDomicilio(ServicioDomicilio servicioDomicilio) {
        this.servicioDomicilio = servicioDomicilio;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public BigDecimal getHorasTotales() {
        return horasTotales;
    }

    public void setHorasTotales(BigDecimal horasTotales) {
        this.horasTotales = horasTotales;
    }

    public BigDecimal getValorHoraAplicado() {
        return valorHoraAplicado;
    }

    public void setValorHoraAplicado(BigDecimal valorHoraAplicado) {
        this.valorHoraAplicado = valorHoraAplicado;
    }
}

package com.cooperativa.cuidado.web.asignacion;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AsignacionForm {

    @NotNull(message = "Seleccioná un socio disponible")
    private Long socioId;

    @NotNull(message = "Seleccioná un cliente")
    private Long clienteId;

    @NotNull(message = "Seleccioná un servicio")
    private Long servicioDomicilioId;

    @NotNull(message = "Ingresá la fecha del turno")
    private LocalDate fecha;

    @NotNull(message = "Ingresá la hora de inicio")
    private LocalTime horaInicio;

    @NotNull(message = "Ingresá la hora de fin")
    private LocalTime horaFin;

    private boolean repetir;

    private UnidadDuracion unidad = UnidadDuracion.SEMANAS;

    private Integer cantidad = 4;

    private List<DayOfWeek> dias = new ArrayList<>();

    public boolean isRepetir() {
        return repetir;
    }

    public void setRepetir(boolean repetir) {
        this.repetir = repetir;
    }

    public UnidadDuracion getUnidad() {
        return unidad;
    }

    public void setUnidad(UnidadDuracion unidad) {
        this.unidad = unidad;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public List<DayOfWeek> getDias() {
        return dias;
    }

    public void setDias(List<DayOfWeek> dias) {
        this.dias = dias == null ? new ArrayList<>() : dias;
    }

    public Long getSocioId() {
        return socioId;
    }

    public void setSocioId(Long socioId) {
        this.socioId = socioId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getServicioDomicilioId() {
        return servicioDomicilioId;
    }

    public void setServicioDomicilioId(Long servicioDomicilioId) {
        this.servicioDomicilioId = servicioDomicilioId;
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
}
